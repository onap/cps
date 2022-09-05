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
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.sync.executor.AsyncTaskExecutor;
import org.onap.cps.spi.model.DataNode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j @RequiredArgsConstructor
@Service
public class ModuleSyncWatchdog {
    private final SyncUtils syncUtils;
    private final BlockingQueue<DataNode> moduleSyncWorkQueue;
    private final Map<String, String> moduleSyncStartedOnCmHandles;
    private final AsyncTaskExecutor asyncTaskExecutor;
    private final ModuleSyncTask moduleSyncTask;
    private static final int SYNC_BATCH_SIZE = 100;
    private static final long ASYNC_TASK_TIMEOUT_IN_MILLISECONDS = TimeUnit.MINUTES.toMillis(5);
    private static final int PREVENT_CPU_BURN_WAIT_TIME_MILLIS = 10;

    @Getter
    private AtomicInteger batchCounter = new AtomicInteger(0);

    /**
     * Execute Cm Handle poll which changes the cm handle state from 'ADVISED' to 'READY'.
     */
    @Scheduled(fixedDelayString = "${timers.advised-modules-sync.sleep-time-ms:5000}")
    public void moduleSyncAdvisedCmHandles() {
        populateWorkQueueIfNeeded();
        while (true) {
            while (moduleSyncWorkQueue.size() > 0 &&
                    batchCounter.getAndIncrement() <= asyncTaskExecutor.getAsyncTaskParallelismLevel()) {
                Collection<DataNode> nextBatch = prepareNextBatch();
                asyncTaskExecutor.executeTask(() ->
                                moduleSyncTask.performModuleSync(nextBatch, batchCounter),
                        ASYNC_TASK_TIMEOUT_IN_MILLISECONDS
                );
            }

            if (moduleSyncWorkQueue.isEmpty()) {
                if (moduleSyncWorkQueue.isEmpty()) {
                    break;
                } else {
                    waitALittle();
                }
            }
        }
    }

    private void waitALittle() {
        try {
            Thread.sleep(PREVENT_CPU_BURN_WAIT_TIME_MILLIS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void populateWorkQueueIfNeeded() {
        if (moduleSyncWorkQueue.isEmpty()) {
            final List<DataNode> advisedDataNodes = syncUtils.getAdvisedCmHandles();
            for (DataNode advisedCmHandle : advisedDataNodes) {
                if (!moduleSyncWorkQueue.offer(advisedCmHandle)) {
                    log.warn("Unable to add cm handle {} to the work queue", advisedCmHandle.getLeaves().get("id"));
                }
            }
        }
    }


    /**
     * Execute Cm Handle poll which changes the cm handle state from 'LOCKED' to 'ADVISED'.
     */
    @Scheduled(fixedDelayString = "${timers.locked-modules-sync.sleep-time-ms:300000}")
    public void executeLockedCmHandlePoll() {
        final List<YangModelCmHandle> lockedCmHandles = syncUtils.getModuleSyncFailedCmHandles();
        asyncTaskExecutor.executeTask(() ->
                moduleSyncTask.resynchronizeLockedCmHandles(lockedCmHandles), ASYNC_TASK_TIMEOUT_IN_MILLISECONDS
        );
    }

    private Collection<DataNode> prepareNextBatch() {
        final Collection<DataNode> nextBatchCandidates = new HashSet(SYNC_BATCH_SIZE);
        final Collection<DataNode> nextBatch = new HashSet(SYNC_BATCH_SIZE);
        moduleSyncWorkQueue.drainTo(nextBatchCandidates, SYNC_BATCH_SIZE);
        log.info("nextBatchCandidates size : {}", nextBatchCandidates.size());
        for (DataNode dataNode : nextBatchCandidates) {
            String cmHandleId = String.valueOf(dataNode.getLeaves().get("id"));
            boolean alreadyInProgress = moduleSyncStartedOnCmHandles.putIfAbsent(cmHandleId, "Started") != null;
            if (alreadyInProgress) {
                log.info("module sync for {} already in progress by other instance", cmHandleId);
            } else {
                nextBatch.add(dataNode);
            }
        }
        log.info("nextBatch size : {}", nextBatch.size());
        return nextBatch;
    }

}
