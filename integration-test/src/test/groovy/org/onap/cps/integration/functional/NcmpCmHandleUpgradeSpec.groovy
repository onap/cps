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
import org.onap.cps.ncmp.api.models.UpgradedCmHandles
import org.springframework.http.HttpStatus
import spock.lang.Ignore
import spock.util.concurrent.PollingConditions

import static org.springframework.test.web.client.match.MockRestRequestMatchers.anything
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus

class NcmpCmHandleUpgradeSpec extends CpsIntegrationSpecBase {

    NetworkCmProxyDataService objectUnderTest

    static final INITIAL_MODULE_REFERENCES_RESPONSE = readResourceDataFile('mock-dmi-responses/bookStoreAWithModules_M1_M2_Response.json')
    static final INITIAL_MODULE_RESOURCES_RESPONSE = readResourceDataFile('mock-dmi-responses/bookStoreAWithModules_M1_M2_ResourcesResponse.json')
    static final UPDATED_MODULE_REFERENCES_RESPONSE = readResourceDataFile('mock-dmi-responses/bookStoreBWithModules_M1_M3_Response.json')
    static final UPDATED_MODULE_RESOURCES_RESPONSE = readResourceDataFile('mock-dmi-responses/bookStoreBWithModules_M1_M3_ResourcesResponse.json')
    static final CM_HANDLE_ID = 'ch-1'
    static final CM_HANDLE_ID_WITH_EXISTING_MODULE_SET_TAG = 'ch-2'

    def setup() {
        objectUnderTest = networkCmProxyDataService
        mockDmiWillRespondToHealthChecks(DMI_URL)
    }

    @Ignore
    def 'Upgrade CM-handle with new moduleSetTag or no moduleSetTag.'() {
        given: 'DMI will return modules for initial registration'
            mockDmiResponsesForModuleSync(DMI_URL, CM_HANDLE_ID, INITIAL_MODULE_REFERENCES_RESPONSE, INITIAL_MODULE_RESOURCES_RESPONSE)
        and: 'DMI returns different modules for upgrade'
            mockDmiResponsesForModuleSync(DMI_URL, CM_HANDLE_ID, UPDATED_MODULE_REFERENCES_RESPONSE, UPDATED_MODULE_RESOURCES_RESPONSE)

        when: 'a CM-handle is created with expected initial modules: M1 and M2'
            registerCmHandle(DMI_URL, CM_HANDLE_ID, initialModuleSetTag)
            assert ['M1', 'M2'] == objectUnderTest.getYangResourcesModuleReferences(CM_HANDLE_ID).moduleName.sort()
        and: "the CM-handle is upgraded with given moduleSetTag '${updatedModuleSetTag}'"
            def cmHandlesToUpgrade = new UpgradedCmHandles(cmHandles: [CM_HANDLE_ID], moduleSetTag: updatedModuleSetTag)
            def dmiPluginRegistrationResponse = networkCmProxyDataService.updateDmiRegistrationAndSyncModule(
                    new DmiPluginRegistration(dmiPlugin: DMI_URL, upgradedCmHandles: cmHandlesToUpgrade))

        then: 'registration gives successful response'
            assert dmiPluginRegistrationResponse.upgradedCmHandles == [CmHandleRegistrationResponse.createSuccessResponse(CM_HANDLE_ID)]

        and: 'CM-handle is in LOCKED state due to MODULE_UPGRADE'
            def cmHandleCompositeState = objectUnderTest.getCmHandleCompositeState(CM_HANDLE_ID)
            assert cmHandleCompositeState.cmHandleState == CmHandleState.LOCKED
            assert cmHandleCompositeState.lockReason.lockReasonCategory == LockReasonCategory.MODULE_UPGRADE
            assert cmHandleCompositeState.lockReason.details == "Upgrade to ModuleSetTag: ${updatedModuleSetTag}"

        when: 'module sync runs'
            moduleSyncWatchdog.resetPreviouslyFailedCmHandles()
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()

        then: 'CM-handle goes to READY state'
            new PollingConditions().within(3, () -> {
                assert CmHandleState.READY == objectUnderTest.getCmHandleCompositeState(CM_HANDLE_ID).cmHandleState
            })

        and: 'the CM-handle has expected moduleSetTag'
            assert objectUnderTest.getNcmpServiceCmHandle(CM_HANDLE_ID).moduleSetTag == updatedModuleSetTag

        and: 'CM-handle has expected updated modules: M1 and M3'
            assert ['M1', 'M3'] == objectUnderTest.getYangResourcesModuleReferences(CM_HANDLE_ID).moduleName.sort()

        and: 'DMI received expected requests'
            mockDmiServer.verify()

        cleanup: 'deregister CM-handle'
            deregisterCmHandle(DMI_URL, CM_HANDLE_ID)

        where:
            initialModuleSetTag | updatedModuleSetTag
            NO_MODULE_SET_TAG   | NO_MODULE_SET_TAG
            NO_MODULE_SET_TAG   | 'new'
            'initial'           | NO_MODULE_SET_TAG
            'initial'           | 'new'
    }

    @Ignore
    def 'Upgrade CM-handle with existing moduleSetTag.'() {
        given: 'DMI will return modules for registration'
            mockDmiResponsesForModuleSync(DMI_URL, CM_HANDLE_ID, INITIAL_MODULE_REFERENCES_RESPONSE, INITIAL_MODULE_RESOURCES_RESPONSE)
            mockDmiResponsesForModuleSync(DMI_URL, CM_HANDLE_ID_WITH_EXISTING_MODULE_SET_TAG, UPDATED_MODULE_REFERENCES_RESPONSE, UPDATED_MODULE_RESOURCES_RESPONSE)
        and: "an existing CM-handle handle with moduleSetTag '${updatedModuleSetTag}'"
            registerCmHandle(DMI_URL, CM_HANDLE_ID_WITH_EXISTING_MODULE_SET_TAG, updatedModuleSetTag)
            assert ['M1', 'M3'] == objectUnderTest.getYangResourcesModuleReferences(CM_HANDLE_ID_WITH_EXISTING_MODULE_SET_TAG).moduleName.sort()
        and: "a CM-handle with moduleSetTag '${initialModuleSetTag}' which will be upgraded"
            registerCmHandle(DMI_URL, CM_HANDLE_ID, initialModuleSetTag)
            assert ['M1', 'M2'] == objectUnderTest.getYangResourcesModuleReferences(CM_HANDLE_ID).moduleName.sort()

        when: "CM-handle is upgraded to moduleSetTag '${updatedModuleSetTag}'"
            def cmHandlesToUpgrade = new UpgradedCmHandles(cmHandles: [CM_HANDLE_ID], moduleSetTag: updatedModuleSetTag)
            def dmiPluginRegistrationResponse = networkCmProxyDataService.updateDmiRegistrationAndSyncModule(
                    new DmiPluginRegistration(dmiPlugin: DMI_URL, upgradedCmHandles: cmHandlesToUpgrade))

        then: 'registration gives successful response'
            assert dmiPluginRegistrationResponse.upgradedCmHandles == [CmHandleRegistrationResponse.createSuccessResponse(CM_HANDLE_ID)]

        when: 'module sync runs'
            moduleSyncWatchdog.resetPreviouslyFailedCmHandles()
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()

        then: 'CM-handle goes to READY state'
            new PollingConditions().within(3, () -> {
                assert CmHandleState.READY == objectUnderTest.getCmHandleCompositeState(CM_HANDLE_ID).cmHandleState
            })

        and: 'the CM-handle has expected moduleSetTag'
            assert objectUnderTest.getNcmpServiceCmHandle(CM_HANDLE_ID).moduleSetTag == updatedModuleSetTag

        and: 'CM-handle has expected updated modules: M1 and M3'
            assert ['M1', 'M3'] == objectUnderTest.getYangResourcesModuleReferences(CM_HANDLE_ID).moduleName.sort()

        cleanup: 'deregister CM-handle'
            deregisterCmHandles(DMI_URL, [CM_HANDLE_ID, CM_HANDLE_ID_WITH_EXISTING_MODULE_SET_TAG])

        where:
            initialModuleSetTag | updatedModuleSetTag
            NO_MODULE_SET_TAG   | 'moduleSet2'
            'moduleSet1'        | 'moduleSet2'
    }

    @Ignore
    def 'Skip upgrade of CM-handle with same moduleSetTag as before.'() {
        given: 'an existing CM-handle with expected initial modules: M1 and M2'
            mockDmiResponsesForModuleSync(DMI_URL, CM_HANDLE_ID, INITIAL_MODULE_REFERENCES_RESPONSE, INITIAL_MODULE_RESOURCES_RESPONSE)
            registerCmHandle(DMI_URL, CM_HANDLE_ID, 'same')
            assert ['M1', 'M2'] == objectUnderTest.getYangResourcesModuleReferences(CM_HANDLE_ID).moduleName.sort()

        when: 'CM-handle is upgraded with the same moduleSetTag'
            def cmHandlesToUpgrade = new UpgradedCmHandles(cmHandles: [CM_HANDLE_ID], moduleSetTag: 'same')
            networkCmProxyDataService.updateDmiRegistrationAndSyncModule(
                    new DmiPluginRegistration(dmiPlugin: DMI_URL, upgradedCmHandles: cmHandlesToUpgrade))

        then: 'CM-handle remains in READY state'
            assert CmHandleState.READY == objectUnderTest.getCmHandleCompositeState(CM_HANDLE_ID).cmHandleState

        and: 'the CM-handle has same moduleSetTag as before'
            assert objectUnderTest.getNcmpServiceCmHandle(CM_HANDLE_ID).moduleSetTag == 'same'

        then: 'CM-handle has same modules as before: M1 and M2'
            assert ['M1', 'M2'] == objectUnderTest.getYangResourcesModuleReferences(CM_HANDLE_ID).moduleName.sort()

        cleanup: 'deregister CM-handle'
            deregisterCmHandle(DMI_URL, CM_HANDLE_ID)
    }

    @Ignore
    def 'Upgrade of CM-handle fails due to DMI error.'() {
        given: 'DMI will return modules for initial registration'
            mockDmiResponsesForModuleSync(DMI_URL, CM_HANDLE_ID, INITIAL_MODULE_REFERENCES_RESPONSE, INITIAL_MODULE_RESOURCES_RESPONSE)
        and: 'DMI returns error code for upgrade'
            mockDmiServer.expect(anything()).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE))

        when: 'a CM-handle is created'
            registerCmHandle(DMI_URL, CM_HANDLE_ID, 'oldTag')
        and: 'the CM-handle is upgraded'
            def cmHandlesToUpgrade = new UpgradedCmHandles(cmHandles: [CM_HANDLE_ID], moduleSetTag: 'newTag')
            networkCmProxyDataService.updateDmiRegistrationAndSyncModule(
                    new DmiPluginRegistration(dmiPlugin: DMI_URL, upgradedCmHandles: cmHandlesToUpgrade))

        and: 'module sync runs'
            moduleSyncWatchdog.resetPreviouslyFailedCmHandles()
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()

        then: 'CM-handle goes to LOCKED state with reason MODULE_UPGRADE_FAILED'
            new PollingConditions().within(3, () -> {
                def cmHandleCompositeState = objectUnderTest.getCmHandleCompositeState(CM_HANDLE_ID)
                assert cmHandleCompositeState.cmHandleState == CmHandleState.LOCKED
                assert cmHandleCompositeState.lockReason.lockReasonCategory == LockReasonCategory.MODULE_UPGRADE_FAILED
            })

        and: 'the CM-handle has same moduleSetTag as before'
            assert objectUnderTest.getNcmpServiceCmHandle(CM_HANDLE_ID).moduleSetTag == 'oldTag'

        cleanup: 'deregister CM-handle'
            deregisterCmHandle(DMI_URL, CM_HANDLE_ID)
    }

}
