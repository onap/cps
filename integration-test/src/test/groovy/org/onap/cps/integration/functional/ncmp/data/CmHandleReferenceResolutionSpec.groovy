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

package org.onap.cps.integration.functional.ncmp.data

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.springframework.http.HttpStatus

class CmHandleReferenceResolutionSpec extends CpsIntegrationSpecBase {

    def setup() {
        dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']
    }

    def 'Pass-through data operation using the (hash) cm handle id as reference.'() {
        given: 'a cm handle registered with both a (hash) cm handle id'
            registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, '/some=fdn')
        when: 'a pass-through data request is sent to NCMP using the (hash) cm handle id as the reference'
            def response = performGet("/ncmp/v1/ch/ch-1/data/ds/ncmp-datastore:passthrough-running",
                    [resourceIdentifier: 'my-resource-id'])
        then: 'response status is Ok'
            assert response.statusCode == HttpStatus.OK
        cleanup: 'remove the test cm handle'
            deregisterCmHandle(DMI1_URL, 'ch-1')
    }

}
