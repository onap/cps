/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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

class CmHandleSearchesSouthBoundRestApiSpec extends CpsIntegrationSpecBase {

    def 'Search for CM Handles by dmi-service using REST API.'() {
        given: 'register some CM Handles for dmi service #DMI1_URL (has to be done in same test otherwise URL/Port changes)'
            def requestBody = '{"dmiPlugin":"'+DMI1_URL+'","createdCmHandles":[{"cmHandle":"ch-1"},{"cmHandle":"ch-2"}]}'
            def regResponse = performPost('/ncmpInventory/v1/ch', requestBody)
            assert regResponse.statusCode.is2xxSuccessful()
        and: 'a cm-handle search on dmi service name request (body)'
            def dmiPluginNameSearchValue = useCorrectDmiForSearch ? DMI1_URL : 'non existing dmi'
            def requestBodyWithModuleCondition = """{
                    "cmHandleQueryParameters": [ {
                                "conditionName": "cmHandleWithDmiPlugin",
                                "conditionParameters": [ {"dmiPluginName": "%s"} ]
                            } ]
                }""".formatted(dmiPluginNameSearchValue)
        when: 'the search is executed'
            def response = performPost("/ncmpInventory/v1/ch/searchCmHandles",
                    requestBodyWithModuleCondition, [outputDmiProperties: outputDmiProperties.toString()])
        then: 'response is successful with expected CM handles'
            assert response.statusCode.is2xxSuccessful()
            def results = parseResponseBody(response) as List
            assert results.size() == expectedCmHandleReferences.size()
            assert results*.cmHandle.containsAll(expectedCmHandleReferences)
        and: 'the result response only contains additional properties when requested'
            assert response.body.contains('cmHandleProperties') == outputDmiProperties
        cleanup: 'deregister the CM Handles'
            def cleanupBody = '{"dmiPlugin":"'+DMI1_URL+'", "removedCmHandles": ["ch-1", "ch-2"]}'
            performPost('/ncmpInventory/v1/ch', cleanupBody)
        where: 'the following parameters are used'
            useCorrectDmiForSearch | outputDmiProperties || expectedCmHandleReferences
            true                   | true                || ['ch-1', 'ch-2']
            true                   | false               || ['ch-1', 'ch-2']
            false                  | false               || []
    }

}
