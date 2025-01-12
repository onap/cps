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

import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.ADVISED
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.READY

import com.hazelcast.core.Hazelcast
import com.hazelcast.map.IMap
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.inventory.sync.lcm.CmHandleStateMonitor.DecreasingEntryProcessor
import org.onap.cps.ncmp.impl.inventory.sync.lcm.CmHandleStateMonitor.IncreasingEntryProcessor
import org.onap.cps.ncmp.utils.events.NcmpModelOnboardingFinishedEvent
import spock.lang.Shared
import spock.lang.Specification;

class CmHandleStateMonitorSpec extends Specification {

    def mockCmHandlesByState = Mock(IMap)
    def mockCmHandleQueryService = Mock(CmHandleQueryService)
    def objectUnderTest = new CmHandleStateMonitor(mockCmHandleQueryService, mockCmHandlesByState)

    @Shared
    def entryProcessingMap =  Hazelcast.newHazelcastInstance().getMap('entryProcessingMap')

    def setup() {
        entryProcessingMap.put('zeroCmHandlesCount', 0)
        entryProcessingMap.put('tenCmHandlesCount', 10)
    }

    def cleanupSpec() {
        Hazelcast.shutdownAll()
    }

    def 'Initialise cm handle state monitor: #scenario'() {
        given: 'the query service returns a value'
            mockCmHandleQueryService.queryCmHandleIdsByState(_) >> queryResult
        and: 'ncmp model onboarding event'
            def mockNcmpModelOnboardingFinishedEvent = Mock(NcmpModelOnboardingFinishedEvent)
        when: 'the method to initialise cm handle state monitor is triggered by onboarding event'
            objectUnderTest.initialiseCmHandleStateMonitor(mockNcmpModelOnboardingFinishedEvent)
        then: 'metrics map is called correct number of times for each state except DELETED with expected value'
            1 * mockCmHandlesByState.putIfAbsent("advisedCmHandlesCount", expectedValue)
            1 * mockCmHandlesByState.putIfAbsent("readyCmHandlesCount", expectedValue)
            1 * mockCmHandlesByState.putIfAbsent("lockedCmHandlesCount", expectedValue)
            1 * mockCmHandlesByState.putIfAbsent("deletingCmHandlesCount", expectedValue)
        where:
            scenario                                 | queryResult                 || expectedValue
            'query service returns zero cm handle id'| []                          || 0
            'query service returns 1 cm handle id'   | ['someId']                  || 1
    }


    def 'Update cm handle state metric'() {
        given: 'a collection of cm handle state pair'
            def cmHandleTransitionPair = new LcmEventsCmHandleStateHandlerImpl.CmHandleTransitionPair()
            cmHandleTransitionPair.currentYangModelCmHandle = new YangModelCmHandle(compositeState: new CompositeState(cmHandleState: ADVISED))
            cmHandleTransitionPair.targetYangModelCmHandle =  new YangModelCmHandle(compositeState: new CompositeState(cmHandleState: READY))
        when: 'method to update cm handle state metrics is called'
            objectUnderTest.updateCmHandleStateMetrics([cmHandleTransitionPair])
        then: 'cm handle by state cache map is called once for current and target state for entry processing'
            1 * mockCmHandlesByState.executeOnKey('advisedCmHandlesCount', _)
            1 * mockCmHandlesByState.executeOnKey('readyCmHandlesCount', _)
    }

    def 'Update cm handle state metric with no previous state'() {
        given: 'a collection of cm handle state pair wherein current state is null'
            def cmHandleTransitionPair = new LcmEventsCmHandleStateHandlerImpl.CmHandleTransitionPair()
            cmHandleTransitionPair.currentYangModelCmHandle = new YangModelCmHandle(compositeState: null)
            cmHandleTransitionPair.targetYangModelCmHandle =  new YangModelCmHandle(compositeState: new CompositeState(cmHandleState: ADVISED))
        when: 'updating cm handle state metrics'
            objectUnderTest.updateCmHandleStateMetrics([cmHandleTransitionPair])
        then: 'cm handle by state cache map is called only once'
            1 * mockCmHandlesByState.executeOnKey(_, _)
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
