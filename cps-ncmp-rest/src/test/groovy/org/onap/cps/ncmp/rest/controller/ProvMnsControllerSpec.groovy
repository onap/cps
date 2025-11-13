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
import org.onap.cps.ncmp.api.data.models.OperationType
import org.onap.cps.ncmp.api.exceptions.ProvMnSException
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.exceptions.NoAlternateIdMatchFoundException
import org.onap.cps.ncmp.impl.data.policyexecutor.PolicyExecutor
import org.onap.cps.ncmp.impl.dmi.DmiRestClient
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.provmns.model.PatchItem
import org.onap.cps.ncmp.impl.provmns.model.PatchOperation
import org.onap.cps.ncmp.impl.provmns.model.ResourceOneOf
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher
import org.onap.cps.ncmp.rest.provmns.ErrorResponseBuilder
import org.onap.cps.ncmp.impl.provmns.ParametersBuilder
import org.onap.cps.ncmp.impl.provmns.ParameterMapper
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.READY
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.ADVISED
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put

@WebMvcTest(ProvMnsController)
class ProvMnsControllerSpec extends Specification {

    @SpringBean
    ParametersBuilder parametersBuilder = new ParametersBuilder()

    @SpringBean
    AlternateIdMatcher alternateIdMatcher = Mock()

    @SpringBean
    InventoryPersistence inventoryPersistence = Mock()

    @SpringBean
    DmiRestClient dmiRestClient = Mock()

    @SpringBean
    ErrorResponseBuilder errorResponseBuilder = new ErrorResponseBuilder()

    @SpringBean
    ParameterMapper parameterMapper = new ParameterMapper()

    @SpringBean
    PolicyExecutor policyExecutor = Mock()

    @Autowired
    MockMvc mvc

    @SpringBean
    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    def jsonBody = jsonObjectMapper.asJsonString(new ResourceOneOf('test'))
    def patchJsonBody = jsonObjectMapper.asJsonString([new PatchItem(op: 'REMOVE', path: 'someUriLdnFirstPart')])

    @Value('${rest.api.provmns-base-path}')
    def provMnSBasePath

    def 'Get resource data request where #scenario'() {
        given: 'resource data url'
            def getUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId?attributes=[test,query,param]"
        and: 'alternate Id can be matched'
            alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/someUriLdnFirstPart/someClassName=someId', "/") >> 'cm-1'
        and: 'persistence service returns yangModelCmHandle'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(id:'cm-1', dmiServiceName: 'someDmiService', dataProducerIdentifier: dataProducerId, compositeState: new CompositeState(cmHandleState: state))
        and: 'dmi provides a response'
            dmiRestClient.synchronousGetOperation(*_) >> new ResponseEntity<>('Some response from DMI service', HttpStatus.OK)
        when: 'get data resource request is performed'
            def response = mvc.perform(get(getUrl).contentType(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'response status as expected'
            assert response.status == expectedHttpStatus.value()
        where:
            scenario                                                 | dataProducerId       | state   || expectedHttpStatus
            'cmHandle state is Ready with populated dataProducerId'  | 'someDataProducerId' | READY   || HttpStatus.OK
            'dataProducerId is blank'                                | ' '                  | READY   || HttpStatus.UNPROCESSABLE_ENTITY
            'dataProducerId is null'                                 | null                 | READY   || HttpStatus.UNPROCESSABLE_ENTITY
            'cmHandle state is Advised'                              | 'someDataProducerId' | ADVISED || HttpStatus.NOT_ACCEPTABLE
    }

    def 'Get resource data request with no match for alternate id'() {
        given: 'resource data url'
            def getUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
        and: 'alternate Id cannot be matched'
            alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(*_) >> {throw new NoAlternateIdMatchFoundException('someUriLdnFirstPart/someClassName=someId')}
        and: 'persistence service returns yangModelCmHandle'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(id:'cm-1', dmiServiceName: 'someDmiService', dataProducerIdentifier: 'someDataProducerId', compositeState: new CompositeState(cmHandleState: ADVISED))
        when: 'get data resource request is performed'
            def response = mvc.perform(get(getUrl).contentType(MediaType.APPLICATION_JSON)).andReturn().response
        then: 'response status is NOT_FOUND (404)'
            assert response.status == HttpStatus.NOT_FOUND.value()
    }

    def 'Patch request where #scenario'() {
        given: 'provmns url'
            def provmnsUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
        and: 'alternate Id can be matched'
            alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/someUriLdnFirstPart/someClassName=someId', "/") >> 'cm-1'
        and: 'persistence service returns yangModelCmHandle'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(id:'cm-1', dmiServiceName: 'someDmiService', dataProducerIdentifier: dataProducerId, compositeState: new CompositeState(cmHandleState: state))
        and: 'dmi provides a response'
            dmiRestClient.synchronousPatchOperation(*_) >> new ResponseEntity<>('Some response from DMI service', HttpStatus.OK)
        when: 'patch request is performed'
            def response = mvc.perform(patch(provmnsUrl)
                    .contentType(new MediaType('application', 'json-patch+json'))
                    .content(patchJsonBody))
                    .andReturn().response
        then: 'response status is as expected'
            assert response.status == expectedHttpStatus.value()
        where:
            scenario                    | dataProducerId       | state   || expectedHttpStatus
            'valid request is made'     | 'someDataProducerId' | READY   || HttpStatus.OK
            'dataProducerId is blank'   | ' '                  | READY   || HttpStatus.UNPROCESSABLE_ENTITY
            'dataProducerId is null'    | null                 | READY   || HttpStatus.UNPROCESSABLE_ENTITY
            'cmHandle state is Advised' | 'someDataProducerId' | ADVISED || HttpStatus.NOT_ACCEPTABLE
    }

    def 'Patch request with no match for alternate id'() {
        given: 'resource data url'
            def provmnsUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
        and: 'alternate Id cannot be matched'
            alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(*_) >> {throw new NoAlternateIdMatchFoundException('someUriLdnFirstPart/someClassName=someId')}
        and: 'persistence service returns valid yangModelCmHandle'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(id:'cm-1', dmiServiceName: 'someDmiService', dataProducerIdentifier: 'someDataProducerId', compositeState: new CompositeState(cmHandleState: READY))
        when: 'patch request is performed'
            def response = mvc.perform(patch(provmnsUrl)
                    .contentType(new MediaType('application', 'json-patch+json'))
                    .content(patchJsonBody))
                    .andReturn().response
        then: 'response status is NOT_FOUND (404)'
            assert response.status == HttpStatus.NOT_FOUND.value()
    }

    def 'Patch request with no permission from coordination management'() {
        given: 'resource data url'
            def url = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
        and: 'alternate Id can be matched'
            alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/someUriLdnFirstPart/someClassName=someId', "/") >> 'cm-1'
        and: 'persistence service returns valid yangModelCmHandle'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(id:'cm-1', dmiServiceName: 'someDmiService', dataProducerIdentifier: 'someDataProducerId', compositeState: new CompositeState(cmHandleState: READY))
        and: 'policy executor throws exception (denied)'
            policyExecutor.checkPermission(*_) >> {throw new RuntimeException()}
        when: 'put data resource request is performed'
            def response = mvc.perform(patch(url)
                    .contentType(new MediaType('application', 'json-patch+json'))
                    .content(patchJsonBody))
                    .andReturn().response
        then: 'response status is NOT_ACCEPTABLE (406)'
            assert response.status == HttpStatus.NOT_ACCEPTABLE.value()
    }

    def 'Put resource data request where #scenario'() {
        given: 'resource data url'
            def putUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
        and: 'alternate Id can be matched'
            alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/someUriLdnFirstPart/someClassName=someId', "/") >> 'cm-1'
        and: 'persistence service returns yangModelCmHandle'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(id:'cm-1', dmiServiceName: 'someDmiService', dataProducerIdentifier: dataProducerId, compositeState: new CompositeState(cmHandleState: state))
        and: 'dmi provides a response'
            dmiRestClient.synchronousPutOperation(*_) >> new ResponseEntity<>('Some response from DMI service', HttpStatus.OK)
        when: 'put data resource request is performed'
            def response = mvc.perform(put(putUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonBody))
                    .andReturn().response
        then: 'response status is as expected'
            assert response.status == expectedHttpStatus.value()
        where:
            scenario                    | dataProducerId       | state   || expectedHttpStatus
            'valid request is made'     | 'someDataProducerId' | READY   || HttpStatus.OK
            'dataProducerId is blank'   | ' '                  | READY   || HttpStatus.UNPROCESSABLE_ENTITY
            'dataProducerId is null'    | null                 | READY   || HttpStatus.UNPROCESSABLE_ENTITY
            'cmHandle state is Advised' | 'someDataProducerId' | ADVISED || HttpStatus.NOT_ACCEPTABLE
    }

    def 'Put resource data request with no match for alternate id'() {
        given: 'resource data url'
            def putUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
        and: 'alternate Id cannot be matched'
            alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(*_) >> {throw new NoAlternateIdMatchFoundException('someUriLdnFirstPart/someClassName=someId')}
        and: 'persistence service returns valid yangModelCmHandle'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(id:'cm-1', dmiServiceName: 'someDmiService', dataProducerIdentifier: 'someDataProducerId', compositeState: new CompositeState(cmHandleState: READY))
        when: 'put data resource request is performed'
            def response = mvc.perform(put(putUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonBody))
                    .andReturn().response
        then: 'response status is NOT_FOUND (404)'
            assert response.status == HttpStatus.NOT_FOUND.value()
    }

    def 'Put resource data request with no permission from coordination management'() {
        given: 'resource data url'
            def putUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
        and: 'alternate Id can be matched'
            alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/someUriLdnFirstPart/someClassName=someId', "/") >> 'cm-1'
        and: 'persistence service returns valid yangModelCmHandle'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(id:'cm-1', dmiServiceName: 'someDmiService', dataProducerIdentifier: 'someDataProducerId', compositeState: new CompositeState(cmHandleState: READY))
        and: 'policy executor throws exception (denied)'
            policyExecutor.checkPermission(*_) >> {throw new RuntimeException()}
        when: 'put data resource request is performed'
            def response = mvc.perform(put(putUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonBody))
                    .andReturn().response
        then: 'response status is NOT_ACCEPTABLE (406)'
            assert response.status == HttpStatus.NOT_ACCEPTABLE.value()
    }

    def 'Delete resource data request where #scenario'() {
        given: 'resource data url'
            def deleteUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
        and: 'alternate Id can be matched'
            alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/someUriLdnFirstPart/someClassName=someId', "/") >> 'cm-1'
        and: 'persistence service returns yangModelCmHandle'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(id:'cm-1', dmiServiceName: 'someDmiService', dataProducerIdentifier: dataProducerId, compositeState: new CompositeState(cmHandleState: state))
        and: 'dmi provides a response'
            dmiRestClient.synchronousDeleteOperation(*_) >> new ResponseEntity<>('Some response from DMI service', HttpStatus.OK)
        when: 'get data resource request is performed'
            def response = mvc.perform(delete(deleteUrl)).andReturn().response
        then: 'response status as expected'
            assert response.status == expectedHttpStatus.value()
        where:
            scenario                    | dataProducerId       | state   || expectedHttpStatus
            'valid request is made'     | 'someDataProducerId' | READY   || HttpStatus.OK
            'dataProducerId is blank'   | ' '                  | READY   || HttpStatus.UNPROCESSABLE_ENTITY
            'dataProducerId is null'    | null                 | READY   || HttpStatus.UNPROCESSABLE_ENTITY
            'cmHandle state is Advised' | 'someDataProducerId' | ADVISED || HttpStatus.NOT_ACCEPTABLE
    }

    def 'Delete resource data request with no match for alternate id'() {
        given: 'resource data url'
            def deleteUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
        and: 'alternate Id cannot be matched'
            alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(*_) >> {throw new NoAlternateIdMatchFoundException('someUriLdnFirstPart/someClassName=someId')}
        and: 'persistence service returns valid yangModelCmHandle'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(id:'cm-1', dmiServiceName: 'someDmiService', dataProducerIdentifier: 'someDataProducerId', compositeState: new CompositeState(cmHandleState: READY))
        when: 'delete data resource request is performed'
            def response = mvc.perform(delete(deleteUrl)).andReturn().response
        then: 'response status is NOT_FOUND (404)'
            assert response.status == HttpStatus.NOT_FOUND.value()
    }

    def 'Delete resource data request with no permission from coordination management'() {
        given: 'resource data url'
            def deleteUrl = "$provMnSBasePath/v1/someUriLdnFirstPart/someClassName=someId"
        and: 'alternate Id can be matched'
            alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId('/someUriLdnFirstPart/someClassName=someId', "/") >> 'cm-1'
        and: 'persistence service returns valid yangModelCmHandle'
            inventoryPersistence.getYangModelCmHandle('cm-1') >> new YangModelCmHandle(id:'cm-1', dmiServiceName: 'someDmiService', dataProducerIdentifier: 'someDataProducerId', compositeState: new CompositeState(cmHandleState: READY))
        and: 'policy executor throws exception (denied)'
            policyExecutor.checkPermission(*_) >> {throw new RuntimeException()}
        when: 'delete data resource request is performed'
            def response = mvc.perform(delete(deleteUrl)).andReturn().response
        then: 'response status is NOT_ACCEPTABLE (406)'
            assert response.status == HttpStatus.NOT_ACCEPTABLE.value()
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
}
