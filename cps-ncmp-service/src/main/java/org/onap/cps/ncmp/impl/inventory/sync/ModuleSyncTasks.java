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
import java.util.stream.Collectors;
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
    private static final CmHandleState NO_STATE_CHANGE = null;

    /**
     * Perform module sync on a batch of cm handles.
     *
     * @param cmHandleIds                  a batch of cm handle ids to perform module sync on
     */
    public void performModuleSync(final Collection<String> cmHandleIds) {
        final Map<YangModelCmHandle, CmHandleState> cmHandleStatePerCmHandle = HashMap.newHashMap(cmHandleIds.size());
        for (final String cmHandleId : cmHandleIds) {
            try {
                final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
                if (isCmHandleInAdvisedState(yangModelCmHandle)) {
                    final CmHandleState newCmHandleState = processCmHandle(yangModelCmHandle);
                    if (newCmHandleState == NO_STATE_CHANGE) {
                        log.info("Skipping state change for CM handle '{}' as it was already synced by another "
                                + "instance", cmHandleId);
                    } else {
                        cmHandleStatePerCmHandle.put(yangModelCmHandle, newCmHandleState);
                    }
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
        persistCmHandleStateBatch(cmHandleStatePerCmHandle);
    }

    private void persistCmHandleStateBatch(final Map<YangModelCmHandle, CmHandleState> cmHandleStatePerCmHandle) {
        log.info("Persisting state for {} cm handles", cmHandleStatePerCmHandle.size());
        try {
            lcmEventsCmHandleStateHandler.updateCmHandleStateBatch(cmHandleStatePerCmHandle);
        } catch (final RuntimeException runtimeException) {
            log.warn("Failed to persist state / emit LCM events for batch of {} cm handles due to: {}. "
                    + "Affected cm handles: {}", cmHandleStatePerCmHandle.size(),
                    runtimeException.getMessage(), cmHandleIds(cmHandleStatePerCmHandle.keySet()));
            throw runtimeException;
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
            final Map<YangModelCmHandle, CmHandleState> cmHandleStatePerCmHandle = HashMap.newHashMap(batch.size());
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
        final boolean inRefresh = ModuleOperationsUtils.inModuleRefreshOrRefreshFailed(compositeState);
        final boolean inUpgrade = ModuleOperationsUtils.inUpgradeOrUpgradeFailed(compositeState);
        try {
            if (inRefresh) {
                moduleSyncService.refreshModuleContent(yangModelCmHandle);
            } else if (inUpgrade) {
                moduleSyncService.syncAndUpgradeSchemaSet(yangModelCmHandle);
            } else {
                moduleSyncService.syncAndCreateSchemaSetAndAnchor(yangModelCmHandle);
            }
            compositeState.setLockReason(null);
            return CmHandleState.READY;
        } catch (final Exception e) {
            log.warn("Module sync failed for CM handle '{}': {}", yangModelCmHandle.getId(), e.getMessage());
            final LockReasonCategory lockReasonCategory;
            if (inRefresh) {
                lockReasonCategory = LockReasonCategory.MODULE_REFRESH_FAILED;
            } else if (inUpgrade) {
                lockReasonCategory = LockReasonCategory.MODULE_UPGRADE_FAILED;
            } else if (isNoLongerLockable(yangModelCmHandle.getId())) {
                return NO_STATE_CHANGE;
            } else {
                lockReasonCategory = LockReasonCategory.MODULE_SYNC_FAILED;
            }
            moduleOperationsUtils.updateLockReasonWithAttempts(compositeState, lockReasonCategory, e.getMessage());
            return CmHandleState.LOCKED;
        }
    }

    /**
     * Determine whether a CM handle should NOT be locked after a failed initial module sync (create path only).
     * This is the case when another instance already synced it to READY (so locking would overwrite a good state
     * and emit a wrong event), or when the CM handle no longer exists (it was deleted during sync). It is only
     * consulted for the initial sync; upgrade and refresh operate on an existing handle deliberately reprocessed on
     * this instance and must always lock on failure so their MODULE_UPGRADE_FAILED / MODULE_REFRESH_FAILED reason
     * is recorded.
     *
     * @param cmHandleId the CM handle id
     * @return true if the CM handle must not be locked
     */
    private boolean isNoLongerLockable(final String cmHandleId) {
        try {
            if (inventoryPersistence.getYangModelCmHandle(cmHandleId).getCompositeState().getCmHandleState()
                    == CmHandleState.READY) {
                log.info("CM handle '{}' is READY (synced by another instance); lock/retry not required", cmHandleId);
                return true;
            }
            return false;
        } catch (final DataNodeNotFoundException dataNodeNotFoundException) {
            log.info("CM handle '{}' no longer exists; lock/retry not applicable", cmHandleId);
            return true;
        }
    }

    private void removeResetCmHandleFromModuleSyncMap(final String resetCmHandleId) {
        moduleSyncStartedOnCmHandles.delete(resetCmHandleId);
        log.debug("{} removed from in progress map", resetCmHandleId);
    }

    private static boolean isCmHandleInAdvisedState(final YangModelCmHandle yangModelCmHandle) {
        return yangModelCmHandle.getCompositeState().getCmHandleState() == CmHandleState.ADVISED;
    }

    private static String cmHandleIds(final Collection<YangModelCmHandle> yangModelCmHandles) {
        return yangModelCmHandles.stream().map(YangModelCmHandle::getId).collect(Collectors.joining(","));
    }

}
