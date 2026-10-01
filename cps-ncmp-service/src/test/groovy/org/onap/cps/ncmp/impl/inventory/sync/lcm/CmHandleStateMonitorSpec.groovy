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

package org.onap.cps.ncmp.impl.inventory.sync.lcm

import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.map.IMap
import org.onap.cps.init.actuator.ReadinessManager
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.inventory.sync.lcm.CmHandleStateMonitor.DecreasingEntryProcessor
import org.onap.cps.ncmp.impl.inventory.sync.lcm.CmHandleStateMonitor.IncreasingEntryProcessor
import org.onap.cps.ncmp.utils.events.NcmpInventoryModelOnboardingFinishedEvent
import spock.lang.Shared
import spock.lang.Specification

import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.ADVISED
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.DELETING
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.LOCKED
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.READY

class CmHandleStateMonitorSpec extends Specification {

    def mockCmHandlesByState = Mock(IMap)
    def mockCmHandleQueryService = Mock(CmHandleQueryService)
    def mockCpsCommonLocks = Mock(IMap)
    def mockReadinessManager = Mock(ReadinessManager)
    def objectUnderTest = new CmHandleStateMonitor(mockCmHandleQueryService, mockCmHandlesByState, mockCpsCommonLocks, mockReadinessManager)

    @Shared
    def entryProcessingMap = Hazelcast.getOrCreateHazelcastInstance(new Config('cmHandleStateMonitorSpecInstance')).getMap('entryProcessingMap')

    def cleanupSpec() {
        Hazelcast.getHazelcastInstanceByName('cmHandleStateMonitorSpecInstance').shutdown()
    }

    def setup() {
        entryProcessingMap.put('zeroCmHandlesCount', 0)
        entryProcessingMap.put('tenCmHandlesCount', 10)
    }

    def 'Initialise cm handle state monitor: #scenario'() {
        given: 'the query service returns a list of cm-handle ids for the given state'
            mockCmHandleQueryService.queryCmHandleIdsByState(_) >> queryResult
        and: 'a mocked NcmpModelOnboardingFinishedEvent is triggered'
            def mockNcmpModelOnboardingFinishedEvent = Mock(NcmpInventoryModelOnboardingFinishedEvent)
        when: 'the method to initialise cm handle state monitor is triggered by onboarding event'
            objectUnderTest.initialiseCmHandleStateMonitor(mockNcmpModelOnboardingFinishedEvent)
        then: 'metrics map is called correct number of times for each state except DELETED, with expected value'
            1 * mockCmHandlesByState.putIfAbsent("advisedCmHandlesCount", expectedValue)
            1 * mockCmHandlesByState.putIfAbsent("readyCmHandlesCount", expectedValue)
            1 * mockCmHandlesByState.putIfAbsent("lockedCmHandlesCount", expectedValue)
            1 * mockCmHandlesByState.putIfAbsent("deletingCmHandlesCount", expectedValue)
        where:
            scenario                                 | queryResult || expectedValue
            'query service returns zero cm handle id'| []          || 0
            'query service returns 1 cm handle id'   | ['someId']  || 1
    }

    def 'Update cm handle state metric'() {
        given: 'a cm handle state pair'
            def cmHandleTransitionPair = new CmHandleTransitionPair(new YangModelCmHandle(compositeState: new CompositeState(cmHandleState: ADVISED)),
                                                                    new YangModelCmHandle(compositeState: new CompositeState(cmHandleState: READY))
            )
        when: 'method to update cm handle state metrics is called'
            objectUnderTest.updateCmHandleStateMetrics([cmHandleTransitionPair])
        then: 'cm handle by state cache map is called once for current and target state for entry processing'
            1 * mockCmHandlesByState.executeOnKey('advisedCmHandlesCount', _)
            1 * mockCmHandlesByState.executeOnKey('readyCmHandlesCount', _)
    }

    def 'Update cm handle state metric with no previous state'() {
        given: 'a collection of cm handle state pair wherein current state is null'
            def cmHandleTransitionPair = new CmHandleTransitionPair(new YangModelCmHandle(compositeState: null),
                                                                    new YangModelCmHandle(compositeState: new CompositeState(cmHandleState: ADVISED)))
        when: 'updating cm handle state metrics'
            objectUnderTest.updateCmHandleStateMetrics([cmHandleTransitionPair])
        then: 'cm handle by state cache map is called only once'
            1 * mockCmHandlesByState.executeOnKey(_, _)
    }

    def 'Drifted cm handle state metrics converge to the database counts.'() {
        given: 'a monitor using a real distributed map'
            def realCmHandlesByState = Hazelcast.getHazelcastInstanceByName('cmHandleStateMonitorSpecInstance').getMap('reconciliationMap')
            def monitorUsingRealMap = new CmHandleStateMonitor(mockCmHandleQueryService, realCmHandlesByState, mockCpsCommonLocks, mockReadinessManager)
        and: 'the database holds 100 cm handles in READY and none in any other state'
            mockCmHandleQueryService.queryCmHandleIdsByState(READY) >> (1..100).collect { 'ch-' + it }
            mockCmHandleQueryService.queryCmHandleIdsByState(_) >> []
        and: 'the counters are seeded correctly at startup'
            monitorUsingRealMap.initialiseCmHandleStateMonitor(Mock(NcmpInventoryModelOnboardingFinishedEvent))
            assert realCmHandlesByState.get('readyCmHandlesCount') == 100
        and: 'churn then counts 20 READY transitions that the database never recorded'
            20.times { realCmHandlesByState.executeOnKey('readyCmHandlesCount', new IncreasingEntryProcessor()) }
        and: 'a deletion has been counted since startup'
            realCmHandlesByState.put('deletedCmHandlesCount', 7)
        and: 'the system is ready and this instance acquires the lock'
            mockReadinessManager.isReady() >> true
            mockCpsCommonLocks.tryLock(_) >> true
        and: 'the gauge now over-reports READY against the database'
            assert realCmHandlesByState.get('readyCmHandlesCount') == 120
        when: 'reconciliation runs'
            monitorUsingRealMap.reconcileCmHandleStateMetrics()
        then: 'the READY counter matches the database again'
            assert realCmHandlesByState.get('readyCmHandlesCount') == 100
        and: 'the deleted counter is preserved because it counts deletions since startup'
            assert realCmHandlesByState.get('deletedCmHandlesCount') == 7
        when: 'further drift accumulates and reconciliation runs again'
            30.times { realCmHandlesByState.executeOnKey('readyCmHandlesCount', new IncreasingEntryProcessor()) }
            monitorUsingRealMap.reconcileCmHandleStateMetrics()
        then: 'the counter converges again rather than accumulating'
            assert realCmHandlesByState.get('readyCmHandlesCount') == 100
        cleanup:
            realCmHandlesByState.destroy()
    }

    def 'Under-reported cm handle state metrics also converge to the database counts.'() {
        given: 'a monitor using a real distributed map'
            def realCmHandlesByState = Hazelcast.getHazelcastInstanceByName('cmHandleStateMonitorSpecInstance').getMap('underCountMap')
            def monitorUsingRealMap = new CmHandleStateMonitor(mockCmHandleQueryService, realCmHandlesByState, mockCpsCommonLocks, mockReadinessManager)
        and: 'the database holds 50 cm handles in READY'
            mockCmHandleQueryService.queryCmHandleIdsByState(READY) >> (1..50).collect { 'ch-' + it }
            mockCmHandleQueryService.queryCmHandleIdsByState(_) >> []
        and: 'the counter has been driven to zero by lost increments'
            realCmHandlesByState.put('readyCmHandlesCount', 0)
        and: 'the system is ready and this instance acquires the lock'
            mockReadinessManager.isReady() >> true
            mockCpsCommonLocks.tryLock(_) >> true
        when: 'reconciliation runs'
            monitorUsingRealMap.reconcileCmHandleStateMetrics()
        then: 'the counter is corrected upwards to the database count'
            assert realCmHandlesByState.get('readyCmHandlesCount') == 50
        cleanup:
            realCmHandlesByState.destroy()
    }

    def 'Reconcile cm handle state metrics.'() {
        given: 'the system is ready and this instance acquires the lock'
            mockReadinessManager.isReady() >> true
            mockCpsCommonLocks.tryLock(_) >> true
        and: 'the database holds 5 cm handles for each state'
            mockCmHandleQueryService.queryCmHandleIdsByState(_) >> ['ch-1', 'ch-2', 'ch-3', 'ch-4', 'ch-5']
        when: 'reconciliation is triggered'
            objectUnderTest.reconcileCmHandleStateMetrics()
        then: 'each current-state counter is overwritten with the count from the database'
            1 * mockCmHandlesByState.put('advisedCmHandlesCount', 5)
            1 * mockCmHandlesByState.put('readyCmHandlesCount', 5)
            1 * mockCmHandlesByState.put('lockedCmHandlesCount', 5)
            1 * mockCmHandlesByState.put('deletingCmHandlesCount', 5)
        and: 'the deleted counter is left alone as it counts deletions since startup'
            0 * mockCmHandlesByState.put('deletedCmHandlesCount', _)
        and: 'the lock is released'
            1 * mockCpsCommonLocks.unlock('cmHandleStateMetricsReconciliationLock')
    }

    def 'Reconcile cm handle state metrics when #scenario.'() {
        given: 'system readiness and lock availability for the scenario'
            mockReadinessManager.isReady() >> isReady
            mockCpsCommonLocks.tryLock(_) >> lockAcquired
        when: 'reconciliation is triggered'
            objectUnderTest.reconcileCmHandleStateMetrics()
        then: 'no counter is overwritten'
            0 * mockCmHandlesByState.put(_, _)
        and: 'the database is not queried'
            0 * mockCmHandleQueryService.queryCmHandleIdsByState(_)
        and: 'no lock is released'
            0 * mockCpsCommonLocks.unlock(_)
        where: 'the following conditions apply'
            scenario                                | isReady | lockAcquired
            'the system is not ready yet'            | false   | true
            'another instance is already reconciling'| true    | false
    }

    def 'Applying decreasing entry processor to a key on map where #scenario'() {
        when: 'decreasing entry processor is applied to subtract 1 to the value'
            entryProcessingMap.executeOnKey(key, new DecreasingEntryProcessor())
        then: 'the new value is as expected'
            assert entryProcessingMap.get(key) == expectedValue
        where: 'the following data is used'
            scenario                        | key                  || expectedValue
            'current value of count is zero'| 'zeroCmHandlesCount' || 0
            'current value of count is >0'  | 'tenCmHandlesCount'  || 9
    }

    def 'Applying increasing entry processor to a key on map'() {
        when: 'increasing entry processor is applied to add 1 to the value'
            entryProcessingMap.executeOnKey('tenCmHandlesCount', new IncreasingEntryProcessor())
        then: 'the new value is as expected'
            assert entryProcessingMap.get('tenCmHandlesCount') == 11
    }
}
