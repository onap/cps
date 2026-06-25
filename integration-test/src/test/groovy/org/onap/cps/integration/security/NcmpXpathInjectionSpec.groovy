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

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle

/**
 * Verifies that XPath/CPS path injection via cm handle IDs, property values,
 * and DMI identifiers cannot read, modify, or delete unauthorized data
 * across NCMP inventory operations.
 */
class NcmpXpathInjectionSpec extends CpsIntegrationSpecBase {

    def setup() {
        def cmHandles = [
            new NcmpServiceCmHandle(cmHandleId: 'ch-1', publicProperties: ['color': 'red']),
            new NcmpServiceCmHandle(cmHandleId: 'ch-2', publicProperties: ['color': 'blue'])
        ]
        def registration = new DmiPluginRegistration(
                dmiPlugin: DMI1_URL,
                createdCmHandles: cmHandles
        )
        networkCmProxyInventoryFacade.updateDmiRegistration(registration)
        moduleSyncWatchdog.moduleSyncAdvisedCmHandles()
    }

    def cleanup() {
        deregisterCmHandles(DMI1_URL, ['ch-1', 'ch-2'])
    }

    def 'Get cm handle state with cm handle ID as #scenario'() {
        when: 'cm handle state is requested with an injection payload'
            inventoryPersistence.getCmHandleState(cmHandleId)
        then: 'an exception is thrown'
            thrown(Exception)
        and: 'legitimate cm handles remain unaffected'
            assert inventoryPersistence.getCmHandleState('ch-1') != null
            assert inventoryPersistence.getCmHandleState('ch-2') != null
        where: 'the following injection payloads are used as cm handle ID'
            scenario                       | cmHandleId
            'XPath OR predicate injection' | "x' or '1'='1"
            'XPath union attempt'          | "'] | //cm-handles[@id='ch-1"
            'SQL-style injection'          | "'; DROP TABLE fragment; --"
            'single quote breakout'        | "test'injection"
    }

    def 'Set data sync enabled with cm handle ID as #scenario'() {
        when: 'set data sync enabled is called via REST with an injection payload'
            def response = performPut(
                    "/ncmp/v1/ch/${URLEncoder.encode(cmHandleId, 'UTF-8')}/data-sync",
                    '',
                    ['dataSyncEnabled': 'true']
            )
        then: 'the request is rejected'
            assert response.statusCode.value() != 200
        and: 'legitimate cm handles remain accessible'
            assert inventoryPersistence.getCmHandleState('ch-1') != null
            assert inventoryPersistence.getCmHandleState('ch-2') != null
        where: 'the following injection payloads are used as cm handle ID'
            scenario                       | cmHandleId
            'XPath OR predicate injection' | "x' or '1'='1"
            'single quote breakout'        | "test'inject"
            'SQL-style injection'          | "'; DROP TABLE fragment; --"
    }

    def 'Search cm handles with property value as #scenario'() {
        when: 'a cm handle search is executed with an injected property value'
            def requestBody = """
                {
                    "cmHandleQueryParameters": [
                        {
                            "conditionName": "hasAllProperties",
                            "conditionParameters": [
                                { "color": "${propertyValue}" }
                            ]
                        }
                    ]
                }
            """
            def response = performPost('/ncmp/v1/ch/id-searches', requestBody)
        then: 'the request is rejected'
            assert response.statusCode.is5xxServerError()
        where: 'the following injection payloads are used as property value'
            scenario                       | propertyValue
            'XPath OR predicate injection' | "x' or '1'='1"
            'single quote breakout'        | "red'] or @name='shape"
            'SQL-style injection'          | "'; DROP TABLE fragment; --"
    }

    def 'Search cm handles with a legitimate property value'() {
        when: 'a cm handle search is executed with a valid property value'
            def requestBody = """
                {
                    "cmHandleQueryParameters": [
                        {
                            "conditionName": "hasAllProperties",
                            "conditionParameters": [
                                { "color": "red" }
                            ]
                        }
                    ]
                }
            """
            def response = performPost('/ncmp/v1/ch/id-searches', requestBody)
        then: 'only the matching cm handle is returned'
            assert response.body.contains('ch-1')
            assert !response.body.contains('ch-2')
    }

    def 'Search cm handles with DMI plugin identifier as #scenario'() {
        when: 'an inventory search with injected DMI identifier is executed'
            def requestBody = """
                {
                    "cmHandleQueryParameters": [
                        {
                            "conditionName": "cmHandleWithDmiPlugin",
                            "conditionParameters": [
                                { "dmi-plugin-identifier": "${dmiPluginIdentifier}" }
                            ]
                        }
                    ]
                }
            """
            def response = performPost('/ncmp/v1/ch/id-searches', requestBody)
        then: 'the request is rejected'
            assert response.statusCode.is5xxServerError()
        where: 'the following injection payloads are used as DMI plugin identifier'
            scenario                  | dmiPluginIdentifier
            'XPath injection attempt' | "x'] or ['1'='1"
            'single quote in URL'     | "http://evil' or '1'='1"
            'SQL-style injection'     | "'; SELECT * FROM fragment; --"
    }
}
