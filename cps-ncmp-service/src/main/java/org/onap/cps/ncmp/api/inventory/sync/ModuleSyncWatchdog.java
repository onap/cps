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

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CmHandleState;
import org.onap.cps.ncmp.api.inventory.CompositeState;
import org.onap.cps.ncmp.api.inventory.DataStoreSyncState;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.inventory.LockReasonCategory;
import org.onap.cps.ncmp.api.inventory.sync.executor.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ModuleSyncWatchdog {

    private static final int ADVISED_CM_HANDLE_BATCH_SIZE = 100;
    private static final int ASYNC_TASK_TIMEOUT_IN_MILLISECONDS = 300000;
    private final InventoryPersistence inventoryPersistence;

    private final SyncUtils syncUtils;

    private final ModuleSyncService moduleSyncService;

    private final ConcurrentMap<YangModelCmHandle, Boolean> moduleSyncSemaphoreMap;

    private final AsyncTaskExecutor asyncTaskExecutor;

    /**
     * Execute Cm Handle poll which changes the cm handle state from 'ADVISED' to 'READY'.
     */
    @Scheduled(fixedDelayString = "${timers.advised-modules-sync.sleep-time-ms:30000}")
    public void executeAdvisedCmHandlePoll() {
        List<YangModelCmHandle> unProcessedYangModelCmHandles = getUnprocessedCmHandles();
        while (!unProcessedYangModelCmHandles.isEmpty()) {
            final List<YangModelCmHandle> yangModelCmHandles = unProcessedYangModelCmHandles;
            log.info("Prepared cm handle(s) batch of size {}  for processing", unProcessedYangModelCmHandles.size());
            asyncTaskExecutor.executeTask(() ->
                    performModuleStateTransition(yangModelCmHandles), ASYNC_TASK_TIMEOUT_IN_MILLISECONDS
            );
            unProcessedYangModelCmHandles = getUnprocessedCmHandles();
        }
        log.debug("No Cm-Handles currently found in an ADVISED state");
    }

    private boolean performModuleStateTransition(final List<YangModelCmHandle> yangModelCmHandles) {
        log.info("Started Module sync watchdog batch of size {} into thread: {}", yangModelCmHandles.size(),
                Thread.currentThread().getName());
        final Map<String, CompositeState> cmHandleStates = new HashMap<>();
        yangModelCmHandles.parallelStream().forEach(advisedCmHandle -> {
            final Instant startTime = Instant.now();
            log.info("Started processing cm handle with id: {} into thread: {}", advisedCmHandle.getId(),
                    Thread.currentThread().getName());
            final String cmHandleId = advisedCmHandle.getId();
            final CompositeState compositeState = inventoryPersistence.getCmHandleState(cmHandleId);
            try {
                moduleSyncService.deleteSchemaSetIfExists(advisedCmHandle);
                moduleSyncService.syncAndCreateSchemaSetAndAnchor(advisedCmHandle);
                setCompositeStateToReadyWithInitialDataStoreSyncState().accept(compositeState);
            } catch (final Exception e) {
                setCompositeStateToLocked().accept(compositeState);
                syncUtils.updateLockReasonDetailsAndAttempts(compositeState,
                        LockReasonCategory.LOCKED_MODULE_SYNC_FAILED, e.getMessage());
            }
            cmHandleStates.put(cmHandleId, compositeState);
            final Instant endTime = Instant.now();
            log.info("Finished processing of cm handle with id: {} in {} ms into thread: {}", cmHandleId,
                    Duration.between(startTime, endTime).toMillis(), Thread.currentThread().getName());
            log.debug("{} is now in {} state", cmHandleId, compositeState.getCmHandleState().name());
        });
        inventoryPersistence.saveCmHandleStates(cmHandleStates);
        removeFromModuleSyncSemaphoreMap(cmHandleStates.keySet());
        log.info("Finished Module sync watchdog batch of size {} into thread: {}", yangModelCmHandles.size(),
                Thread.currentThread().getName());
        return true;
    }

    /**
     * Execute Cm Handle poll which changes the cm handle state from 'LOCKED' to 'ADVISED'.
     */
    @Scheduled(fixedDelayString = "${timers.locked-modules-sync.sleep-time-ms:300000}")
    public void executeLockedCmHandlePoll() {
        final List<YangModelCmHandle> lockedCmHandles = syncUtils.getModuleSyncFailedCmHandles();
        asyncTaskExecutor.executeTask(() ->
                resynchronizeLockedCmHandles(lockedCmHandles), ASYNC_TASK_TIMEOUT_IN_MILLISECONDS
        );
    }

    private String resynchronizeLockedCmHandles(final List<YangModelCmHandle> lockedCmHandles) {
        lockedCmHandles.parallelStream().forEach(lockedCmHandle -> {
            final CompositeState compositeState = lockedCmHandle.getCompositeState();
            final boolean isReadyForRetry = syncUtils.isReadyForRetry(compositeState);
            if (isReadyForRetry) {
                setCompositeStateToAdvisedAndRetainOldLockReasonDetails(compositeState);
                log.debug("Reset cm handle {} state to ADVISED to be re-attempted by module-sync watchdog",
                        lockedCmHandle.getId());
                inventoryPersistence.saveCmHandleState(lockedCmHandle.getId(), compositeState);
            }
        });
        return "Locked cm handle(s) of size " + lockedCmHandles.size() + " is(are) moved to ADVISED state";
    }

    private Consumer<CompositeState> setCompositeStateToLocked() {
        return compositeState -> {
            compositeState.setCmHandleState(CmHandleState.LOCKED);
            compositeState.setLastUpdateTimeNow();
        };
    }

    private Consumer<CompositeState> setCompositeStateToReadyWithInitialDataStoreSyncState() {
        return compositeState -> {
            compositeState.setDataSyncEnabled(false);
            compositeState.setLastUpdateTimeNow();
            compositeState.setCmHandleState(CmHandleState.READY);
            final CompositeState.Operational operational = getDataStoreSyncState();
            final CompositeState.DataStores dataStores = CompositeState.DataStores.builder()
                    .operationalDataStore(operational)
                    .build();
            compositeState.setDataStores(dataStores);
        };
    }

    private void setCompositeStateToAdvisedAndRetainOldLockReasonDetails(final CompositeState compositeState) {
        compositeState.setCmHandleState(CmHandleState.ADVISED);
        compositeState.setLastUpdateTimeNow();
        final String oldLockReasonDetails = compositeState.getLockReason().getDetails();
        final CompositeState.LockReason lockReason = CompositeState.LockReason.builder()
                .details(oldLockReasonDetails).build();
        compositeState.setLockReason(lockReason);
    }

    private CompositeState.Operational getDataStoreSyncState() {
        final DataStoreSyncState dataStoreSyncState = DataStoreSyncState.NONE_REQUESTED;
        return CompositeState.Operational.builder().dataStoreSyncState(dataStoreSyncState).build();
    }

    private void updateModuleSyncSemaphoreMap(final YangModelCmHandle yangModelCmHandle) {
        moduleSyncSemaphoreMap.replace(yangModelCmHandle, true);
    }

    private void removeFromModuleSyncSemaphoreMap(final Set<String> yangModelCmHandleIds) {
        final List<YangModelCmHandle> yangModelCmHandles = moduleSyncSemaphoreMap.keySet().stream()
                .filter(yangModelCmHandle -> yangModelCmHandleIds.contains(yangModelCmHandle.getId()))
                .collect(Collectors.toList());
        yangModelCmHandles.stream().forEach(yangModelCmHandle -> moduleSyncSemaphoreMap.remove(yangModelCmHandle));
    }

    private List<YangModelCmHandle> getUnprocessedCmHandles() {
        if (moduleSyncSemaphoreMap.isEmpty()) {
            syncUtils.getAdvisedCmHandles(moduleSyncSemaphoreMap);
        }
        final List<YangModelCmHandle> yangModelCmHandles = moduleSyncSemaphoreMap.keySet().stream()
                .filter(cmHandle -> cmHandle != null
                        && !moduleSyncSemaphoreMap.get(cmHandle))
                .limit(ADVISED_CM_HANDLE_BATCH_SIZE)
                .collect(Collectors.toList());
        markTrueAsBeingPickedByWatchdog().accept(yangModelCmHandles);
        return yangModelCmHandles;
    }

    private Consumer<List<YangModelCmHandle>> markTrueAsBeingPickedByWatchdog() {
        return yangModelCmHandles -> yangModelCmHandles.parallelStream().forEach(this::updateModuleSyncSemaphoreMap);
    }
}
