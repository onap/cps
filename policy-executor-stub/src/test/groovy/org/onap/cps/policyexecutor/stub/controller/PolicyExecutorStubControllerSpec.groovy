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
import org.onap.cps.policyexecutor.stub.model.NcmpDelete
import org.onap.cps.policyexecutor.stub.model.PolicyExecutionRequest
import org.onap.cps.policyexecutor.stub.model.PolicyExecutionResponse
import org.onap.cps.policyexecutor.stub.model.Request
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

    def url = '/policy-executor/api/v1/some-action'

    def 'Execute policy action.'() {
        given: 'a policy execution request with target: #targetIdentifier'
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
            def policyExecutionResponse = objectMapper.readValue(responseBody, PolicyExecutionResponse.class)
            assert policyExecutionResponse.decisionId == expectedDecsisonId
            assert policyExecutionResponse.decision == expectedDecision
            assert policyExecutionResponse.message == expectedMessage
        where: 'the following targets are used'
            targetIdentifier        || expectedDecsisonId | expectedDecision | expectedMessage
            'some fdn'              || '1'                | 'deny'           | "Only FDNs containing 'cps-is-great' are allowed"
            'fdn with cps-is-great' || '2'                | 'allow'          | 'All good'
    }

    def 'Execute policy action with a HTTP error code.'() {
        given: 'a policy execution request with a target fdn with a 3-digit error code'
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

    def 'Execute policy action without authorization header.'() {
        given: 'a valid policy execution request'
            def requestBody = createRequestBody('some target')
        when: 'request is posted without authorization header'
            def response = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andReturn().response
        then: 'response status is OK'
            assert response.status == HttpStatus.OK.value()
    }

    def 'Execute policy action with no requests.'() {
        given: 'a policy execution request'
            def policyExecutionRequest = new PolicyExecutionRequest('some decision type', [])
            def requestBody = objectMapper.writeValueAsString(policyExecutionRequest)
        when: 'request is posted'
            def response = mockMvc.perform(post(url)
                .header('Authorization','some string')
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andReturn().response
        then: 'response status is Bad Request'
            assert response.status == HttpStatus.BAD_REQUEST.value()
    }

    def 'Execute policy action with invalid json for request data.'() {
        given: 'a policy execution request'
            def request = new Request('ncmp-delete-schema:1.0.0', 'invalid json')
            def policyExecutionRequest = new PolicyExecutionRequest('some decision type', [request])
            def requestBody = objectMapper.writeValueAsString(policyExecutionRequest)
        when: 'request is posted'
            def response = mockMvc.perform(post(url)
                .header('Authorization','some string')
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andReturn().response
        then: 'response status is Bad Request'
            assert response.status == HttpStatus.BAD_REQUEST.value()
    }

    def 'Execute policy action with missing or invalid attributes.'() {
        given: 'a policy execution request with decisionType=#decisionType, schema=#schema, targetIdentifier=#targetIdentifier'
            def requestBody = createRequestBody(decisionType, schema, targetIdentifier)
        when: 'request is posted'
            def response = mockMvc.perform(post(url)
                .header('Authorization','something')
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andReturn().response
        then: 'response status as expected'
            assert response.status == expectedStatus.value()
        where: 'following parameters are used'
            decisionType | schema                     | targetIdentifier || expectedStatus
            'something'  | 'ncmp-delete-schema:1.0.0' | 'something'      || HttpStatus.OK
            null         | 'ncmp-delete-schema:1.0.0' | 'something'      || HttpStatus.BAD_REQUEST
            'something'  | 'other schema'             | 'something'      || HttpStatus.BAD_REQUEST
            'something'  | 'ncmp-delete-schema:1.0.0' | null             || HttpStatus.BAD_REQUEST
    }

    def createRequestBody(decisionType, schema, targetIdentifier) {
        def ncmpDelete = new NcmpDelete(targetIdentifier: targetIdentifier)
        def request = new Request(schema, objectMapper.writeValueAsString(ncmpDelete))
        def policyExecutionRequest = new PolicyExecutionRequest(decisionType, [request])
        return objectMapper.writeValueAsString(policyExecutionRequest)
    }

    def createRequestBody(targetIdentifier) {
        return createRequestBody('some decision type', 'ncmp-delete-schema:1.0.0', targetIdentifier)
    }

}
