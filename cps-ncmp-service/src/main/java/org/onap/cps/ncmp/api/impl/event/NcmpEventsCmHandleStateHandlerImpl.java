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

package org.onap.cps.ncmp.api.impl.event;

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
import org.onap.cps.utils.JsonObjectMapper;
import org.onap.ncmp.cmhandle.lcm.event.NcmpEvent;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NcmpEventsCmHandleStateHandlerImpl implements NcmpEventsCmHandleStateHandler {

    private final InventoryPersistence inventoryPersistence;
    private final NcmpEventsCreator ncmpEventsCreator;
    private final JsonObjectMapper jsonObjectMapper;
    private final NcmpEventsService ncmpEventsService;


    @Override
    public void updateState(final YangModelCmHandle yangModelCmHandle, final CmHandleState targetCmHandleState) {

        if (yangModelCmHandle.getCompositeState().getCmHandleState() == targetCmHandleState) {
            log.debug("CmHandle with id : {} already in state : {}", yangModelCmHandle.getId(), targetCmHandleState);
        } else {
            updateToSpecifiedCmHandleState(yangModelCmHandle, targetCmHandleState);
            publishNcmpEvent(yangModelCmHandle);
        }

    }

    private void updateToSpecifiedCmHandleState(final YangModelCmHandle yangModelCmHandle,
            final CmHandleState targetCmHandleState) {

        if (READY == targetCmHandleState) {
            CompositeStateUtils.setCompositeStateToReadyWithInitialDataStoreSyncState()
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
        CompositeStateUtils.setCompositeStateToAdvisedAndRetainOldLockReasonDetails()
                .accept(yangModelCmHandle.getCompositeState());
        inventoryPersistence.saveCmHandleState(yangModelCmHandle.getId(), yangModelCmHandle.getCompositeState());
    }

    private void registerNewCmHandle(final YangModelCmHandle yangModelCmHandle) {
        CompositeStateUtils.setCompositeState(ADVISED).accept(yangModelCmHandle.getCompositeState());
        inventoryPersistence.saveListElements(
                String.format("{\"cm-handles\":[%s]}", jsonObjectMapper.asJsonString(yangModelCmHandle)));
    }

    private void publishNcmpEvent(final YangModelCmHandle yangModelCmHandle) {
        final NcmpServiceCmHandle ncmpServiceCmHandle =
                YangDataConverter.convertYangModelCmHandleToNcmpServiceCmHandle(yangModelCmHandle);
        final String cmHandleId = ncmpServiceCmHandle.getCmHandleId();
        final NcmpEvent ncmpEvent = ncmpEventsCreator.populateNcmpEvent(cmHandleId, ncmpServiceCmHandle);
        ncmpEventsService.publishNcmpEvent(cmHandleId, ncmpEvent);
    }
}
