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

package org.onap.cps.ncmp.impl.inventory.sync.lcm

import static org.onap.cps.ncmp.impl.inventory.models.CmHandleState.ADVISED
import static org.onap.cps.ncmp.impl.inventory.models.CmHandleState.READY

import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventsCmHandleStateHandlerImpl.CmHandleTransitionPair
import spock.lang.Specification;

class CmHandleStateGaugeManagerSpec extends Specification {

    def mockCmHandleStateMonitor = Mock(CmHandleStateMonitor)
    def objectUnderTest = new CmHandleStateGaugeManager(mockCmHandleStateMonitor)

    def 'Update cm handle state metric'() {
        given: 'a collection of cm handle state pair'
            def cmHandleTransitionPair = new CmHandleTransitionPair()
            cmHandleTransitionPair.currentYangModelCmHandle = new YangModelCmHandle(compositeState: new CompositeState(cmHandleState: ADVISED))
            cmHandleTransitionPair.targetYangModelCmHandle =  new YangModelCmHandle(compositeState: new CompositeState(cmHandleState: READY))
        when: 'method to update cm handle state metrics is called'
            objectUnderTest.updateCmHandleStateMetrics([cmHandleTransitionPair])
        then: 'the cm handle state monitor is called once with the correct parameters'
            1 * mockCmHandleStateMonitor.updateMetricWithStateChange(ADVISED, READY)
    }

}
