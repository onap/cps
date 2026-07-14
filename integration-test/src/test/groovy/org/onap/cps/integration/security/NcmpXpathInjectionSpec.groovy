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
        and: 'existing cm handles are not exposed or affected'
            assert inventoryPersistence.getCmHandleState('ch-1') != null
            assert inventoryPersistence.getCmHandleState('ch-2') != null
        where: 'the following injection payloads are used as cm handle ID'
            scenario                                                              | cmHandleId                      | expectedException
            'single quote closing predicate to attempt matching a second handle'  | "ch-1' or @id='ch-2"            | DataNodeNotFoundException
            'semicolon to attempt breaking out of XPath into SQL'                 | "ch-1'; DROP TABLE fragment;--" | CpsPathException
    }
}
