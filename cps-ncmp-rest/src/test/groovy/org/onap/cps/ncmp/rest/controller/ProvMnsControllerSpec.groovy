/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.cps.ncmp.api.data.models.OperationType
import org.onap.cps.ncmp.api.inventory.models.CompositeStateBuilder
import org.onap.cps.ncmp.impl.data.policyexecutor.PolicyExecutor
import org.onap.cps.ncmp.impl.dmi.DmiRestClient
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher
import org.onap.cps.ncmp.rest.provmns.model.ResourceOneOf
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.READY
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

@WebMvcTest(ProvMnsController)
class ProvMnsControllerSpec extends Specification {

    @Autowired
    MockMvc mvc

    @SpringBean
    AlternateIdMatcher alternateIdMatcher = Mock()

    @SpringBean
    DmiRestClient dmiRestClient = Mock()

    @SpringBean
    PolicyExecutor policyExecutor = Mock()

    @SpringBean
    InventoryPersistence inventoryPersistence = Mock()

    @SpringBean
    JsonObjectMapper jsonObjectMapper = Mock()

    @Value('${rest.api.provmns-base-path}')
    def provMnSBasePath

    def 'Get Resource Data from provmns interface.'() {
        given: 'resource data url'
            def getUrl = "$provMnSBasePath/v1/A=1/B=2/C=3"
        when: 'get data resource request is performed'
            def response = mvc.perform(get(getUrl).contentType(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'response status is Not Implemented (501)'
            assert response.status == HttpStatus.NOT_IMPLEMENTED.value()
    }

    def 'Put Resource Data from provmns interface.'() {
        given: 'resource data url'
            def putUrl = "$provMnSBasePath/v1/A=1/B=2/C=3"
        and: 'an example resource json object'
            def jsonBody = jsonObjectMapper.asJsonString(new ResourceOneOf('test'))
        when: 'put data resource request is performed'
            def response = mvc.perform(put(putUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonBody))
                    .andReturn().response
        then: 'response status is Not Implemented (501)'
            assert response.status == HttpStatus.NOT_IMPLEMENTED.value()
    }

    def 'Patch Resource Data from provmns interface.'() {
        given: 'resource data url'
            def patchUrl = "$provMnSBasePath/v1/A=1/B=2/C=3"
        and: 'an example resource json object'
            def jsonBody = jsonObjectMapper.asJsonString(new ResourceOneOf('test'))
        when: 'patch data resource request is performed'
            def response = mvc.perform(patch(patchUrl)
                    .contentType(new MediaType('application', 'json-patch+json'))
                    .content(jsonBody))
                    .andReturn().response
        then: 'response status is Not Implemented (501)'
            assert response.status == HttpStatus.NOT_IMPLEMENTED.value()
    }

    def 'Delete Resource Data from provmns interface.'() {
        given: 'resource data url'
            def deleteUrl = "$provMnSBasePath/v1/A=1/B=2/C=3"
            def readyState = new CompositeStateBuilder().withCmHandleState(READY).withLastUpdatedTimeNow().build()
        and: 'a cm handle found by alternate id'
            alternateIdMatcher.getCmHandleId("A=1/B=2") >> "cm-1"
            inventoryPersistence.getYangModelCmHandle("cm-1") >> new YangModelCmHandle(dmiServiceName: "sampleDmiService", dataProducerIdentifier: "some-producer", compositeState: readyState)
            1 * dmiRestClient.synchronousDeleteOperationWithJsonData(*_)
        and: 'the policy executor invoked'
            1 * policyExecutor.checkPermission(_, OperationType.DELETE, null, "A=1/B=2", _)
        when: 'delete data resource request is performed'
            def response = mvc.perform(delete(deleteUrl)).andReturn().response
        then: 'response status is OK (200)'
            assert response.status == HttpStatus.OK.value()
    }

    def 'Get Resource Data from provmns interface with query param.'() {
        given: 'resource data url with query parameter'
            def getUrl = "$provMnSBasePath/v1/A=1/B=2/C=3?attributes=[test,query,param]"
        when: 'get data resource request is performed'
            def response = mvc.perform(get(getUrl).contentType(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'response status is Not Implemented (501)'
            assert response.status == HttpStatus.NOT_IMPLEMENTED.value()
    }
}
