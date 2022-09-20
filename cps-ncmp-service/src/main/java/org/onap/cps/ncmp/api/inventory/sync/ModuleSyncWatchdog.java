/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.inventory.sync;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.sync.executor.AsyncTaskExecutor;
import org.onap.cps.spi.model.DataNode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class ModuleSyncWatchdog {

    private final SyncUtils syncUtils;
    private final BlockingQueue<DataNode> moduleSyncWorkQueue;
    private final Map<String, Object> moduleSyncStartedOnCmHandles;
    private final ModuleSyncTasks moduleSyncTasks;
    private final AsyncTaskExecutor asyncTaskExecutor;
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
        log.info("Processing module sync watchdog waking up.");
        populateWorkQueueIfNeeded();
        final int asyncTaskParallelismLevel = asyncTaskExecutor.getAsyncTaskParallelismLevel();
        while (!moduleSyncWorkQueue.isEmpty()) {
            if (batchCounter.get() <= asyncTaskParallelismLevel) {
                final Collection<DataNode> nextBatch = prepareNextBatch();
                log.debug("Processing module sync batch of {}. {} batch(es) active.",
                        nextBatch.size(), batchCounter.get());
                asyncTaskExecutor.executeTask(() ->
                                moduleSyncTasks.performModuleSync(nextBatch, batchCounter),
                        ASYNC_TASK_TIMEOUT_IN_MILLISECONDS
                );
                batchCounter.getAndIncrement();
            } else {
                preventBusyWait();
            }
        }
    }

    /**
     * Find any failed (locked) cm handles and change state back to 'ADVISED'.
     */
    @Scheduled(fixedDelayString = "${ncmp.timers.locked-modules-sync.sleep-time-ms:300000}")
    public void resetPreviouslyFailedCmHandles() {
        log.info("Processing module sync retry-watchdog waking up.");
        final List<YangModelCmHandle> failedCmHandles = syncUtils.getModuleSyncFailedCmHandles();
        moduleSyncTasks.resetFailedCmHandles(failedCmHandles);
    }

    private void preventBusyWait() {
        try {
            TimeUnit.MILLISECONDS.sleep(PREVENT_CPU_BURN_WAIT_TIME_MILLIS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void populateWorkQueueIfNeeded() {
        if (moduleSyncWorkQueue.isEmpty()) {
            final List<DataNode> advisedCmHandles = syncUtils.getAdvisedCmHandles();
            log.info("Processing module sync fetched {} advised cm handles from DB", advisedCmHandles.size());
            for (final DataNode advisedCmHandle : advisedCmHandles) {
                if (!moduleSyncWorkQueue.offer(advisedCmHandle)) {
                    log.warn("Unable to add cm handle {} to the work queue", advisedCmHandle.getLeaves().get("id"));
                }
            }
        }
    }

    private Collection<DataNode> prepareNextBatch() {
        final Collection<DataNode> nextBatchCandidates = new HashSet<>(MODULE_SYNC_BATCH_SIZE);
        final Collection<DataNode> nextBatch = new HashSet<>(MODULE_SYNC_BATCH_SIZE);
        moduleSyncWorkQueue.drainTo(nextBatchCandidates, MODULE_SYNC_BATCH_SIZE);
        log.debug("nextBatchCandidates size : {}", nextBatchCandidates.size());
        for (final DataNode batchCandidate : nextBatchCandidates) {
            final String cmHandleId = String.valueOf(batchCandidate.getLeaves().get("id"));
            final boolean alreadyAddedToInProgressMap = VALUE_FOR_HAZELCAST_IN_PROGRESS_MAP
                .equals(moduleSyncStartedOnCmHandles.putIfAbsent(cmHandleId, VALUE_FOR_HAZELCAST_IN_PROGRESS_MAP));
            if (alreadyAddedToInProgressMap) {
                log.debug("module sync for {} already in progress by other instance", cmHandleId);
            } else {
                nextBatch.add(batchCandidate);
            }
        }
        log.debug("nextBatch size : {}", nextBatch.size());
        return nextBatch;
    }

}
