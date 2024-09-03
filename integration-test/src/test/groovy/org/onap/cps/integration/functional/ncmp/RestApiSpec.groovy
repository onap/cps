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
            dmiDispatcher1.moduleNamesPerCmHandleId = [
                'ch-1': ['M1', 'M2'],
                'ch-2': ['M1', 'M2'],
                'ch-3': ['M1', 'M3']
            ]
        when: 'a POST request is made to register the CM Handles'
            def requestBody = '{"dmiPlugin":"'+DMI1_URL+'","createdCmHandles":[{"cmHandle":"ch-1","alternateId":"alt-1"},{"cmHandle":"ch-2","alternateId":"alt-2"},{"cmHandle":"ch-3","alternateId":"alt-3"}]}'
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
            mvc.perform(post('/ncmp/v1/ch/id-searches'+outputAlternateId).contentType(MediaType.APPLICATION_JSON).content(requestBodyWithModuleCondition))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath('$[*]', containsInAnyOrder(expectedCmHandleReferences.toArray())))
                    .andExpect(jsonPath('$', hasSize(expectedCmHandleReferences.size())));
        where:
            moduleName | outputAlternateId           || expectedCmHandleReferences
            'M1'       | '?outputAlternateId=false'  || ['ch-1', 'ch-2', 'ch-3']
            'M2'       | '?outputAlternateId=false'  || ['ch-1', 'ch-2']
            'M3'       | '?outputAlternateId=false'  || ['ch-3']
            'M1'       | '?outputAlternateId=true'   || ['alt-1', 'alt-2', 'alt-3']
            'M2'       | '?outputAlternateId=true'   || ['alt-1', 'alt-2']
            'M3'       | '?outputAlternateId=true'   || ['alt-3']
            'M1'       | ''                          || ['ch-1', 'ch-2', 'ch-3']

    }

    def 'Search for CM Handles using Cps Path Query.'() {
        given: 'a JSON request body containing search parameter'
            def requestBodyWithSearchCondition = """{
                    "cmHandleQueryParameters": [
                            {
                                "conditionName": "cmHandleWithCpsPath",
                                "conditionParameters": [ {"cpsPath" : "%s"} ]
                            }
                    ]
                }""".formatted(cpsPath)
        expect: "a search for cps path ${cpsPath} returns expected CM handles"
            mvc.perform(post('/ncmp/v1/ch/id-searches').contentType(MediaType.APPLICATION_JSON).content(requestBodyWithSearchCondition))
                    .andExpect(status().is2xxSuccessful())
                    .andExpect(jsonPath('$[*]', containsInAnyOrder(expectedCmHandles.toArray())))
                    .andExpect(jsonPath('$', hasSize(expectedCmHandles.size())));
        where:
            scenario                    | cpsPath                                 || expectedCmHandles
            'All Ready CM handles'      | "//state[@cm-handle-state='READY']"     || ['ch-1', 'ch-2', 'ch-3']
            'Having Alternate ID alt-3' | "//cm-handles[@alternate-id='alt-3']"   || ['ch-3']
    }

    def 'De-register CM handles using REST API.'() {
        when: 'a POST request is made to deregister the CM Handle'
            def requestBody = '{"dmiPlugin":"'+DMI1_URL+'", "removedCmHandles": ["ch-1", "ch-2", "ch-3"]}'
            mvc.perform(post('/ncmpInventory/v1/ch').contentType(MediaType.APPLICATION_JSON).content(requestBody))
                    .andExpect(status().is2xxSuccessful())
        then: 'the CM handles are not found using GET'
            (1..3).each {
                mvc.perform(get('/ncmp/v1/ch/ch-'+it)).andExpect(status().is4xxClientError())
            }
    }
}
