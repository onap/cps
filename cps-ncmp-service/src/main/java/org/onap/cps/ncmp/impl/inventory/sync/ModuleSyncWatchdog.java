/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2026 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2022 Bell Canada
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.inventory.sync;

import com.hazelcast.map.IMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.init.actuator.ReadinessManager;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class ModuleSyncWatchdog {

    private final ModuleOperationsUtils moduleOperationsUtils;
    private final BlockingQueue<String> moduleSyncWorkQueue;
    private final IMap<String, Object> moduleSyncStartedOnCmHandles;
    private final ModuleSyncTasks moduleSyncTasks;
    @Qualifier("cpsCommonLocks") private final IMap<String, String> cpsCommonLocks;
    private final ReadinessManager readinessManager;

    @Value("${ncmp.timers.advised-modules-sync.master-only:false}")
    private boolean masterOnlyModuleSync;

    private static final int MODULE_SYNC_BATCH_SIZE = 300;
    private static final String VALUE_FOR_HAZELCAST_IN_PROGRESS_MAP = "Started";
    private static final String MODULE_SYNC_WORK_QUEUE_COMMON_LOCK_NAME = "workQueueLock";
    private static final String MODULE_SYNC_MASTER_LOCK_NAME = "moduleSyncMasterLock";

    // PoC (CPS-3437): when master-only module sync is enabled, once this instance has become the module-sync master
    // it stays master for its lifetime.
    private volatile boolean isThisInstanceModuleSyncMaster = false;

    /**
     * Check DB for any cm handles in 'ADVISED' state.
     * Queue and create batches to process them asynchronously.
     * This method will only finish when there are no more 'ADVISED' cm handles in the DB.
     * This method is triggered on a configurable interval (ncmp.timers.advised-modules-sync.sleep-time-ms) and when the
     * system is in the ready state.
     */
    @Scheduled(fixedDelayString = "${ncmp.timers.advised-modules-sync.sleep-time-ms:5000}")
    public void scheduledModuleSyncAdvisedCmHandles() {
        if (!readinessManager.isReady()) {
            log.info("System is not ready yet");
            return;
        }
        if (!isModuleSyncEnabledOnThisInstance()) {
            log.debug("This instance is not the module-sync master; skipping module sync");
            return;
        }
        moduleSyncAdvisedCmHandles();
    }

    /**
     * Determine whether this instance should run module sync (PoC, CPS-3437).
     * When master-only module sync is disabled (default) every instance runs module sync, as before. When it is
     * enabled only the master instance runs module sync, so there is no concurrency on the same cm handle. Master
     * election reuses the shared cpsCommonLocks map: the first instance to acquire the module-sync master lock
     * becomes master and stays master for its lifetime. A non-master re-attempts the lock every cycle.
     *
     * <p>Re-election needs no extra code and no lock TTL/lease. The lock is a Hazelcast IMap key lock, which
     * Hazelcast releases automatically when the owning member leaves the cluster. So if the master pod dies (or is
     * restarted by Kubernetes after a failed liveness probe, e.g. because it hung) it leaves the cluster, the lock
     * is freed, and a surviving instance acquires it on its next {@code tryLock} attempt. Do NOT add a time-based
     * lease to "hand over" the lock: a lease would expire on a healthy but idle master (it only re-checks the lock
     * once per interval, it does not renew it), letting a second instance also become master and reintroducing the
     * concurrent-sync race this design removes.
     *
     * @return true if this instance should run module sync
     */
    private boolean isModuleSyncEnabledOnThisInstance() {
        if (!masterOnlyModuleSync) {
            return true;
        }
        if (!isThisInstanceModuleSyncMaster) {
            isThisInstanceModuleSyncMaster = cpsCommonLocks.tryLock(MODULE_SYNC_MASTER_LOCK_NAME);
            if (isThisInstanceModuleSyncMaster) {
                log.info("This instance is now the module-sync master");
            }
        }
        return isThisInstanceModuleSyncMaster;
    }

    /**
     * This method is used when we dont want the scheduled behaviour.
     * Mainly used in the integration testware.
     */
    public void moduleSyncAdvisedCmHandles() {
        log.debug("Processing module sync watchdog waking up.");
        populateWorkQueueIfNeeded();
        while (!moduleSyncWorkQueue.isEmpty()) {
            final Collection<String> nextBatch = prepareNextBatch();
            if (!nextBatch.isEmpty()) {
                log.info("Processing module sync batch of {}. 1 batch(es) active.", nextBatch.size());
                moduleSyncTasks.performModuleSync(nextBatch);
                log.info("Processing module sync batch finished. 0 batch(es) active.");
            }
        }
    }

    /**
     * Populate module sync work queue with advised cm handles from db.
     * This method is made public for (integration) testing purposes.
     * So it can be tested without the queue being emptied immediately as the main public method does.
     */
    public void populateWorkQueueIfNeeded() {
        if (masterOnlyModuleSync) {
            // Single master: no other instance populates the (local) work queue, so the distributed workQueueLock
            // is not needed. A local boolean check replaces a distributed lock attempt (a network round-trip).
            if (moduleSyncWorkQueue.isEmpty()) {
                setPreviouslyLockedCmHandlesToAdvised();
                populateWorkQueue();
            }
            return;
        }
        if (moduleSyncWorkQueue.isEmpty() && cpsCommonLocks.tryLock(MODULE_SYNC_WORK_QUEUE_COMMON_LOCK_NAME)) {
            log.debug("Lock acquired by thread : {}", Thread.currentThread().getName());
            try {
                setPreviouslyLockedCmHandlesToAdvised();
                populateWorkQueue();
            } finally {
                cpsCommonLocks.unlock(MODULE_SYNC_WORK_QUEUE_COMMON_LOCK_NAME);
                log.debug("Lock released by thread : {}", Thread.currentThread().getName());
            }
        }
    }

    private void populateWorkQueue() {
        final Collection<String> advisedCmHandleIds = moduleOperationsUtils.getAdvisedCmHandleIds();
        if (advisedCmHandleIds.isEmpty()) {
            log.debug("No advised CM handles found in DB.");
        } else {
            log.info("Fetched {} advised CM handles from DB. Adding them to the work queue.",
                    advisedCmHandleIds.size());
            advisedCmHandleIds.forEach(cmHandleId -> {
                if (moduleSyncWorkQueue.offer(cmHandleId)) {
                    log.debug("CM handle {} added to the work queue.", cmHandleId);
                } else {
                    log.warn("Failed to add CM handle {} to the work queue.", cmHandleId);
                }
            });
            log.info("Work queue contains {} items.", moduleSyncWorkQueue.size());
        }
    }

    private void setPreviouslyLockedCmHandlesToAdvised() {
        final Collection<YangModelCmHandle> lockedCmHandles
                = moduleOperationsUtils.getCmHandlesThatFailedModelSyncOrUpgrade();
        if (lockedCmHandles.isEmpty()) {
            log.debug("No locked CM handles found in DB.");
        } else {
            log.info("Found {} Locked CM Handles. Changing state to Advise to retry syncing them again.",
                    lockedCmHandles.size());
            moduleSyncTasks.setCmHandlesToAdvised(lockedCmHandles);
        }
    }

    private Collection<String> prepareNextBatch() {
        final Collection<String> nextBatchCandidates = HashSet.newHashSet(MODULE_SYNC_BATCH_SIZE);
        final Collection<String> nextBatch = HashSet.newHashSet(MODULE_SYNC_BATCH_SIZE);
        moduleSyncWorkQueue.drainTo(nextBatchCandidates, MODULE_SYNC_BATCH_SIZE);
        log.info("nextBatchCandidates size : {}", nextBatchCandidates.size());
        if (masterOnlyModuleSync) {
            // Single-instance sync: no other instance can be processing these cm handles, so the cross-instance
            // in-progress map (moduleSyncStartedOnCmHandles) serves no purpose and is not populated, avoiding two
            // distributed map operations per cm handle.
            nextBatch.addAll(nextBatchCandidates);
            log.info("nextBatch size : {}", nextBatch.size());
            return nextBatch;
        }
        int skippedCount = 0;
        for (final String cmHandleId : nextBatchCandidates) {
            final boolean alreadyAddedToInProgressMap = VALUE_FOR_HAZELCAST_IN_PROGRESS_MAP.equals(
                    moduleSyncStartedOnCmHandles.putIfAbsent(cmHandleId, VALUE_FOR_HAZELCAST_IN_PROGRESS_MAP));
            if (alreadyAddedToInProgressMap) {
                if (skippedCount == 0) {
                    log.warn("module sync for {} already in progress by other instance", cmHandleId);
                    log.warn("Similar warnings for other cm handles in this batch not logged");
                }
                skippedCount++;
            } else {
                log.debug("Adding cmHandle : {} to current batch", cmHandleId);
                nextBatch.add(cmHandleId);
            }
        }
        if (skippedCount > 0) {
            log.warn("{} of {} candidates already in progress by other instance. In-progress map size: {}",
                    skippedCount, nextBatchCandidates.size(), moduleSyncStartedOnCmHandles.size());
        }
        log.info("nextBatch size : {}", nextBatch.size());
        return nextBatch;
    }
}
