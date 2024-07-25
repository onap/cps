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
import org.springframework.http.MediaType

import static org.springframework.http.HttpMethod.DELETE
import static org.springframework.http.HttpMethod.GET
import static org.springframework.http.HttpMethod.PATCH
import static org.springframework.http.HttpMethod.POST
import static org.springframework.http.HttpMethod.PUT
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class DmiUrlEncodingPassthroughSpec extends CpsIntegrationSpecBase {

    def setup() {
        dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']
        registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG)
    }

    def cleanup() {
        deregisterCmHandle(DMI1_URL, 'ch-1')
    }

    def 'DMI URL encoding for pass-through operational data operations with GET request'() {
        when: 'sending a GET pass-through data request to NCMP'
            mvc.perform(request(GET, '/ncmp/v1/ch/ch-1/data/ds/ncmp-datastore:passthrough-operational')
                    .queryParam('resourceIdentifier', 'parent/child')
                    .queryParam('options', '(a=1,b=2)'))
                    .andExpect(status().is2xxSuccessful())
        then: 'verify that DMI received the request with the correctly encoded URL'
            assert dmiDispatcher1.dmiResourceDataUrl == '/dmi/v1/ch/ch-1/data/ds/ncmp-datastore%3Apassthrough-operational?resourceIdentifier=parent%2Fchild&options=%28a%3D1%2Cb%3D2%29'
    }

    def 'DMI URL encoding for pass-through running data operations with #httpMethod requests'() {
        when: 'sending a pass-through data request to NCMP with various HTTP methods'
            mvc.perform(request(httpMethod, '/ncmp/v1/ch/ch-1/data/ds/ncmp-datastore:passthrough-running')
                    .queryParam('resourceIdentifier', 'parent/child')
                    .contentType(MediaType.APPLICATION_JSON)
                    .content('{ "some-json": "data" }'))
                    .andExpect(status().is2xxSuccessful())
        then: 'verify that DMI received the request with the correctly encoded URL'
            assert dmiDispatcher1.dmiResourceDataUrl == '/dmi/v1/ch/ch-1/data/ds/ncmp-datastore%3Apassthrough-running?resourceIdentifier=parent%2Fchild'
        where: 'testing various HTTP methods'
            httpMethod << [POST, PUT, PATCH, DELETE]
    }
}
