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
import static org.onap.cps.ncmp.api.inventory.CmHandleState.DELETED;
import static org.onap.cps.ncmp.api.inventory.CmHandleState.LOCKED;
import static org.onap.cps.ncmp.api.inventory.CmHandleState.READY;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CmHandleState;
import org.onap.cps.ncmp.api.inventory.CompositeState;
import org.onap.cps.ncmp.api.inventory.CompositeStateUtils;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.utils.JsonObjectMapper;
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
    private final JsonObjectMapper jsonObjectMapper;

    @Value("${data-sync.cache.enabled:false}")
    private boolean isGlobalDataSyncCacheEnabled;


    @Override
    public void updateCmHandleState(final YangModelCmHandle yangModelCmHandle,
            final CmHandleState targetCmHandleState) {

        final CompositeState compositeState = yangModelCmHandle.getCompositeState();

        if (compositeState != null && compositeState.getCmHandleState() == targetCmHandleState) {
            log.debug("CmHandle with id : {} already in state : {}", yangModelCmHandle.getId(), targetCmHandleState);
        } else {
            final NcmpServiceCmHandlerPair ncmpServiceCmHandlerPair =
                    updateAndGetCmHandlePair(yangModelCmHandle, targetCmHandleState);
            publishLcmEvent(ncmpServiceCmHandlerPair);
        }

    }

    private NcmpServiceCmHandlerPair updateAndGetCmHandlePair(final YangModelCmHandle yangModelCmHandle,
            final CmHandleState targetCmHandleState) {
        final NcmpServiceCmHandlerPair ncmpServiceCmHandlerPair = new NcmpServiceCmHandlerPair();
        ncmpServiceCmHandlerPair.setExistingNcmpServiceCmHandle(
                mapYangModelCmHandleToNcmpServiceCmHandle(yangModelCmHandle));

        updateToSpecifiedCmHandleState(yangModelCmHandle, targetCmHandleState);

        ncmpServiceCmHandlerPair.setTargetNcmpServiceCmHandle(
                YangDataConverter.convertYangModelCmHandleToNcmpServiceCmHandle(yangModelCmHandle));
        return ncmpServiceCmHandlerPair;

    }

    private void updateToSpecifiedCmHandleState(final YangModelCmHandle yangModelCmHandle,
            final CmHandleState targetCmHandleState) {

        if (READY == targetCmHandleState) {
            CompositeStateUtils.setCompositeStateToReadyWithInitialDataStoreSyncState(isGlobalDataSyncCacheEnabled)
                    .accept(yangModelCmHandle.getCompositeState());
            inventoryPersistence.saveCmHandleState(yangModelCmHandle.getId(), yangModelCmHandle.getCompositeState());
        } else if (ADVISED == targetCmHandleState) {
            if (yangModelCmHandle.getCompositeState() == null) {
                registerNewCmHandle(yangModelCmHandle);
            } else if (yangModelCmHandle.getCompositeState().getCmHandleState() == LOCKED) {
                retryCmHandle(yangModelCmHandle);
            }
        } else if (DELETED == targetCmHandleState) {
            setCmHandleState(yangModelCmHandle, targetCmHandleState);
        } else {
            updateAndSaveCmHandleState(yangModelCmHandle, targetCmHandleState);
        }
    }

    private void retryCmHandle(final YangModelCmHandle yangModelCmHandle) {
        CompositeStateUtils.setCompositeStateForRetry().accept(yangModelCmHandle.getCompositeState());
        inventoryPersistence.saveCmHandleState(yangModelCmHandle.getId(), yangModelCmHandle.getCompositeState());
    }

    private void registerNewCmHandle(final YangModelCmHandle yangModelCmHandle) {
        yangModelCmHandle.setCompositeState(new CompositeState());
        setCmHandleState(yangModelCmHandle, ADVISED);
        inventoryPersistence.saveCmHandle(yangModelCmHandle);
    }

    private void publishLcmEvent(final NcmpServiceCmHandlerPair ncmpServiceCmHandlerPair) {
        final String cmHandleId = ncmpServiceCmHandlerPair.getTargetNcmpServiceCmHandle().getCmHandleId();
        final LcmEvent lcmEvent =
                lcmEventsCreator.populateLcmEvent(cmHandleId, ncmpServiceCmHandlerPair.getTargetNcmpServiceCmHandle(),
                        ncmpServiceCmHandlerPair.getExistingNcmpServiceCmHandle());
        lcmEventsService.publishLcmEvent(cmHandleId, lcmEvent);
    }

    private void updateAndSaveCmHandleState(final YangModelCmHandle yangModelCmHandle,
            final CmHandleState targetCmHandleState) {
        setCmHandleState(yangModelCmHandle, targetCmHandleState);
        inventoryPersistence.saveCmHandleState(yangModelCmHandle.getId(), yangModelCmHandle.getCompositeState());
    }

    private void setCmHandleState(final YangModelCmHandle yangModelCmHandle, final CmHandleState targetCmHandleState) {
        CompositeStateUtils.setCompositeState(targetCmHandleState).accept(yangModelCmHandle.getCompositeState());
    }


    private NcmpServiceCmHandle mapYangModelCmHandleToNcmpServiceCmHandle(final YangModelCmHandle yangModelCmHandle) {
        final NcmpServiceCmHandle existingNcmpServiceCmHandle =
                YangDataConverter.convertYangModelCmHandleToNcmpServiceCmHandle(yangModelCmHandle);
        return jsonObjectMapper.convertJsonString(jsonObjectMapper.asJsonString(existingNcmpServiceCmHandle),
                NcmpServiceCmHandle.class);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    static class NcmpServiceCmHandlerPair {

        private NcmpServiceCmHandle existingNcmpServiceCmHandle;
        private NcmpServiceCmHandle targetNcmpServiceCmHandle;

    }
}
