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

package org.onap.cps.integration.security

import org.onap.cps.api.exceptions.CpsPathException
import org.onap.cps.api.exceptions.DataNodeNotFoundException
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle

class NcmpXpathInjectionSpec extends CpsIntegrationSpecBase {

    def setup() {
        dmiDispatcher1.moduleNamesPerCmHandleId['ch-1'] = ['M1', 'M2']
        dmiDispatcher2.moduleNamesPerCmHandleId['ch-2'] = ['M1', 'M2']
        networkCmProxyInventoryFacade.updateDmiRegistration(new DmiPluginRegistration(
                dmiPlugin: DMI1_URL,
                createdCmHandles: [new NcmpServiceCmHandle(cmHandleId: 'ch-1', publicProperties: ['color': 'red'])]
        ))
        networkCmProxyInventoryFacade.updateDmiRegistration(new DmiPluginRegistration(
                dmiPlugin: DMI2_URL,
                createdCmHandles: [new NcmpServiceCmHandle(cmHandleId: 'ch-2', publicProperties: ['color': 'blue'])]
        ))
        moduleSyncWatchdog.moduleSyncAdvisedCmHandles()
    }

    def cleanup() {
        deregisterCmHandle(DMI1_URL, 'ch-1')
        deregisterCmHandle(DMI2_URL, 'ch-2')
    }

    def 'Get cm handle state with #scenario as cm handle ID'() {
        when: 'cm handle state is requested with a cm handle ID containing injection characters'
            inventoryPersistence.getCmHandleState(cmHandleId)
        then: 'the request throws an exception'
            thrown(expectedException)
        where: 'the following injection payloads are used as cm handle ID'
            scenario                                                            | cmHandleId                     | expectedException
            'single quote closing predicate to attempt matching a second handle'| "ch-1' or @id='ch-2"           | DataNodeNotFoundException
            'semicolon to attempt breaking out of XPath into SQL'               | "ch-1'; DROP TABLE fragment;--"| CpsPathException
    }

    def 'Search cm handles with single quote closing value to attempt adding OR condition as property value'() {
        given: 'a property value with a single quote to break out of the value context and add an OR condition'
            def injectedPropertyValue = "red' or @name='color"
            def requestBody = """
                {
                    "cmHandleQueryParameters": [
                        {
                            "conditionName": "hasAllProperties",
                            "conditionParameters": [
                                { "color": "${injectedPropertyValue}" }
                            ]
                        }
                    ]
                }
            """
        when: 'a cm handle search is executed with this injection payload'
            def response = performPost('/ncmp/v1/ch/id-searches', requestBody)
        then: 'the request is rejected with a client error'
            assert response.statusCode.is4xxClientError()
        and: 'the response indicates a possible injection attempt'
            assert response.body.contains('Possible injection attempt')
    }

    def 'Search cm handles with single quote closing attribute to attempt OR condition as DMI plugin identifier'() {
        given: 'a DMI plugin identifier with a single quote to break out and OR to match the other DMI'
            def injectedIdentifier = DMI1_URL + "' or @dmi-service-name='" + DMI2_URL
            def requestBody = """
                {
                    "cmHandleQueryParameters": [
                        {
                            "conditionName": "cmHandleWithDmiPlugin",
                            "conditionParameters": [
                                { "dmiPluginName": "${injectedIdentifier}" }
                            ]
                        }
                    ]
                }
            """
        when: 'an inventory search is executed with this injection payload'
            def response = performPost('/ncmpInventory/v1/ch/searches', requestBody)
        then: 'the request is rejected with a client error'
            assert response.statusCode.is4xxClientError()
        and: 'the response indicates a leaf condition validation failure'
            assert response.body.contains('Possible injection attempt')
    }

}
