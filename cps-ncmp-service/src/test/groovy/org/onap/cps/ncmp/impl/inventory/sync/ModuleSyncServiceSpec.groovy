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

import com.hazelcast.collection.ISet
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.exceptions.NcmpException
import org.onap.cps.ncmp.api.inventory.models.CompositeStateBuilder
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.inventory.sync.ModuleSyncService.ModuleDelta
import org.onap.cps.spi.CascadeDeleteAllowed
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException
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
    def mockModuleSetTagsBeingProcessed = Mock(ISet<String>);

    def objectUnderTest = new ModuleSyncService(mockDmiModelOperations, mockCpsModuleService, mockCpsDataService, mockCpsAnchorService, mockJsonObjectMapper, mockModuleSetTagsBeingProcessed)

    def expectedDataspaceName = NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME

    def setup() {
        // Allow tags for al test except 'duplicate-processing-tag' to be added to processing semaphore
        mockModuleSetTagsBeingProcessed.add('new-tag') >> true
        mockModuleSetTagsBeingProcessed.add('same-tag') >> true
        mockModuleSetTagsBeingProcessed.add('cached-tag') >> true
    }

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
        and: 'the service returns a list of module references when queried with the specified attributes'
            mockCpsModuleService.getModuleReferencesByAttribute(*_) >> existingModuleReferences
        when: 'module sync is triggered'
            objectUnderTest.syncAndCreateSchemaSetAndAnchor(yangModelCmHandle)
        then: 'create schema set from module is invoked with correct parameters'
            1 * mockCpsModuleService.createSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, 'ch-1', newModuleNameContentToMap, moduleReferences)
        and: 'anchor is created with the correct parameters'
            1 * mockCpsAnchorService.createAnchor(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, 'ch-1', 'ch-1')
        where: 'the following parameters are used'
            scenario                  | identifiedNewModuleReferences         | newModuleNameContentToMap     | moduleSetTag | existingModuleReferences
            'one new module, new tag' | [new ModuleReference('module1', '1')] | [module1: 'some yang source'] | ''           | []
            'no new module, new tag'  | []                                    | [:]                           | 'new-tag'    | []
            'same tag'                | []                                    | [:]                           | 'same-tag'   | [new ModuleReference('module1', '1'), new ModuleReference('module2', '2')]
    }

    def 'Attempt Sync models for a cm handle with exception and #scenario module set tag'() {
        given: 'a cm handle to be synced'
            def yangModelCmHandle = createAdvisedCmHandle(moduleSetTag)
        and: 'the service returns a list of module references when queried with the specified attributes'
            mockCpsModuleService.getModuleReferencesByAttribute(*_) >> [new ModuleReference('module1', '1')]
        and: 'exception occurs when trying to store result'
            def testException = new RuntimeException('test')
            mockCpsModuleService.createSchemaSetFromModules(*_) >> { throw testException }
        when: 'module sync is triggered'
            objectUnderTest.syncAndCreateSchemaSetAndAnchor(yangModelCmHandle)
        then: 'the same exception is thrown up'
            def exceptionThrown = thrown(Exception)
            assert testException == exceptionThrown
        and: 'module set tag is removed from processing semaphores only when needed'
            expectedCallsToRemoveTag * mockModuleSetTagsBeingProcessed.remove('new-tag')
        where: 'following module set tags are used'
            scenario  | moduleSetTag || expectedCallsToRemoveTag
            'with'    | 'new-tag'    || 1
            'without' | ' '          || 0
    }

    def 'Sync models for a cm handle with previously cached module set tag.'() {
        given: 'a cm handle to be synced'
            def yangModelCmHandle = createAdvisedCmHandle('cached-tag')
        and: 'The module set tag exists in the private cache'
            def moduleReferences = [ new ModuleReference('module1','1') ]
            def cachedModuleDelta = new ModuleDelta(moduleReferences, [:])
            objectUnderTest.privateModuleSetCache.put('cached-tag', cachedModuleDelta)
        when: 'module sync is triggered'
            objectUnderTest.syncAndCreateSchemaSetAndAnchor(yangModelCmHandle)
        then: 'create schema set from module is invoked with correct parameters'
            1 * mockCpsModuleService.createSchemaSetFromModules(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, 'ch-1', [:], moduleReferences)
        and: 'anchor is created with the correct parameters'
            1 * mockCpsAnchorService.createAnchor(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, 'ch-1', 'ch-1')
    }

    def 'Attempt to sync using a module set tag already being processed by a different instance or thread.'() {
        given: 'a cm handle to be synced'
            def yangModelCmHandle = createAdvisedCmHandle('duplicateTag')
        and: 'The module set tag already exists in the processing semaphore set'
            mockModuleSetTagsBeingProcessed.add('duplicate-processing-tag') > false
        when: 'module sync is triggered'
            objectUnderTest.syncAndCreateSchemaSetAndAnchor(yangModelCmHandle)
        then: 'a ncmp exception is thrown with the relevant details'
            def exceptionThrown = thrown(NcmpException)
            assert exceptionThrown.message.contains('duplicateTag')
            assert exceptionThrown.details.contains('duplicateTag')
            assert exceptionThrown.details.contains('ch-1')
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
        and: 'the service returns a list of module references when queried with the specified attributes'
            mockCpsModuleService.getModuleReferencesByAttribute(*_) >> existingModuleReferences
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
            scenario      | existingModuleReferences
            'new'         | []
            'in database' | [new ModuleReference('module1', '1')]
    }

    def 'upgrade model for an existing cm handle'() {
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
        and: 'the service returns a list of module references when queried with the specified attributes'
            mockCpsModuleService.getModuleReferencesByAttribute(*_) >> moduleReferences
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

    def 'Clear module set cache.'() {
        given: 'something in the module set cache'
            objectUnderTest.privateModuleSetCache.put('test',new ModuleDelta([],[:]))
        when: 'the cache is cleared'
            objectUnderTest.clearPrivateModuleSetCache()
        then: 'the cache is empty'
            objectUnderTest.privateModuleSetCache.isEmpty()
    }

    def createAdvisedCmHandle(moduleSetTag) {
        def ncmpServiceCmHandle = new NcmpServiceCmHandle()
        ncmpServiceCmHandle.setCompositeState(new CompositeStateBuilder().withCmHandleState(CmHandleState.ADVISED).build())
        ncmpServiceCmHandle.cmHandleId = 'ch-1'
        return YangModelCmHandle.toYangModelCmHandle('some service name', '', '', ncmpServiceCmHandle, moduleSetTag, '', '')
    }

}
