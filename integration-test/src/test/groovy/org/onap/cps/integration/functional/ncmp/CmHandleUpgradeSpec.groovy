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

package org.onap.cps.integration.functional.ncmp

import org.onap.cps.integration.base.CpsIntegrationSpecBase
import org.onap.cps.ncmp.api.inventory.models.CmHandleRegistrationResponse
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.inventory.models.UpgradedCmHandles
import org.onap.cps.ncmp.impl.NetworkCmProxyInventoryFacadeImpl
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState
import org.onap.cps.ncmp.impl.inventory.models.LockReasonCategory
import spock.util.concurrent.PollingConditions

class CmHandleUpgradeSpec extends CpsIntegrationSpecBase {

    NetworkCmProxyInventoryFacadeImpl objectUnderTest

    def cmHandleId = 'ch-1'
    def cmHandleIdWithExistingModuleSetTag = 'ch-2'

    def setup() {
        objectUnderTest = networkCmProxyInventoryFacade
    }

    def 'Upgrade CM-handle with and without (new) module set tags.'() {
        given: 'a CM-handle is created with expected initial modules: M1 and M2'
            dmiDispatcher1.moduleNamesPerCmHandleId[cmHandleId] = ['M1', 'M2']
            registerCmHandle(DMI1_URL, cmHandleId, initialModuleSetTag)
            assert ['M1', 'M2'] == objectUnderTest.getYangResourcesModuleReferences(cmHandleId).moduleName.sort()

        when: "the CM-handle is upgraded with given moduleSetTag '${updatedModuleSetTag}'"
            def cmHandlesToUpgrade = new UpgradedCmHandles(cmHandles: [cmHandleId], moduleSetTag: updatedModuleSetTag)
            def dmiPluginRegistrationResponse = objectUnderTest.updateDmiRegistration(
                    new DmiPluginRegistration(dmiPlugin: DMI1_URL, upgradedCmHandles: cmHandlesToUpgrade))

        then: 'registration gives successful response'
            assert dmiPluginRegistrationResponse.upgradedCmHandles == [CmHandleRegistrationResponse.createSuccessResponse(cmHandleId)]

        and: 'CM-handle is in LOCKED state due to MODULE_UPGRADE'
            def cmHandleCompositeState = objectUnderTest.getCmHandleCompositeState(cmHandleId)
            assert cmHandleCompositeState.cmHandleState == CmHandleState.LOCKED
            assert cmHandleCompositeState.lockReason.lockReasonCategory == LockReasonCategory.MODULE_UPGRADE
            assert cmHandleCompositeState.lockReason.details == "Upgrade to ModuleSetTag: ${updatedModuleSetTag}"

        when: 'DMI will return different modules for upgrade: M1 and M3'
            dmiDispatcher1.moduleNamesPerCmHandleId[cmHandleId] = ['M1', 'M3']

        and: 'the module sync watchdog is triggered twice'
            2.times { moduleSyncWatchdog.moduleSyncAdvisedCmHandles() }

        then: 'CM-handle goes to READY state'
            new PollingConditions().within(MODULE_SYNC_WAIT_TIME_IN_SECONDS, () -> {
                assert CmHandleState.READY == objectUnderTest.getCmHandleCompositeState(cmHandleId).cmHandleState
            })

        and: 'the CM-handle has expected moduleSetTag'
            assert objectUnderTest.getNcmpServiceCmHandle(cmHandleId).moduleSetTag == updatedModuleSetTag

        and: 'CM-handle has expected updated modules: M1 and M3'
            assert ['M1', 'M3'] == objectUnderTest.getYangResourcesModuleReferences(cmHandleId).moduleName.sort()

        cleanup: 'deregister CM-handle and remove all associated module resources'
            deregisterCmHandle(DMI1_URL, cmHandleId)
            cpsModuleService.deleteAllUnusedYangModuleData()

        where: 'following module set tags are used'
            initialModuleSetTag | updatedModuleSetTag
            NO_MODULE_SET_TAG   | NO_MODULE_SET_TAG
            NO_MODULE_SET_TAG   | 'new'
            'initial'           | NO_MODULE_SET_TAG
            'initial'           | 'new'
    }

    def 'Upgrade CM-handle with existing moduleSetTag.'() {
        given: 'DMI will return modules for registration'
            dmiDispatcher1.moduleNamesPerCmHandleId[cmHandleId] = ['M1', 'M2']
            dmiDispatcher1.moduleNamesPerCmHandleId[cmHandleIdWithExistingModuleSetTag] = ['M1', 'M3']
        and: "an existing CM-handle handle with moduleSetTag '${updatedModuleSetTag}'"
            registerCmHandle(DMI1_URL, cmHandleIdWithExistingModuleSetTag, updatedModuleSetTag)
            assert ['M1', 'M3'] == objectUnderTest.getYangResourcesModuleReferences(cmHandleIdWithExistingModuleSetTag).moduleName.sort()
        and: "a CM-handle with moduleSetTag '${initialModuleSetTag}' which will be upgraded"
            registerCmHandle(DMI1_URL, cmHandleId, initialModuleSetTag)
            assert ['M1', 'M2'] == objectUnderTest.getYangResourcesModuleReferences(cmHandleId).moduleName.sort()

        when: "CM-handle is upgraded to moduleSetTag '${updatedModuleSetTag}'"
            def cmHandlesToUpgrade = new UpgradedCmHandles(cmHandles: [cmHandleId], moduleSetTag: updatedModuleSetTag)
            def dmiPluginRegistrationResponse = objectUnderTest.updateDmiRegistration(
                    new DmiPluginRegistration(dmiPlugin: DMI1_URL, upgradedCmHandles: cmHandlesToUpgrade))

        then: 'registration gives successful response'
            assert dmiPluginRegistrationResponse.upgradedCmHandles == [CmHandleRegistrationResponse.createSuccessResponse(cmHandleId)]

        and: 'the module sync watchdog is triggered twice'
            2.times { moduleSyncWatchdog.moduleSyncAdvisedCmHandles() }

        and: 'CM-handle goes to READY state'
            new PollingConditions().within(MODULE_SYNC_WAIT_TIME_IN_SECONDS, () -> {
                assert CmHandleState.READY == objectUnderTest.getCmHandleCompositeState(cmHandleId).cmHandleState
            })

        and: 'the CM-handle has expected moduleSetTag'
            assert objectUnderTest.getNcmpServiceCmHandle(cmHandleId).moduleSetTag == updatedModuleSetTag

        and: 'CM-handle has expected updated modules: M1 and M3'
            assert ['M1', 'M3'] == objectUnderTest.getYangResourcesModuleReferences(cmHandleId).moduleName.sort()

        cleanup: 'deregister CM-handle'
            deregisterCmHandles(DMI1_URL, [cmHandleId, cmHandleIdWithExistingModuleSetTag])

        where:
            initialModuleSetTag | updatedModuleSetTag
            NO_MODULE_SET_TAG   | 'moduleSet2'
            'moduleSet1'        | 'moduleSet2'
    }

    def 'Skip upgrade of CM-handle with same moduleSetTag as before.'() {
        given: 'an existing CM-handle with expected initial modules: M1 and M2'
            dmiDispatcher1.moduleNamesPerCmHandleId[cmHandleId] = ['M1', 'M2']
            registerCmHandle(DMI1_URL, cmHandleId, 'same')
            assert ['M1', 'M2'] == objectUnderTest.getYangResourcesModuleReferences(cmHandleId).moduleName.sort()

        when: 'CM-handle is upgraded with the same moduleSetTag'
            def cmHandlesToUpgrade = new UpgradedCmHandles(cmHandles: [cmHandleId], moduleSetTag: 'same')
            objectUnderTest.updateDmiRegistration(
                    new DmiPluginRegistration(dmiPlugin: DMI1_URL, upgradedCmHandles: cmHandlesToUpgrade))

        then: 'CM-handle remains in READY state'
            assert CmHandleState.READY == objectUnderTest.getCmHandleCompositeState(cmHandleId).cmHandleState

        and: 'the CM-handle has same moduleSetTag as before'
            assert objectUnderTest.getNcmpServiceCmHandle(cmHandleId).moduleSetTag == 'same'

        then: 'CM-handle has same modules as before: M1 and M2'
            assert ['M1', 'M2'] == objectUnderTest.getYangResourcesModuleReferences(cmHandleId).moduleName.sort()

        cleanup: 'deregister CM-handle'
            deregisterCmHandle(DMI1_URL, cmHandleId)
    }

    def 'Upgrade of CM-handle fails due to DMI error.'() {
        given: 'a CM-handle exists'
            dmiDispatcher1.moduleNamesPerCmHandleId[cmHandleId] = ['M1', 'M2']
            registerCmHandle(DMI1_URL, cmHandleId, 'oldTag')
        and: 'DMI is not available for upgrade'
            dmiDispatcher1.isAvailable = false

        when: 'the CM-handle is upgraded'
            def cmHandlesToUpgrade = new UpgradedCmHandles(cmHandles: [cmHandleId], moduleSetTag: 'newTag')
            objectUnderTest.updateDmiRegistration(
                    new DmiPluginRegistration(dmiPlugin: DMI1_URL, upgradedCmHandles: cmHandlesToUpgrade))

        and: 'the module sync watchdog is triggered twice'
            2.times { moduleSyncWatchdog.moduleSyncAdvisedCmHandles() }

        then: 'CM-handle goes to LOCKED state with reason MODULE_UPGRADE_FAILED'
            new PollingConditions().within(MODULE_SYNC_WAIT_TIME_IN_SECONDS, () -> {
                def cmHandleCompositeState = objectUnderTest.getCmHandleCompositeState(cmHandleId)
                assert cmHandleCompositeState.cmHandleState == CmHandleState.LOCKED
                assert cmHandleCompositeState.lockReason.lockReasonCategory == LockReasonCategory.MODULE_UPGRADE_FAILED
            })

        and: 'the CM-handle has same moduleSetTag as before'
            assert objectUnderTest.getNcmpServiceCmHandle(cmHandleId).moduleSetTag == 'oldTag'

        cleanup: 'deregister CM-handle'
            deregisterCmHandle(DMI1_URL, cmHandleId)
    }

}
