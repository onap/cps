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
            assert result.broadcast().isEmpty()
            assert result.error().isEmpty()
            assert result.notReady().isEmpty()
            assert result.dmiSelectors() == [
                'dmi-plugin-1': [(new ProducerKey('dmi-plugin-1', 'producer-A')): ['/ManagedElement[id="ME1"]']]
            ]
    }

    def 'Classify selectors as broadcast for #scenario'() {
        when: 'selectors are classified'
            def result = objectUnderTest.classifySelectors(dataNodeSelector)
        then: 'all selectors are classified as broadcast'
            assert result.broadcast() == expectedBroadcast
            assert result.dmiSelectors().isEmpty()
        where: 'the following selectors are used'
            scenario                               | dataNodeSelector                            || expectedBroadcast
            'deep search'                          | '//ManagedElement[contains(id, "Ireland")]' || ['//ManagedElement[contains(id, "Ireland")]']
            'no ManagedElement or MeContext id'    | '/SubNetwork[id="SN1"]'                     || ['/SubNetwork[id="SN1"]']
            'ManagedElement without id'            | '/SubNetwork[id="SN1"]/ManagedElement'      || ['/SubNetwork[id="SN1"]/ManagedElement']
            'multiple broadcast selectors'         | '/SubNetwork[id="SN1"] OR //ManagedElement' || ['/SubNetwork[id="SN1"]', '//ManagedElement']
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

    def 'Classify selector as error when target not found'() {
        given: 'alternate id matcher throws exception'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(_, '/') >> { throw new NoAlternateIdMatchFoundException('test') }
        when: 'selectors are classified'
            def result = objectUnderTest.classifySelectors('/ManagedElement[id="ME1"]')
        then: 'the selector is classified as error'
            assert result.error() == ['/ManagedElement[id="ME1"]']
            assert result.broadcast().isEmpty()
            assert result.notReady().isEmpty()
            assert result.dmiSelectors().isEmpty()
    }

    def 'Classify selector as not ready when #scenario'() {
        given: 'a cm handle that is not READY'
            mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(_, '/') >> 'ch-1'
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> createYangModelCmHandle('ch-1', 'dmi-1', 'p1', cmHandleState)
        when: 'selectors are classified'
            def result = objectUnderTest.classifySelectors('/ManagedElement[id="ME1"]')
        then: 'the selector is classified as not ready'
            assert result.notReady() == ['/ManagedElement[id="ME1"]']
            assert result.error().isEmpty()
            assert result.broadcast().isEmpty()
            assert result.dmiSelectors().isEmpty()
        where: 'the following scenarios apply'
            scenario               | cmHandleState
            'target is LOCKED'     | CmHandleState.LOCKED
            'target state is null' | null
    }

    def 'Classify contains selector matching multiple CM handles across different DMIs'() {
        given: 'two CM handles whose alternate IDs contain "Ireland"'
            def yangModelCmHandle1 = createYangModelCmHandle('ch-1', 'dmi-plugin-1', 'producer-A', CmHandleState.READY)
            def yangModelCmHandle2 = createYangModelCmHandle('ch-2', 'dmi-plugin-2', 'producer-B', CmHandleState.READY)
            mockAlternateIdMatcher.getCmHandleIdsByAlternateIdContaining('Ireland') >> ['ch-1', 'ch-2']
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >> yangModelCmHandle1
            mockInventoryPersistence.getYangModelCmHandle('ch-2') >> yangModelCmHandle2
        when: 'a contains selector is classified'
            def result = objectUnderTest.classifySelectors('/ManagedElement[contains(id, "Ireland")]')
        then: 'the selector is grouped under both DMI services'
            def containsSelector = '/ManagedElement[contains(id, "Ireland")]'
            assert result.broadcast().isEmpty()
            assert result.dmiSelectors() == [
                'dmi-plugin-1': [(new ProducerKey('dmi-plugin-1', 'producer-A')): [containsSelector]],
                'dmi-plugin-2': [(new ProducerKey('dmi-plugin-2', 'producer-B')): [containsSelector]]
            ]
    }

    def 'Classify contains selector matching multiple CM handles on same DMI'() {
        given: 'two CM handles on the same DMI whose alternate IDs contain "Ireland"'
            def yangModelCmHandle1 = createYangModelCmHandle('ch-1', 'dmi-plugin-1', 'producer-A', CmHandleState.READY)
            def yangModelCmHandle2 = createYangModelCmHandle('ch-2', 'dmi-plugin-1', 'producer-A', CmHandleState.READY)
            mockAlternateIdMatcher.getCmHandleIdsByAlternateIdContaining('Ireland') >> ['ch-1', 'ch-2']
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

    def 'Classify contains selector as error when no CM handles match'() {
        given: 'no CM handles contain the search term'
            mockAlternateIdMatcher.getCmHandleIdsByAlternateIdContaining('Ireland') >> []
        when: 'a contains selector is classified'
            def result = objectUnderTest.classifySelectors('/ManagedElement[contains(id, "Ireland")]')
        then: 'the selector is classified as error'
            assert result.error() == ['/ManagedElement[contains(id, "Ireland")]']
            assert result.broadcast().isEmpty()
            assert result.dmiSelectors().isEmpty()
    }

    def 'Classify contains selector as not ready when matched CM handle is not READY'() {
        given: 'a CM handle matching the search term but not READY'
            mockAlternateIdMatcher.getCmHandleIdsByAlternateIdContaining('Ireland') >> ['ch-1']
            mockInventoryPersistence.getYangModelCmHandle('ch-1') >>
                createYangModelCmHandle('ch-1', 'dmi-1', 'p1', CmHandleState.LOCKED)
        when: 'a contains selector is classified'
            def result = objectUnderTest.classifySelectors('/ManagedElement[contains(id, "Ireland")]')
        then: 'the selector is classified as not ready'
            assert result.notReady() == ['/ManagedElement[contains(id, "Ireland")]']
            assert result.error().isEmpty()
    }

    def 'Classify selector as error when FDN cannot be extracted'() {
        when: 'a selector with an unparseable expression is classified'
            def result = objectUnderTest.classifySelectors('/ManagedElement[id=no-quotes]')
        then: 'the selector is classified as error'
            assert result.error() == ['/ManagedElement[id=no-quotes]']
            assert result.broadcast().isEmpty()
    }

    def 'Classify empty or null selector for #scenario'() {
        when: 'selectors are classified'
            def result = objectUnderTest.classifySelectors(dataNodeSelector)
        then: 'all lists are empty'
            assert result.broadcast().isEmpty()
            assert result.error().isEmpty()
            assert result.notReady().isEmpty()
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
