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

package org.onap.cps.integration.functional

import org.onap.cps.integration.base.NcmpIntegrationSpecBase
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import spock.util.concurrent.PollingConditions

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class NcmpRestApiSpec extends NcmpIntegrationSpecBase {

    def 'Register a CM Handle using REST API.'() {
        given: 'DMI will return modules'
            mockDmiResponse("/dmi/v1/ch/cm-1/modules", HttpStatus.OK, readResourceDataFile('mock-dmi-responses/ietfYangModuleResponse.json'))
            mockDmiResponse("/dmi/v1/ch/cm-1/moduleResources", HttpStatus.OK, readResourceDataFile('mock-dmi-responses/ietfYangModuleResourcesResponse.json'))

        and: 'a POST request is made to register a CM Handle'
            def requestBody = '{ "dmiPlugin":"'+DMI_URL+'", "createdCmHandles":[{"cmHandle": "cm-1"}]}'
            mvc.perform(post('/ncmpInventory/v1/ch').contentType(MediaType.APPLICATION_JSON).content(requestBody))
                    .andExpect(status().is2xxSuccessful())

        when: 'module sync runs'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()

        then: 'CM-handle goes to READY state'
            new PollingConditions(timeout: 3, delay: 0.5).eventually {
                mvc.perform(get('/ncmp/v1/ch/cm-1'))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath('$.state.cmHandleState').value('READY'))
            }
    }

    def 'De-register a CM handle using REST API.'() {
        when: 'a POST request is made to deregister the CM Handle'
            def requestBody = '{"dmiPlugin":"'+DMI_URL+'", "removedCmHandles": ["cm-1"]}'
            mvc.perform(post('/ncmpInventory/v1/ch').contentType(MediaType.APPLICATION_JSON).content(requestBody))
                    .andExpect(status().is2xxSuccessful())
        then: 'a client error is received when getting the CM handle'
            mvc.perform(get('/ncmp/v1/ch/cm-1'))
                    .andExpect(status().is4xxClientError())
    }
}
