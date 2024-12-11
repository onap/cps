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

import com.hazelcast.core.Hazelcast
import com.hazelcast.map.IMap
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.inventory.sync.lcm.CmHandleStateMonitor.DecreasingEntryProcessor
import org.onap.cps.ncmp.impl.inventory.sync.lcm.CmHandleStateMonitor.IncreasingEntryProcessor
import spock.lang.Shared
import spock.lang.Specification;

class CmHandleStateMonitorSpec extends Specification {

    def cmHandlesByState = Mock(IMap)
    def objectUnderTest = new CmHandleStateMonitor(cmHandlesByState)

    @Shared
    def entryProcessingMap =  Hazelcast.newHazelcastInstance().getMap('entryProcessingMap')

    def setup() {
        entryProcessingMap.put('zeroCmHandlesCount', 0)
        entryProcessingMap.put('tenCmHandlesCount', 10)
    }

    def cleanupSpec() {
        Hazelcast.shutdownAll()
    }

    def 'Update cm handle state metric'() {
        given: 'a collection of cm handle state pair'
            def cmHandleTransitionPair = new LcmEventsCmHandleStateHandlerImpl.CmHandleTransitionPair()
            cmHandleTransitionPair.currentYangModelCmHandle = new YangModelCmHandle(compositeState: new CompositeState(cmHandleState: ADVISED))
            cmHandleTransitionPair.targetYangModelCmHandle =  new YangModelCmHandle(compositeState: new CompositeState(cmHandleState: READY))
        when: 'method to update cm handle state metrics is called'
            objectUnderTest.updateCmHandleStateMetrics([cmHandleTransitionPair])
        then: 'cm handle by state cache map is called once for current and target state for entry processing'
            1 * cmHandlesByState.executeOnKey('advisedCmHandlesCount', _)
            1 * cmHandlesByState.executeOnKey('readyCmHandlesCount', _)
    }

    def 'Applying decreasing entry processor to a key on map where #scenario'() {
        when: 'decreasing entry processor is applied to subtract 1 to the value'
            entryProcessingMap.executeOnKey(key, new DecreasingEntryProcessor())
        then: 'the new value is as expected'
            assert entryProcessingMap.get(key) == expectedValue
        where: 'the following data is used'
            scenario                        | key                 || expectedValue
            'current value of count is zero'| 'zeroCmHandlesCount'|| 0
            'current value of count is >0'  | 'tenCmHandlesCount' || 9
    }

    def 'Applying increasing entry processor to a key on map'() {
        when: 'increasing entry processor is applied to add 1 to the value'
            entryProcessingMap.executeOnKey('tenCmHandlesCount', new IncreasingEntryProcessor())
        then: 'the new value is as expected'
            assert entryProcessingMap.get('tenCmHandlesCount') == 11
    }
}
