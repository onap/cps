/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.onap.cps.ncmp.impl.cache.CpsAndNcmpLockConfig.MODULE_SYNC_WORK_QUEUE_LOCK_NAME;

import com.hazelcast.map.IMap;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.springframework.beans.factory.annotation.Qualifier;
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
    @Qualifier("cpsAndNcmpLock")
    private final IMap<String, String> cpsAndNcmpLock;

    private static final int MODULE_SYNC_BATCH_SIZE = 300;
    private static final String VALUE_FOR_HAZELCAST_IN_PROGRESS_MAP = "Started";

    /**
     * Check DB for any cm handles in 'ADVISED' state.
     * Queue and create batches to process them asynchronously.
     * This method will only finish when there are no more 'ADVISED' cm handles in the DB.
     * This method is triggered on a configurable interval (ncmp.timers.advised-modules-sync.sleep-time-ms)
     */
    @Scheduled(initialDelayString = "${ncmp.timers.advised-modules-sync.initial-delay-ms:40000}",
               fixedDelayString = "${ncmp.timers.advised-modules-sync.sleep-time-ms:5000}")
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
     * Populate work queue with advised cm handles from db.
     * This method is made public for (integration) testing purposes.
     * So it can be tested without the queue being emptied immediately as the main public method does.
     */
    public void populateWorkQueueIfNeeded() {
        if (moduleSyncWorkQueue.isEmpty() && cpsAndNcmpLock.tryLock(MODULE_SYNC_WORK_QUEUE_LOCK_NAME)) {
            log.debug("Lock acquired by thread : {}", Thread.currentThread().getName());
            try {
                populateWorkQueue();
                if (moduleSyncWorkQueue.isEmpty()) {
                    setPreviouslyLockedCmHandlesToAdvised();
                }
            } finally {
                cpsAndNcmpLock.unlock(MODULE_SYNC_WORK_QUEUE_LOCK_NAME);
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
                    log.info("CM handle {} added to the work queue.", cmHandleId);
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
        final Collection<String> nextBatchCandidates = new HashSet<>(MODULE_SYNC_BATCH_SIZE);
        final Collection<String> nextBatch = new HashSet<>(MODULE_SYNC_BATCH_SIZE);
        moduleSyncWorkQueue.drainTo(nextBatchCandidates, MODULE_SYNC_BATCH_SIZE);
        log.info("nextBatchCandidates size : {}", nextBatchCandidates.size());
        for (final String cmHandleId : nextBatchCandidates) {
            final boolean alreadyAddedToInProgressMap = VALUE_FOR_HAZELCAST_IN_PROGRESS_MAP.equals(
                    moduleSyncStartedOnCmHandles.putIfAbsent(cmHandleId, VALUE_FOR_HAZELCAST_IN_PROGRESS_MAP));
            if (alreadyAddedToInProgressMap) {
                log.info("module sync for {} already in progress by other instance", cmHandleId);
            } else {
                log.info("Adding cmHandle : {} to current batch", cmHandleId);
                nextBatch.add(cmHandleId);
            }
        }
        log.info("nextBatch size : {}", nextBatch.size());
        return nextBatch;
    }
}
