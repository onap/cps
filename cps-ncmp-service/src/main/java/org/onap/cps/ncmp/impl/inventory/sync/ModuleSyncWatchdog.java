/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.spi.model.DataNode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class ModuleSyncWatchdog {

    private final ModuleOperationsUtils moduleOperationsUtils;
    private final BlockingQueue<DataNode> moduleSyncWorkQueue;
    private final IMap<String, Object> moduleSyncStartedOnCmHandles;
    private final ModuleSyncTasks moduleSyncTasks;
    private final AsyncTaskExecutor asyncTaskExecutor;
    private final Lock workQueueLock;

    private static final int MODULE_SYNC_BATCH_SIZE = 100;
    private static final long PREVENT_CPU_BURN_WAIT_TIME_MILLIS = 10;
    private static final String VALUE_FOR_HAZELCAST_IN_PROGRESS_MAP = "Started";
    private static final long ASYNC_TASK_TIMEOUT_IN_MILLISECONDS = TimeUnit.MINUTES.toMillis(5);
    @Getter
    private AtomicInteger batchCounter = new AtomicInteger(1);

    /**
     * Check DB for any cm handles in 'ADVISED' state.
     * Queue and create batches to process them asynchronously.
     * This method will only finish when there are no more 'ADVISED' cm handles in the DB.
     * This method wil be triggered on a configurable interval
     */
    @Scheduled(fixedDelayString = "${ncmp.timers.advised-modules-sync.sleep-time-ms:5000}")
    public void moduleSyncAdvisedCmHandles() {
        log.debug("Processing module sync watchdog waking up.");
        populateWorkQueueIfNeeded();
        while (!moduleSyncWorkQueue.isEmpty()) {
            if (batchCounter.get() <= asyncTaskExecutor.getAsyncTaskParallelismLevel()) {
                final Collection<DataNode> nextBatch = prepareNextBatch();
                log.info("Processing module sync batch of {}. {} batch(es) active.",
                    nextBatch.size(), batchCounter.get());
                if (!nextBatch.isEmpty()) {
                    asyncTaskExecutor.executeTask(() ->
                            moduleSyncTasks.performModuleSync(nextBatch, batchCounter),
                        ASYNC_TASK_TIMEOUT_IN_MILLISECONDS);
                    batchCounter.getAndIncrement();
                }
            } else {
                preventBusyWait();
            }
        }
    }

    private void preventBusyWait() {
        try {
            log.debug("Busy waiting now");
            TimeUnit.MILLISECONDS.sleep(PREVENT_CPU_BURN_WAIT_TIME_MILLIS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void populateWorkQueueIfNeeded() {
        if (moduleSyncWorkQueue.isEmpty() && workQueueLock.tryLock()) {
            try {
                populateWorkQueue();
                if (moduleSyncWorkQueue.isEmpty()) {
                    setPreviouslyLockedCmHandlesToAdvised();
                }
            } finally {
                workQueueLock.unlock();
            }
        }
    }

    private void populateWorkQueue() {
        final Collection<DataNode> advisedCmHandles = moduleOperationsUtils.getAdvisedCmHandles();
        if (advisedCmHandles.isEmpty()) {
            log.debug("No advised CM handles found in DB.");
        } else {
            log.info("Fetched {} advised CM handles from DB. Adding them to the work queue.", advisedCmHandles.size());
            advisedCmHandles.forEach(advisedCmHandle -> {
                final String cmHandleId = String.valueOf(advisedCmHandle.getLeaves().get("id"));
                if (moduleSyncWorkQueue.offer(advisedCmHandle)) {
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

    private Collection<DataNode> prepareNextBatch() {
        final Collection<DataNode> nextBatchCandidates = new HashSet<>(MODULE_SYNC_BATCH_SIZE);
        final Collection<DataNode> nextBatch = new HashSet<>(MODULE_SYNC_BATCH_SIZE);
        moduleSyncWorkQueue.drainTo(nextBatchCandidates, MODULE_SYNC_BATCH_SIZE);
        log.info("nextBatchCandidates size : {}", nextBatchCandidates.size());
        for (final DataNode batchCandidate : nextBatchCandidates) {
            final String cmHandleId = String.valueOf(batchCandidate.getLeaves().get("id"));
            final boolean alreadyAddedToInProgressMap = VALUE_FOR_HAZELCAST_IN_PROGRESS_MAP.equals(
                    moduleSyncStartedOnCmHandles.putIfAbsent(cmHandleId, VALUE_FOR_HAZELCAST_IN_PROGRESS_MAP,
                            SynchronizationCacheConfig.MODULE_SYNC_STARTED_TTL_SECS, TimeUnit.SECONDS));
            if (alreadyAddedToInProgressMap) {
                log.info("module sync for {} already in progress by other instance", cmHandleId);
            } else {
                log.info("Adding cmHandle : {} to current batch", cmHandleId);
                nextBatch.add(batchCandidate);
            }
        }
        log.info("nextBatch size : {}", nextBatch.size());
        return nextBatch;
    }
}
