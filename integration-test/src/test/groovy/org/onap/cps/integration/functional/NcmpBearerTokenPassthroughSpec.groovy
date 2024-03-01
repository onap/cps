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

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.match.MockRestRequestMatchers

import static org.springframework.http.HttpMethod.*
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class NcmpBearerTokenPassthroughSpec extends CpsIntegrationSpecBase {

    static final MODULE_REFERENCES_RESPONSE_A = readResourceDataFile('mock-dmi-responses/bookStoreAWithModules_M1_M2_Response.json')
    static final MODULE_RESOURCES_RESPONSE_A = readResourceDataFile('mock-dmi-responses/bookStoreAWithModules_M1_M2_ResourcesResponse.json')

    def 'Bearer token is passed from NCMP to DMI in pass-through data operations.'() {
        given: 'a CM-handle is registered'
            registerCmHandle(DMI_URL, 'ch-1', '', MODULE_REFERENCES_RESPONSE_A, MODULE_RESOURCES_RESPONSE_A)

        and: 'DMI will expect to receive a request with a bearer token'
            mockDmiServer.expect(requestTo("$DMI_URL/dmi/v1/ch/ch-1/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=my-resource-id"))
                    .andExpect(MockRestRequestMatchers.header(HttpHeaders.AUTHORIZATION, 'Bearer some-bearer-token'))
                    .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON))

        when: 'a pass-through data request is sent to NCMP'
            mvc.perform(request(httpMethod, '/ncmp/v1/ch/ch-1/data/ds/ncmp-datastore:passthrough-running')
                    .queryParam('resourceIdentifier', 'my-resource-id')
                    .contentType(MediaType.APPLICATION_JSON)
                    .content('{ "some-json": "data" }')
                    .header(HttpHeaders.AUTHORIZATION, 'Bearer some-bearer-token'))
                    .andExpect(status().is2xxSuccessful())

        then: 'DMI has received request with bearer token'
            mockDmiServer.verify()

        cleanup:
            deregisterCmHandle(DMI_URL, 'ch-1')

        where:
            httpMethod << [GET, POST, PUT, PATCH, DELETE]
    }

    def 'Basic auth header is NOT passed from NCMP to DMI in pass-through data operations.'() {
        given: 'a CM-handle is registered'
            registerCmHandle(DMI_URL, 'ch-1', '', MODULE_REFERENCES_RESPONSE_A, MODULE_RESOURCES_RESPONSE_A)

        and: 'DMI will expect to receive a request with no authorization header'
            mockDmiServer.expect(requestTo("$DMI_URL/dmi/v1/ch/ch-1/data/ds/ncmp-datastore:passthrough-running?resourceIdentifier=my-resource-id"))
                    .andExpect(MockRestRequestMatchers.headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                    .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON))

        when: 'a pass-through data request is sent to NCMP with a Basic auth header'
            mvc.perform(request(httpMethod, '/ncmp/v1/ch/ch-1/data/ds/ncmp-datastore:passthrough-running')
                    .queryParam('resourceIdentifier', 'my-resource-id')
                    .contentType(MediaType.APPLICATION_JSON)
                    .content('{ "some-json": "data" }')
                    .header(HttpHeaders.AUTHORIZATION, 'Basic Y3BzdXNlcjpjcHNyMGNrcyE='))
                    .andExpect(status().is2xxSuccessful())

        then: 'DMI has received request with no authorization header'
            mockDmiServer.verify()

        cleanup:
            deregisterCmHandle(DMI_URL, 'ch-1')

        where:
            httpMethod << [GET, POST, PUT, PATCH, DELETE]
    }

}
