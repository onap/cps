/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2026 OpenInfra Foundation Europe. All rights reserved.
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.exceptions.DataNodeNotFoundException;
import org.onap.cps.ncmp.api.inventory.models.CmHandleState;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.api.inventory.models.LockReasonCategory;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventsCmHandleStateHandler;
import org.slf4j.event.Level;
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

    private static final int RESET_BATCH_SIZE = 300;

    /**
     * Perform module sync on a batch of cm handles.
     *
     * @param cmHandleIds                  a batch of cm handle ids to perform module sync on
     */
    public void performModuleSync(final Collection<String> cmHandleIds) {
        final Map<YangModelCmHandle, CmHandleState> cmHandleStatePerCmHandle = new HashMap<>(cmHandleIds.size());
        try {
            for (final String cmHandleId : cmHandleIds) {
                try {
                    final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
                    if (isCmHandleInAdvisedState(yangModelCmHandle)) {
                        final CmHandleState newCmHandleState = processCmHandle(yangModelCmHandle);
                        cmHandleStatePerCmHandle.put(yangModelCmHandle, newCmHandleState);
                    } else {
                        log.warn("Skipping module sync for CM handle '{}' as it is in {} state", cmHandleId,
                                yangModelCmHandle.getCompositeState().getCmHandleState().name());
                    }
                } catch (final DataNodeNotFoundException dataNodeNotFoundException) {
                    log.warn("Skipping module sync for CM handle '{}' as it does not exist", cmHandleId);
                } finally {
                    moduleSyncStartedOnCmHandles.delete(cmHandleId);
                }
            }
        } finally {
            log.warn("Persisting state for {} cm handles", cmHandleStatePerCmHandle.size());
            lcmEventsCmHandleStateHandler.updateCmHandleStateBatch(cmHandleStatePerCmHandle);
        }
    }

    /**
     * Set the state of CM handles to ADVISED.
     * This method processes a collection of CM handles, logs their lock reason, and resets their state
     * to ADVISED. Once reset, it updates the CM handle states in a batch to allow for re-attempt by the module-sync
     * watchdog. Processing is done in sub-batches to avoid holding database connections for too long.
     *
     * @param yangModelCmHandles a collection of CM handles that needs their state reset
     */
    public void setCmHandlesToAdvised(final Collection<YangModelCmHandle> yangModelCmHandles) {
        final List<YangModelCmHandle> cmHandlesList = new ArrayList<>(yangModelCmHandles);
        boolean firstHandle = true;
        for (int batchStart = 0; batchStart < cmHandlesList.size(); batchStart += RESET_BATCH_SIZE) {
            final int batchEnd = Math.min(batchStart + RESET_BATCH_SIZE, cmHandlesList.size());
            final List<YangModelCmHandle> batch = cmHandlesList.subList(batchStart, batchEnd);
            final Map<YangModelCmHandle, CmHandleState> cmHandleStatePerCmHandle = new HashMap<>(batch.size());
            for (final YangModelCmHandle yangModelCmHandle : batch) {
                final CompositeState compositeState = yangModelCmHandle.getCompositeState();
                final String message = "Resetting CM handle {} state to ADVISED for retry. Total: {}, lock reason: {}"
                        + (firstHandle ? " (subsequent similar warnings are logged at DEBUG level)" : "");
                log.atLevel(firstHandle ? Level.WARN : Level.DEBUG)
                    .log(message, yangModelCmHandle.getId(), cmHandlesList.size(),
                        compositeState.getLockReason().getLockReasonCategory().name());
                firstHandle = false;
                cmHandleStatePerCmHandle.put(yangModelCmHandle, CmHandleState.ADVISED);
                removeResetCmHandleFromModuleSyncMap(yangModelCmHandle.getId());
            }
            lcmEventsCmHandleStateHandler.updateCmHandleStateBatch(cmHandleStatePerCmHandle);
        }
    }

    private CmHandleState processCmHandle(final YangModelCmHandle yangModelCmHandle) {
        final CompositeState compositeState = yangModelCmHandle.getCompositeState();
        final boolean inUpgrade = ModuleOperationsUtils.inUpgradeOrUpgradeFailed(compositeState);
        try {
            if (inUpgrade) {
                moduleSyncService.syncAndUpgradeSchemaSet(yangModelCmHandle);
            } else {
                moduleSyncService.syncAndCreateSchemaSetAndAnchor(yangModelCmHandle);
            }
            compositeState.setLockReason(null);
            return CmHandleState.READY;
        } catch (final Exception e) {
            log.warn("Module sync failed for CM handle '{}': {}", yangModelCmHandle.getId(), e.getMessage());
            final LockReasonCategory lockReasonCategory = inUpgrade
                    ? LockReasonCategory.MODULE_UPGRADE_FAILED
                    : LockReasonCategory.MODULE_SYNC_FAILED;
            moduleOperationsUtils.updateLockReasonWithAttempts(compositeState, lockReasonCategory, e.getMessage());
            return CmHandleState.LOCKED;
        }
    }

    private void removeResetCmHandleFromModuleSyncMap(final String resetCmHandleId) {
        moduleSyncStartedOnCmHandles.delete(resetCmHandleId);
        log.debug("{} removed from in progress map", resetCmHandleId);
    }

    private static boolean isCmHandleInAdvisedState(final YangModelCmHandle yangModelCmHandle) {
        return yangModelCmHandle.getCompositeState().getCmHandleState() == CmHandleState.ADVISED;
    }
}
