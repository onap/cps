/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.cps.init.actuator.ReadinessManager;
import org.onap.cps.ncmp.api.inventory.models.CmHandleState;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService;
import org.onap.cps.ncmp.utils.events.NcmpInventoryModelOnboardingFinishedEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@DependsOn("adminCacheConfig")
@Slf4j
public class CmHandleStateMonitor {
    private static final String METRIC_POSTFIX = "CmHandlesCount";
    private static final String RECONCILIATION_LOCK_NAME = "cmHandleStateMetricsReconciliationLock";

    private final CmHandleQueryService cmHandleQueryService;
    private final IMap<String, Integer> cmHandlesByState;
    @Qualifier("cpsCommonLocks") private final IMap<String, String> cpsCommonLocks;
    private final ReadinessManager readinessManager;

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
            final int cmHandleCountForState = cmHandleQueryService.queryCmHandleIdsByState(cmHandleState).size();
            cmHandlesByState.putIfAbsent(stateMetricKey, cmHandleCountForState);
            log.info("Cm handle state monitor has set {} to {}", stateMetricKey, cmHandleCountForState);
        }
    }

    /**
     * Reset the current-state counters to the counts held in the database.
     * The counters are maintained as increments and decrements per state transition, so any update that is lost or
     * applied twice leaves an error that no later transition corrects: a decrement is floored at zero while an
     * increment is not, so the error only ever grows. Seeding happens once at onboarding and uses putIfAbsent, which
     * cannot correct an already populated map, and the map is shared across instances, so restarting a single
     * instance does not clear the error either. Re-reading the counts from the database makes the gauge converge
     * regardless of which transition was miscounted.
     * DELETED is excluded on purpose: it counts cm handles deleted since startup rather than cm handles currently in
     * that state, and a deleted cm handle is removed from the registry, so the database can never report it.
     * The interval is set by ncmp.timers.cm-handle-state-metrics-reconciliation.sleep-time-ms.
     */
    @Scheduled(fixedDelayString =
            "${ncmp.timers.cm-handle-state-metrics-reconciliation.sleep-time-ms:300000}")
    public void reconcileCmHandleStateMetrics() {
        if (!readinessManager.isReady()) {
            log.debug("Skipping cm handle state metrics reconciliation, system is not ready yet");
            return;
        }
        if (!cpsCommonLocks.tryLock(RECONCILIATION_LOCK_NAME)) {
            log.debug("Skipping cm handle state metrics reconciliation, another instance is already reconciling");
            return;
        }
        try {
            for (final CmHandleState cmHandleState : CmHandleState.values()) {
                if (CmHandleState.DELETED != cmHandleState) {
                    reconcileCountForState(cmHandleState);
                }
            }
        } finally {
            cpsCommonLocks.unlock(RECONCILIATION_LOCK_NAME);
        }
    }

    private void reconcileCountForState(final CmHandleState cmHandleState) {
        final String stateMetricKey = cmHandleState.name().toLowerCase() + METRIC_POSTFIX;
        final int cmHandleCountInDb = cmHandleQueryService.queryCmHandleIdsByState(cmHandleState).size();
        final Integer previousCmHandleCount = cmHandlesByState.put(stateMetricKey, cmHandleCountInDb);
        if (previousCmHandleCount == null || previousCmHandleCount != cmHandleCountInDb) {
            log.info("Cm handle state metrics reconciliation corrected {} from {} to {}", stateMetricKey,
                    previousCmHandleCount, cmHandleCountInDb);
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
        final CmHandleState targetCmHandleState = cmHandleTransitionPair.targetYangModelCmHandle()
                .getCompositeState().getCmHandleState();
        if (isNew(cmHandleTransitionPair.currentYangModelCmHandle().getCompositeState())) {
            updateTargetStateCount(targetCmHandleState);
        } else {
            final CmHandleState previousCmHandleState = cmHandleTransitionPair.currentYangModelCmHandle()
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

    private boolean isNew(final CompositeState currentCompositeState) {
        return currentCompositeState == null;
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
