/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.rest.controller

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.impl.data.policyexecutor.OperationDetailsFactory
import org.onap.cps.ncmp.impl.data.policyexecutor.PolicyExecutor
import org.onap.cps.ncmp.impl.dmi.DmiRestClient
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.provmns.ParametersBuilder
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.ADVISED
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.READY

@WebMvcTest([ProvMnSController, OperationDetailsFactory])
abstract class ProvMnSControllerBaseSpec extends Specification {

    @SpringBean
    ParametersBuilder parametersBuilder = new ParametersBuilder()

    @SpringBean
    AlternateIdMatcher mockAlternateIdMatcher = Mock()

    @SpringBean
    InventoryPersistence mockInventoryPersistence = Mock()

    @SpringBean
    DmiRestClient mockDmiRestClient = Mock()

    @Autowired
    OperationDetailsFactory operationDetailsFactory

    @SpringBean
    PolicyExecutor mockPolicyExecutor = Mock()

    @Autowired
    MockMvc mvc

    @SpringBean
    ObjectMapper objectMapper = new ObjectMapper()

    @SpringBean
    JsonObjectMapper spiedJsonObjectMapper = Spy(new JsonObjectMapper(objectMapper))

    @Value('${rest.api.provmns-base-path}')
    def provMnSBasePath

    @Value('${app.ncmp.provmns.max-patch-operations:10}')
    int maxNumberOfPatchOperations

    static def resourceAsJson = '{"id":"test", "objectClass": "Test", "attributes": { "attr1": "value1"} }'
    static def validCmHandle = new YangModelCmHandle(id:'ch-1', alternateId: '/managedElement=1', dmiServiceName: 'someDmiService', dataProducerIdentifier: 'someDataProducerId', compositeState: new CompositeState(cmHandleState: READY))
    static def cmHandleWithoutDataProducer = new YangModelCmHandle(id:'ch-1', dmiServiceName: 'someDmiService', compositeState: new CompositeState(cmHandleState: READY))
    static def cmHandleNotReady = new YangModelCmHandle(id:'ch-1', dmiServiceName: 'someDmiService', dataProducerIdentifier: 'someDataProducerId', compositeState: new CompositeState(cmHandleState: ADVISED))

    static def patchMediaType = new MediaType('application', 'json-patch+json')
    static def patchMediaType3gpp = new MediaType('application', '3gpp-json-patch+json')
    static def patchJsonBody = '[{"op":"replace","path":"/otherChild=id2/attributes","value":{"attr1":"test"}}]'
    static def patchWithoutChild = '[{"op":"replace","path":"/attributes","value":{"attr2":"test2"}}]'
    static def patchJsonBody3gpp = '[{"op":"replace","path":"/otherChild=id2#/attributes/attr1","value":"test"}]'

    static def expectedDeleteChangeRequest = '{"":[]}'
    static def NO_CONTENT = ''

    def mockedCmHandle = Mock(YangModelCmHandle)

    def setup() {
        mockedCmHandle.getCompositeState() >> new CompositeState(cmHandleState: READY)
        mockedCmHandle.getDataProducerIdentifier() >> 'some-dataproducerId'
    }

    def setupMocksForCmHandle(fdn, cmHandleId = 'ch-1') {
        mockAlternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(fdn, "/") >> cmHandleId
        mockInventoryPersistence.getYangModelCmHandle(cmHandleId) >> validCmHandle
    }
}
