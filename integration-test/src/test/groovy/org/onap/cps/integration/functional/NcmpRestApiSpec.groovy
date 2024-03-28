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

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.springframework.http.MediaType
import spock.lang.Ignore
import spock.util.concurrent.PollingConditions
import static org.hamcrest.Matchers.containsInAnyOrder
import static org.hamcrest.Matchers.hasSize
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class NcmpRestApiSpec extends CpsIntegrationSpecBase {

    static final MODULE_REFERENCES_RESPONSE_A = readResourceDataFile('mock-dmi-responses/bookStoreAWithModules_M1_M2_Response.json')
    static final MODULE_RESOURCES_RESPONSE_A = readResourceDataFile('mock-dmi-responses/bookStoreAWithModules_M1_M2_ResourcesResponse.json')
    static final MODULE_REFERENCES_RESPONSE_B = readResourceDataFile('mock-dmi-responses/bookStoreBWithModules_M1_M3_Response.json')
    static final MODULE_RESOURCES_RESPONSE_B = readResourceDataFile('mock-dmi-responses/bookStoreBWithModules_M1_M3_ResourcesResponse.json')

    def setup() {
        mockDmiWillRespondToHealthChecks(DMI_URL)
    }

    @Ignore
    def 'Register CM Handles using REST API.'() {
        given: 'DMI will return modules'
            mockDmiResponsesForModuleSync(DMI_URL, 'ch-1', MODULE_REFERENCES_RESPONSE_A, MODULE_RESOURCES_RESPONSE_A)
            mockDmiResponsesForModuleSync(DMI_URL, 'ch-2', MODULE_REFERENCES_RESPONSE_A, MODULE_RESOURCES_RESPONSE_A)
            mockDmiResponsesForModuleSync(DMI_URL, 'ch-3', MODULE_REFERENCES_RESPONSE_B, MODULE_RESOURCES_RESPONSE_B)
        and: 'a POST request is made to register the CM Handles'
            def requestBody = '{"dmiPlugin":"'+DMI_URL+'","createdCmHandles":[{"cmHandle":"ch-1"},{"cmHandle":"ch-2"},{"cmHandle":"ch-3"}]}'
            mvc.perform(post('/ncmpInventory/v1/ch').contentType(MediaType.APPLICATION_JSON).content(requestBody))
                    .andExpect(status().is2xxSuccessful())
        when: 'module sync runs'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()
        then: 'CM-handles go to READY state'
            new PollingConditions(timeout: 3, delay: 0.5).eventually {
                mvc.perform(get('/ncmp/v1/ch/ch-1'))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath('$.state.cmHandleState').value('READY'))
            }
    }

    @Ignore
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
            mvc.perform(post(DMI_URL+'/ncmp/v1/ch/id-searches').contentType(MediaType.APPLICATION_JSON).content(requestBodyWithModuleCondition))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath('$[*]', containsInAnyOrder(expectedCmHandles.toArray())))
                    .andExpect(jsonPath('$', hasSize(expectedCmHandles.size())));
        where:
            moduleName || expectedCmHandles
            'M1'       || ['ch-1', 'ch-2', 'ch-3']
            'M2'       || ['ch-1', 'ch-2']
            'M3'       || ['ch-3']
    }

    @Ignore
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
