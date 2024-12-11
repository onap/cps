/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Nordix Foundation.
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

import static org.onap.cps.ncmp.impl.inventory.models.CmHandleState.ADVISED;
import static org.onap.cps.ncmp.impl.inventory.models.CmHandleState.DELETING;
import static org.onap.cps.ncmp.impl.inventory.models.CmHandleState.LOCKED;
import static org.onap.cps.ncmp.impl.inventory.models.CmHandleState.READY;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.IMap;
import java.util.Map;
import lombok.Getter;
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState;
import org.springframework.stereotype.Component;

@Component
@Getter
public class CmHandleStateMetrics {

    final HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance();
    final IMap<String, Integer> cmHandlesByState = hazelcastInstance.getMap("cmHandlesByState");

    public CmHandleStateMetrics(){
        this.getCmHandlesByState().putIfAbsent("advisedCmHandlesCount", 0);
        this.getCmHandlesByState().putIfAbsent("readyCmHandlesCount", 0);
        this.getCmHandlesByState().putIfAbsent("lockedCmHandlesCount", 0);
        this.getCmHandlesByState().putIfAbsent("deletingCmHandlesCount", 0);
    }

    /**
     * Update the Gauge values for cm handles by state.
     *
     * @param previousCmHandleState previous cm handle state
     * @param targetCmHandleState target cm handle state
     */
    public void updateMetricWithStateChange(final CmHandleState previousCmHandleState,
                                            final CmHandleState targetCmHandleState) {
        updatePreviousState(previousCmHandleState);
        updateTargetState(targetCmHandleState);
    }

    private void updatePreviousState(final CmHandleState previousCmHandleState) {
        if (previousCmHandleState.equals(ADVISED)) {
            cmHandlesByState.executeOnKey("advisedCmHandlesCount", new GaugeEntryProcessor(-1));
        }
        if (previousCmHandleState.equals(READY)) {
            cmHandlesByState.executeOnKey("readyCmHandlesCount", new GaugeEntryProcessor(-1));
        }
        if (previousCmHandleState.equals(LOCKED)) {
            cmHandlesByState.executeOnKey("lockedCmHandlesCount", new GaugeEntryProcessor(-1));
        }
    }

    private void updateTargetState(final CmHandleState targetCmHandleState) {
        if (targetCmHandleState.equals(ADVISED)) {
            cmHandlesByState.executeOnKey("advisedCmHandlesCount", new GaugeEntryProcessor(1));
        }
        if (targetCmHandleState.equals(READY)) {
            cmHandlesByState.executeOnKey("readyCmHandlesCount", new GaugeEntryProcessor(1));
        }
        if (targetCmHandleState.equals(LOCKED)) {
            cmHandlesByState.executeOnKey("lockedCmHandlesCount", new GaugeEntryProcessor(1));
        }
        if (targetCmHandleState.equals(DELETING)) {
            cmHandlesByState.executeOnKey("deletingCmHandlesCount", new GaugeEntryProcessor(1));
        }
    }

    static class GaugeEntryProcessor implements EntryProcessor<String, Integer, Void> {
        private final int delta;
        public GaugeEntryProcessor(int delta) {
            this.delta = delta;
        }

        @Override
        public Void process(Map.Entry<String, Integer> entry) {
            final int currentValue = entry.getValue();
            final int newValue = currentValue + delta;
            if (newValue != -1) {
                entry.setValue(currentValue + delta);
            }
            return null;
        }
    }


    }