/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.utils

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeStateBuilder
import org.onap.cps.ncmp.api.models.ResourceDataBatchRequest
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import spock.lang.Specification

class ResourceDataBatchRequestUtilsSpec extends Specification {

    @SpringBean
    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    def 'Process per operation in batch request with #serviceName.'() {
        given: 'batch request with 3 operations'
            def resourceDataBatchRequestJsonData = TestUtils.getResourceFileContent('resourceDataBatchRequest.json')
            def resourceDataBatchRequest = jsonObjectMapper.convertJsonString(resourceDataBatchRequestJsonData, ResourceDataBatchRequest.class)
        and: '4 known cm handles: ch1-dmi1, ch2-dmi1, ch3-dmi2, ch4-dmi2'
            def yangModelCmHandles = getYangModelCmHandles()
        when: 'Operation in batch request is processed'
            def operationsOutPerDmiServiceName = ResourceDataBatchRequestUtils.processPerOperationInBatchRequest(resourceDataBatchRequest, yangModelCmHandles)
        and: 'converted to a json node'
            def dmiBatchRequestBody = jsonObjectMapper.asJsonString(operationsOutPerDmiServiceName.get(serviceName))
            def dmiBatchRequestBodyAsJsonNode = jsonObjectMapper.convertToJsonNode(dmiBatchRequestBody).get(operationIndex)
        then: 'it contains the correct operation details'
            assert dmiBatchRequestBodyAsJsonNode.get('operation').asText() == 'read'
            assert dmiBatchRequestBodyAsJsonNode.get('operationId').asText() == expectedOperationId
            assert dmiBatchRequestBodyAsJsonNode.get('datastore').asText() == expectedDatastore
        and: 'the correct cm handles (just for #serviceName)'
            assert dmiBatchRequestBodyAsJsonNode.get('cmHandles').size() == expectedCmHandleIds.size()
            expectedCmHandleIds.each {
                dmiBatchRequestBodyAsJsonNode.get('cmHandles').toString().contains(it)
            }
        where: 'the following dmi service is applied'
            serviceName | operationIndex || expectedOperationId | expectedDatastore                        | expectedCmHandleIds
            'dmi1'      | 0              || 'operational-14'    | 'ncmp-datastore:passthrough-operational' | ['ch6-dmi1']
            'dmi1'      | 1              || 'running-12'        | 'ncmp-datastore:passthrough-running'     | ['ch1-dmi1', 'ch2-dmi1']
            'dmi1'      | 2              || 'operational-15'    | 'ncmp-datastore:passthrough-operational' | ['ch8-dmi1']
            'dmi2'      | 0              || 'operational-14'    | 'ncmp-datastore:passthrough-operational' | ['ch3-dmi2']
            'dmi2'      | 1              || 'running-12'        | 'ncmp-datastore:passthrough-running'     | ['ch7-dmi2']
            'dmi2'      | 2              || 'operational-15'    | 'ncmp-datastore:passthrough-operational' | ['ch4-dmi2']
    }

    static def getYangModelCmHandles() {
        def dmiProperties = [new YangModelCmHandle.Property('prop', 'some DMI property')]
        def readyState = new CompositeStateBuilder().withCmHandleState(CmHandleState.READY).withLastUpdatedTimeNow().build()
        return [new YangModelCmHandle(id: 'ch1-dmi1', dmiServiceName: 'dmi1', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'ch2-dmi1', dmiServiceName: 'dmi1', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'ch6-dmi1', dmiServiceName: 'dmi1', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'ch8-dmi1', dmiServiceName: 'dmi1', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'ch3-dmi2', dmiServiceName: 'dmi2', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'ch4-dmi2', dmiServiceName: 'dmi2', dmiProperties: dmiProperties, compositeState: readyState),
                new YangModelCmHandle(id: 'ch7-dmi2', dmiServiceName: 'dmi2', dmiProperties: dmiProperties, compositeState: readyState),
        ]
    }
}
