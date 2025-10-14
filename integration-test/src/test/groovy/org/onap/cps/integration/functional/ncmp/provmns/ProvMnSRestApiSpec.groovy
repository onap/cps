/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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
import org.onap.cps.ncmp.impl.provmns.model.ResourceOneOf
import org.springframework.http.MediaType

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SuppressWarnings('SpellCheckingInspection')
class ProvMnSRestApiSpec extends CpsIntegrationSpecBase {

    def 'Get Resource Data from provmns interface.'() {
        given: 'a registered cm handle'
            dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']
            registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, 'A=1/B=2/C=3')
        expect: 'not implemented response on GET endpoint'
            mvc.perform(get("/ProvMnS/v1/A=1/B=2/C=3"))
                    .andExpect(status().is2xxSuccessful())
        cleanup: 'deregister CM handles'
            deregisterCmHandle(DMI1_URL, 'ch-1')
    }

    def 'Put Resource Data from provmns interface.'() {
        given: 'an example resource json body'
            def jsonBody = jsonObjectMapper.asJsonString(new ResourceOneOf('test'))
        expect: 'not implemented response on PUT endpoint'
            mvc.perform(put("/ProvMnS/v1/A=1/B=2/C=3")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonBody))
                    .andExpect(status().isNotImplemented())
    }

    def 'Patch Resource Data from provmns interface.'() {
        given: 'an example resource json body and a cm handle'
            def jsonBody = jsonObjectMapper.asJsonString(new ResourceOneOf('test'))
            dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']
            registerCmHandle(DMI1_URL, 'ch-1', NO_MODULE_SET_TAG, 'ldnFirstPart')
        expect: 'not implemented response on PATCH endpoint'
            mvc.perform(patch("/ProvMnS/v1/ldnFirstPart/someClass=someId")
                    .contentType(new MediaType('application', 'json-patch+json'))
                    .content(jsonBody))
                    .andExpect(status().isOk())
    }

    def 'Delete Resource Data from provmns interface.'() {
        expect: 'not implemented response on DELETE endpoint'
            mvc.perform(delete("/ProvMnS/v1/A=1/B=2/C=3"))
                    .andExpect(status().isNotImplemented())
    }
}
