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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.RecordedRequest
import org.jetbrains.annotations.NotNull
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.springframework.http.MediaType

class DmiRestClientErrorHandlingSpec extends CpsIntegrationSpecBase {

    def setup() {
        mockDmiServer.setDispatcher(new Dispatcher() {
            @Override
            MockResponse dispatch(@NotNull RecordedRequest request) throws InterruptedException {
                if (request.path == '/some') {
                    return new MockResponse().setResponseCode(HttpStatus.BAD_REQUEST.value())
                }
                return new MockResponse().setResponseCode(HttpStatus.OK.value())
            }
        })
    }

    def 'Bearer token is passed from NCMP to DMI in pass-through data operations.'() {
        when: 'a pass-through data request is sent to NCMP with a bearer token'
        mvc.perform(request(httpMethod, '/some')
                .contentType(MediaType.APPLICATION_JSON)
                .content('{ "some-json": "data" }')
                .header(HttpHeaders.AUTHORIZATION, 'Bearer some-bearer-token'))
                .andExpect(status().isBadRequest())

        then: 'DMI has received request with bearer token'

        where: 'all HTTP operations are applied'
        httpMethod << [GET]
    }
}
