/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.inventory.sync.lcm;

import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.ADVISED;
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.DELETED;
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.LOCKED;
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.READY;

import io.micrometer.core.annotation.Timed;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.inventory.models.CmHandleState;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.impl.inventory.CompositeStateUtils;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class LcmEventsCmHandleStateHandlerImpl implements LcmEventsCmHandleStateHandler {

    private final InventoryPersistence inventoryPersistence;
    private final LcmEventProducer lcmEventProducer;
    private final CmHandleStateMonitor cmHandleStateMonitor;

    @Override
    @Timed(value = "cps.ncmp.cmhandle.state.update.batch",
            description = "Time taken to update a batch of cm handle states")
    public void updateCmHandleStateBatch(final Map<YangModelCmHandle, CmHandleState> targetCmHandleStatePerCmHandle) {
        final Collection<CmHandleTransitionPair> cmHandleTransitionPairs =
                prepareCmHandleTransitionBatch(targetCmHandleStatePerCmHandle);
        persistCmHandleBatch(cmHandleTransitionPairs);
        if (!cmHandleTransitionPairs.isEmpty()) {
            lcmEventProducer.sendLcmEventBatchAsynchronously(cmHandleTransitionPairs);
            cmHandleStateMonitor.updateCmHandleStateMetrics(cmHandleTransitionPairs);
        }
    }

    @Override
    public void initiateStateAdvised(final Collection<YangModelCmHandle> yangModelCmHandles) {
        final Map<YangModelCmHandle, CmHandleState> targetCmHandleStatePerCmHandle
            = new HashMap<>(yangModelCmHandles.size());
        for (final YangModelCmHandle yangModelCmHandle : yangModelCmHandles) {
            targetCmHandleStatePerCmHandle.put(yangModelCmHandle, ADVISED);
        }
        updateCmHandleStateBatch(targetCmHandleStatePerCmHandle);
    }

    private Collection<CmHandleTransitionPair> prepareCmHandleTransitionBatch(
            final Map<YangModelCmHandle, CmHandleState> targetCmHandleStatePerCmHandle) {
        final List<CmHandleTransitionPair> cmHandleTransitionPairs
            = new ArrayList<>(targetCmHandleStatePerCmHandle.size());
        targetCmHandleStatePerCmHandle.forEach((yangModelCmHandle, targetCmHandleState) -> {
            final CompositeState currentCmHandleState = yangModelCmHandle.getCompositeState();
            if (isCompositeStateSame(currentCmHandleState, targetCmHandleState)) {
                log.debug("CmHandle: {} already in state: {}", yangModelCmHandle.getId(), targetCmHandleState);
            } else {
                final YangModelCmHandle oldYangModelCmHandle = YangModelCmHandle.deepCopyOf(yangModelCmHandle);
                updateCmHandleState(yangModelCmHandle, targetCmHandleState);
                final CmHandleTransitionPair cmHandleTransitionPair = new CmHandleTransitionPair(
                    oldYangModelCmHandle, yangModelCmHandle);
                cmHandleTransitionPairs.add(cmHandleTransitionPair);
            }
        });
        return cmHandleTransitionPairs;
    }

    private void persistCmHandleBatch(final Collection<CmHandleTransitionPair> cmHandleTransitionPairs) {

        final List<YangModelCmHandle> newCmHandles = new ArrayList<>();
        final Map<String, CompositeState> compositeStatePerCmHandleId = new LinkedHashMap<>();

        cmHandleTransitionPairs.forEach(cmHandleTransitionPair -> {
            if (isNew(cmHandleTransitionPair.currentYangModelCmHandle().getCompositeState())) {
                newCmHandles.add(cmHandleTransitionPair.targetYangModelCmHandle());
            } else if (!isDeleted(cmHandleTransitionPair.targetYangModelCmHandle().getCompositeState())) {
                compositeStatePerCmHandleId.put(cmHandleTransitionPair.targetYangModelCmHandle().getId(),
                        cmHandleTransitionPair.targetYangModelCmHandle().getCompositeState());
            }
        });
        inventoryPersistence.saveCmHandleBatch(newCmHandles);
        inventoryPersistence.saveCmHandleStateBatch(compositeStatePerCmHandleId);
        logCmHandleStateChanges(cmHandleTransitionPairs);
    }

    private void updateCmHandleState(final YangModelCmHandle yangModelCmHandle,
                                     final CmHandleState targetCmHandleState) {
        if (READY == targetCmHandleState) {
            setInitialState(yangModelCmHandle);
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

    private void setInitialState(final YangModelCmHandle yangModelCmHandle) {
        CompositeStateUtils.setInitialDataStoreSyncState(yangModelCmHandle.getCompositeState());
        CompositeStateUtils.setCompositeState(READY, yangModelCmHandle.getCompositeState());
    }

    private void retryCmHandle(final YangModelCmHandle yangModelCmHandle) {
        CompositeStateUtils.setCompositeStateForRetry(yangModelCmHandle.getCompositeState());
    }

    private void registerNewCmHandle(final YangModelCmHandle yangModelCmHandle) {
        yangModelCmHandle.setCompositeState(new CompositeState());
        setCmHandleState(yangModelCmHandle, ADVISED);
    }

    private void setCmHandleState(final YangModelCmHandle yangModelCmHandle, final CmHandleState targetCmHandleState) {
        CompositeStateUtils.setCompositeState(targetCmHandleState, yangModelCmHandle.getCompositeState());
    }

    private boolean isNew(final CompositeState existingCompositeState) {
        return (existingCompositeState == null);
    }

    private boolean isDeleted(final CompositeState targetCompositeState) {
        return targetCompositeState.getCmHandleState() == DELETED;
    }

    private boolean isCompositeStateSame(final CompositeState compositeState, final CmHandleState targetCmHandleState) {
        return (compositeState != null && compositeState.getCmHandleState() == targetCmHandleState);
    }

    private static void logCmHandleStateChanges(final Collection<CmHandleTransitionPair> cmHandleTransitionPairs) {
        cmHandleTransitionPairs.stream()
                .map(CmHandleTransitionPair::targetYangModelCmHandle)
                .forEach(yangModelCmHandle -> log.debug("{} is now in {} state", yangModelCmHandle.getId(),
                        yangModelCmHandle.getCompositeState().getCmHandleState().name()));
    }


}
