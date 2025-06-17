/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest
import org.onap.cps.ncmp.api.datajobs.models.WriteOperation
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher
import spock.lang.Specification

class WriteRequestExaminerSpec extends Specification {

    def mockAlternateIdMatcher = Mock(AlternateIdMatcher)
    def mockInventoryPersistence = Mock(InventoryPersistence)
    def objectUnderTest = new WriteRequestExaminer(mockAlternateIdMatcher, mockInventoryPersistence)

    def setup() {
        def yangModelCmHandle1 = new YangModelCmHandle(id: 'ch1', dmiServiceName: 'dmiA', additionalProperties: [],
            publicProperties: [], compositeState: null, moduleSetTag: '', alternateId: 'fdn1', dataProducerIdentifier: 'p1')
        def yangModelCmHandle2 = new YangModelCmHandle(id: 'ch2', dmiServiceName: 'dmiA', additionalProperties: [],
            publicProperties: [], compositeState: null, moduleSetTag: '', alternateId: 'fdn2', dataProducerIdentifier: 'p1')
        def yangModelCmHandle3 = new YangModelCmHandle(id: 'ch3', dmiServiceName: 'dmiA', additionalProperties: [],
            publicProperties: [], compositeState: null, moduleSetTag: '', alternateId: 'fdn3', dataProducerIdentifier: 'p2')
        def yangModelCmHandle4 = new YangModelCmHandle(id: 'ch4', dmiServiceName: 'dmiB', additionalProperties: [],
            publicProperties: [], compositeState: null, moduleSetTag: '', alternateId: 'fdn4', dataProducerIdentifier: 'p1')
        mockAlternateIdMatcher.getCmHandleIdsByLongestMatchingAlternateIds(_, '/') > ['ch1','ch2','ch3','ch4']

        mockAlternateIdMatcher.getCmHandleByLongestMatchingAlternateId('fdn1', '/', _) >> yangModelCmHandle1
        mockAlternateIdMatcher.getCmHandleByLongestMatchingAlternateId('fdn2', '/', _) >> yangModelCmHandle2
        mockAlternateIdMatcher.getCmHandleByLongestMatchingAlternateId('fdn3', '/', _) >> yangModelCmHandle3
        mockAlternateIdMatcher.getCmHandleByLongestMatchingAlternateId('fdn4', '/', _) >> yangModelCmHandle4

        mockInventoryPersistence.getYangModelCmHandlesWithoutProperties(_) >>
            [ yangModelCmHandle1, yangModelCmHandle2, yangModelCmHandle3, yangModelCmHandle4 ]
    }

    def 'Create a map of dmi write requests per producer key with #scenario.'() {
        given: 'a write request with some write operations'
            def writeOperations = writeOperationFdns.collect {
                new WriteOperation(it, '', '', null)
            }
        and: 'operations are wrapped in a write request'
            def dataJobWriteRequest = new DataJobWriteRequest(writeOperations)
        when: 'the DMI write operations are split from the request'
            def dmiWriteOperationsPerProducerKey = objectUnderTest.splitDmiWriteOperationsFromRequest('some id', dataJobWriteRequest)
        then: 'we get the expected number of keys and values.'
            def producerKeysAsStrings = dmiWriteOperationsPerProducerKey.keySet().collect {
                it.toString()
            }
            assert producerKeysAsStrings.size() == expectedKeys.size()
            assert expectedKeys.containsAll(producerKeysAsStrings)
        where:
            scenario                                                          | writeOperationFdns               || expectedKeys
            'one fdn'                                                         | ['fdn1']                         || ['dmiA#p1']
            'a duplicated target'                                             | ['fdn1','fdn1']                  || ['dmiA#p1']
            'two different targets'                                           | ['fdn1','fdn2']                  || ['dmiA#p1']
            'two different targets and different producer keys'               | ['fdn1','fdn3']                  || ['dmiA#p1', 'dmiA#p2']
            'two different targets and different DMI services'                | ['fdn1','fdn4']                  || ['dmiA#p1', 'dmiB#p1']
            'many targets with different dmi service names and producer keys' | ['fdn1', 'fdn2', 'fdn3', 'fdn4'] || ['dmiA#p1', 'dmiA#p2', 'dmiB#p1']
    }

    def 'Validate the ordering of the created sub jobs.'() {
        given: 'a few write operations for the same producer'
            def writeOperations = (1..3).collect {
                new WriteOperation('fdn1', '', it.toString(), null)
            }
        and: 'operation is wrapped in a write request'
            def dataJobWriteRequest = new DataJobWriteRequest(writeOperations)
        when: 'the DMI write operations are split from the request'
            def dmiWriteOperations = objectUnderTest.splitDmiWriteOperationsFromRequest('some id', dataJobWriteRequest).values().iterator().next()
        then: 'we get the operation ids in the expected order.'
            assert dmiWriteOperations.operationId == ['1', '2', '3']
    }

    def 'Validate the creation of a ProducerKey with correct dmiservicename.'() {
        given: 'yangModelCmHandles with service name: "#dmiServiceName" and data service name: "#dataServiceName"'
            def yangModelCmHandle = new YangModelCmHandle(dmiServiceName: dmiServiceName, dmiDataServiceName: dataServiceName, dataProducerIdentifier: 'dpi1')
        when: 'the ProducerKey is created'
            def result = objectUnderTest.createProducerKey(yangModelCmHandle).toString()
        then: 'we get the ProducerKey with the correct service name'
            assert result == expectedProducerKey
        where: 'the following services are registered'
            dmiServiceName     | dataServiceName          || expectedProducerKey
            'dmi-service-name' | ''                       || 'dmi-service-name#dpi1'
            ''                 | 'dmi-data-service-name'  || 'dmi-data-service-name#dpi1'
            'dmi-service-name' | 'dmi-data-service-name'  || 'dmi-service-name#dpi1'
    }
}
