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

package org.onap.cps.integration.functional

import static org.springframework.http.HttpMethod.GET
import static org.springframework.http.HttpMethod.DELETE
import static org.springframework.http.HttpMethod.PATCH
import static org.springframework.http.HttpMethod.POST
import static org.springframework.http.HttpMethod.PUT
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.jetbrains.annotations.NotNull
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import spock.util.concurrent.PollingConditions

class NcmpBearerTokenPassthroughSpec extends CpsIntegrationSpecBase {

    def lastAuthHeaderReceived = null

    def setup() {
        dmiDispatcher.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']
        registerCmHandle(DMI_URL, 'ch-1', NO_MODULE_SET_TAG)

        mockDmiServer.setDispatcher(new Dispatcher() {
            @Override
            MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
                if (request.path == '/actuator/health') {
                        return new MockResponse()
                                .addHeader("Content-Type", MediaType.APPLICATION_JSON).setBody('{"status":"UP"}')
                                .setResponseCode(HttpStatus.OK.value())
                } else {
                    lastAuthHeaderReceived = request.getHeader('Authorization')
                    return new MockResponse().setResponseCode(HttpStatus.OK.value())
                }
            }
        })
    }

    def cleanup() {
        deregisterCmHandle(DMI_URL, 'ch-1')
    }

    def 'Bearer token is passed from NCMP to DMI in pass-through data operations.'() {
        when: 'a pass-through data request is sent to NCMP with a bearer token'
            mvc.perform(request(httpMethod, '/ncmp/v1/ch/ch-1/data/ds/ncmp-datastore:passthrough-running')
                    .queryParam('resourceIdentifier', 'my-resource-id')
                    .contentType(MediaType.APPLICATION_JSON)
                    .content('{ "some-json": "data" }')
                    .header(HttpHeaders.AUTHORIZATION, 'Bearer some-bearer-token'))
                    .andExpect(status().is2xxSuccessful())

        then: 'DMI has received request with bearer token'
            lastAuthHeaderReceived == 'Bearer some-bearer-token'

        where: 'all HTTP operations are applied'
            httpMethod << [GET, POST, PUT, PATCH, DELETE]
    }

    def 'Basic auth header is NOT passed from NCMP to DMI in pass-through data operations.'() {
        when: 'a pass-through data request is sent to NCMP with basic authentication'
            mvc.perform(request(httpMethod, '/ncmp/v1/ch/ch-1/data/ds/ncmp-datastore:passthrough-running')
                    .queryParam('resourceIdentifier', 'my-resource-id')
                    .contentType(MediaType.APPLICATION_JSON)
                    .content('{ "some-json": "data" }')
                    .header(HttpHeaders.AUTHORIZATION, 'Basic Y3BzdXNlcjpjcHNyMGNrcyE='))
                    .andExpect(status().is2xxSuccessful())

        then: 'DMI has received request with no authorization header'
            lastAuthHeaderReceived == null

        where: 'all HTTP operations are applied'
            httpMethod << [GET, POST, PUT, PATCH, DELETE]
    }

    def 'Bearer token is passed from NCMP to DMI in async batch pass-through data operation.'() {
        when: 'a pass-through async data request is sent to NCMP with a bearer token'
            def requestBody = """{"operations": [{
                "operation": "read",
                "operationId": "operational-1",
                "datastore": "ncmp-datastore:passthrough-running",
                "resourceIdentifier": "my-resource-id",
                "targetIds": ["ch-1"]
            }]}"""
        mvc.perform(request(POST, '/ncmp/v1/data')
                    .queryParam('topic', 'my-topic')
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestBody)
                    .header(HttpHeaders.AUTHORIZATION, 'Bearer some-bearer-token'))
                    .andExpect(status().is2xxSuccessful())

        then: 'DMI will receive the async request with bearer token'
            new PollingConditions().within(3, () -> {
                assert lastAuthHeaderReceived == 'Bearer some-bearer-token'
            })
    }

}
