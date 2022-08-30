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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.onap.ncmp.cmhandle.event.lcm.LcmEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LcmEventsCmHandleStateHandlerImpl implements LcmEventsCmHandleStateHandler {

    private final InventoryPersistence inventoryPersistence;
    private final LcmEventsCreator lcmEventsCreator;
    private final LcmEventsService lcmEventsService;

    @Override
    public void updateCmHandleState(final YangModelCmHandle yangModelCmHandle,
            final CmHandleState targetCmHandleState) {

        final CompositeState compositeState = yangModelCmHandle.getCompositeState();

        if (compositeState != null && compositeState.getCmHandleState() == targetCmHandleState) {
            log.debug("CmHandle with id : {} already in state : {}", yangModelCmHandle.getId(), targetCmHandleState);
        } else {
            final YangModelCmHandle existingYangModelCmHandle = new YangModelCmHandle(yangModelCmHandle);
            updateToSpecifiedCmHandleState(yangModelCmHandle, targetCmHandleState);
            persistCmHandle(yangModelCmHandle, existingYangModelCmHandle);
            publishLcmEventAsync(toNcmpServiceCmHandle(yangModelCmHandle),
                    toNcmpServiceCmHandle(existingYangModelCmHandle));
        }
    }

    @Async("notificationExecutor")
    @Override
    public void publishLcmEventAsync(final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle) {
        publishLcmEvent(targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
    }

    @Override
    public void updateCmHandleStateBatch(final Map<YangModelCmHandle, CmHandleState> cmHandleStatePerCmHandle) {

        final List<CmHandleTransitionPair> cmHandleTransitionPairs = new ArrayList<>();
        cmHandleStatePerCmHandle.forEach((yangModelCmHandle, targetCmHandleState) -> {

            final CompositeState compositeState = yangModelCmHandle.getCompositeState();

            if (compositeState != null && compositeState.getCmHandleState() == targetCmHandleState) {
                log.debug("CmHandle with id : {} already in state : {}", yangModelCmHandle.getId(),
                        targetCmHandleState);
            } else {
                final CmHandleTransitionPair cmHandleTransitionPair = new CmHandleTransitionPair();
                cmHandleTransitionPair.setExistingYangModelCmHandle(new YangModelCmHandle(yangModelCmHandle));
                updateToSpecifiedCmHandleState(yangModelCmHandle, targetCmHandleState);
                cmHandleTransitionPair.setTargetYangModelCmHandle(yangModelCmHandle);
                cmHandleTransitionPairs.add(cmHandleTransitionPair);
            }
        });

        persistCmHandleBatch(cmHandleTransitionPairs);
        publishLcmEventBatch(cmHandleTransitionPairs);
    }

    /**
     * publish LcmEvent in batches.
     *
     * @param cmHandleTransitionPairs Pair of existing and modified cm handle represented as YangModelCmHandle
     */
    @Async("notificationExecutor")
    public void publishLcmEventBatch(final Collection<CmHandleTransitionPair> cmHandleTransitionPairs) {
        cmHandleTransitionPairs.forEach(cmHandleTransitionPair -> publishLcmEvent(
                toNcmpServiceCmHandle(cmHandleTransitionPair.getTargetYangModelCmHandle()),
                toNcmpServiceCmHandle(cmHandleTransitionPair.getExistingYangModelCmHandle())));
    }

    private void publishLcmEvent(final NcmpServiceCmHandle targetNcmpServiceCmHandle,
            final NcmpServiceCmHandle existingNcmpServiceCmHandle) {
        final String cmHandleId = targetNcmpServiceCmHandle.getCmHandleId();
        final LcmEvent lcmEvent =
                lcmEventsCreator.populateLcmEvent(cmHandleId, targetNcmpServiceCmHandle, existingNcmpServiceCmHandle);
        lcmEventsService.publishLcmEvent(cmHandleId, lcmEvent);
    }


    private void persistCmHandle(final YangModelCmHandle targetYangModelCmHandle,
            final YangModelCmHandle existingYangModelCmHandle) {
        if (isNewCmHandle(existingYangModelCmHandle.getCompositeState(), targetYangModelCmHandle.getCompositeState())) {
            log.debug("Registering a new cm handle {}", targetYangModelCmHandle.getId());
            inventoryPersistence.saveCmHandle(targetYangModelCmHandle);
        } else if (!isDeleted(targetYangModelCmHandle.getCompositeState())) {
            inventoryPersistence.saveCmHandleState(targetYangModelCmHandle.getId(),
                    targetYangModelCmHandle.getCompositeState());
        }
    }

    private void persistCmHandleBatch(final Collection<CmHandleTransitionPair> cmHandleTransitionPairCollection) {

        final List<YangModelCmHandle> newCmHandles = new ArrayList<>();
        final Map<String, CompositeState> updatedCmHandles = new LinkedHashMap<>();

        cmHandleTransitionPairCollection.forEach(cmHandleTransitionPair -> {
            if (isNewCmHandle(cmHandleTransitionPair.getExistingYangModelCmHandle().getCompositeState(),
                    cmHandleTransitionPair.getTargetYangModelCmHandle().getCompositeState())) {
                newCmHandles.add(cmHandleTransitionPair.getTargetYangModelCmHandle());
            } else if (!isDeleted(cmHandleTransitionPair.getTargetYangModelCmHandle().getCompositeState())) {
                updatedCmHandles.put(cmHandleTransitionPair.getTargetYangModelCmHandle().getId(),
                        cmHandleTransitionPair.getTargetYangModelCmHandle().getCompositeState());
            }
        });

        if (!newCmHandles.isEmpty()) {
            inventoryPersistence.saveCmHandleBatch(newCmHandles);
        }

        if (!updatedCmHandles.isEmpty()) {
            inventoryPersistence.saveCmHandleStateBatch(updatedCmHandles);
        }
    }


    private void updateToSpecifiedCmHandleState(final YangModelCmHandle yangModelCmHandle,
            final CmHandleState targetCmHandleState) {

        if (READY == targetCmHandleState) {
            readyCmHandle(yangModelCmHandle);
        } else if (ADVISED == targetCmHandleState) {
            if (yangModelCmHandle.getCompositeState() == null) {
                registerNewCmHandle(yangModelCmHandle);
            } else if (yangModelCmHandle.getCompositeState().getCmHandleState() == LOCKED) {
                retryCmHandle(yangModelCmHandle);
            }
        } else {
            setCmHandleState(yangModelCmHandle, targetCmHandleState);
        }
    }

    private void readyCmHandle(final YangModelCmHandle yangModelCmHandle) {
        CompositeStateUtils.setInitialDataStoreSyncState().accept(yangModelCmHandle.getCompositeState());
        CompositeStateUtils.setCompositeState(READY).accept(yangModelCmHandle.getCompositeState());
    }

    private void retryCmHandle(final YangModelCmHandle yangModelCmHandle) {
        CompositeStateUtils.setCompositeStateForRetry().accept(yangModelCmHandle.getCompositeState());
    }

    private void registerNewCmHandle(final YangModelCmHandle yangModelCmHandle) {
        yangModelCmHandle.setCompositeState(new CompositeState());
        setCmHandleState(yangModelCmHandle, ADVISED);
    }

    private void setCmHandleState(final YangModelCmHandle yangModelCmHandle, final CmHandleState targetCmHandleState) {
        CompositeStateUtils.setCompositeState(targetCmHandleState).accept(yangModelCmHandle.getCompositeState());
    }

    private boolean isNewCmHandle(final CompositeState existingCompositeState,
            final CompositeState targetCompositeState) {
        return (existingCompositeState == null && targetCompositeState.getCmHandleState() == ADVISED);
    }

    private boolean isDeleted(final CompositeState targetCompositeState) {
        return targetCompositeState.getCmHandleState() == DELETED;
    }

    private NcmpServiceCmHandle toNcmpServiceCmHandle(final YangModelCmHandle yangModelCmHandle) {
        return YangDataConverter.convertYangModelCmHandleToNcmpServiceCmHandle(yangModelCmHandle);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    static class CmHandleTransitionPair {

        private YangModelCmHandle existingYangModelCmHandle;
        private YangModelCmHandle targetYangModelCmHandle;
    }
}
