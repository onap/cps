/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.integration.functional.ncmp

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

import static org.springframework.http.HttpMethod.POST
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request

class PolicyExecutorIntegrationSpec extends CpsIntegrationSpecBase {

    def objectMapper = new ObjectMapper()

    def setup() {
        // Enable mocked policy executor logic
        policyDispatcher.allowAll = false;
        //minimum setup for cm handles with alternate ids
        dmiDispatcher1.moduleNamesPerCmHandleId = ['ch-1': [], 'ch-2': [], 'ch-3':[]]
        registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, 'fdn1')
        registerCmHandle(DMI1_URL, 'ch-2', NO_MODULE_SET_TAG, 'fdn2')
        registerCmHandle(DMI1_URL, 'ch-3', NO_MODULE_SET_TAG, 'mock slow response')
    }

    def cleanup() {
        deregisterSequenceOfCmHandles(DMI1_URL, 3, 1)
    }

    def 'Policy Executor create request with #scenario.'() {
        when: 'a pass-through write request is sent to NCMP'
            def response = mvc.perform(request(POST, "/ncmp/v1/ch/$cmHandle/data/ds/ncmp-datastore:passthrough-running")
                    .queryParam('resourceIdentifier', 'my-resource-id')
                    .contentType(MediaType.APPLICATION_JSON)
                    .content('{ "some-json": "data" }')
                    .header(HttpHeaders.AUTHORIZATION, authorization))
                    .andReturn().response
        then: 'the expected status code is returned'
            response.getStatus() == execpectedStatusCode
        and: 'when not allowed the response body contains the expected message'
            if (expectedMessage!='allow') {
                def bodyAsMap = objectMapper.readValue(response.getContentAsByteArray(), Map.class)
                assert bodyAsMap.get('message').endsWith(expectedMessage)
            }
        where: 'following parameters are used'
            scenario                | cmHandle | authorization         || execpectedStatusCode || expectedMessage
            'accepted cm handle'    | 'ch-1'   | 'mock expects "ABC"'  || 201                  || 'allow'
            'un-accepted cm handle' | 'ch-2'   | 'mock expects "ABC"'  || 409                  || 'deny from mock server (dispatcher)'
            'timeout'               | 'ch-3'   | 'mock expects "ABC"'  || 409                  || 'test default decision'
            'invalid authorization' | 'ch-1'   | 'something else'      || 409                  || 'test default decision'
    }

}
