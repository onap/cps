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

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.NetworkCmProxyDataService
import org.onap.cps.ncmp.api.impl.inventory.CmHandleState
import org.onap.cps.ncmp.api.impl.inventory.LockReasonCategory
import org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse
import org.onap.cps.ncmp.api.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.springframework.http.HttpStatus
import spock.util.concurrent.PollingConditions

import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus

class NcmpCmHandleCreateSpec extends CpsIntegrationSpecBase {

    NetworkCmProxyDataService objectUnderTest

    static final MODULE_REFERENCES_RESPONSE_A = readResourceDataFile('mock-dmi-responses/bookStoreAWithModules_M1_M2_Response.json')
    static final MODULE_RESOURCES_RESPONSE_A = readResourceDataFile('mock-dmi-responses/bookStoreAWithModules_M1_M2_ResourcesResponse.json')
    static final MODULE_REFERENCES_RESPONSE_B = readResourceDataFile('mock-dmi-responses/bookStoreBWithModules_M1_M3_Response.json')
    static final MODULE_RESOURCES_RESPONSE_B = readResourceDataFile('mock-dmi-responses/bookStoreBWithModules_M1_M3_ResourcesResponse.json')

    def setup() {
        objectUnderTest = networkCmProxyDataService
    }

    def 'CM Handle registration is successful.'() {
        given: 'DMI will return modules when requested'
            mockDmiResponsesForRegistration(DMI_URL, 'ch-1', MODULE_REFERENCES_RESPONSE_A, MODULE_RESOURCES_RESPONSE_A)

        when: 'a CM-handle is registered for creation'
            def cmHandleToCreate = new NcmpServiceCmHandle(cmHandleId: 'ch-1')
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: DMI_URL, createdCmHandles: [cmHandleToCreate])
            def dmiPluginRegistrationResponse = networkCmProxyDataService.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)

        then: 'registration gives successful response'
            assert dmiPluginRegistrationResponse.createdCmHandles == [CmHandleRegistrationResponse.createSuccessResponse('ch-1')]

        and: 'CM-handle is initially in ADVISED state'
            assert CmHandleState.ADVISED == objectUnderTest.getCmHandleCompositeState('ch-1').cmHandleState

        when: 'module sync runs'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()

        then: 'CM-handle goes to READY state'
            new PollingConditions().within(3, () -> {
                assert CmHandleState.READY == objectUnderTest.getCmHandleCompositeState('ch-1').cmHandleState
            })

        and: 'the CM-handle has expected modules'
            assert ['M1', 'M2'] == objectUnderTest.getYangResourcesModuleReferences('ch-1').moduleName.sort()

        and: 'DMI received expected requests'
            mockDmiServer.verify()

        cleanup: 'deregister CM handle'
            deregisterCmHandle(DMI_URL, 'ch-1')
    }

    def 'CM Handle goes to LOCKED state when DMI gives error during module sync.'() {
        given: 'DMI is not available to handle requests'
            mockDmiServer.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        when: 'a CM-handle is registered for creation'
            def cmHandleToCreate = new NcmpServiceCmHandle(cmHandleId: 'ch-1')
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: DMI_URL, createdCmHandles: [cmHandleToCreate])
            networkCmProxyDataService.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)

        and: 'module sync runs'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()

        then: 'CM-handle goes to LOCKED state with reason MODULE_SYNC_FAILED'
            new PollingConditions().within(3, () -> {
                def cmHandleCompositeState = objectUnderTest.getCmHandleCompositeState('ch-1')
                assert cmHandleCompositeState.cmHandleState == CmHandleState.LOCKED
                assert cmHandleCompositeState.lockReason.lockReasonCategory == LockReasonCategory.MODULE_SYNC_FAILED
            })

        and: 'CM-handle has no modules'
            assert objectUnderTest.getYangResourcesModuleReferences('ch-1').empty

        cleanup: 'deregister CM handle'
            deregisterCmHandle(DMI_URL, 'ch-1')
    }

    def 'Create a CM-handle with existing moduleSetTag.'() {
        given: 'existing CM-handles cm-1 with moduleSetTag "A", and cm-2 with moduleSetTag "B"'
            registerCmHandle(DMI_URL, 'ch-1', 'A', MODULE_REFERENCES_RESPONSE_A, MODULE_RESOURCES_RESPONSE_A)
            registerCmHandle(DMI_URL, 'ch-2', 'B', MODULE_REFERENCES_RESPONSE_B, MODULE_RESOURCES_RESPONSE_B)
            assert ['M1', 'M2'] == objectUnderTest.getYangResourcesModuleReferences('ch-1').moduleName.sort()
            assert ['M1', 'M3'] == objectUnderTest.getYangResourcesModuleReferences('ch-2').moduleName.sort()

        when: 'a CM-handle is registered for creation with moduleSetTag "B"'
            def cmHandleToCreate = new NcmpServiceCmHandle(cmHandleId: 'ch-3', moduleSetTag: 'B')
            networkCmProxyDataService.updateDmiRegistrationAndSyncModule(new DmiPluginRegistration(dmiPlugin: DMI_URL, createdCmHandles: [cmHandleToCreate]))

        then: 'the CM-handle goes to READY state after module sync'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()
            new PollingConditions().within(3, () -> {
                assert CmHandleState.READY == objectUnderTest.getCmHandleCompositeState('ch-3').cmHandleState
            })

        and: 'the CM-handle has expected moduleSetTag'
            assert objectUnderTest.getNcmpServiceCmHandle('ch-3').moduleSetTag == 'B'

        and: 'the CM-handle has expected modules from module set "B": M1 and M3'
            assert ['M1', 'M3'] == objectUnderTest.getYangResourcesModuleReferences('ch-3').moduleName.sort()

        cleanup: 'deregister CM handles'
            deregisterCmHandles(DMI_URL, ['ch-1', 'ch-2', 'ch-3'])
    }
}
