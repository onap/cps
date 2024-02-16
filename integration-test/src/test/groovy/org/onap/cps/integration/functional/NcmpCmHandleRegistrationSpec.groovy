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

import org.onap.cps.integration.base.NcmpIntegrationSpecBase
import org.onap.cps.ncmp.api.impl.inventory.CmHandleState
import org.onap.cps.ncmp.api.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.springframework.http.HttpStatus
import spock.util.concurrent.PollingConditions

class NcmpCmHandleRegistrationSpec extends NcmpIntegrationSpecBase {

    def 'CM Handle is READY when Registration is successful.'() {
        given: 'DMI will return modules'
            mockDmiResponse('/dmi/v1/ch/cm-1/modules', HttpStatus.OK, readResourceDataFile('mock-dmi-responses/ietfYangModuleResponse.json'))
            mockDmiResponse('/dmi/v1/ch/cm-1/moduleResources', HttpStatus.OK, readResourceDataFile('mock-dmi-responses/ietfYangModuleResourcesResponse.json'))

        when: 'a CM-handle is registered'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: DMI_URL, createdCmHandles: [new NcmpServiceCmHandle(cmHandleId: 'cm-1')])
            networkCmProxyDataService.updateDmiRegistrationAndSyncModule(dmiPluginRegistration);

        then: 'CM-handle is initially in ADVISED state'
            assert CmHandleState.ADVISED == networkCmProxyDataService.getCmHandleCompositeState('cm-1').cmHandleState

        when: 'module sync runs'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()

        then: 'CM-handle goes to READY state'
            new PollingConditions(timeout: 3).eventually {
                assert CmHandleState.READY == networkCmProxyDataService.getCmHandleCompositeState('cm-1').cmHandleState
            }
    }

    def 'CM Handle goes to LOCKED state when DMI gives error during module sync.'() {
        given: 'DMI will return an error'
            mockDmiResponse('/dmi/v1/ch/cm-2/modules', HttpStatus.BAD_REQUEST, '')

        when: 'a CM-handle is registered'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: DMI_URL, createdCmHandles: [new NcmpServiceCmHandle(cmHandleId: 'cm-2')])
            networkCmProxyDataService.updateDmiRegistrationAndSyncModule(dmiPluginRegistration);

        then: 'CM-handle is initially in ADVISED state'
            assert CmHandleState.ADVISED == networkCmProxyDataService.getCmHandleCompositeState('cm-2').cmHandleState

        when: 'module sync runs'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()

        then: 'CM-handle goes to LOCKED state'
            new PollingConditions(timeout: 3).eventually {
                assert CmHandleState.LOCKED == networkCmProxyDataService.getCmHandleCompositeState('cm-2').cmHandleState
            }
    }

    def 'Deregister CM-handles.'() {
        given: 'existing CM handles'
            assert networkCmProxyDataService.getAllCmHandleIdsByDmiPluginIdentifier(DMI_URL).sort() == ['cm-1', 'cm-2']

        when: 'the CM-handles are deregistered'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: DMI_URL, removedCmHandles: ['cm-1', 'cm-2'])
            networkCmProxyDataService.updateDmiRegistrationAndSyncModule(dmiPluginRegistration);

        then: 'the CM-handles no longer exist'
            assert networkCmProxyDataService.getAllCmHandleIdsByDmiPluginIdentifier(DMI_URL).empty
    }
}
