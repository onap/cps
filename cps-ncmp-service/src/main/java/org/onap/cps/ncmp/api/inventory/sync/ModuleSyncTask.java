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
public class ModuleSyncTask {

    private final InventoryPersistence inventoryPersistence;
    private final SyncUtils syncUtils;
    private final ModuleSyncService moduleSyncService;
    private final LcmEventsCmHandleStateHandler lcmEventsCmHandleStateHandler;

    public CompletableFuture<Void> performModuleSync(final Collection<DataNode> cmHandlesAsDataNodes,
                                                     final AtomicInteger batchCounter) {
        final Map<YangModelCmHandle, CmHandleState> cmHandleStates = new HashMap<>();
        for (final DataNode cmHandleAsDataNode : cmHandlesAsDataNodes) {
            String cmHandleId = String.valueOf(cmHandleAsDataNode.getLeaves().get("id"));
            YangModelCmHandle yangModelCmHandle =
                YangDataConverter.convertCmHandleToYangModel(cmHandleAsDataNode, cmHandleId);
            final CompositeState compositeState = inventoryPersistence.getCmHandleState(cmHandleId);
            try {
                moduleSyncService.deleteSchemaSetIfExists(cmHandleId);
                moduleSyncService.syncAndCreateSchemaSetAndAnchor(yangModelCmHandle);
                cmHandleStates.put(yangModelCmHandle, CmHandleState.READY);
            } catch (final Exception e) {
                syncUtils.updateLockReasonDetailsAndAttempts(compositeState,
                    LockReasonCategory.LOCKED_MODULE_SYNC_FAILED, e.getMessage());
                setCmHandleStateLocked(yangModelCmHandle, compositeState.getLockReason());
                cmHandleStates.put(yangModelCmHandle, CmHandleState.LOCKED);
            }
            log.debug("{} is now in {} state", cmHandleId, compositeState.getCmHandleState().name());
        }
        lcmEventsCmHandleStateHandler.updateCmHandlesStateBatch(cmHandleStates);
        batchCounter.getAndDecrement();
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<Void> resynchronizeLockedCmHandles(final List<YangModelCmHandle> lockedCmHandles) {
        final Map<YangModelCmHandle, CmHandleState> cmHandleStates = new HashMap<>();
        lockedCmHandles.parallelStream().forEach(lockedCmHandle -> {
            final CompositeState compositeState = lockedCmHandle.getCompositeState();
            final boolean isReadyForRetry = syncUtils.isReadyForRetry(compositeState);
            if (isReadyForRetry) {
                log.debug("Reset cm handle {} state to ADVISED to be re-attempted by module-sync watchdog",
                        lockedCmHandle.getId());
                cmHandleStates.put(lockedCmHandle, CmHandleState.ADVISED);
            }
        });
        lcmEventsCmHandleStateHandler.updateCmHandlesStateBatch(cmHandleStates);
        return CompletableFuture.completedFuture(null);
    }

    private void setCmHandleStateLocked(final YangModelCmHandle advisedCmHandle,
                                        final CompositeState.LockReason lockReason) {
        advisedCmHandle.getCompositeState().setLockReason(lockReason);
    }
}
