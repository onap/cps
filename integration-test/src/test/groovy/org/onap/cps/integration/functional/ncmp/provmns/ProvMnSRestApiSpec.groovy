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

    def setup() {
        given: 'a registered cm handle'
            dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = []
            registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, '/A=1/B=2')
    }

    def cleanup() {
        deregisterCmHandle(DMI1_URL, 'ch-1')
    }

    def 'Get Resource Data from ProvMnS interface.'() {
        when: 'a GET request is sent'
            def response = performGet('/ProvMnS/v1/A=1/B=2')
        then: 'an OK response is returned'
            assert response.statusCode == HttpStatus.OK
    }

    def 'Put Resource Data from ProvMnS interface.'() {
        when: 'a PUT request with a valid resource is sent'
            def jsonBody = jsonObjectMapper.asJsonString(new ResourceOneOf('something'))
            def response = performPut('/ProvMnS/v1/A=1/B=2/C=3', jsonBody)
        then: 'an OK response is returned'
            assert response.statusCode == HttpStatus.OK
    }

    def 'Patch Resource Data from ProvMnS interface.'() {
        when: 'a PATCH (remove) request is sent with json-patch+json content type'
            def jsonBody = jsonObjectMapper.asJsonString([new PatchItem(op: 'REMOVE', path: 'something')])
            def response = performPatch('/ProvMnS/v1/A=1/B=2/C=3', jsonBody, new MediaType('application', 'json-patch+json'))
        then: 'an OK response is returned'
            assert response.statusCode == HttpStatus.OK
    }

    def 'Delete Resource Data from ProvMnS interface.'() {
        when: 'a DELETE request is sent'
            def response = performDelete('/ProvMnS/v1/A=1/B=2/some=child')
        then: 'an OK response is returned'
            assert response.statusCode == HttpStatus.OK
    }
}
