/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

import com.hazelcast.map.IMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.event.lcm.LcmEventsCmHandleStateHandler;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CmHandleState;
import org.onap.cps.ncmp.api.inventory.CompositeState;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.inventory.LockReasonCategory;
import org.onap.cps.spi.model.DataNode;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class ModuleSyncTasks {
    private final InventoryPersistence inventoryPersistence;
    private final SyncUtils syncUtils;
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
            final Map<YangModelCmHandle, CmHandleState> cmHandelStatePerCmHandle = new HashMap<>();
            for (final DataNode cmHandleAsDataNode : cmHandlesAsDataNodes) {
                final String cmHandleId = String.valueOf(cmHandleAsDataNode.getLeaves().get("id"));
                final YangModelCmHandle yangModelCmHandle =
                        YangDataConverter.convertCmHandleToYangModel(cmHandleAsDataNode, cmHandleId);
                final CompositeState compositeState = inventoryPersistence.getCmHandleState(cmHandleId);
                try {
                    moduleSyncService.deleteSchemaSetIfExists(cmHandleId);
                    moduleSyncService.syncAndCreateSchemaSetAndAnchor(yangModelCmHandle);
                    cmHandelStatePerCmHandle.put(yangModelCmHandle, CmHandleState.READY);
                } catch (final Exception e) {
                    log.warn("Processing of {} module sync  failed.", cmHandleId);
                    syncUtils.updateLockReasonDetailsAndAttempts(compositeState,
                            LockReasonCategory.LOCKED_MODULE_SYNC_FAILED, e.getMessage());
                    setCmHandleStateLocked(yangModelCmHandle, compositeState.getLockReason());
                    cmHandelStatePerCmHandle.put(yangModelCmHandle, CmHandleState.LOCKED);
                }
                log.debug("{} is now in {} state", cmHandleId, compositeState.getCmHandleState().name());
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
        final List<String> resetCmHandleIds = new ArrayList<>(failedCmHandles.size());
        for (final YangModelCmHandle failedCmHandle : failedCmHandles) {
            final CompositeState compositeState = failedCmHandle.getCompositeState();
            final boolean isReadyForRetry = syncUtils.isReadyForRetry(compositeState);
            if (isReadyForRetry) {
                log.debug("Reset cm handle {} state to ADVISED to be re-attempted by module-sync watchdog",
                        failedCmHandle.getId());
                cmHandleStatePerCmHandle.put(failedCmHandle, CmHandleState.ADVISED);
                resetCmHandleIds.add(failedCmHandle.getId());
            }
        }
        lcmEventsCmHandleStateHandler.updateCmHandleStateBatch(cmHandleStatePerCmHandle);
        removeResetCmHandlesFromModuleSyncMap(resetCmHandleIds);
    }

    private void setCmHandleStateLocked(final YangModelCmHandle advisedCmHandle,
                                        final CompositeState.LockReason lockReason) {
        advisedCmHandle.getCompositeState().setLockReason(lockReason);
    }

    private void removeResetCmHandlesFromModuleSyncMap(final List<String> resetCmHandleIds) {
        for (final String resetCmHandleId : resetCmHandleIds) {
            if (moduleSyncStartedOnCmHandles.remove(resetCmHandleId) == null) {
                log.warn("{} state reset into DB but can not be removed from in progress map",
                        resetCmHandleId);
            } else {
                log.debug("{} removed from in progress map", resetCmHandleId);
            }
        }
    }
}
