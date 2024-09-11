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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState;
import org.onap.cps.ncmp.impl.inventory.models.LockReasonCategory;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventsCmHandleStateHandler;
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

    private static final Map<LockReasonCategory, String> LOCK_REASON_MESSAGES;

    static {
        LOCK_REASON_MESSAGES = new EnumMap<>(LockReasonCategory.class);
        LOCK_REASON_MESSAGES.put(LockReasonCategory.MODULE_UPGRADE, "CM handle locked for module upgrade.");
        LOCK_REASON_MESSAGES.put(LockReasonCategory.MODULE_SYNC_FAILED,
                "CM handle is locked due to synchronization failure.");
        LOCK_REASON_MESSAGES.put(LockReasonCategory.MODULE_UPGRADE_FAILED,
                "CM handle is locked due to module upgrade failure.");
    }

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
                final YangModelCmHandle yangModelCmHandle = YangDataConverter.toYangModelCmHandle(cmHandleAsDataNode);
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
                    moduleOperationsUtils.updateLockReasonWithAttempts(compositeState,
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
     * Resets the state of failed CM handles and updates their status to ADVISED for retry.

     * This method processes a collection of failed CM handles, logs their lock reason, and resets their state
     * to ADVISED. Once reset, it updates the CM handle states in a batch to allow for re-attempt by the module-sync
     * watchdog.
     *
     * @param failedCmHandles a collection of CM handles that have failed and need their state reset
     */
    public void resetFailedCmHandles(final Collection<YangModelCmHandle> failedCmHandles) {
        final Map<YangModelCmHandle, CmHandleState> cmHandleStatePerCmHandle = new HashMap<>(failedCmHandles.size());
        for (final YangModelCmHandle failedCmHandle : failedCmHandles) {
            final CompositeState compositeState = failedCmHandle.getCompositeState();
            logCmHandleLockReason(compositeState);
            final String resetCmHandleId = failedCmHandle.getId();
            log.info("Resetting CM handle {} state to ADVISED for retry by the module-sync watchdog.",
                    failedCmHandle.getId());
            cmHandleStatePerCmHandle.put(failedCmHandle, CmHandleState.ADVISED);
            removeResetCmHandleFromModuleSyncMap(resetCmHandleId);
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

    private void logCmHandleLockReason(final CompositeState compositeState) {
        final CompositeState.LockReason lockReason = compositeState.getLockReason();
        final LockReasonCategory lockReasonCategory = lockReason.getLockReasonCategory();
        final String lockReasonMessage = LOCK_REASON_MESSAGES.get(lockReasonCategory);
        log.info(lockReasonMessage);
    }
}
