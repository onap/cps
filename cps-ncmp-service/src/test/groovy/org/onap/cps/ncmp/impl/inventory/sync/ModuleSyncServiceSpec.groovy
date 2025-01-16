/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2025 Nordix Foundation
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
import org.onap.cps.api.exceptions.AlreadyDefinedException
import org.onap.cps.api.exceptions.DuplicatedYangResourceException
import org.onap.cps.api.model.ModuleReference
import org.onap.cps.ncmp.api.inventory.models.CompositeStateBuilder
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService
import org.onap.cps.ncmp.api.inventory.models.CmHandleState
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME
import static org.onap.cps.ncmp.api.inventory.models.LockReasonCategory.MODULE_UPGRADE

class ModuleSyncServiceSpec extends Specification {

    def mockCpsModuleService = Mock(CpsModuleService)
    def mockDmiModelOperations = Mock(DmiModelOperations)
    def mockCpsAnchorService = Mock(CpsAnchorService)
    def mockCmHandleQueries = Mock(CmHandleQueryService)
    def mockCpsDataService = Mock(CpsDataService)
    def mockJsonObjectMapper = Mock(JsonObjectMapper)

    def objectUnderTest = new ModuleSyncService(mockDmiModelOperations, mockCpsModuleService, mockCpsDataService, mockCpsAnchorService, mockJsonObjectMapper)

    def 'Sync models for a NEW cm handle using module set tags: #scenario.'() {
        given: 'a cm handle to be synced'
            def yangModelCmHandle = createAdvisedCmHandle(moduleSetTag)
        and: 'DMI operations returns some module references'
            def moduleReferences =  [ new ModuleReference('module1','1'), new ModuleReference('module2','2') ]
            mockDmiModelOperations.getModuleReferences(yangModelCmHandle) >> moduleReferences
        and: 'DMI-Plugin returns resource(s) for "new" module(s)'
            mockDmiModelOperations.getNewYangResourcesFromDmi(yangModelCmHandle, identifiedNewModuleReferences) >> newModuleNameContentToMap
        and: 'the module service identifies #identifiedNewModuleReferences.size() new modules'
            mockCpsModuleService.identifyNewModuleReferences(moduleReferences) >> identifiedNewModuleReferences
        when: 'module sync is triggered'
            objectUnderTest.syncAndCreateSchemaSetAndAnchor(yangModelCmHandle)
        then: 'create schema set from module is invoked with correct parameters'
            1 * mockCpsModuleService.createSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, expectedSchemaSetName, newModuleNameContentToMap, moduleReferences)
        and: 'anchor is created with the correct parameters'
            1 * mockCpsAnchorService.createAnchor(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, expectedSchemaSetName, 'ch-1')
        where: 'the following parameters are used'
            scenario                  | identifiedNewModuleReferences         | newModuleNameContentToMap     | moduleSetTag | existingModuleReferences                                                   || expectedSchemaSetName
            'one new module, new tag' | [new ModuleReference('module1', '1')] | [module1: 'some yang source'] | ''           | []                                                                         || 'ch-1'
            'no new module, new tag'  | []                                    | [:]                           | 'new-tag'    | []                                                                         || 'new-tag'
            'same tag'                | []                                    | [:]                           | 'same-tag'   | [new ModuleReference('module1', '1'), new ModuleReference('module2', '2')] || 'same-tag'
    }

    def 'Attempt Sync models for a cm handle with exception and #scenario module set tag.'() {
        given: 'a cm handle to be synced'
            def yangModelCmHandle = createAdvisedCmHandle(moduleSetTag)
        and: 'dmi returns no new yang resources'
            mockDmiModelOperations.getNewYangResourcesFromDmi(*_) >> [:]
        and: 'exception occurs when trying to store result'
            def testException = new RuntimeException('test')
            mockCpsModuleService.createSchemaSetFromModules(*_) >> { throw testException }
        when: 'module sync is triggered'
            objectUnderTest.syncAndCreateSchemaSetAndAnchor(yangModelCmHandle)
        then: 'the same exception is thrown up'
            def exceptionThrown = thrown(Exception)
            assert testException == exceptionThrown
        where: 'following module set tags are used'
            scenario  | moduleSetTag
            'with'    | 'new-tag'
            'without' | ''
    }

    def 'Attempt Sync models for a cm handle with existing schema set (#exception).'() {
        given: 'a cm handle to be synced'
            def yangModelCmHandle = createAdvisedCmHandle('existing tag')
        and: 'dmi returns no new yang resources'
            mockDmiModelOperations.getNewYangResourcesFromDmi(*_) >> [:]
        and: 'already defined exception occurs when creating schema (existing)'
            mockCpsModuleService.createSchemaSetFromModules(*_) >> { throw exception  }
        when: 'module sync is triggered'
            objectUnderTest.syncAndCreateSchemaSetAndAnchor(yangModelCmHandle)
        then: 'no exception is thrown up'
            noExceptionThrown()
        where: 'following exceptions occur'
            exception << [ AlreadyDefinedException.forSchemaSet('', '', null),
                           new DuplicatedYangResourceException('', '', null) ]
    }

    def 'Model upgrade without using Module Set Tags (legacy) where the modules are in database.'() {
        given: 'a cm handle being upgraded without using module set tags'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle()
            ncmpServiceCmHandle.setCompositeState(new CompositeStateBuilder().withLockReason(MODULE_UPGRADE, '').build())
            def dmiServiceName = 'some service name'
            ncmpServiceCmHandle.cmHandleId = 'upgraded-ch'
            def yangModelCmHandle = YangModelCmHandle.toYangModelCmHandle(dmiServiceName, '', '', ncmpServiceCmHandle,'', '', '')
        and: 'DMI operations returns some module references for upgraded cm handle'
            def moduleReferences =  [ new ModuleReference('module1','1') ]
            mockDmiModelOperations.getModuleReferences(yangModelCmHandle) >> moduleReferences
            mockDmiModelOperations.getNewYangResourcesFromDmi(_, []) >> [:]
        and: 'none of these module references are new (all already known to the system)'
            mockCpsModuleService.identifyNewModuleReferences(_) >> []
        when: 'module sync is triggered'
            objectUnderTest.syncAndUpgradeSchemaSet(yangModelCmHandle)
        then: 'update schema set from module is invoked for the upgraded cm handle'
            1 * mockCpsModuleService.upgradeSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, 'upgraded-ch', [:], moduleReferences)
        and: 'No anchor is created for the upgraded cm handle'
            0 * mockCpsAnchorService.createAnchor(*_)
    }

    def 'Model upgrade using to existing Module Set Tag'() {
        given: 'a cm handle that is ready but locked for upgrade'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle()
            ncmpServiceCmHandle.setCompositeState(new CompositeStateBuilder().withLockReason(MODULE_UPGRADE, 'Upgrade to ModuleSetTag: ' + tagTo).build())
            ncmpServiceCmHandle.setCmHandleId('cmHandleId-1')
            def yangModelCmHandle = YangModelCmHandle.toYangModelCmHandle('some service name', '', '', ncmpServiceCmHandle, tagFrom, '', '')
            mockCmHandleQueries.cmHandleHasState('cmHandleId-1', CmHandleState.READY) >> true
        and: 'the module tag (schemaset) exists is #schemaExists'
            mockCpsModuleService.schemaSetExists(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, tagTo) >> schemaExists
        and: 'DMI operations returns some module references for upgraded cm handle'
            def moduleReferences =  [ new ModuleReference('module1','1') ]
            expectedCallsToDmi * mockDmiModelOperations.getModuleReferences(yangModelCmHandle) >> moduleReferences
        and: 'dmi returns no new yang resources'
            mockDmiModelOperations.getNewYangResourcesFromDmi(*_) >> [:]
        and: 'none of these module references are new (all already known to the system)'
            expectedCallsToModuleService * mockCpsModuleService.identifyNewModuleReferences(_) >> []
        when: 'module upgrade is triggered'
            objectUnderTest.syncAndUpgradeSchemaSet(yangModelCmHandle)
        then: 'the upgrade is delegated to the anchor service (with the correct parameters) except when new tag is blank'
            expectedCallsToAnchorService * mockCpsAnchorService.updateAnchorSchemaSet(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, 'cmHandleId-1', tagTo)
        where: 'with or without from tag'
            scenario                         | schemaExists | tagFrom  | tagTo  || expectedCallsToDmi | expectedCallsToModuleService | expectedCallsToAnchorService
            'from no tag to existing tag'    | true         | ''       | 'tagTo'|| 0                  | 0                            | 1
            'from tag to other existing tag' | true         | 'oldTag' | 'tagTo'|| 0                  | 0                            | 1
            'to new tag'                     | false        | 'oldTag' | 'tagTo'|| 1                  | 1                            | 1
            'to NO tag'                      | true         | 'oldTag' | ''     || 1                  | 1                            | 0
    }

    def createAdvisedCmHandle(moduleSetTag) {
        def ncmpServiceCmHandle = new NcmpServiceCmHandle()
        ncmpServiceCmHandle.setCompositeState(new CompositeStateBuilder().withCmHandleState(CmHandleState.ADVISED).build())
        ncmpServiceCmHandle.cmHandleId = 'ch-1'
        return YangModelCmHandle.toYangModelCmHandle('some service name', '', '', ncmpServiceCmHandle, moduleSetTag, '', '')
    }

}
