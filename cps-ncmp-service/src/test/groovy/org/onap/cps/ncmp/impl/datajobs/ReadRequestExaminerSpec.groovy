/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.datajobs

import org.onap.cps.ncmp.api.datajobs.models.ProducerKey
import org.onap.cps.ncmp.api.inventory.models.CmHandleState
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.exceptions.NoAlternateIdMatchFoundException
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher
import spock.lang.Specification

class ReadRequestExaminerSpec extends Specification {

    def mockAlternateIdMatcher = Mock(AlternateIdMatcher)
    def mockInventoryPersistence = Mock(InventoryPersistence)
    def objectUnderTest = new ReadRequestExaminer(mockAlternateIdMatcher, mockInventoryPersistence)

    def 'Classify selector into DMI group when target is READY'() {
        given: 'a READY cm handle resolved from the selector'
            def yangModelCmHandle = createYangModelCmHandle('ch-1', 'dmi-plugin-1', 'producer-A', CmHandleState.READY)
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/ManagedElement=ME1', '/') >> 'ch-1'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> yangModelCmHandle
        when: 'selectors are classified'
            def result = objectUnderTest.classifySelectors('/ManagedElement[id="ME1"]')
        then: 'the selector is grouped under the correct DMI service and producer key'
            assert result.broadcastSelectors().isEmpty()
            assert result.errorSelectors().isEmpty()
            assert result.notReadySelectors().isEmpty()
            assert result.dmiSelectors() == [
                'dmi-plugin-1': [(new ProducerKey('dmi-plugin-1', 'producer-A')): ['/ManagedElement[id="ME1"]']]
            ]
    }

    def 'Classify broadcast selectors with #scenario'() {
        when: 'selectors are classified'
            def result = objectUnderTest.classifySelectors(dataNodeSelectors)
        then: 'all selectors are classified as broadcast'
            assert result.broadcastSelectors() == expectedBroadcastFdns
            assert result.dmiSelectors().isEmpty()
        where: 'the following selectors are used'
            scenario                           | dataNodeSelectors                           || expectedBroadcastFdns
            'deep search'                      | '//ManagedElement[contains(id, "Ireland")]' || ['//ManagedElement[contains(id, "Ireland")]']
            'no ManagedElement or MeContext id'| '/SubNetwork[id="SN1"]'                     || ['/SubNetwork[id="SN1"]']
            'ManagedElement without id'        | '/SubNetwork[id="SN1"]/ManagedElement'      || ['/SubNetwork[id="SN1"]/ManagedElement']
            'contains on SubNetwork'           | '/SubNetwork[contains(id, "Ireland")]'      || ['/SubNetwork[contains(id, "Ireland")]']
            'multiple broadcast selectors'     | '/SubNetwork[id="SN1"] OR //ManagedElement' || ['/SubNetwork[id="SN1"]', '//ManagedElement']
    }

    def 'Classify multiple selectors into same DMI group'() {
        given: 'two READY cm handles on the same DMI and producer'
            def yangModelCmHandle1 = createYangModelCmHandle('ch-1', 'dmi-plugin-1', 'producer-A', CmHandleState.READY)
            def yangModelCmHandle2 = createYangModelCmHandle('ch-2', 'dmi-plugin-1', 'producer-A', CmHandleState.READY)
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/ManagedElement=ME1', '/') >> 'ch-1'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/SubNetwork=SN1/ManagedElement=ME2', '/') >> 'ch-2'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> yangModelCmHandle1
            mockInventoryPersistence.getYangModelCmHandle('ch-2') >> yangModelCmHandle2
        when: 'selectors are classified'
            def result = objectUnderTest.classifySelectors('/ManagedElement[id="ME1"]\n/SubNetwork[id="SN1"]/ManagedElement[id="ME2"]')
        then: 'both selectors are grouped under the same DMI and producer key'
            assert result.dmiSelectors() == [
                'dmi-plugin-1': [(new ProducerKey('dmi-plugin-1', 'producer-A')): [
                    '/ManagedElement[id="ME1"]',
                    '/SubNetwork[id="SN1"]/ManagedElement[id="ME2"]'
                ]]
            ]
    }

    def 'Classify selectors into different DMI groups'() {
        given: 'two READY cm handles on different DMIs'
            def yangModelCmHandle1 = createYangModelCmHandle('ch-1', 'dmi-plugin-1', 'producer-A', CmHandleState.READY)
            def yangModelCmHandle2 = createYangModelCmHandle('ch-2', 'dmi-plugin-2', 'producer-B', CmHandleState.READY)
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/ManagedElement=ME1', '/') >> 'ch-1'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/MeContext=MC1', '/') >> 'ch-2'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> yangModelCmHandle1
            mockInventoryPersistence.getYangModelCmHandle('ch-2') >> yangModelCmHandle2
        when: 'selectors are classified'
            def result = objectUnderTest.classifySelectors('/ManagedElement[id="ME1"]\n/MeContext[id="MC1"]')
        then: 'selectors are grouped under different DMI services'
            assert result.dmiSelectors() == [
                'dmi-plugin-1': [(new ProducerKey('dmi-plugin-1', 'producer-A')): ['/ManagedElement[id="ME1"]']],
                'dmi-plugin-2': [(new ProducerKey('dmi-plugin-2', 'producer-B')): ['/MeContext[id="MC1"]']]
            ]
    }

    def 'Classify selector with target not found'() {
        given: 'alternate id matcher throws exception'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(_, '/') >> { throw new NoAlternateIdMatchFoundException('test') }
        when: 'selectors are classified'
            def result = objectUnderTest.classifySelectors('/ManagedElement[id="ME1"]')
        then: 'the selector is classified as error'
            assert result.errorSelectors() == ['/ManagedElement[id="ME1"]']
            assert result.broadcastSelectors().isEmpty()
            assert result.notReadySelectors().isEmpty()
            assert result.dmiSelectors().isEmpty()
    }

    def 'Classify selector with #scenario'() {
        given: 'a cm handle that is not READY'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(_, '/') >> 'ch-1'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> createYangModelCmHandle('ch-1', 'dmi-1', 'p1', cmHandleState)
        when: 'selectors are classified'
            def result = objectUnderTest.classifySelectors('/ManagedElement[id="ME1"]')
        then: 'the selector is classified as not ready'
            assert result.notReadySelectors() == ['/ManagedElement[id="ME1"]']
            assert result.errorSelectors().isEmpty()
            assert result.broadcastSelectors().isEmpty()
            assert result.dmiSelectors().isEmpty()
        where: 'the following scenarios apply'
            scenario               | cmHandleState
            'target is LOCKED'     | CmHandleState.LOCKED
            'target state is null' | null
    }

    def 'Classify selector with search term matching multiple CM handles across different DMIs'() {
        given: 'two CM handles whose alternate IDs contain "Ireland"'
            def yangModelCmHandle1 = createYangModelCmHandle('ch-1', 'dmi-plugin-1', 'producer-A', CmHandleState.READY)
            def yangModelCmHandle2 = createYangModelCmHandle('ch-2', 'dmi-plugin-2', 'producer-B', CmHandleState.READY)
            mockAlternateIdMatcher.getCmHandleIds('ManagedElement=Ireland') >> ['ch-1', 'ch-2']
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> yangModelCmHandle1
            mockInventoryPersistence.getYangModelCmHandle('ch-2') >> yangModelCmHandle2
        when: 'a selector is classified'
            def result = objectUnderTest.classifySelectors('/ManagedElement[contains(id, "Ireland")]')
        then: 'the selector is grouped under both DMI services'
            def containsSelector = '/ManagedElement[contains(id, "Ireland")]'
            assert result.broadcastSelectors().isEmpty()
            assert result.dmiSelectors() == [
                'dmi-plugin-1': [(new ProducerKey('dmi-plugin-1', 'producer-A')): [containsSelector]],
                'dmi-plugin-2': [(new ProducerKey('dmi-plugin-2', 'producer-B')): [containsSelector]]
            ]
    }

    def 'Classify selector with search term matching multiple CM handles on same DMI'() {
        given: 'two CM handles on the same DMI whose alternate IDs contain "Ireland"'
            def yangModelCmHandle1 = createYangModelCmHandle('ch-1', 'dmi-plugin-1', 'producer-A', CmHandleState.READY)
            def yangModelCmHandle2 = createYangModelCmHandle('ch-2', 'dmi-plugin-1', 'producer-A', CmHandleState.READY)
            mockAlternateIdMatcher.getCmHandleIds('ManagedElement=Ireland') >> ['ch-1', 'ch-2']
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> yangModelCmHandle1
            mockInventoryPersistence.getYangModelCmHandle('ch-2') >> yangModelCmHandle2
        when: 'a contains selector is classified'
            def result = objectUnderTest.classifySelectors('/ManagedElement[contains(id, "Ireland")]')
        then: 'the selector appears twice under the single DMI and producer key'
            def containsSelector = '/ManagedElement[contains(id, "Ireland")]'
            assert result.dmiSelectors() == [
                'dmi-plugin-1': [(new ProducerKey('dmi-plugin-1', 'producer-A')): [containsSelector, containsSelector]]
            ]
    }

    def 'Classify selector with search term as error when no CM handles match'() {
        given: 'no CM handles contain the search term'
            mockAlternateIdMatcher.getCmHandleIds('ManagedElement=Ireland') >> []
        when: 'a contains selector is classified'
            def result = objectUnderTest.classifySelectors('/ManagedElement[contains(id, "Ireland")]')
        then: 'the selector is classified as error'
            assert result.errorSelectors() == ['/ManagedElement[contains(id, "Ireland")]']
            assert result.broadcastSelectors().isEmpty()
            assert result.dmiSelectors().isEmpty()
    }

    def 'Classify contains selector with mix of READY and not READY CM handles'() {
        given: 'one READY and one LOCKED CM handle matching the search term'
            def readyCmHandle = createYangModelCmHandle('ch-1', 'dmi-plugin-1', 'producer-A', CmHandleState.READY)
            def lockedCmHandle = createYangModelCmHandle('ch-2', 'dmi-plugin-1', 'producer-A', CmHandleState.LOCKED)
            mockAlternateIdMatcher.getCmHandleIds('ManagedElement=Ireland') >> ['ch-1', 'ch-2']
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> readyCmHandle
            mockInventoryPersistence.getYangModelCmHandle('ch-2') >> lockedCmHandle
        when: 'a contains selector is classified'
            def containsSelector = '/ManagedElement[contains(id, "Ireland")]'
            def result = objectUnderTest.classifySelectors(containsSelector)
        then: 'the selector appears in both dmiSelectors and notReadySelectors'
            assert result.dmiSelectors() == [
                'dmi-plugin-1': [(new ProducerKey('dmi-plugin-1', 'producer-A')): [containsSelector]]
            ]
            assert result.notReadySelectors() == [containsSelector]
            assert result.errorSelectors().isEmpty()
    }

    def 'Classify selector when FDN cannot be extracted'() {
        when: 'a selector with an unparseable expression is classified'
            def result = objectUnderTest.classifySelectors('/ManagedElement[id=no-quotes]')
        then: 'the selector is classified as error'
            assert result.errorSelectors() == ['/ManagedElement[id=no-quotes]']
            assert result.broadcastSelectors().isEmpty()
    }

    def 'Classify selector for #scenario'() {
        when: 'selectors are classified'
            def result = objectUnderTest.classifySelectors(dataNodeSelector)
        then: 'all lists are empty'
            assert result.broadcastSelectors().isEmpty()
            assert result.errorSelectors().isEmpty()
            assert result.notReadySelectors().isEmpty()
            assert result.dmiSelectors().isEmpty()
        where: 'the following inputs are used'
            scenario | dataNodeSelector
            'null'   | null
            'empty'  | ''
    }

    def createYangModelCmHandle(id, dmiServiceName, dataProducerIdentifier, cmHandleState) {
        def compositeState = cmHandleState == null ? null : new CompositeState(cmHandleState: cmHandleState)
        return new YangModelCmHandle(id: id, dmiServiceName: dmiServiceName,
                dataProducerIdentifier: dataProducerIdentifier, compositeState: compositeState)
    }
}
