/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.IMap;
import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.inventory.models.CmHandleState;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService;
import org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventsCmHandleStateHandlerImpl.CmHandleTransitionPair;
import org.onap.cps.ncmp.utils.events.NcmpInventoryModelOnboardingFinishedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CmHandleStateMonitor {
    private static final String METRIC_POSTFIX = "CmHandlesCount";

    private final CmHandleQueryService cmHandleQueryService;
    private final IMap<String, Integer> cmHandlesByState;

    /**
     * Method to initialise cm handle state monitor  by querying the current state counts
     * and storing them in the provided map. This method is triggered by NcmpInventoryModelOnboardingFinishedEvent.
     *
     * @param ncmpInventoryModelOnboardingFinishedEvent the event that triggers the initialization
     */
    @EventListener
    public void initialiseCmHandleStateMonitor(
            final NcmpInventoryModelOnboardingFinishedEvent ncmpInventoryModelOnboardingFinishedEvent) {
        for (final CmHandleState cmHandleState : CmHandleState.values()) {
            final String cmHandleStateAsString = cmHandleState.name().toLowerCase();
            final String stateMetricKey = cmHandleStateAsString + METRIC_POSTFIX;
            final int cmHandleCountForState =  cmHandleQueryService.queryCmHandleIdsByState(cmHandleState).size();
            cmHandlesByState.putIfAbsent(stateMetricKey, cmHandleCountForState);
            log.info("Cm handle state monitor has set " + stateMetricKey + " to " + cmHandleCountForState);
        }
    }

    /**
     * Asynchronously update the cm handle state metrics.
     *
     * @param cmHandleTransitionPairs cm handle transition pairs
     */
    @Async
    public void updateCmHandleStateMetrics(final Collection<CmHandleTransitionPair>
                                                       cmHandleTransitionPairs) {
        cmHandleTransitionPairs.forEach(this::updateMetricWithStateChange);
    }

    private void updateMetricWithStateChange(final CmHandleTransitionPair cmHandleTransitionPair) {
        final CmHandleState targetCmHandleState = cmHandleTransitionPair.getTargetYangModelCmHandle()
                .getCompositeState().getCmHandleState();
        if (isNew(cmHandleTransitionPair.getCurrentYangModelCmHandle().getCompositeState())) {
            updateTargetStateCount(targetCmHandleState);
        } else {
            final CmHandleState previousCmHandleState = cmHandleTransitionPair.getCurrentYangModelCmHandle()
                    .getCompositeState().getCmHandleState();
            updatePreviousStateCount(previousCmHandleState);
            updateTargetStateCount(targetCmHandleState);
        }
    }

    private void updatePreviousStateCount(final CmHandleState previousCmHandleState) {
        final String keyName = previousCmHandleState.name().toLowerCase() + METRIC_POSTFIX;
        cmHandlesByState.executeOnKey(keyName, new DecreasingEntryProcessor());
    }

    private void updateTargetStateCount(final CmHandleState targetCmHandleState) {
        final String keyName = targetCmHandleState.name().toLowerCase() + METRIC_POSTFIX;
        cmHandlesByState.executeOnKey(keyName, new IncreasingEntryProcessor());
    }

    private boolean isNew(final CompositeState existingCompositeState) {
        return (existingCompositeState == null);
    }

    static class DecreasingEntryProcessor implements EntryProcessor<String, Integer, Void> {
        @Override
        public Void process(final Map.Entry<String, Integer> entry) {
            final int currentValue = entry.getValue();
            if (currentValue > 0) {
                entry.setValue(currentValue - 1);
            }
            return null;
        }
    }

    static class IncreasingEntryProcessor implements EntryProcessor<String, Integer, Void> {
        @Override
        public Void process(final Map.Entry<String, Integer> entry) {
            final int currentValue = entry.getValue();
            entry.setValue(currentValue + 1);
            return null;
        }
    }


}