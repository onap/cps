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

import com.hazelcast.map.EntryProcessor;
import com.hazelcast.map.IMap;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Getter
public class CmHandleStateMonitor {

    private static final String METRIC_POSTFIX = "CmHandlesCount" ;
    final IMap<String, Integer> cmHandlesByState;


    /**
     * Constructor initialises the map with cm handle states as key and value 0.
     */
    public void init() {
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
        updatePreviousStateCount(previousCmHandleState);
        updateTargetStateCount(targetCmHandleState);
    }

    private void updatePreviousStateCount(final CmHandleState previousCmHandleState) {
        final String keyName = previousCmHandleState.toString().toLowerCase() + METRIC_POSTFIX;
        cmHandlesByState.executeOnKey(keyName, new DecreasingEntryProcessor());
    }

    private void updateTargetStateCount(final CmHandleState targetCmHandleState) {
        final String keyName = targetCmHandleState.toString().toLowerCase() + METRIC_POSTFIX;
        cmHandlesByState.executeOnKey(keyName, new IncreasingEntryProcessor());
    }

    static class DecreasingEntryProcessor implements EntryProcessor<String, Integer, Void> {
        @Override
        public Void process(final Map.Entry<String, Integer> entry) {
            final int currentValue = entry.getValue();
            final int newValue = currentValue - 1;
            if (newValue != -1) {
                entry.setValue(newValue);
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