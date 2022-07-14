/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.event.lcm;

import static org.onap.cps.ncmp.api.inventory.CmHandleState.ADVISED;
import static org.onap.cps.ncmp.api.inventory.CmHandleState.LOCKED;
import static org.onap.cps.ncmp.api.inventory.CmHandleState.READY;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CmHandleState;
import org.onap.cps.ncmp.api.inventory.CompositeStateUtils;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.ncmp.cmhandle.event.lcm.LcmEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LcmEventsCmHandleStateHandlerImpl implements LcmEventsCmHandleStateHandler {

    private final InventoryPersistence inventoryPersistence;
    private final LcmEventsCreator lcmEventsCreator;
    private final LcmEventsService lcmEventsService;

    @Value("${data-sync.cache.enabled:false}")
    private boolean isGlobalDataSyncCacheEnabled;


    @Override
    public void updateCmHandleState(final YangModelCmHandle yangModelCmHandle,
            final CmHandleState targetCmHandleState) {

        if (yangModelCmHandle.getCompositeState().getCmHandleState() == targetCmHandleState) {
            log.debug("CmHandle with id : {} already in state : {}", yangModelCmHandle.getId(), targetCmHandleState);
        } else {
            updateToSpecifiedCmHandleState(yangModelCmHandle, targetCmHandleState);
            publishLcmEvent(yangModelCmHandle);
        }

    }

    private void updateToSpecifiedCmHandleState(final YangModelCmHandle yangModelCmHandle,
            final CmHandleState targetCmHandleState) {

        if (READY == targetCmHandleState) {
            CompositeStateUtils.setCompositeStateToReadyWithInitialDataStoreSyncState(isGlobalDataSyncCacheEnabled)
                    .accept(yangModelCmHandle.getCompositeState());
            inventoryPersistence.saveCmHandleState(yangModelCmHandle.getId(), yangModelCmHandle.getCompositeState());
        } else if (ADVISED == targetCmHandleState) {
            if (yangModelCmHandle.getCompositeState().getCmHandleState() == LOCKED) {
                retryCmHandle(yangModelCmHandle);
            } else {
                registerNewCmHandle(yangModelCmHandle);
            }
        } else {
            CompositeStateUtils.setCompositeState(targetCmHandleState).accept(yangModelCmHandle.getCompositeState());
            inventoryPersistence.saveCmHandleState(yangModelCmHandle.getId(), yangModelCmHandle.getCompositeState());
        }

    }

    private void retryCmHandle(final YangModelCmHandle yangModelCmHandle) {
        CompositeStateUtils.setCompositeStateForRetry().accept(yangModelCmHandle.getCompositeState());
        inventoryPersistence.saveCmHandleState(yangModelCmHandle.getId(), yangModelCmHandle.getCompositeState());
    }

    private void registerNewCmHandle(final YangModelCmHandle yangModelCmHandle) {
        CompositeStateUtils.setCompositeState(ADVISED).accept(yangModelCmHandle.getCompositeState());
        inventoryPersistence.saveCmHandle(yangModelCmHandle);
    }

    private void publishLcmEvent(final YangModelCmHandle yangModelCmHandle) {
        final NcmpServiceCmHandle ncmpServiceCmHandle =
                YangDataConverter.convertYangModelCmHandleToNcmpServiceCmHandle(yangModelCmHandle);
        final String cmHandleId = ncmpServiceCmHandle.getCmHandleId();
        final LcmEvent lcmEvent = lcmEventsCreator.populateLcmEvent(cmHandleId, ncmpServiceCmHandle);
        lcmEventsService.publishLcmEvent(cmHandleId, lcmEvent);
    }
}
