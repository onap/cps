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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState;
import org.onap.cps.ncmp.impl.inventory.models.LockReasonCategory;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventsCmHandleStateHandler;
import org.onap.cps.ncmp.impl.utils.YangDataConverter;
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
        final Map<YangModelCmHandle, CmHandleState> cmHandleStatePerCmHandle =
            new HashMap<>(cmHandlesAsDataNodes.size());
        try {
            cmHandlesAsDataNodes.forEach(cmHandleAsDataNode -> {
                final YangModelCmHandle yangModelCmHandle = YangDataConverter.toYangModelCmHandle(cmHandleAsDataNode);
                final CmHandleState cmHandleState = processCmHandle(yangModelCmHandle);
                cmHandleStatePerCmHandle.put(yangModelCmHandle, cmHandleState);
            });
        } finally {
            batchCounter.getAndDecrement();
            lcmEventsCmHandleStateHandler.updateCmHandleStateBatch(cmHandleStatePerCmHandle);
            log.info("Processing module sync batch finished. {} batch(es) active.", batchCounter.get());
        }
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Set the state of CM handles to ADVISED.
     * This method processes a collection of CM handles, logs their lock reason, and resets their state
     * to ADVISED. Once reset, it updates the CM handle states in a batch to allow for re-attempt by the module-sync
     * watchdog.
     *
     * @param yangModelCmHandles a collection of CM handles that needs their state reset
     */
    public void setCmHandlesToAdvised(final Collection<YangModelCmHandle> yangModelCmHandles) {
        final Map<YangModelCmHandle, CmHandleState> cmHandleStatePerCmHandle = new HashMap<>(yangModelCmHandles.size());
        for (final YangModelCmHandle yangModelCmHandle : yangModelCmHandles) {
            final CompositeState compositeState = yangModelCmHandle.getCompositeState();
            final String resetCmHandleId = yangModelCmHandle.getId();
            log.debug("Resetting CM handle {} state to ADVISED for retry by the module-sync watchdog. Lock reason: {}",
                yangModelCmHandle.getId(), compositeState.getLockReason().getLockReasonCategory().name());
            cmHandleStatePerCmHandle.put(yangModelCmHandle, CmHandleState.ADVISED);
            removeResetCmHandleFromModuleSyncMap(resetCmHandleId);
        }
        lcmEventsCmHandleStateHandler.updateCmHandleStateBatch(cmHandleStatePerCmHandle);
    }

    private CmHandleState processCmHandle(final YangModelCmHandle yangModelCmHandle) {
        final CompositeState compositeState = inventoryPersistence.getCmHandleState(yangModelCmHandle.getId());
        final boolean inUpgrade = ModuleOperationsUtils.inUpgradeOrUpgradeFailed(compositeState);
        try {
            if (inUpgrade) {
                moduleSyncService.syncAndUpgradeSchemaSet(yangModelCmHandle);
            } else {
                moduleSyncService.deleteSchemaSetIfExists(yangModelCmHandle.getId());
                moduleSyncService.syncAndCreateSchemaSetAndAnchor(yangModelCmHandle);
            }
            yangModelCmHandle.getCompositeState().setLockReason(null);
            return CmHandleState.READY;
        } catch (final Exception e) {
            log.warn("Processing of {} module failed due to reason {}.", yangModelCmHandle.getId(), e.getMessage());
            final LockReasonCategory lockReasonCategory = inUpgrade ? LockReasonCategory.MODULE_UPGRADE_FAILED
                : LockReasonCategory.MODULE_SYNC_FAILED;
            moduleOperationsUtils.updateLockReasonWithAttempts(compositeState,
                lockReasonCategory, e.getMessage());
            setCmHandleStateLocked(yangModelCmHandle, compositeState.getLockReason());
            return CmHandleState.LOCKED;
        }
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
