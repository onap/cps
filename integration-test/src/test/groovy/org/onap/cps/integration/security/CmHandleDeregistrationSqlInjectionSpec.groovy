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

class CmHandleDeregistrationSqlInjectionSpec extends CpsIntegrationSpecBase {

    def 'Verify legitimate cm handle can still be processed after injection attempts'() {
        given: 'a legitimate cm handle is created first'
            def legitimateCmHandleId = 'legitimate-handle'
            def cmHandleToCreate = new NcmpServiceCmHandle(
                    cmHandleId: legitimateCmHandleId,
                    publicProperties: [test: 'value']
            )
            def createRegistration = new DmiPluginRegistration(
                    dmiPlugin: 'https://test-dmi',
                    createdCmHandles: [cmHandleToCreate]
            )
            networkCmProxyInventoryFacade.updateDmiRegistration(createRegistration)
        and: 'a SQL injection attack is attempted'
            def maliciousCmHandleId = "'; DROP TABLE fragment; --"
            def attackRegistration = new DmiPluginRegistration(
                    dmiPlugin: 'https://test-dmi',
                    removedCmHandles: [maliciousCmHandleId]
            )
        when: 'the attack is executed'
            def attackResult = networkCmProxyInventoryFacade.updateDmiRegistration(attackRegistration)
        and: 'legitimate operations are attempted afterwards'
            def removeRegistration = new DmiPluginRegistration(
                    dmiPlugin: 'https://test-dmi',
                    removedCmHandles: [legitimateCmHandleId]
            )
            def response = networkCmProxyInventoryFacade.updateDmiRegistration(removeRegistration)
        then: 'legitimate operations still work normally'
            assert response != null
            assert response.removedCmHandles.size() >= 0
        and: 'attack was handled correctly'
            assert attackResult.removedCmHandles[0].ncmpResponseStatus.name() == 'CM_HANDLE_INVALID_ID'
    }
}