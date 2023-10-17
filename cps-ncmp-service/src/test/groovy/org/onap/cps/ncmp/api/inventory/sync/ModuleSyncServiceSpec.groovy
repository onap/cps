/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.inventory.sync

import static org.onap.cps.ncmp.api.impl.inventory.LockReasonCategory.LOCKED_MISBEHAVING
import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DATASPACE_NAME
import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DMI_REGISTRY_ANCHOR
import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME
import static org.onap.cps.ncmp.api.impl.inventory.LockReasonCategory.MODULE_UPGRADE

import org.onap.cps.ncmp.api.impl.inventory.CmHandleState
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.DataNode
import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.impl.inventory.sync.ModuleSyncService
import org.onap.cps.ncmp.api.impl.operations.DmiModelOperations
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.impl.inventory.CmHandleQueries
import org.onap.cps.ncmp.api.impl.inventory.CompositeStateBuilder
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.spi.CascadeDeleteAllowed
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException
import org.onap.cps.spi.model.ModuleReference
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

class ModuleSyncServiceSpec extends Specification {

    def mockCpsModuleService = Mock(CpsModuleService)
    def mockDmiModelOperations = Mock(DmiModelOperations)
    def mockCpsAdminService = Mock(CpsAdminService)
    def mockCmHandleQueries = Mock(CmHandleQueries)
    def mockCpsDataService = Mock(CpsDataService)
    def mockJsonObjectMapper = Mock(JsonObjectMapper)

    def objectUnderTest = new ModuleSyncService(mockDmiModelOperations, mockCpsModuleService, mockCpsAdminService,
            mockCmHandleQueries, mockCpsDataService, mockJsonObjectMapper)

    def expectedDataspaceName = NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME

    def 'Sync model for a (new) cm handle with #scenario'() {
        given: 'a cm handle'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle()
            ncmpServiceCmHandle.setCompositeState(new CompositeStateBuilder().build())
            def dmiServiceName = 'some service name'
            ncmpServiceCmHandle.cmHandleId = 'cmHandleId-1'
            def yangModelCmHandle = YangModelCmHandle.toYangModelCmHandle(dmiServiceName, '', '', ncmpServiceCmHandle,'')
        and: 'DMI operations returns some module references'
            def moduleReferences =  [ new ModuleReference('module1','1'), new ModuleReference('module2','2') ]
            mockDmiModelOperations.getModuleReferences(yangModelCmHandle) >> moduleReferences
        and: 'CPS-Core returns list of existing module resources'
            mockCpsModuleService.getYangResourceModuleReferences(expectedDataspaceName) >> toModuleReference(existingModuleResourcesInCps)
        and: 'DMI-Plugin returns resource(s) for "new" module(s)'
            mockDmiModelOperations.getNewYangResourcesFromDmi(yangModelCmHandle, [new ModuleReference('module1', '1')]) >> newModuleNameContentToMap
        when: 'module sync is triggered'
            mockCpsModuleService.identifyNewModuleReferences(moduleReferences) >> toModuleReference(identifiedNewModuleReferences)
            objectUnderTest.syncAndCreateOrUpgradeSchemaSetAndAnchor(yangModelCmHandle)
        then: 'create schema set from module is invoked with correct parameters'
            1 * mockCpsModuleService.createOrUpgradeSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, 'cmHandleId-1', newModuleNameContentToMap, moduleReferences)
        and: 'anchor is created with the correct parameters'
            1 * mockCpsAdminService.createAnchor(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, 'cmHandleId-1', 'cmHandleId-1')
        where: 'the following parameters are used'
            scenario             | existingModuleResourcesInCps           | identifiedNewModuleReferences | newModuleNameContentToMap
            'one new module'     | [['module2' : '2'], ['module3' : '3']] | [['module1' : '1']]           | [module1: 'some yang source']
            'no add. properties' | [['module2' : '2'], ['module3' : '3']] | [['module1' : '1']]           | [module1: 'some yang source']
            'no new module'      | [['module1' : '1'], ['module2' : '2']] | []                            | [:]
    }

    def 'upgrade model for a existing cm handle'() {
        given: 'a cm handle that is ready but locked for upgrade'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle()
            ncmpServiceCmHandle.setCompositeState(new CompositeStateBuilder()
                .withLockReason(MODULE_UPGRADE, 'new moduleSetTag: targetModuleSetTag').build())
            ncmpServiceCmHandle.setCmHandleId('cmHandleId-1')
            def yangModelCmHandle = YangModelCmHandle.toYangModelCmHandle('some service name', '', '', ncmpServiceCmHandle, 'targetModuleSetTag')
            mockCmHandleQueries.cmHandleHasState('cmHandleId-1', CmHandleState.READY) >> true
        and: 'the module service returns some module references'
            def moduleReferences = [new ModuleReference('module1', '1'), new ModuleReference('module2', '2')]
            mockCpsModuleService.getYangResourcesModuleReferences(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR) >> moduleReferences
        and: 'a cm handle with the same moduleSetTag can be found in the registry'
            mockCmHandleQueries.queryNcmpRegistryByCpsPath("//cm-handles[@module-set-tag='targetModuleSetTag']",
                FetchDescendantsOption.OMIT_DESCENDANTS) >> [new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'cmHandleId-1\']', leaves: ['id': 'cmHandleId-1', 'cm-handle-state': 'READY'])]
        when: 'module upgrade is triggered'
            objectUnderTest.syncAndCreateOrUpgradeSchemaSetAndAnchor(yangModelCmHandle)
        then: 'the upgrade is delegated to the module service (with the correct parameters)'
            1 * mockCpsModuleService.createOrUpgradeSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, 'cmHandleId-1', Collections.emptyMap(), moduleReferences)
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

    def 'Extract module set tag with #scenario'() {
        given: 'the DB throws an exception because its Schema Set does not exist'
            mockCpsModuleService.deleteSchemaSet(*_) >> { throw new SchemaSetNotFoundException('some-dataspace-name', 'some-cmhandle-id') }
        when: 'delete schema set if exists is called'
            def result = objectUnderTest.extractModuleSetTag(new CompositeStateBuilder().withLockReason(lockReason, 'new moduleSetTag: targetModuleSetTag').build())
        then: 'an exception is thrown'
            result == expectedModuleSetTag
        where: 'the following parameters are used'
            scenario                    | lockReason         || expectedModuleSetTag
            'local reason is null'      | null               || ''
            'locked for module upgrade' | LOCKED_MISBEHAVING || ''
            'locked for other reason'   | MODULE_UPGRADE     || 'targetModuleSetTag'
    }

    def toModuleReference(moduleReferenceAsMap) {
        def moduleReferences = [].withDefault { [:] }
        moduleReferenceAsMap.forEach(property ->
            property.forEach((moduleName, revision) -> {
                moduleReferences.add(new ModuleReference('moduleName' : moduleName, 'revision' : revision))
            }))
        return moduleReferences
    }

}
