/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*

@SuppressWarnings('SpellCheckingInspection')
class ProvMnSExtensionsRestApiSpec extends CpsIntegrationSpecBase{

    def 'Send Post request to ProvMnS Extension interface'() {
        given: 'a registered cm handle'
            dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']
            registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, '/A=1/B=2')
        when: 'a POST request is sent'
            def result = mvc.perform(post("/ProvMnSExtensions/v1/A=1/B=2/update").contentType(MediaType.APPLICATION_JSON).content('{}'))
        then: 'a NOT IMPLEMENTED is returned'
            result.andReturn().response.getStatus() == HttpStatus.NOT_IMPLEMENTED.value()
        cleanup: 'deregister CM handles'
            deregisterCmHandle(DMI1_URL, 'ch-1')
    }
}
