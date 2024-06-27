/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
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

package org.onap.cps.ncmp.impl.inventory.sync

import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.inventory.models.CompositeStateBuilder
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.spi.CascadeDeleteAllowed
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import org.onap.cps.spi.model.ModuleReference
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME
import static org.onap.cps.ncmp.impl.inventory.models.LockReasonCategory.MODULE_UPGRADE

class ModuleSyncServiceSpec extends Specification {

    def mockCpsModuleService = Mock(CpsModuleService)
    def mockDmiModelOperations = Mock(DmiModelOperations)
    def mockCpsAnchorService = Mock(CpsAnchorService)
    def mockCmHandleQueries = Mock(CmHandleQueryService)
    def mockCpsDataService = Mock(CpsDataService)
    def mockJsonObjectMapper = Mock(JsonObjectMapper)

    def objectUnderTest = new ModuleSyncService(mockDmiModelOperations, mockCpsModuleService,
            mockCmHandleQueries, mockCpsDataService, mockCpsAnchorService, mockJsonObjectMapper)

    def expectedDataspaceName = NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME
    def static cmHandleWithModuleSetTag = new DataNodeBuilder()
            .withXpath("/dmi-registry/cm-handles[@id='otherId']")
            .withLeaves(['id': 'otherId', 'module-set-tag': 'tag-1'])
            .withAnchor('otherId').build()

    def 'Sync model for a NEW cm handle using module set tags: #scenario.'() {
        given: 'a cm handle state to be synced'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle()
            ncmpServiceCmHandle.setCompositeState(new CompositeStateBuilder().withCmHandleState(CmHandleState.ADVISED).build())
            ncmpServiceCmHandle.cmHandleId = 'ch-1'
            def yangModelCmHandle = YangModelCmHandle.toYangModelCmHandle('some service name', '', '', ncmpServiceCmHandle, moduleSetTag, '', '')
        and: 'DMI operations returns some module references'
            def moduleReferences =  [ new ModuleReference('module1','1'), new ModuleReference('module2','2') ]
            mockDmiModelOperations.getModuleReferences(yangModelCmHandle) >> moduleReferences
        and: 'DMI-Plugin returns resource(s) for "new" module(s)'
            mockDmiModelOperations.getNewYangResourcesFromDmi(yangModelCmHandle, identifiedNewModuleReferences) >> newModuleNameContentToMap
        and: 'the module service identifies #identifiedNewModuleReferences.size() new modules'
            mockCpsModuleService.identifyNewModuleReferences(moduleReferences) >> identifiedNewModuleReferences
        and: 'system contains other cm handle with "same tag" (that is READY)'
            mockCmHandleQueries.queryNcmpRegistryByCpsPath(*_) >> existingCmHandlesWithSameTag
        when: 'module sync is triggered'
            objectUnderTest.syncAndCreateSchemaSetAndAnchor(yangModelCmHandle)
        then: 'create schema set from module is invoked with correct parameters'
            1 * mockCpsModuleService.createSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, 'ch-1', newModuleNameContentToMap, moduleReferences)
        and: 'anchor is created with the correct parameters'
            1 * mockCpsAnchorService.createAnchor(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, 'ch-1', 'ch-1')
        where: 'the following parameters are used'
            scenario                  | existingModuleResourcesInCps         | identifiedNewModuleReferences         | newModuleNameContentToMap     | moduleSetTag | existingCmHandlesWithSameTag
            'one new module, new tag' | [['module2': '2'], ['module3': '3']] | [new ModuleReference('module1', '1')] | [module1: 'some yang source'] | ''           | []
            'no new module, new tag'  | [['module1': '1'], ['module2': '2']] | []                                    | [:]                           | 'new-tag-1'  | []
            'same tag'                | [['module1': '1'], ['module2': '2']] | []                                    | [:]                           | 'same-tag'   | [cmHandleWithModuleSetTag]
    }

    def 'Upgrade model for an existing cm handle with Module Set Tag where the modules are #scenario'() {
        given: 'a cm handle being upgraded to module set tag: tag-1'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle()
            ncmpServiceCmHandle.setCompositeState(new CompositeStateBuilder().withLockReason(MODULE_UPGRADE, 'Upgrade to ModuleSetTag: tag-1').build())
            def dmiServiceName = 'some service name'
            ncmpServiceCmHandle.cmHandleId = 'upgraded-ch'
            def yangModelCmHandle = YangModelCmHandle.toYangModelCmHandle(dmiServiceName, '', '', ncmpServiceCmHandle,'tag-1', '', '')
        and: 'some module references'
            def moduleReferences =  [ new ModuleReference('module1','1') ]
        and: 'DMI operations returns some module references for upgraded cm handle'
            mockDmiModelOperations.getModuleReferences(yangModelCmHandle) >> moduleReferences
            mockDmiModelOperations.getNewYangResourcesFromDmi(_, []) >> [:]
        and: 'none of these module references are new (unknown to the system)'
            mockCpsModuleService.identifyNewModuleReferences(_) >> []
        and: 'CPS-Core returns list of existing module resources for TBD'
            mockCpsModuleService.getYangResourcesModuleReferences(*_) >> [ new ModuleReference('module1','1') ]
        and: 'system contains #existingCmHandlesWithSameTag.size() cm handles with same tag'
            mockCmHandleQueries.queryNcmpRegistryByCpsPath(*_) >> existingCmHandlesWithSameTag
        and: 'the other cm handle is a state ready'
            mockCmHandleQueries.cmHandleHasState('otherId', CmHandleState.READY) >> true
        when: 'module sync is triggered'
            objectUnderTest.syncAndUpgradeSchemaSet(yangModelCmHandle)
        then: 'update schema set from module is invoked for the upgraded cm handle'
            1 * mockCpsModuleService.upgradeSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, 'upgraded-ch', [:], moduleReferences)
        and: 'create schema set from module is not invoked for the upgraded cm handle'
            0 * mockCpsModuleService.createSchemaSetFromModules(*_)
        and: 'No anchor is created for the upgraded cm handle'
            0 * mockCpsAnchorService.createAnchor(*_)
        where: 'the following parameters are used'
            scenario      | existingCmHandlesWithSameTag
            'new'         | []
            'in database' | [cmHandleWithModuleSetTag]
    }

    def 'upgrade model for a existing cm handle'() {
        given: 'a cm handle that is ready but locked for upgrade'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle()
            ncmpServiceCmHandle.setCompositeState(new CompositeStateBuilder()
                .withLockReason(MODULE_UPGRADE, 'Upgrade to ModuleSetTag: targetModuleSetTag').build())
            ncmpServiceCmHandle.setCmHandleId('cmHandleId-1')
            def yangModelCmHandle = YangModelCmHandle.toYangModelCmHandle('some service name', '', '', ncmpServiceCmHandle, 'targetModuleSetTag', '', '')
            mockCmHandleQueries.cmHandleHasState('cmHandleId-1', CmHandleState.READY) >> true
        and: 'the module service returns some module references'
            def moduleReferences = [new ModuleReference('module1', '1'), new ModuleReference('module2', '2')]
            mockCpsModuleService.getYangResourcesModuleReferences(*_)>> moduleReferences
        and: 'a cm handle with the same moduleSetTag can be found in the registry'
            mockCmHandleQueries.queryNcmpRegistryByCpsPath(*_) >> [new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'cmHandleId-1\']', leaves: ['id': 'cmHandleId-1'],
                    childDataNodes: [new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'cmHandleId-1\']/state', leaves: ['cm-handle-state': 'READY'])])]
        when: 'module upgrade is triggered'
            objectUnderTest.syncAndUpgradeSchemaSet(yangModelCmHandle)
        then: 'the upgrade is delegated to the module service (with the correct parameters)'
            1 * mockCpsModuleService.upgradeSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, 'cmHandleId-1', Collections.emptyMap(), moduleReferences)
    }

    def 'Delete Schema Set for CmHandle'() {
        when: 'delete schema set if exists is called'
            objectUnderTest.deleteSchemaSetIfExists('some-cmhandle-id')
        then: 'the module service is invoked to delete the correct schema set'
            1 * mockCpsModuleService.deleteSchemaSet(expectedDataspaceName, 'some-cmhandle-id', CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED)
    }

    def 'Delete a non-existing Schema Set for CmHandle' () {
        given: 'the DB throws an exception because its Schema Set does not exist'
           mockCpsModuleService.deleteSchemaSet(*_) >> { throw new SchemaSetNotFoundException('some-dataspace-name', 'some-cmhandle-id') }
        when: 'delete schema set if exists is called'
            objectUnderTest.deleteSchemaSetIfExists('some-cmhandle-id')
        then: 'the exception from the DB is ignored; there are no exceptions'
            noExceptionThrown()
    }

    def 'Delete Schema Set for CmHandle with other exception' () {
        given: 'an exception other than SchemaSetNotFoundException is thrown'
            UnsupportedOperationException unsupportedOperationException = new UnsupportedOperationException();
            1 * mockCpsModuleService.deleteSchemaSet(*_) >> { throw unsupportedOperationException }
        when: 'delete schema set if exists is called'
            objectUnderTest.deleteSchemaSetIfExists('some-cmhandle-id')
        then: 'an exception is thrown'
            def result = thrown(UnsupportedOperationException)
            result == unsupportedOperationException
    }

}
