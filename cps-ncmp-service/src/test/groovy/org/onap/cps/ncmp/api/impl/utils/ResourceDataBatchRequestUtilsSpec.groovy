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

    def 'Process per operation in batch request with #scenario.'() {
        given: 'resource data batch request is given'
            def resourceDataBatchRequestJsonData = TestUtils.getResourceFileContent('resourceDataBatchRequest.json')
            def resourceDataBatchRequest = jsonObjectMapper.convertJsonString(resourceDataBatchRequestJsonData, ResourceDataBatchRequest.class)
        when: 'Operation in batch request is processed'
            def operationsOutPerDmiServiceName = ResourceDataBatchRequestUtils.processPerOperationInBatchRequest(resourceDataBatchRequest, getYangModelCmHandles())
        then: 'operation details are generated for dmi service as expected'
            def dmiBatchRequestBody = jsonObjectMapper.asJsonString(operationsOutPerDmiServiceName.get(scenario))
            def dmiBatchRequestBodyAsJsonNode = jsonObjectMapper.convertToJsonNode(dmiBatchRequestBody).get(operationElement)

            assert dmiBatchRequestBodyAsJsonNode.get('operation').asText() == "read"
            assert dmiBatchRequestBodyAsJsonNode.get('operationId').asText() == operationId
            assert dmiBatchRequestBodyAsJsonNode.get('datastore').asText() == datastore
            assert dmiBatchRequestBodyAsJsonNode.get('cmHandles').size() == numberOfcmHandles

        where: 'the following parameters are used'
            scenario             | operationElement | operationId | datastore                                || numberOfcmHandles
            'dmi service name 1' | 0                | "14"        | "ncmp-datastore:passthrough-running"     || 2
            'dmi service name 2' | 0                | "12"        | "ncmp-datastore:passthrough-operational" || 1
            'dmi service name 2' | 1                | "15"        | "ncmp-datastore:passthrough-operational" || 1
    }

    static def getYangModelCmHandles() {
        def readyState = new CompositeStateBuilder().withCmHandleState(CmHandleState.READY).withLastUpdatedTimeNow().build();

        def dmiProperties = [new YangModelCmHandle.Property('prop', 'some DMI property')]

        def yangModelCmHandleReadyState1 = new YangModelCmHandle(id: 'some-cm-handle', dmiServiceName: 'dmi service name 1', dmiProperties: dmiProperties, compositeState: readyState);
        def yangModelCmHandleReadyState2 = new YangModelCmHandle(id: 'some-cm-handle-1', dmiServiceName: 'dmi service name 1', dmiProperties: dmiProperties, compositeState: readyState);
        def yangModelCmHandleReadyState3 = new YangModelCmHandle(id: 'some-cm-handle-2', dmiServiceName: 'dmi service name 2', dmiProperties: dmiProperties, compositeState: readyState);
        def yangModelCmHandleReadyState4 = new YangModelCmHandle(id: 'some-cm-handle-3', dmiServiceName: 'dmi service name 2', dmiProperties: dmiProperties, compositeState: readyState);
        return [yangModelCmHandleReadyState1, yangModelCmHandleReadyState2, yangModelCmHandleReadyState3, yangModelCmHandleReadyState4]
    }
}
