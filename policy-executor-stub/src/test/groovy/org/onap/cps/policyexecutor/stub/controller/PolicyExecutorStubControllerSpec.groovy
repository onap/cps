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

import com.fasterxml.jackson.databind.ObjectMapper
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

    @Autowired
    ObjectMapper objectMapper

    @SpringBean
    Sleeper sleeper = Spy()

    def url = '/operation-permission/v1/permissions'

    def setup() {
        PolicyExecutorStubController.slowResponseTimeInSeconds = 1
    }

    def 'Permission request with #targetIdentifier.'() {
        given: 'a permission request with target: #targetIdentifier'
            def requestBody = createRequestBody(targetIdentifier)
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
            def permissionResponse = objectMapper.readValue(responseBody, PermissionResponse.class)
            assert permissionResponse.id == expectedId
            assert permissionResponse.permissionResult == expectedResult
            assert permissionResponse.message == expectedMessage
        where: 'the following targets are used'
            targetIdentifier        || expectedId | expectedResult | expectedMessage
            'some fdn'              || '1'        | 'deny'         | "Only FDNs containing 'cps-is-great' are allowed"
            'fdn with cps-is-great' || '2'        | 'allow'        | 'All good'
            'slow'                  || '3'        | 'deny'         | "Only FDNs containing 'cps-is-great' are allowed"
    }

    def 'Permission request with a HTTP error code.'() {
        given: 'a permission request with a target fdn with a 3-digit error code'
            def requestBody = createRequestBody('target with error code 418')
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
            def requestBody = objectMapper.writeValueAsString(permissionRequest)
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

    def 'Permission request with interrupted exception during slow response.'() {
        given: 'a permission request with target: "slow" (stub will be slow)'
            def requestBody = createRequestBody('slow')
            sleeper.haveALittleRest(_) >> { throw new InterruptedException() }
        when: 'request is posted'
            mockMvc.perform(post(url)
                .header('Authorization','some string')
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
        then: 'response status is Bad Request'
            noExceptionThrown()
    }

    def 'Permission request with missing or invalid attributes.'() {
        given: 'Permission request with operation=#operation and targetIdentifier=#targetIdentifier'
            def requestBody = createRequestBody(operation, targetIdentifier, changeRequest)
        when: 'request is posted'
            def response = mockMvc.perform(post(url)
                .header('Authorization','something')
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andReturn().response
        then: 'response status as expected'
            assert response.status == expectedStatus.value()
        where: 'following parameters are used'
            operation | targetIdentifier | changeRequest || expectedStatus
            'delete'  | 'something'      | null          || HttpStatus.OK
            'other'   | 'something'      | '{}'          || HttpStatus.OK
            'delete'  | null             | null          || HttpStatus.BAD_REQUEST
            'other'   | 'something'      | null          || HttpStatus.BAD_REQUEST
    }

    def createRequestBody(targetIdentifier) {
        return createRequestBody('delete', targetIdentifier, '{}')
    }

    def createRequestBody(operationName, targetIdentifier, changeRequest) {
        def operation = new Operation(operationName, targetIdentifier)
        operation.setChangeRequest(changeRequest)
        def permissionRequest = new PermissionRequest('cm-legacy', [operation])
        return objectMapper.writeValueAsString(permissionRequest)
    }

}
