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
import jakarta.servlet.ServletException
import org.onap.cps.ncmp.api.inventory.models.CmHandleState
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.exceptions.NoAlternateIdMatchFoundException
import org.onap.cps.ncmp.impl.data.policyexecutor.PolicyExecutor
import org.onap.cps.ncmp.impl.dmi.DmiRestClient
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.provmns.model.Resource
import org.onap.cps.ncmp.impl.provmns.model.ResourceOneOf
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher
import org.onap.cps.ncmp.rest.provmns.exception.InvalidPathException
import org.onap.cps.ncmp.rest.util.ProvMnSErrorResponseBuilder
import org.onap.cps.ncmp.rest.util.ProvMnSParametersMapper
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
import org.springframework.util.MultiValueMap
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

@WebMvcTest(ProvMnsController)
class ProvMnsControllerSpec extends Specification {

    @SpringBean
    ProvMnSParametersMapper provMnSParametersMapper = new ProvMnSParametersMapper(new ProvMnSErrorResponseBuilder(),new JsonObjectMapper(new ObjectMapper()), new ObjectMapper())

    @SpringBean
    AlternateIdMatcher alternateIdMatcher = Mock()

    @SpringBean
    InventoryPersistence inventoryPersistence = Mock()

    @SpringBean
    DmiRestClient dmiRestClient = Mock()

    @SpringBean
    ProvMnSErrorResponseBuilder provMnSErrorResponseBuilder = new ProvMnSErrorResponseBuilder()

    @SpringBean
    PolicyExecutor policyExecutor = Mock()

    @Autowired
    MockMvc mvc

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    @Value('${rest.api.provmns-base-path}')
    def provMnSBasePath

    def 'Get Resource Data from provmns interface #scenario.'() {
        given: 'resource data url'
            def getUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"+path
        and: 'request classes return correct information'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(dmiServiceName: 'someDmiService', dataProducerIdentifier: 'someUriLdnFirstPart/someClassName=someId', compositeState: new CompositeState(cmHandleState: CmHandleState.READY))
            alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('someUriLdnFirstPart/someClassName=someId', "/") >> 'cm-1'
            dmiRestClient.synchronousGetOperation(*_) >> new ResponseEntity<Object>(HttpStatusCode.valueOf(200))
        when: 'get data resource request is performed'
            def response = mvc.perform(get(getUrl).contentType(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'response status is OK (200)'
            assert response.status == HttpStatus.OK.value()
        where:
            scenario                 | path
            'with no query params'   | ''
            'with query params'      | '?attributes=[test,query,param]'
    }

    def 'Patch Resource Data from provmns interface.'() {
        given: 'resource data url'
            def patchUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
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

    def 'Put Resource Data from provmns interface.'() {
        given: 'resource data url'
            def putUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
        and: 'an example resource json object'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(dmiServiceName: 'someDmiService', dataProducerIdentifier: 'someUriLdnFirstPart/someClassName=someId', compositeState: new CompositeState(cmHandleState: CmHandleState.READY))
            alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('someUriLdnFirstPart/someClassName=someId', "/") >> 'cm-1'
            dmiRestClient.synchronousGetOperation(*_) >> new ResponseEntity<Resource>(new ResourceOneOf('test') as Resource, ['type': 'application/json'] as MultiValueMap<String, String>, HttpStatusCode.valueOf(200))
            def jsonBody = jsonObjectMapper.asJsonString(new ResourceOneOf('test'))
        when: 'put data resource request is performed'
            def response = mvc.perform(put(putUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonBody))
                    .andReturn().response
        then: 'response status is OK (200)'
            assert response.status == HttpStatus.OK.value()
    }

    def 'Delete Resource Data from provmns interface.'() {
        given: 'resource data url'
            def deleteUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
        when: 'delete data resource request is performed'
            def response = mvc.perform(delete(deleteUrl)).andReturn().response
        then: 'response status is Not Implemented (501)'
            assert response.status == HttpStatus.NOT_IMPLEMENTED.value()
    }

    def 'Invalid path passed in to provmns interface, #scenario'() {
        given: 'an invalid path'
            def url = "$provMnSBasePath/v1/" + invalidPath
        when: 'get data resource request is performed'
            def response = mvc.perform(get(url).contentType(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'invalid path exception is thrown'
            thrown(ServletException)
        where:
            scenario                     | invalidPath
            'Missing URI-LDN-first-part' | 'someClassName=someId'
            'Missing ClassName and Id'   | 'someUriLdnFirstPart/'
    }

    def 'Get Resource Data from provmns interface with cmhandle not in READY state'() {
        given: 'resource data url'
            def getUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
        and: 'request classes return correct information'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(id:'cm-1', dmiServiceName: 'someDmiService', dataProducerIdentifier: 'someUriLdnFirstPart/someClassName=someId', compositeState: new CompositeState(cmHandleState: CmHandleState.ADVISED))
            alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('someUriLdnFirstPart/someClassName=someId', "/") >> 'cm-1'
        when: 'get data resource request is performed'
            def response = mvc.perform(get(getUrl).contentType(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'response status is OK (200)'
            assert response.status == HttpStatus.NOT_ACCEPTABLE.value()
    }

    def 'Get Resource Data from provmns interface with no match for alternate id'() {
        given: 'resource data url'
            def getUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
        and: 'request classes return correct information'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(id:'cm-1', dmiServiceName: 'someDmiService', dataProducerIdentifier: 'someUriLdnFirstPart/someClassName=someId', compositeState: new CompositeState(cmHandleState: CmHandleState.ADVISED))
            alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('someUriLdnFirstPart/someClassName=someId', "/") >> {throw new NoAlternateIdMatchFoundException("someUriLdnFirstPart/someClassName=someId")}
        when: 'get data resource request is performed'
            def response = mvc.perform(get(getUrl).contentType(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'response status is NOT_FOUND (404)'
            assert response.status == HttpStatus.NOT_FOUND.value()
    }

    def 'Get Resource Data from provmns interface with no data producer id'() {
        given: 'resource data url'
            def getUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
        and: 'request classes return correct information'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(id:'cm-1', dmiServiceName: 'someDmiService', compositeState: new CompositeState(cmHandleState: CmHandleState.READY))
            alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('someUriLdnFirstPart/someClassName=someId', "/") >> 'cm-1'
        when: 'get data resource request is performed'
            def response = mvc.perform(get(getUrl).contentType(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'response status is UNPROCESSABLE_ENTITY (422)'
            assert response.status == HttpStatus.UNPROCESSABLE_ENTITY.value()
    }

    def 'Put Resource Data from provmns interface with no match for alternate id'() {
        given: 'resource data url'
            def putUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
        and: 'request classes return correct information'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(id:'cm-1', dmiServiceName: 'someDmiService', dataProducerIdentifier: 'someUriLdnFirstPart/someClassName=someId', compositeState: new CompositeState(cmHandleState: CmHandleState.ADVISED))
            alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('someUriLdnFirstPart/someClassName=someId', "/") >> {throw new NoAlternateIdMatchFoundException("someUriLdnFirstPart/someClassName=someId")}
            def jsonBody = jsonObjectMapper.asJsonString(new ResourceOneOf('test'))
        when: 'put data resource request is performed'
            def response = mvc.perform(put(putUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonBody))
                    .andReturn().response
        then: 'response status is NOT_FOUND (404)'
            assert response.status == HttpStatus.NOT_FOUND.value()
    }

    def 'Put Resource Data from provmns interface with no permission from coordination management'() {
        given: 'resource data url'
            def putUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
        and: 'request classes return correct information'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(id:'cm-1', dmiServiceName: 'someDmiService', dataProducerIdentifier: 'someUriLdnFirstPart/someClassName=someId', compositeState: new CompositeState(cmHandleState: CmHandleState.READY))
            alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('someUriLdnFirstPart/someClassName=someId', "/") >> 'cm-1'
            policyExecutor.checkPermission(*_) >> {throw new RuntimeException()}
            def jsonBody = jsonObjectMapper.asJsonString(new ResourceOneOf('test'))
        when: 'put data resource request is performed'
            def response = mvc.perform(put(putUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonBody))
                    .andReturn().response
        then: 'response status is NOT_ACCEPTABLE (406)'
            assert response.status == HttpStatus.NOT_ACCEPTABLE.value()
    }

    def 'Put Resource Data from provmns interface with no data producer id'() {
        given: 'resource data url'
            def putUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
        and: 'request classes return correct information'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(id:'cm-1', dmiServiceName: 'someDmiService', compositeState: new CompositeState(cmHandleState: CmHandleState.READY))
            alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('someUriLdnFirstPart/someClassName=someId', "/") >> 'cm-1'
            def jsonBody = jsonObjectMapper.asJsonString(new ResourceOneOf('test'))
        when: 'get data resource request is performed'
            def response = mvc.perform(put(putUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonBody))
                    .andReturn().response
        then: 'response status is UNPROCESSABLE_ENTITY (422)'
            assert response.status == HttpStatus.UNPROCESSABLE_ENTITY.value()
    }

    def 'Patch Resource Data from provmns interface with no match for alternate id'() {
        given: 'resource data url'
            def patchUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
        and: 'request classes return correct information'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(id:'cm-1', dmiServiceName: 'someDmiService', dataProducerIdentifier: 'someUriLdnFirstPart/someClassName=someId', compositeState: new CompositeState(cmHandleState: CmHandleState.ADVISED))
         alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('someUriLdnFirstPart/someClassName=someId', "/") >> {throw new NoAlternateIdMatchFoundException("someUriLdnFirstPart/someClassName=someId")}
            def jsonBody = jsonObjectMapper.asJsonString(new ResourceOneOf('test'))
        when: 'patch data resource request is performed'
            def response = mvc.perform(patch(patchUrl)
                    .contentType("application/json-patch+json")
                    .content(jsonBody))
                    .andReturn().response
        then: 'response status is NOT_FOUND (404)'
            assert response.status == HttpStatus.NOT_FOUND.value()
    }
}
