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

package org.onap.cps.integration.base

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper

import java.util.concurrent.TimeUnit

/**
 * This class simulates responses from the Policy Execution server in NCMP integration tests.
 */
class PolicyDispatcher extends Dispatcher {

    def objectMapper = new ObjectMapper()
    def expectedAuthorizationToken = 'ABC'
    def allowAll = true; // Prevents legacy test being affected

    @Override
    MockResponse dispatch(RecordedRequest recordedRequest) {

        if (!allowAll && !recordedRequest.getHeader('Authorization').contains(expectedAuthorizationToken)) {
            return new MockResponse().setResponseCode(401)
        }

        if (recordedRequest.path != '/operation-permission/v1/permissions') {
            return new MockResponse().setResponseCode(400)
        }

        def body = objectMapper.readValue(recordedRequest.getBody().readUtf8(), Map.class)
        def targetIdentifier = body.get('operations').get(0).get('targetIdentifier')
        def responseAsMap = [:]
        responseAsMap.put('id',1)
        if (targetIdentifier == "mock slow response") {
            TimeUnit.SECONDS.sleep(2) // One second more then configured readTimeoutInSeconds
        }
        if (allowAll || targetIdentifier == 'fdn1') {
            responseAsMap.put('permissionResult','allow')
            responseAsMap.put('message','')
        } else {
            responseAsMap.put('permissionResult','deny from mock server (dispatcher)')
            responseAsMap.put('message','I only like fdn1')
        }
        def responseAsString = objectMapper.writeValueAsString(responseAsMap)

        return mockResponseWithBody(HttpStatus.OK, responseAsString)
    }

    static mockResponseWithBody(status, responseBody) {
        return new MockResponse()
                .setResponseCode(status.value())
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .setBody(responseBody)
    }
}
