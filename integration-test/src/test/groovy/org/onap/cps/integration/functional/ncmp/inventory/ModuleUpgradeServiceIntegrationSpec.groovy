/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.integration.functional.ncmp.inventory

import org.onap.cps.api.model.DataNode
import org.onap.cps.api.model.ModuleReference
import org.onap.cps.api.parameters.FetchDescendantsOption
import org.onap.cps.integration.base.FunctionalSpecBase
import org.onap.cps.ncmp.api.inventory.models.*
import org.onap.cps.ncmp.impl.NetworkCmProxyInventoryFacadeImpl
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.utils.YangDataConverter

class ModuleUpgradeServiceIntegrationSpec extends FunctionalSpecBase {

    NetworkCmProxyInventoryFacadeImpl objectUnderTest
    def cmHandleId = 'ch-1'

    def setup() {
        objectUnderTest = networkCmProxyInventoryFacade
    }

    def 'CM Handle registry (inventory) model upgrade poc (incl. backward compatibility)'() {
        given: 'DMI plugin provides initial modules for the CM handle'
            dmiDispatcher1.moduleNamesPerCmHandleId[cmHandleId] = ['M1', 'M2']
        and: 'NCMP already has an existing module reference (old revision)'
            def existingModule = new ModuleReference(moduleName: "dmi-registry", revision: "2024-02-23")
            cpsModulePersistenceService.getYangResourceModuleReferences(_, _) >> [existingModule]
        when: 'A CM-handle is registered'
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin: DMI1_URL)
            dmiPluginRegistration.setCreatedCmHandles([new NcmpServiceCmHandle(cmHandleId: cmHandleId, additionalProperties: ['addProp1': 'some-value'])])
            def dmiPluginRegistrationResponse = objectUnderTest.updateDmiRegistration(dmiPluginRegistration)
        then: 'The CM-handle registration succeeds'
            assert dmiPluginRegistrationResponse.createdCmHandles == [CmHandleRegistrationResponse.createSuccessResponse(cmHandleId)]
        and: 'The CM-handle is initialized with ADVISED state'
            assert CmHandleState.ADVISED == objectUnderTest.getCmHandleCompositeState(cmHandleId).cmHandleState
        then:  'The module sync watchdog is invoked for advised CM-handles'
            moduleSyncWatchdog.moduleSyncAdvisedCmHandles()
        then: 'After module sync, the CM-handle transitions to READY state'
            assert CmHandleState.READY == objectUnderTest.getCmHandleCompositeState(cmHandleId).cmHandleState
        when: 'A new version of the dmi-registry module (upgrade) is available'
            def newYangContent = readResourceDataFile('inventory/dmi-registry@2025-07-22.yang')
            def newYangResourceContentPerName = ["dmi-registry@2025-07-22.yang": newYangContent]
        then: 'The schema set is upgraded with the new module revision'
            if (!cpsModulePersistenceService.schemaSetExists('NCMP-Admin', 'dmi-registry-2025-07-22')) {
                    cpsModulePersistenceService.createSchemaSet('NCMP-Admin', 'dmi-registry-2025-07-22', newYangResourceContentPerName)
            }
            cpsAnchorService.updateAnchorSchemaSet('NCMP-Admin','ncmp-dmi-registry','dmi-registry-2025-07-22')
        when: 'that state gets updated to a different value'
            final Collection<DataNode> cmHandleDataNodes = inventoryPersistence.getCmHandleDataNodeByCmHandleId('ch-1', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
            YangModelCmHandle yangModelCmHandle= YangDataConverter.toYangModelCmHandle(cmHandleDataNodes[0])
            CompositeState compositeState= yangModelCmHandle.getCompositeState()
            compositeState.setCmHandleState(CmHandleState.LOCKED)
        then: 'the CM handle gets saved'
            inventoryPersistence.saveCmHandleState(cmHandleId, compositeState)
        and: 'we load the CM handle again'
            final Collection<DataNode> updatedCmHandleDataNodes = inventoryPersistence.getCmHandleDataNodeByCmHandleId(cmHandleId, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
            YangModelCmHandle updatedYangModelCmHandle= YangDataConverter.toYangModelCmHandle(updatedCmHandleDataNodes[0])
        and: 'the state has the new value i.e. load and save worked successfully'
            assert updatedYangModelCmHandle.getCompositeState().cmHandleState == CmHandleState.LOCKED
        when: 'The CM-handle additional properties are updated'
            def dmiPluginRegistrationToUpdate = new DmiPluginRegistration(dmiPlugin: DMI1_URL)
            dmiPluginRegistrationToUpdate.setUpdatedCmHandles([new NcmpServiceCmHandle(cmHandleId: cmHandleId, additionalProperties: ['addProp1': 'value1','addProp2': 'value2'])])
            def updatedDmiPluginRegistrationResponse = objectUnderTest.updateDmiRegistration(dmiPluginRegistrationToUpdate)
        then: 'The update response confirms SUCCESS for the CM-handle'
            assert updatedDmiPluginRegistrationResponse.updatedCmHandles.size() == 1
            def updatedHandleResponse = updatedDmiPluginRegistrationResponse.updatedCmHandles[0]
            assert updatedHandleResponse.cmHandle == cmHandleId
            assert updatedHandleResponse.status == CmHandleRegistrationResponse.Status.SUCCESS
        and: 'Reloaded CM-handle contains the new additional properties (backward compatibility preserved)'
            def updatedCmHandleDataNodesAfterUpdate  = inventoryPersistence.getCmHandleDataNodeByCmHandleId(cmHandleId, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
            def reloadedCmHandleAfterUpdate= YangDataConverter.toYangModelCmHandle(updatedCmHandleDataNodesAfterUpdate[0])
            assert reloadedCmHandleAfterUpdate.additionalProperties.collectEntries { [it.name, it.value] } == [addProp1: "value1", addProp2: "value2"]
        cleanup: 'deregister CM handle'
            deregisterCmHandle(DMI1_URL, cmHandleId)
    }

}
