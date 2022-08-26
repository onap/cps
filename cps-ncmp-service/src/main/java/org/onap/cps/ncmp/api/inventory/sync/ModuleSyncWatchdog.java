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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.spi.model.DataNode;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ModuleSyncWatchdog {

    private final SyncUtils syncUtils;
    private final BlockingQueue<DataNode> moduleSyncWorkQueue;
    private final Map<String, String> moduleSyncStartedOnCmHandles;
    private final ModuleSyncTasks moduleSyncTasks;

    private static final int MODULE_SYNC_BATCH_SIZE = 100;
    private static final int PREVENT_CPU_BURN_WAIT_TIME_MILLIS = 10;
    private static final String VALUE_FOR_HAZELCAST_IN_PROGRESS_MAP = "Started";

    /**
     * Execute Cm Handle poll which changes the cm handle state from 'ADVISED' to 'READY'.
     */
    @Scheduled(fixedDelayString = "${timers.advised-modules-sync.sleep-time-ms:5000}")
    public void moduleSyncAdvisedCmHandles() {
        populateWorkQueueIfNeeded();
        while (moduleSyncWorkQueue.size() > 0) {
            final Collection<DataNode> nextBatch = prepareNextBatch();
            moduleSyncTasks.performModuleSync(nextBatch);
            waitALittle();
        }
    }

    /**
     * Find any failed (locked) cm handles and change state back to 'ADVISED'.
     */
    @Scheduled(fixedDelayString = "${timers.locked-modules-sync.sleep-time-ms:300000}")
    public void executeLockedCmHandlePoll() {
        final List<YangModelCmHandle> failedCmHandles = syncUtils.getModuleSyncFailedCmHandles();
        moduleSyncTasks.resetFailedCmHandles(failedCmHandles);
    }

    private void waitALittle() {
        // This method isn't really needed until CPS-1200 Performance Improvement: Watchdog Parallel execution
        // but leaving here to minimize impacts on this class for that Jira
        try {
            Thread.sleep(PREVENT_CPU_BURN_WAIT_TIME_MILLIS);
        } catch (final InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void populateWorkQueueIfNeeded() {
        if (moduleSyncWorkQueue.isEmpty()) {
            final List<DataNode> advisedDataNodes = syncUtils.getAdvisedCmHandles();
            for (final DataNode advisedCmHandle : advisedDataNodes) {
                if (!moduleSyncWorkQueue.offer(advisedCmHandle)) {
                    log.warn("Unable to add cm handle {} to the work queue", advisedCmHandle.getLeaves().get("id"));
                }
            }
        }
    }

    private Collection<DataNode> prepareNextBatch() {
        final Collection<DataNode> nextBatchCandidates = new HashSet(MODULE_SYNC_BATCH_SIZE);
        final Collection<DataNode> nextBatch = new HashSet(MODULE_SYNC_BATCH_SIZE);
        moduleSyncWorkQueue.drainTo(nextBatchCandidates, MODULE_SYNC_BATCH_SIZE);
        log.info("nextBatchCandidates size : {}", nextBatchCandidates.size());
        for (final DataNode batchCandidate : nextBatchCandidates) {
            final String cmHandleId = String.valueOf(batchCandidate.getLeaves().get("id"));
            final boolean alreadyAddedToInProgressMap = VALUE_FOR_HAZELCAST_IN_PROGRESS_MAP
                .equals(moduleSyncStartedOnCmHandles.putIfAbsent(cmHandleId, VALUE_FOR_HAZELCAST_IN_PROGRESS_MAP));
            if (alreadyAddedToInProgressMap) {
                log.info("module sync for {} already in progress by other instance", cmHandleId);
            } else {
                nextBatch.add(batchCandidate);
            }
        }
        log.info("nextBatch size : {}", nextBatch.size());
        return nextBatch;
    }

}
