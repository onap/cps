/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

package org.onap.cps.policyexecutor.stub.controller

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import org.onap.cps.policyexecutor.stub.model.Operation
import org.onap.cps.policyexecutor.stub.model.PermissionRequest
import org.onap.cps.policyexecutor.stub.model.PermissionResponse
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post

@WebMvcTest(PolicyExecutorStubController)
class PolicyExecutorStubControllerSpec extends Specification {

    @Autowired
    MockMvc mockMvc

    @SpringBean
    ObjectMapper spiedObjectMapper = Spy(new ObjectMapper())

    @SpringBean
    Sleeper spiedSleeper = Spy()

    def url = '/operation-permission/v1/permissions'

    def 'Permission request with #resourceIdentifier.'() {
        given: 'a permission request with target: #targetIdentifier'
            def requestBody = createRequestBody(resourceIdentifier)
        when: 'request is posted'
            def response = mockMvc.perform(post(url)
                    .header('Authorization','some string')
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andReturn().response
        then: 'response status is Ok'
            assert response.status == HttpStatus.OK.value()
        and: 'the response body has the expected decision details'
            def responseBody = response.contentAsString
            def permissionResponse = spiedObjectMapper.readValue(responseBody, PermissionResponse.class)
            assert permissionResponse.id == expectedId
            assert permissionResponse.permissionResult == expectedResult
            assert permissionResponse.message == expectedMessage
        where: 'the following targets are used'
            resourceIdentifier                             || expectedId | expectedResult | expectedMessage
            'some fdn'                                     || '1'        | 'allow'        | 'all good'
            'prefix/policySimulation=slowResponse_1'       || '2'        | 'allow'        | 'all good'
            'prefix/policySimulation=policyResponse_deny'  || '3'        | 'deny'         | 'Stub is mocking a policy response: deny'
            'prefix/policySimulation=policyResponse_other' || '4'        | 'other'        | 'Stub is mocking a policy response: other'
    }

    def 'Permission request with a HTTP error code.'() {
        given: 'a permission request with a target fdn to simulate an http error code'
            def requestBody = createRequestBody('segment1=1/policySimulation=httpError_418')
        when: 'request is posted'
            def response = mockMvc.perform(post(url)
                .header('Authorization','some string')
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andReturn().response
        then: 'response status the same error code as in target fdn'
            assert response.status == 418
    }

    def 'Permission request without authorization header.'() {
        given: 'a valid permission request'
            def requestBody = createRequestBody('some target')
        when: 'request is posted without authorization header'
            def response = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andReturn().response
        then: 'response status is OK'
            assert response.status == HttpStatus.OK.value()
    }

    def 'Permission request with no operations.'() {
        given: 'a permission request with no operations'
            def permissionRequest = new PermissionRequest('some decision type', [])
            def requestBody = spiedObjectMapper.writeValueAsString(permissionRequest)
        when: 'request is posted'
            def response = mockMvc.perform(post(url)
                .header('Authorization','some string')
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andReturn().response
        then: 'response status is Bad Request'
            assert response.status == HttpStatus.BAD_REQUEST.value()
    }

    def 'Request with invalid json for request data.'() {
        when: 'request with invalid json is posted'
            def response = mockMvc.perform(post(url)
                .header('Authorization','some string')
                .contentType(MediaType.APPLICATION_JSON)
                .content('invalid json'))
                .andReturn().response
        then: 'response status is Bad Request'
            assert response.status == HttpStatus.BAD_REQUEST.value()
    }

    def 'Exception thrown while printing pretty json.'() {
        given: 'object writer for pretty json throws an exception'
            def mockObjectWriter = Mock(ObjectWriter)
            spiedObjectMapper.writerWithDefaultPrettyPrinter() >> mockObjectWriter
            mockObjectWriter.writeValueAsString(_) >> { throw new JsonProcessingException('test') }
        when: 'request is posted'
            def response = mockMvc.perform(post(url)
                .header('Authorization','some string')
                .contentType(MediaType.APPLICATION_JSON)
                .content(createRequestBody('some fdn')))
                .andReturn().response
        then: 'response status is OK (exception is ignored)'
            assert response.status == HttpStatus.OK.value()
    }


    def 'Permission request with interrupted exception during slow response.'() {
        given: 'a permission request with a target fdn to simulate a slow response'
            def requestBody = createRequestBody('policySimulation=slowResponse_5')
            spiedSleeper.haveALittleRest(_) >> { throw new InterruptedException() }
        when: 'request is posted'
            mockMvc.perform(post(url)
                .header('Authorization','some string')
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        then: 'response status is Bad Request'
            noExceptionThrown()
    }

    def 'Permission request with missing or invalid attributes.'() {
        given: 'Permission request with operation=#operation and resourceIdentifier=#resourceIdentifier'
            def requestBody = createRequestBody(operation, 'some resource', changeRequest)
        when: 'request is posted'
            def response = mockMvc.perform(post(url)
                .header('Authorization','something')
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andReturn().response
        then: 'response status as expected'
            assert response.status == expectedStatus.value()
        where: 'following parameters are used'
            operation | changeRequest || expectedStatus
            'delete'  | null          || HttpStatus.OK
            'other'   | '{}'          || HttpStatus.OK
            'other'   | null          || HttpStatus.BAD_REQUEST
    }

    def createRequestBody(resourceIdentifier) {
        return createRequestBody('delete', resourceIdentifier, '{}')
    }

    def createRequestBody(operationName, resourceIdentifier, changeRequest) {
        def operation = new Operation(operationName, 'some cm-handle')
        operation.setChangeRequest(changeRequest)
        operation.setResourceIdentifier(resourceIdentifier)
        def permissionRequest = new PermissionRequest('cm-legacy', [operation])
        return spiedObjectMapper.writeValueAsString(permissionRequest)
    }

}
