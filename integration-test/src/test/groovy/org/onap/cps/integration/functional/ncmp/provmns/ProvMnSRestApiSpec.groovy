/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.integration.functional.ncmp.provmns

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.provmns.model.PatchItem
import org.onap.cps.ncmp.provmns.model.ResourceOneOf
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

@SuppressWarnings('SpellCheckingInspection')
class ProvMnSRestApiSpec extends CpsIntegrationSpecBase {

    def 'Get Resource Data from provmns interface.'() {
        given: 'a registered cm handle'
            dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']
            registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, '/A=1/B=2/C=3')
        when: 'a GET request is sent'
            def response = performGet('/ProvMnS/v1/A=1/B=2/C=3')
        then: 'an OK response is returned'
            assert response.statusCode == HttpStatus.OK
        cleanup: 'deregister CM handles'
            deregisterCmHandle(DMI1_URL, 'ch-1')
    }

    def 'Put Resource Data from provmns interface.'() {
        given: 'a registered cm handle and an example resource json body'
            dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']
            registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, '/A=1/B=2')
            def jsonBody = jsonObjectMapper.asJsonString(new ResourceOneOf('test'))
        when: 'a PUT request is sent'
            def response = performPut('/ProvMnS/v1/A=1/B=2/C=3', jsonBody)
        then: 'an OK response is returned'
            assert response.statusCode == HttpStatus.OK
        cleanup: 'deregister CM handles'
            deregisterCmHandle(DMI1_URL, 'ch-1')
    }

    def 'Patch Resource Data from provmns interface.'() {
        given: 'a registered cm handle and an example resource json body'
            dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']
            registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, '/A=1/B=2')
            def jsonBody = jsonObjectMapper.asJsonString([new PatchItem(op: 'REMOVE', path: '/D=3/C=4')])
        when: 'a PATCH request is sent with json-patch+json content type'
            def response = performPatch('/ProvMnS/v1/A=1/B=2/C=3', jsonBody, new MediaType('application', 'json-patch+json'))
        then: 'an OK response is returned'
            assert response.statusCode == HttpStatus.OK
        cleanup: 'deregister CM handles'
            deregisterCmHandle(DMI1_URL, 'ch-1')
    }

    def 'Delete Resource Data from provmns interface.'() {
        given: 'a registered cm handle'
            dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']
            registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, '/A=1/B=2')
        when: 'a DELETE request is sent'
            def response = performDelete('/ProvMnS/v1/A=1/B=2/C=3')
        then: 'an OK response is returned'
            assert response.statusCode == HttpStatus.OK
        cleanup: 'deregister CM handles'
            deregisterCmHandle(DMI1_URL, 'ch-1')
    }
}
