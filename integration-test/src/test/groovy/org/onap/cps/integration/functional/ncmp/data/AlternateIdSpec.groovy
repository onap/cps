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
import org.springframework.http.HttpStatus

class AlternateIdSpec extends CpsIntegrationSpecBase {

    def setup() {
        dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']
    }

    def 'Pass-through data operations using #scenario as reference.'() {
        given: 'a cm handle with an alternate id'
            registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, alternateId)
        when: 'a pass-through data request is sent to NCMP'
            def encodedCmHandleReference = URLEncoder.encode(cmHandleReference, 'UTF-8')
            def response = performGet("/ncmp/v1/ch/${encodedCmHandleReference}/data/ds/ncmp-datastore:passthrough-running",
                    [resourceIdentifier: 'my-resource-id'])
        then: 'response status is Ok'
            assert response.statusCode == HttpStatus.OK
        cleanup: 'remove the test cm handle'
            deregisterCmHandle(DMI1_URL, 'ch-1')
        where: 'the following ids are used'
            scenario           | alternateId                          | cmHandleReference
            'cm handle id'     | 'dont care'                          | 'ch-1'
            'simple alt-id'    | 'alt-1'                              | 'alt-1'
            'alt-id with ='    | 'alt=1'                              | 'alt=1'
            'FDN with slashes' | '/SubNetwork=Europe/MeContext=node1' | '/SubNetwork=Europe/MeContext=node1'
    }

}
