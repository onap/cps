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

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.event.lcm.LcmEventsCmHandleStateHandler;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CmHandleState;
import org.onap.cps.ncmp.api.inventory.CompositeState;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.inventory.LockReasonCategory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class ModuleSyncWatchdog {

    private final InventoryPersistence inventoryPersistence;

    private final SyncUtils syncUtils;

    private final ModuleSyncService moduleSyncService;

    private final ConcurrentMap<String, Boolean> moduleSyncSemaphoreMap;

    private final LcmEventsCmHandleStateHandler lcmEventsCmHandleStateHandler;

    /**
     * Execute Cm Handle poll which changes the cm handle state from 'ADVISED' to 'READY'.
     */
    @Scheduled(fixedDelayString = "${timers.advised-modules-sync.sleep-time-ms:30000}")
    public void executeAdvisedCmHandlePoll() {
        syncUtils.getAdvisedCmHandles().forEach(advisedCmHandle -> {
            final String cmHandleId = advisedCmHandle.getId();
            if (hasPushedIntoSemaphoreMap(cmHandleId)) {
                log.debug("executing module sync on {}", cmHandleId);
                final CompositeState compositeState = inventoryPersistence.getCmHandleState(cmHandleId);
                try {
                    moduleSyncService.deleteSchemaSetIfExists(advisedCmHandle);
                    moduleSyncService.syncAndCreateSchemaSetAndAnchor(advisedCmHandle);
                    lcmEventsCmHandleStateHandler.updateCmHandleState(advisedCmHandle, CmHandleState.READY);
                    updateModuleSyncSemaphoreMap(cmHandleId);
                } catch (final Exception e) {
                    syncUtils.updateLockReasonDetailsAndAttempts(compositeState,
                            LockReasonCategory.LOCKED_MODULE_SYNC_FAILED, e.getMessage());
                    setCmHandleStateLocked(compositeState, advisedCmHandle);
                }
                log.debug("{} is now in {} state", cmHandleId, compositeState.getCmHandleState().name());
            } else {
                log.debug("{} already processed by another instance", cmHandleId);
            }
        });
        log.debug("No Cm-Handles currently found in an ADVISED state");
    }

    /**
     * Execute Cm Handle poll which changes the cm handle state from 'LOCKED' to 'ADVISED'.
     */
    @Scheduled(fixedDelayString = "${timers.locked-modules-sync.sleep-time-ms:300000}")
    public void executeLockedCmHandlePoll() {
        final List<YangModelCmHandle> lockedCmHandles = syncUtils.getModuleSyncFailedCmHandles();
        for (final YangModelCmHandle lockedCmHandle : lockedCmHandles) {
            final CompositeState compositeState = lockedCmHandle.getCompositeState();
            final boolean isReadyForRetry = syncUtils.isReadyForRetry(compositeState);
            if (isReadyForRetry) {
                log.debug("Reset cm handle {} state to ADVISED to re-attempt module-sync", lockedCmHandle.getId());
                lcmEventsCmHandleStateHandler.updateCmHandleState(lockedCmHandle, CmHandleState.ADVISED);
            }
        }
    }

    private void setCmHandleStateLocked(final CompositeState compositeState, final YangModelCmHandle advisedCmHandle) {
        advisedCmHandle.getCompositeState().setLockReason(compositeState.getLockReason());
        lcmEventsCmHandleStateHandler.updateCmHandleState(advisedCmHandle, CmHandleState.LOCKED);
    }

    private void updateModuleSyncSemaphoreMap(final String cmHandleId) {
        moduleSyncSemaphoreMap.replace(cmHandleId, true);
    }

    private boolean hasPushedIntoSemaphoreMap(final String cmHandleId) {
        return moduleSyncSemaphoreMap.putIfAbsent(cmHandleId, false) == null;
    }
}
