/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.integration.functional.ncmp.data

import org.onap.cps.integration.base.CpsIntegrationSpecBase

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
            def response = performGet('/ncmp/v1/ch/ch-1/data/ds/ncmp-datastore:passthrough-operational',
                    [resourceIdentifier: 'parent/child', options: '(a=1,b=2)'])
        then: 'response is successful'
            assert response.statusCode.is2xxSuccessful()
        and: 'verify that DMI received the request with the correctly encoded URL'
            assert dmiDispatcher1.dmiResourceDataUrl == '/dmi/v1/ch/ch-1/data/ds/ncmp-datastore%3Apassthrough-operational?resourceIdentifier=parent%2Fchild&options=%28a%3D1%2Cb%3D2%29'
    }

    def 'DMI URL encoding for pass-through running data operations with POST request'() {
        when: 'sending a pass-through data request to NCMP with POST'
            def response = performPost('/ncmp/v1/ch/ch-1/data/ds/ncmp-datastore:passthrough-running',
                    '{ "some-json": "data" }', [resourceIdentifier: 'parent/child'])
        then: 'response is successful'
            assert response.statusCode.is2xxSuccessful()
        and: 'verify that DMI received the request with the correctly encoded URL'
            assert dmiDispatcher1.dmiResourceDataUrl == '/dmi/v1/ch/ch-1/data/ds/ncmp-datastore%3Apassthrough-running?resourceIdentifier=parent%2Fchild'
    }
}
