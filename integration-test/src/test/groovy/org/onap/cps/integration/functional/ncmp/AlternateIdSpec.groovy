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
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

class AlternateIdSpec extends CpsIntegrationSpecBase {

    def setup() {
        dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']
        registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, 'alternateId')
    }

    def cleanup() {
        deregisterCmHandle(DMI1_URL, 'ch-1')
    }

    def 'AlternateId in pass-through data operations should return OK status.'() {
        given: 'the URL for the pass-through data request'
            def url = '/ncmp/v1/ch/alternateId/data/ds/ncmp-datastore:passthrough-running'
        when: 'a pass-through data request is sent to NCMP'
            def response = mvc.perform(get(url)
                    .queryParam('resourceIdentifier', 'my-resource-id')
                    .contentType(MediaType.APPLICATION_JSON))
                    .andReturn().response
        then: 'response status is Ok'
            assert response.status == HttpStatus.OK.value()
    }



}
