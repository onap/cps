/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation.
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

import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState;
import org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventsCmHandleStateHandlerImpl.CmHandleTransitionPair;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CmHandleStateGaugeManager {

    private final CmHandleStateMonitor cmHandleStateMonitor;

    /**
     * Asynchronously update the cm handle state metrics.
     *
     * @param cmHandleTransitionPairs cm handle transition pairs
     */
    @Async
    public void updateCmHandleStateMetrics(final Collection<CmHandleTransitionPair> cmHandleTransitionPairs) {
        cmHandleTransitionPairs.forEach(cmHandleTransitionPair -> updateGaugeForLcm(
                cmHandleTransitionPair.getCurrentYangModelCmHandle().getCompositeState().getCmHandleState(),
                cmHandleTransitionPair.getTargetYangModelCmHandle().getCompositeState().getCmHandleState()));
    }

    private void updateGaugeForLcm(final CmHandleState previousCmHandleState,
                                   final CmHandleState targetCmHandleState) {
        cmHandleStateMonitor.updateMetricWithStateChange(previousCmHandleState, targetCmHandleState);
    }

}
