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

import static java.util.concurrent.TimeUnit.SECONDS;

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

    // Cross-instance coordination (distributed work-queue lock + in-progress map + distributed work queue) is only
    // needed when every instance runs module sync (the default). A client can set the
    // cross-instance-coordination-enabled property to false to run module sync on a single master instance only
    // (CPS-3437); the redundant distributed operations are then skipped.
    @Value("${ncmp.timers.advised-modules-sync.cross-instance-coordination-enabled:true}")
    private boolean crossInstanceCoordinationEnabled = true;

    private static final int MODULE_SYNC_BATCH_SIZE = 300;
    private static final String VALUE_FOR_HAZELCAST_IN_PROGRESS_MAP = "Started";
    private static final String MODULE_SYNC_WORK_QUEUE_COMMON_LOCK_NAME = "workQueueLock";
    private static final String MODULE_SYNC_MASTER_LOCK_NAME = "moduleSyncMasterLock";

    // The master lock is acquired with a lease (TTL) and renewed by the master on every cycle. The lease must be
    // comfortably longer than the gap between renewals so a healthy master never loses it, while still being short
    // enough that a surviving instance takes over promptly if the master dies without leaving the cluster cleanly.
    // Note the scheduler uses fixedDelay, so the renewal gap is the sleep interval (default 5s) plus the duration of
    // a sync cycle; 25s leaves headroom for that on top of the default interval.
    private static final long MASTER_LOCK_LEASE_SECONDS = 25;

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
     * Determine whether this instance should run module sync (CPS-3437).
     * When cross-instance coordination is enabled (default) every instance runs module sync, as before. When it is
     * disabled only the master instance runs module sync, so there is no concurrency on the same cm handle.
     *
     * <p>Master election uses a single key in the shared cpsCommonLocks map, acquired with a lease (TTL) of
     * {@value #MASTER_LOCK_LEASE_SECONDS} seconds. The current master renews the lease every cycle, so while it is
     * healthy the lock never expires and no other instance can acquire it. If the master dies or hangs, it stops
     * renewing; the lease then expires and the next instance to run its cycle acquires the lock and becomes master.
     *
     * <p>The lease is what makes take-over robust: it does not rely solely on Hazelcast releasing the lock when a
     * member leaves the cluster, so it also covers a master that is still a cluster member but no longer making
     * progress (e.g. hung before Kubernetes restarts it). The lease must stay comfortably larger than the sync
     * interval so a healthy master always renews in time; see {@link #MASTER_LOCK_LEASE_SECONDS}.
     *
     * @return true if this instance should run module sync
     */
    private boolean isModuleSyncEnabledOnThisInstance() {
        if (crossInstanceCoordinationEnabled) {
            return true;
        }
        // tryLock is re-entrant: calling it again while already holding the lock renews the lease. So a single call
        // both acquires the lock (for a non-master) and renews it (for the current master).
        try {
            isThisInstanceModuleSyncMaster = cpsCommonLocks.tryLock(MODULE_SYNC_MASTER_LOCK_NAME, 0, SECONDS,
                    MASTER_LOCK_LEASE_SECONDS, SECONDS);
        } catch (final InterruptedException e) {
            // Restore the interrupt status and skip module sync this cycle; the next cycle will retry the lock.
            Thread.currentThread().interrupt();
            isThisInstanceModuleSyncMaster = false;
            log.warn("Interrupted while acquiring the module-sync master lock; skipping module sync this cycle");
        }
        if (isThisInstanceModuleSyncMaster) {
            log.debug("This instance holds the module-sync master lock");
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
        if (!moduleSyncWorkQueue.isEmpty()) {
            return;
        }
        if (!crossInstanceCoordinationEnabled) {
            // Single master: no other instance populates the (local) work queue, so the distributed workQueueLock
            // is not needed. The local isEmpty() check above replaces a distributed lock attempt.
            refreshWorkQueue();
            return;
        }
        if (cpsCommonLocks.tryLock(MODULE_SYNC_WORK_QUEUE_COMMON_LOCK_NAME)) {
            log.debug("Lock acquired by thread : {}", Thread.currentThread().getName());
            try {
                refreshWorkQueue();
            } finally {
                cpsCommonLocks.unlock(MODULE_SYNC_WORK_QUEUE_COMMON_LOCK_NAME);
                log.debug("Lock released by thread : {}", Thread.currentThread().getName());
            }
        }
    }

    private void refreshWorkQueue() {
        setPreviouslyLockedCmHandlesToAdvised();
        populateWorkQueue();
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
        moduleSyncWorkQueue.drainTo(nextBatchCandidates, MODULE_SYNC_BATCH_SIZE);
        log.info("nextBatchCandidates size : {}", nextBatchCandidates.size());
        final Collection<String> nextBatch = crossInstanceCoordinationEnabled
                ? candidatesNotAlreadyInProgressOnOtherInstance(nextBatchCandidates)
                : nextBatchCandidates;
        log.info("nextBatch size : {}", nextBatch.size());
        return nextBatch;
    }

    // Filters out cm handles another instance has already started syncing, using the distributed in-progress map
    // (moduleSyncStartedOnCmHandles) as the cross-instance guard: putIfAbsent claims a cm handle, and a non-null
    // result means another instance claimed it first so it is skipped. Only needed when cross-instance coordination
    // is enabled; when it is disabled a single master instance owns all cm handles and this guard is bypassed.
    private Collection<String> candidatesNotAlreadyInProgressOnOtherInstance(
            final Collection<String> nextBatchCandidates) {
        final Collection<String> nextBatch = HashSet.newHashSet(MODULE_SYNC_BATCH_SIZE);
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
        return nextBatch;
    }
}
