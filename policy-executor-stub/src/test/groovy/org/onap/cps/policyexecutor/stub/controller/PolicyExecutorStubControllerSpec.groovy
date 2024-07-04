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
import org.onap.cps.policyexecutor.stub.model.Payload
import org.onap.cps.policyexecutor.stub.model.PolicyExecutionRequest
import org.onap.cps.policyexecutor.stub.model.PolicyExecutionResponse
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

    def 'Execute Policy Action.'() {
        given: 'a policy execution request with target fdn: #targetFdn'
            def payload = new Payload(targetFdn: targetFdn)
            def policyExecutionRequest = new PolicyExecutionRequest(payload: [payload])
            def requestBody = objectMapper.writeValueAsString(policyExecutionRequest)
        when: 'request is posted'
            def response = mockMvc.perform(post(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody))
                    .andReturn().response
        then: 'response status is Ok'
            assert response.status == HttpStatus.OK.value()
        and: 'the response body has the expected decision details'
            def responseBody = response.contentAsString
            def policyExecutionResponse = objectMapper.readValue(responseBody, PolicyExecutionResponse.class)
            assert policyExecutionResponse.decisionId == expectedDecsisonId
            assert policyExecutionResponse.decision == expectedDecsison
            assert policyExecutionResponse.message == expectedMessage
        where: 'the following targets are used'
            targetFdn               || expectedDecsisonId | expectedDecsison | expectedMessage
            'some fdn'              || '1'                | 'deny'           | "Only FDNs containing 'cps-is-great' are permitted"
            'fdn with cps-is-great' || '2'                | 'permit'          | null
    }

    def 'Execute Policy Action with a HTTP Error Code.'() {
        given: 'a policy execution request with a target fdn with a 3-digit error code'
            def payload = new Payload(targetFdn: 'fdn with error code 418')
            def policyExecutionRequest = new PolicyExecutionRequest(payload: [payload])
            def requestBody = objectMapper.writeValueAsString(policyExecutionRequest)
        when: 'request is posted'
            def response = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andReturn().response
        then: 'response status the same error code as in target fdn'
            assert response.status == 418
    }

    def 'Execute Policy Action with Empty Payload.'() {
        given: 'a policy execution request with a target fdn with a 3-digit error code'
            def policyExecutionRequest = new PolicyExecutionRequest(payload: [])
            def requestBody = objectMapper.writeValueAsString(policyExecutionRequest)
        when: 'request is posted'
            def response = mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andReturn().response
        then: 'response status is Bad Request'
            assert response.status == HttpStatus.BAD_REQUEST.value()
    }

}
