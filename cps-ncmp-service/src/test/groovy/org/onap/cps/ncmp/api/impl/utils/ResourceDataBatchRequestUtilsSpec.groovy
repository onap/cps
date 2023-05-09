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
import org.onap.cps.ncmp.api.impl.operations.DmiBatchRequestBody
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

    def 'Process per operation in batch request with #scenario.'() {
        given: 'uri variables'
            def resourceDataBatchRequestJsonData = TestUtils.getResourceFileContent('resourceDataBatchRequest.json')
            def resourceDataBatchRequest = jsonObjectMapper.convertJsonString(resourceDataBatchRequestJsonData, ResourceDataBatchRequest.class)
        when: 'a dmi datastore service url is generated'
            def operationsOutPerDmiServiceName = ResourceDataBatchRequestUtils.processPerOperationInBatchRequest(resourceDataBatchRequest, getYangModelCmHandle())
        then: 'service url is generated as expected'
        println(operationsOutPerDmiServiceName)
        assert operationsOutPerDmiServiceName.size()==2

            def firstDmiBatchRequestBody = jsonObjectMapper.asJsonString(operationsOutPerDmiServiceName.get('dmi service name 1'))
            def firstDmiBatchRequestBodyAsJsonNode=jsonObjectMapper.convertToJsonNode(firstDmiBatchRequestBody).get(0);

            assert firstDmiBatchRequestBodyAsJsonNode.get('operationType').asText() == "read"
            assert firstDmiBatchRequestBodyAsJsonNode.get('operationId').asText() == "14"
            assert firstDmiBatchRequestBodyAsJsonNode.get('datastore').asText() == "ncmp-datastore:passthrough-running"
            assert firstDmiBatchRequestBodyAsJsonNode.get('cmHandles').size() == 2

            def secondDmiServiceBatchRequestBody = jsonObjectMapper.asJsonString(operationsOutPerDmiServiceName.get('dmi service name 2'))
            def secondDmiBatchRequestBodyAsJsonNode=jsonObjectMapper.convertToJsonNode(secondDmiServiceBatchRequestBody).get(0);

            assert secondDmiBatchRequestBodyAsJsonNode.get('operationType').asText() == "read"
            assert secondDmiBatchRequestBodyAsJsonNode.get('operationId').asText() == "12"
            assert secondDmiBatchRequestBodyAsJsonNode.get('datastore').asText() == "ncmp-datastore:passthrough-operational"
            assert secondDmiBatchRequestBodyAsJsonNode.get('cmHandles').size() == 1
        where: 'the following parameters are used'
            scenario                | topic               | resourceId   || expectedDmiServiceUrl
            'With valid resourceId' | 'topicParamInQuery' | 'resourceId' || 'dmiServiceName/dmi/v1/ch/cmHandle/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=resourceId&options=optionsParamInQuery&topic=topicParamInQuery'
    }

    static def getYangModelCmHandle() {
        def readyState = new CompositeStateBuilder().withCmHandleState(CmHandleState.READY).withLastUpdatedTimeNow().build();

        def dmiProperties = [new YangModelCmHandle.Property('prop', 'some DMI property')]

        def yangModelCmHandleReadyState1 = new YangModelCmHandle(id: 'ec2e9495679a43c58659c07d87025e72', dmiServiceName: 'dmi service name 1', dmiProperties: dmiProperties, compositeState: readyState);
        def yangModelCmHandleReadyState2 = new YangModelCmHandle(id: '0df4d39af6514d99b816758148389cfd', dmiServiceName: 'dmi service name 1', dmiProperties: dmiProperties, compositeState: readyState);
        def yangModelCmHandleReadyState3 = new YangModelCmHandle(id: '202acb75b4a54e43bb1ff8c0c17a8e08', dmiServiceName: 'dmi service name 2', dmiProperties: dmiProperties, compositeState: readyState);
        def yangModelCmHandleReadyState4 = new YangModelCmHandle(id: 'c722f7371b9945bda007a2079fe50289', dmiServiceName: 'dmi service name 2', dmiProperties: dmiProperties, compositeState: readyState);
        return [yangModelCmHandleReadyState1, yangModelCmHandleReadyState2, yangModelCmHandleReadyState3, yangModelCmHandleReadyState4]
    }

}
