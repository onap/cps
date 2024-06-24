/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.events.lcm.LcmEventsCmHandleStateHandler;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState;
import org.onap.cps.ncmp.impl.inventory.models.LockReasonCategory;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.utils.YangDataConverter;
import org.onap.cps.spi.model.DataNode;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class ModuleSyncTasks {
    private final InventoryPersistence inventoryPersistence;
    private final ModuleOperationsUtils moduleOperationsUtils;
    private final ModuleSyncService moduleSyncService;
    private final LcmEventsCmHandleStateHandler lcmEventsCmHandleStateHandler;
    private final IMap<String, Object> moduleSyncStartedOnCmHandles;

    /**
     * Perform module sync on a batch of cm handles.
     *
     * @param cmHandlesAsDataNodes         a batch of Data nodes representing cm handles to perform module sync on
     * @param batchCounter                 the number of batches currently being processed, will be decreased when
     *                                     task is finished or fails
     * @return completed future to handle post-processing
     */
    public CompletableFuture<Void> performModuleSync(final Collection<DataNode> cmHandlesAsDataNodes,
                                                     final AtomicInteger batchCounter) {
        try {
            final Map<YangModelCmHandle, CmHandleState> cmHandelStatePerCmHandle
                    = new HashMap<>(cmHandlesAsDataNodes.size());
            for (final DataNode cmHandleAsDataNode : cmHandlesAsDataNodes) {
                final String cmHandleId = String.valueOf(cmHandleAsDataNode.getLeaves().get("id"));
                final YangModelCmHandle yangModelCmHandle =
                        YangDataConverter.convertCmHandleToYangModel(cmHandleAsDataNode);
                final CompositeState compositeState = inventoryPersistence.getCmHandleState(cmHandleId);
                final boolean inUpgrade = ModuleOperationsUtils.inUpgradeOrUpgradeFailed(compositeState);
                try {
                    if (inUpgrade) {
                        moduleSyncService.syncAndUpgradeSchemaSet(yangModelCmHandle);
                    } else {
                        moduleSyncService.deleteSchemaSetIfExists(cmHandleId);
                        moduleSyncService.syncAndCreateSchemaSetAndAnchor(yangModelCmHandle);
                    }
                    yangModelCmHandle.getCompositeState().setLockReason(null);
                    cmHandelStatePerCmHandle.put(yangModelCmHandle, CmHandleState.READY);
                } catch (final Exception e) {
                    log.warn("Processing of {} module failed due to reason {}.", cmHandleId, e.getMessage());
                    final LockReasonCategory lockReasonCategory = inUpgrade ? LockReasonCategory.MODULE_UPGRADE_FAILED
                            : LockReasonCategory.MODULE_SYNC_FAILED;
                    moduleOperationsUtils.updateLockReasonDetailsAndAttempts(compositeState,
                            lockReasonCategory, e.getMessage());
                    setCmHandleStateLocked(yangModelCmHandle, compositeState.getLockReason());
                    cmHandelStatePerCmHandle.put(yangModelCmHandle, CmHandleState.LOCKED);
                }
                log.info("{} is now in {} state", cmHandleId, cmHandelStatePerCmHandle.get(yangModelCmHandle).name());
            }
            lcmEventsCmHandleStateHandler.updateCmHandleStateBatch(cmHandelStatePerCmHandle);
        } finally {
            batchCounter.getAndDecrement();
            log.info("Processing module sync batch finished. {} batch(es) active.", batchCounter.get());
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Reset state to "ADVISED" for any previously failed cm handles.
     *
     * @param failedCmHandles previously failed (locked) cm handles
     */
    public void resetFailedCmHandles(final List<YangModelCmHandle> failedCmHandles) {
        final Map<YangModelCmHandle, CmHandleState> cmHandleStatePerCmHandle = new HashMap<>(failedCmHandles.size());
        for (final YangModelCmHandle failedCmHandle : failedCmHandles) {
            final CompositeState compositeState = failedCmHandle.getCompositeState();
            final boolean isReadyForRetry = moduleOperationsUtils.needsModuleSyncRetryOrUpgrade(compositeState);
            log.info("Retry for cmHandleId : {} is {}", failedCmHandle.getId(), isReadyForRetry);
            if (isReadyForRetry) {
                final String resetCmHandleId = failedCmHandle.getId();
                log.debug("Reset cm handle {} state to ADVISED to be re-attempted by module-sync watchdog",
                        resetCmHandleId);
                cmHandleStatePerCmHandle.put(failedCmHandle, CmHandleState.ADVISED);
                removeResetCmHandleFromModuleSyncMap(resetCmHandleId);
            }
        }
        lcmEventsCmHandleStateHandler.updateCmHandleStateBatch(cmHandleStatePerCmHandle);
    }

    private void setCmHandleStateLocked(final YangModelCmHandle advisedCmHandle,
                                        final CompositeState.LockReason lockReason) {
        advisedCmHandle.getCompositeState().setLockReason(lockReason);
    }

    private void removeResetCmHandleFromModuleSyncMap(final String resetCmHandleId) {
        if (moduleSyncStartedOnCmHandles.remove(resetCmHandleId) != null) {
            log.info("{} removed from in progress map", resetCmHandleId);
        }
    }
}
