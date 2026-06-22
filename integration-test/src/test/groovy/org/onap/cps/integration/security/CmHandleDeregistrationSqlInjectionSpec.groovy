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

    def 'Delete a cmhandle with #scenario'() {
        given: 'a legitimate cm handle is created first'
            def cmHandleToCreate = new NcmpServiceCmHandle(
                    cmHandleId: 'ch-1',
                    publicProperties: [test: 'value']
            )
            def createRegistration = new DmiPluginRegistration(
                    dmiPlugin: 'https://test-dmi',
                    createdCmHandles: [cmHandleToCreate]
            )
            networkCmProxyInventoryFacade.updateDmiRegistration(createRegistration)
        and: 'a cm handle update is attempted'
            def deleteRegistration = new DmiPluginRegistration(
                    dmiPlugin: 'https://test-dmi',
                    removedCmHandles: [cmHandleId]
            )
        when: 'the delete is executed'
            def result = networkCmProxyInventoryFacade.updateDmiRegistration(deleteRegistration)
        then: 'delete was handled correctly'
            assert result.removedCmHandles[0].status.name() == expectedResultMessage
        where:
            scenario               | cmHandleId                   || expectedResultMessage
            'an injection attack'  | "'; DROP TABLE fragment; --" || 'FAILURE'
            'a legitimate request' | 'ch-1'                       || 'SUCCESS'
    }
}