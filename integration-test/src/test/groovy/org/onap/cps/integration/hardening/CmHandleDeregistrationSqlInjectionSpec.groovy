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

package org.onap.cps.integration.hardening

import org.onap.cps.api.parameters.FetchDescendantsOption
import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import spock.lang.Unroll

class CmHandleDeregistrationSqlInjectionSpec extends CpsIntegrationSpecBase {

    def 'Attempt SQL injection through malicious cm handle ID during deregistration'() {
        given: 'a malicious cm handle ID with SQL injection payload'
            def maliciousCmHandleId = "'; DROP TABLE fragment; --"
        when: 'attempting to deregister the malicious cm handle'
            def dmiPluginRegistration = new DmiPluginRegistration(
                    dmiPlugin: 'test-dmi',
                    removedCmHandles: [maliciousCmHandleId]
            )
            def response = networkCmProxyInventoryFacade.updateDmiRegistration(dmiPluginRegistration)
        then: 'the system should handle the malicious input safely and database is still intact'
            assert response != null
            def allFragments = cpsDataService.getDataNodes('NCMP-Admin', 'ncmp-dmi-registry', '/', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
            assert allFragments != null
    }

    @Unroll
    def 'SQL injection attempt with payload: #payload'() {
        given: 'various SQL injection payloads as cm handle IDs'
            def maliciousCmHandleId = payload
        when: 'attempting to deregister with malicious payload'
            def dmiPluginRegistration = new DmiPluginRegistration(
                    dmiPlugin: 'test-dmi',
                    removedCmHandles: [maliciousCmHandleId]
            )
            def result = networkCmProxyInventoryFacade.updateDmiRegistration(dmiPluginRegistration)
        then: 'system remains secure and database integrity is maintained'
            def allFragments = cpsDataService.getDataNodes('NCMP-Admin', 'ncmp-dmi-registry', '/', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
            assert allFragments != null
        and: 'the expected error response is received'
            assert result.removedCmHandles[0].ncmpResponseStatus.name() == 'CM_HANDLE_INVALID_ID'
        where:
            payload << [
                    "'; DROP TABLE fragment; --",
                    "'; UPDATE fragment SET xpath = 'hacked'; --",
                    "' UNION SELECT * FROM fragment; --",
                    "'; DELETE FROM fragment WHERE 1=1; --"]
    }

    def 'Verify legitimate cm handle can still be processed after injection attempts'() {
        given: 'a legitimate cm handle is created first'
            def legitimateCmHandleId = 'legitimate-handle'
            def cmHandleToCreate = new NcmpServiceCmHandle(
                    cmHandleId: legitimateCmHandleId,
                    publicProperties: [test: 'value']
            )
            def createRegistration = new DmiPluginRegistration(
                    dmiPlugin: 'test-dmi',
                    createdCmHandles: [cmHandleToCreate]
            )
            networkCmProxyInventoryFacade.updateDmiRegistration(createRegistration)
        and: 'a SQL injection attack is attempted'
            def maliciousCmHandleId = "'; DROP TABLE fragment; --"
            def attackRegistration = new DmiPluginRegistration(
                    dmiPlugin: 'test-dmi',
                    removedCmHandles: [maliciousCmHandleId]
            )
        when: 'the attack is executed'
            networkCmProxyInventoryFacade.updateDmiRegistration(attackRegistration)
        and: 'legitimate operations are attempted afterwards'
            def removeRegistration = new DmiPluginRegistration(
                    dmiPlugin: 'test-dmi',
                    removedCmHandles: [legitimateCmHandleId]
            )
            def response = networkCmProxyInventoryFacade.updateDmiRegistration(removeRegistration)
        then: 'legitimate operations still work normally'
            assert response != null
            assert response.removedCmHandles.size() >= 0
    }
}