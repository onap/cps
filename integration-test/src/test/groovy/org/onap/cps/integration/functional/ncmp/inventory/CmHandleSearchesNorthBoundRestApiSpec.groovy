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

package org.onap.cps.integration.functional.ncmp.inventory

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.springframework.http.HttpStatus

class CmHandleSearchesNorthBoundRestApiSpec extends CpsIntegrationSpecBase {

    def 'Register CM Handles using REST API (setup).'() {
        given: 'DMI will return modules'
            dmiDispatcher1.moduleNamesPerCmHandleId = [
                'ch-1': ['M1', 'M2'],
                'ch-2': ['M1', 'M2'],
                'ch-3': ['M1', 'M3']
            ]
        when: 'register the CM Handles'
            def requestBody = '{"dmiPlugin":"'+DMI1_URL+'","createdCmHandles":[{"cmHandle":"ch-1","alternateId":"alt-1"},{"cmHandle":"ch-2","alternateId":"alt-2"},{"cmHandle":"ch-3","alternateId":"alt-3"}]}'
            def response = performPost('/ncmpInventory/v1/ch', requestBody)
        then: 'registration is successful'
            assert response.statusCode.is2xxSuccessful()
        when: 'the module sync watchdog is triggered'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()
        then: 'CM-handles go to READY state'
            (1..3).each {
                def getResponse = performGet("/ncmp/v1/ch/ch-${it}")
                assert getResponse.statusCode == HttpStatus.OK
                assert parseResponseBody(getResponse).state.cmHandleState == 'READY'
            }
    }

    def 'Search for CM Handles by module using REST API.'() {
        given: 'a JSON request body containing search parameter'
            def requestBodyWithModuleCondition = """{
                    "cmHandleQueryParameters": [ {
                                "conditionName": "hasAllModules",
                                "conditionParameters": [ {"moduleName": "%s"} ]
                            } ]
                }""".formatted(moduleName)
        when: 'a search is performed'
            def queryParams = outputAlternateId ? [outputAlternateId: outputAlternateId] : [:]
            def response = performPost('/ncmp/v1/ch/id-searches', requestBodyWithModuleCondition, queryParams)
        then: 'response is successful with expected CM handles'
            assert response.statusCode.is2xxSuccessful()
            def results = parseResponseBody(response) as List
            assert results.size() == expectedCmHandleReferences.size()
            assert results.containsAll(expectedCmHandleReferences)
        where: 'the following parameters are used'
            moduleName | outputAlternateId || expectedCmHandleReferences
            'M1'       | 'false'           || ['ch-1', 'ch-2', 'ch-3']
            'M2'       | 'false'           || ['ch-1', 'ch-2']
            'M3'       | 'false'           || ['ch-3']
            'M1'       | 'true'            || ['alt-1', 'alt-2', 'alt-3']
            'M2'       | 'true'            || ['alt-1', 'alt-2']
            'M3'       | 'true'            || ['alt-3']
            'M1'       | ''                || ['ch-1', 'ch-2', 'ch-3']
            'M1'       | null              || ['ch-1', 'ch-2', 'ch-3']
    }

    def 'Search for CM Handles using Cps Path Query.'() {
        given: 'a JSON request body containing search parameter'
            def requestBodyWithSearchCondition = """{
                    "cmHandleQueryParameters": [ {
                                "conditionName": "cmHandleWithCpsPath",
                                "conditionParameters": [ {"cpsPath" : "%s"} ]
                            } ]
                }""".formatted(cpsPath)
        when: 'a search is performed'
            def response = performPost('/ncmp/v1/ch/id-searches', requestBodyWithSearchCondition)
        then: 'response is successful with expected CM handles'
            assert response.statusCode.is2xxSuccessful()
            def results = parseResponseBody(response) as List
            assert results.size() == expectedCmHandles.size()
            assert results.containsAll(expectedCmHandles)
        where: 'the following parameters are used'
            scenario                    | cpsPath                               || expectedCmHandles
            'All Ready CM handles'      | "//state[@cm-handle-state='READY']"   || ['ch-1', 'ch-2', 'ch-3']
            'Having Alternate ID alt-3' | "//cm-handles[@alternate-id='alt-3']" || ['ch-3']
    }

    def 'De-register CM handles using REST API.'() {
        when: 'deregister the CM Handles'
            def requestBody = '{"dmiPlugin":"'+DMI1_URL+'", "removedCmHandles": ["ch-1", "ch-2", "ch-3"]}'
            def response = performPost('/ncmpInventory/v1/ch', requestBody)
        then: 'deregistration is successful'
            assert response.statusCode.is2xxSuccessful()
        and: 'the CM handles are gone'
            (1..3).each {
                def getResponse = performGet("/ncmp/v1/ch/ch-${it}")
                assert getResponse.statusCode.is4xxClientError()
            }
    }
}
