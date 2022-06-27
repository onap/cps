/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.impl.operations.DmiModelOperations
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeState
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.spi.CascadeDeleteAllowed
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException
import org.onap.cps.spi.model.ModuleReference
import spock.lang.Specification

class ModuleSyncServiceSpec extends Specification {


    def mockCpsModuleService = Mock(CpsModuleService)
    def mockDmiModelOperations = Mock(DmiModelOperations)
    def mockCpsAdminService = Mock(CpsAdminService)

    def objectUnderTest = new ModuleSyncService(mockDmiModelOperations, mockCpsModuleService, mockCpsAdminService)

    def expectedDataspaceName = 'NFP-Operational'

    def 'Sync model for a (new) cm handle with #scenario'() {
        given: 'a cm handle'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle()
            def dmiServiceName = 'some service name'
            ncmpServiceCmHandle.cmHandleId = 'cmHandleId-1'
            def yangModelCmHandle = YangModelCmHandle.toYangModelCmHandle(dmiServiceName, '', '', ncmpServiceCmHandle)
        and: 'DMI operations returns some module references'
            def moduleReferences =  [ new ModuleReference(moduleName:'module1',revision:'1'),
                                                            new ModuleReference(moduleName:'module2',revision:'2') ]
            mockDmiModelOperations.getModuleReferences(yangModelCmHandle) >> moduleReferences
        and: 'CPS-Core returns list of existing module resources'
            mockCpsModuleService.getYangResourceModuleReferences(expectedDataspaceName) >> toModuleReference(existingModuleResourcesInCps)
        and: 'DMI-Plugin returns resource(s) for "new" module(s)'
            mockDmiModelOperations.getNewYangResourcesFromDmi(yangModelCmHandle, [new ModuleReference('module1', '1')]) >> newModuleNameContentToMap
        when: 'module sync is triggered'
            mockCpsModuleService.identifyNewModuleReferences(moduleReferences) >> toModuleReference(identifiedNewModuleReferences)
            objectUnderTest.syncAndCreateSchemaSetAndAnchor(yangModelCmHandle)
        then: 'create schema set from module is invoked with correct parameters'
            1 * mockCpsModuleService.createSchemaSetFromModules('NFP-Operational', 'cmHandleId-1', newModuleNameContentToMap, existingModuleReferencesInCps)
        and: 'anchor is created with the correct parameters'
            1 * mockCpsAdminService.createAnchor('NFP-Operational', 'cmHandleId-1', 'cmHandleId-1')
        where: 'the following parameters are used'
            scenario             | existingModuleResourcesInCps           | identifiedNewModuleReferences | newModuleNameContentToMap       | existingModuleReferencesInCps
            'one new module'     | [['module2' : '2'], ['module3' : '3']] | [['module1' : '1']]           | [module1: 'some yang source']   | [new ModuleReference(moduleName:'module2',revision:'2')]
            'no add. properties' | [['module2' : '2'], ['module3' : '3']] | [['module1' : '1']]           | [module1: 'some yang source']   | [new ModuleReference(moduleName:'module2',revision:'2')]
            'no new module'      | [['module1' : '1'], ['module2' : '2']] | []                            | [:]                             | [new ModuleReference(moduleName:'module1',revision:'1'), new ModuleReference(moduleName:'module2',revision:'2')]
    }

    def 'Delete Schema Set for CmHandle where the CmHandle has not already been synced' () {
        given: 'a CmHandle in the advised state'
            def cmHandle = new YangModelCmHandle(id: 'some-cmhandle-id', compositeState: new CompositeState(cmHandleState: CmHandleState.ADVISED))
        when: 'the Schema Set does not exist for the CmHandle'
            1 * mockCpsModuleService.deleteSchemaSet(_ as String, 'some-cmhandle-id',
                CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED) >> { throw new SchemaSetNotFoundException('some-dataspace-name', 'some-cmhandle-id') }
        and: 'delete schema set if exists is called'
            objectUnderTest.deleteSchemaSetIfExists(cmHandle)
        then: 'there are no exceptions'
            noExceptionThrown()
    }

    def 'Delete Schema Set for CmHandle where the CmHandle has already been synced' () {
        given: 'a CmHandle in the advised state'
            def cmHandle = new YangModelCmHandle(id: 'some-cmhandle-id', compositeState: new CompositeState(cmHandleState: CmHandleState.ADVISED))
        when: 'the Schema Set exists for the CmHandle'
            1 * mockCpsModuleService.deleteSchemaSet(_ as String, 'some-cmhandle-id',
                CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED)
        and: 'delete schema set if exists is called'
            objectUnderTest.deleteSchemaSetIfExists(cmHandle)
        then: 'there are no exceptions'
            noExceptionThrown()
    }

    def 'Delete Schema Set for CmHandle where there is an Error other than SchemaSetNotFound' () {
        given: 'a CmHandle in the advised state'
            def cmHandle = new YangModelCmHandle(id: 'some-cmhandle-id', compositeState: new CompositeState(cmHandleState: CmHandleState.ADVISED))
        when: 'the Schema Set exists for the CmHandle'
            1 * mockCpsModuleService.deleteSchemaSet(_ as String, 'some-cmhandle-id',
                CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED) >> { throw new Exception() }
        and: 'delete schema set if exists is called'
            objectUnderTest.deleteSchemaSetIfExists(cmHandle)
        then: 'there is an exception thrown'
            thrown(Exception)
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
