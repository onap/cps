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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CmHandleState;
import org.onap.cps.ncmp.api.inventory.CompositeState;
import org.onap.cps.ncmp.api.inventory.CompositeStateUtils;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.ncmp.cmhandle.event.lcm.LcmEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
git @RequiredArgsConstructor
public class LcmEventsCmHandleStateHandlerImpl implements LcmEventsCmHandleStateHandler {

    private final InventoryPersistence inventoryPersistence;
    private final LcmEventsCreator lcmEventsCreator;
    private final LcmEventsService lcmEventsService;

    @Override
    public void updateCmHandlesStateBatch(Map<YangModelCmHandle, CmHandleState> yangModelCmHandlesToNewCmHandleStates) {
        final Map<String, CompositeState> modifiedCmHandleStates = new HashMap<>();
        final List<YangModelCmHandle> newCmHandles = new ArrayList<>();
        yangModelCmHandlesToNewCmHandleStates.entrySet().stream().forEach(yangModelCmHandleState -> {
            YangModelCmHandle updatedYangModelCmHandle = getUpdatedYangModelCmHandle(
                    yangModelCmHandleState.getKey(), yangModelCmHandleState.getValue());

            if (updatedYangModelCmHandle != null && !DELETED.equals(
                    updatedYangModelCmHandle.getCompositeState().getCmHandleState())) {
                if (ADVISED.equals(updatedYangModelCmHandle.getCompositeState().getCmHandleState())) {
                    newCmHandles.add(updatedYangModelCmHandle);
                } else {
                    modifiedCmHandleStates.put(updatedYangModelCmHandle.getId(),
                            updatedYangModelCmHandle.getCompositeState());
                }
            }
        });
        persistOrUpdateYangModuleCmHandles(modifiedCmHandleStates, newCmHandles);
    }

    @Override
    public void updateCmHandleState(final YangModelCmHandle yangModelCmHandle,
                                    final CmHandleState targetCmHandleState) {

        if (READY == targetCmHandleState) {
            final CompositeState compositeState = yangModelCmHandle.getCompositeState();
            CompositeStateUtils.setCompositeStateToReady().accept(compositeState);
            CompositeStateUtils.setInitialDataStoreSyncState(compositeState.getDataSyncEnabled(), compositeState);
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

    private void persistOrUpdateYangModuleCmHandles(final Map<String, CompositeState> modifiedCmHandleStates,
                                                    final List<YangModelCmHandle> newCmHandles) {
        if (!newCmHandles.isEmpty()) {
            inventoryPersistence.saveCmHandles(newCmHandles);
        }
        if (!modifiedCmHandleStates.isEmpty()) {
            inventoryPersistence.saveCmHandleStates(modifiedCmHandleStates);
        }
    }

    private YangModelCmHandle getUpdatedYangModelCmHandle(final YangModelCmHandle yangModelCmHandle,
                                                          final CmHandleState targetCmHandleState) {
        final CompositeState compositeState = yangModelCmHandle.getCompositeState();

        if (compositeState != null && compositeState.getCmHandleState() == targetCmHandleState) {
            log.debug("CmHandle with id : {} already in state : {}", yangModelCmHandle.getId(), targetCmHandleState);
        } else {
            final NcmpServiceCmHandle existingNcmpServiceCmHandle =
                    new NcmpServiceCmHandle(toNcmpServiceCmHandle(yangModelCmHandle));
            updateCmHandleState(yangModelCmHandle, targetCmHandleState);
            populateAndPublishLcmEvent(yangModelCmHandle, existingNcmpServiceCmHandle);
            return yangModelCmHandle;
        }
        return null;
    }

    @Async("notificationExecutor")
    private void populateAndPublishLcmEvent(final YangModelCmHandle yangModelCmHandle,
                                            final NcmpServiceCmHandle existingNcmpServiceCmHandle) {
        final NcmpServiceCmHandle targetNcmpServiceCmHandle = toNcmpServiceCmHandle(yangModelCmHandle);
        publishLcmEvent(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
    }

    private void retryCmHandle(final YangModelCmHandle yangModelCmHandle) {
        CompositeStateUtils.setCompositeStateForRetry().accept(yangModelCmHandle.getCompositeState());
    }

    private void registerNewCmHandle(final YangModelCmHandle yangModelCmHandle) {
        yangModelCmHandle.setCompositeState(new CompositeState());
        setCmHandleState(yangModelCmHandle, ADVISED);
    }

    private void publishLcmEvent(final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle) {
        final String cmHandleId = targetNcmpServiceCmHandle.getCmHandleId();
        final LcmEvent lcmEvent =
                lcmEventsCreator.populateLcmEvent(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
        lcmEventsService.publishLcmEvent(cmHandleId, lcmEvent);
    }

    private void updateAndSaveCmHandleState(final YangModelCmHandle yangModelCmHandle,
            final CmHandleState targetCmHandleState) {
        setCmHandleState(yangModelCmHandle, targetCmHandleState);
    }

    private void setCmHandleState(final YangModelCmHandle yangModelCmHandle, final CmHandleState targetCmHandleState) {
        CompositeStateUtils.setCompositeState(targetCmHandleState).accept(yangModelCmHandle.getCompositeState());
    }

    private NcmpServiceCmHandle toNcmpServiceCmHandle(final YangModelCmHandle yangModelCmHandle) {
        return YangDataConverter.convertYangModelCmHandleToNcmpServiceCmHandle(yangModelCmHandle);
    }
}
