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

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType

import static org.springframework.http.HttpMethod.POST
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request

class PolicyExecutorIntegrationSpec extends CpsIntegrationSpecBase {

    def setup() {
        //minimum setup for 2 cm handles with alternate ids
        dmiDispatcher1.moduleNamesPerCmHandleId = ['ch-1': [], 'ch-2': []]
        registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, 'fdn1')
        registerCmHandle(DMI1_URL, 'ch-2', NO_MODULE_SET_TAG, 'fdn2')
    }

    def cleanup() {
        deregisterCmHandle(DMI1_URL, 'ch-1')
        deregisterCmHandle(DMI1_URL, 'ch-2')
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
        where: 'following parameters are used'
            scenario                | cmHandle | authorization         || execpectedStatusCode
            'accepted cm handle'    | 'ch-1'   | 'mock expects "ABC"'  || 201
            'un-accepted cm handle' | 'ch-2'   | 'mock expects "ABC"'  || 409
            'invalid authorization' | 'ch-1'   | 'something else'      || 500
    }

}
