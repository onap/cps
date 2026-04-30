/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.integration.functional.ncmp.inventory

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.springframework.http.HttpStatus

class CmHandleGetNorthBoundRestApiSpec extends CpsIntegrationSpecBase {

    def 'Get CM handle by distinguished name using REST API when #scenario.'() {
        given: 'a CM Handle is registered with an alternate ID'
            registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, '/alt=1/b=2')
        when: 'the get CM handle endpoint is called with the given reference'
            def response = performGet('/ncmpInventory/v1/ch', [distinguishedName: distinguishedName])
        then: 'the response status is OK'
            assert response.statusCode == HttpStatus.OK
        and: 'the response contains expected cm handle details'
            def body = parseResponseBody(response)
            assert body.cmHandle == 'ch-1'
            assert body.alternateId == '/alt=1/b=2'
        cleanup: 'deregister the CM Handle'
            deregisterCmHandle(DMI1_URL, 'ch-1')
        where: 'the following references are used'
            scenario                                | distinguishedName
            'just alternate id'                     | '/alt=1/b=2'
            'fdn, child object of the alternate id' | '/alt=1/b=2/c=3'
            'cm handle id'                          | 'ch-1'  // Not documented but works too!
    }

}
