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

import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.hasSize
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.springframework.http.MediaType
import spock.util.concurrent.PollingConditions

class RestApiSpec extends CpsIntegrationSpecBase {

    def 'Register CM Handles using REST API.'() {
        given: 'DMI will return modules'
            dmiDispatcher.moduleNamesPerCmHandleId = [
                'ch-1': ['M1', 'M2'],
                'ch-2': ['M1', 'M2'],
                'ch-3': ['M1', 'M3']
            ]
        when: 'a POST request is made to register the CM Handles'
            def requestBody = '{"dmiPlugin":"'+DMI_URL+'","createdCmHandles":[{"cmHandle":"ch-1"},{"cmHandle":"ch-2"},{"cmHandle":"ch-3"}]}'
            mvc.perform(post('/ncmpInventory/v1/ch').contentType(MediaType.APPLICATION_JSON).content(requestBody))
                    .andExpect(status().is2xxSuccessful())
        then: 'CM-handles go to READY state'
            new PollingConditions().within(MODULE_SYNC_WAIT_TIME_IN_SECONDS, () -> {
                (1..3).each {
                    mvc.perform(get('/ncmp/v1/ch/ch-'+it))
                            .andExpect(status().isOk())
                            .andExpect(jsonPath('$.state.cmHandleState').value('READY'))
                }
            })
    }

    def 'Search for CM Handles by module using REST API.'() {
        given: 'a JSON request body containing search parameter'
            def requestBodyWithModuleCondition = """{
                    "cmHandleQueryParameters": [
                            {
                                "conditionName": "hasAllModules",
                                "conditionParameters": [ {"moduleName": "%s"} ]
                            }
                    ]
                }""".formatted(moduleName)
        expect: "a search for module ${moduleName} returns expected CM handles"
            mvc.perform(post('/ncmp/v1/ch/id-searches').contentType(MediaType.APPLICATION_JSON).content(requestBodyWithModuleCondition))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath('$[*]', containsInAnyOrder(expectedCmHandles.toArray())))
                    .andExpect(jsonPath('$', hasSize(expectedCmHandles.size())));
        where:
            moduleName || expectedCmHandles
            'M1'       || ['ch-1', 'ch-2', 'ch-3']
            'M2'       || ['ch-1', 'ch-2']
            'M3'       || ['ch-3']
    }

    def 'De-register CM handles using REST API.'() {
        when: 'a POST request is made to deregister the CM Handle'
            def requestBody = '{"dmiPlugin":"'+DMI_URL+'", "removedCmHandles": ["ch-1", "ch-2", "ch-3"]}'
            mvc.perform(post('/ncmpInventory/v1/ch').contentType(MediaType.APPLICATION_JSON).content(requestBody))
                    .andExpect(status().is2xxSuccessful())
        then: 'the CM handles are not found using GET'
            (1..3).each {
                mvc.perform(get('/ncmp/v1/ch/ch-'+it)).andExpect(status().is4xxClientError())
            }
    }
}
