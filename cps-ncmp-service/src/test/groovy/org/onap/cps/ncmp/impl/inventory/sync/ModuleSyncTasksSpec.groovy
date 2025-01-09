/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada
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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.hazelcast.config.Config
import com.hazelcast.core.Hazelcast
import com.hazelcast.instance.impl.HazelcastInstanceFactory
import com.hazelcast.map.IMap
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.api.inventory.models.CompositeStateBuilder
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventsCmHandleStateHandler
import org.onap.cps.api.exceptions.DataNodeNotFoundException
import org.slf4j.LoggerFactory
import spock.lang.Specification
import java.util.concurrent.atomic.AtomicInteger

import static org.onap.cps.ncmp.api.inventory.models.LockReasonCategory.MODULE_SYNC_FAILED
import static org.onap.cps.ncmp.api.inventory.models.LockReasonCategory.MODULE_UPGRADE
import static org.onap.cps.ncmp.api.inventory.models.LockReasonCategory.MODULE_UPGRADE_FAILED

class ModuleSyncTasksSpec extends Specification {

    def logger = Spy(ListAppender<ILoggingEvent>)

    void setup() {
        ((Logger) LoggerFactory.getLogger(ModuleSyncTasks.class)).addAppender(logger)
        logger.start()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(ModuleSyncTasks.class)).detachAndStopAllAppenders()
    }

    def mockInventoryPersistence = Mock(InventoryPersistence)

    def mockSyncUtils = Mock(ModuleOperationsUtils)

    def mockModuleSyncService = Mock(ModuleSyncService)

    def mockLcmEventsCmHandleStateHandler = Mock(LcmEventsCmHandleStateHandler)

    IMap<String, Object> moduleSyncStartedOnCmHandles = HazelcastInstanceFactory
            .getOrCreateHazelcastInstance(new Config('hazelcastInstanceName'))
            .getMap('mapInstanceName')

    def batchCount = new AtomicInteger(5)

    def objectUnderTest = new ModuleSyncTasks(mockInventoryPersistence, mockSyncUtils, mockModuleSyncService,
            mockLcmEventsCmHandleStateHandler, moduleSyncStartedOnCmHandles)

    def cleanupSpec() {
        Hazelcast.getHazelcastInstanceByName('hazelcastInstanceName').shutdown()
    }

    def 'Module Sync ADVISED cm handles.'() {
        given: 'cm handles in an ADVISED state'
            def cmHandle1 = cmHandleByIdAndState('cm-handle-1', CmHandleState.ADVISED)
            def cmHandle2 = cmHandleByIdAndState('cm-handle-2', CmHandleState.ADVISED)
        and: 'the inventory persistence cm handle returns a ADVISED state for the handles'
            mockInventoryPersistence.getYangModelCmHandle('cm-handle-1') >> cmHandle1
            mockInventoryPersistence.getYangModelCmHandle('cm-handle-2') >> cmHandle2
        when: 'module sync poll is executed'
            objectUnderTest.performModuleSync(['cm-handle-1', 'cm-handle-2'], batchCount)
        then: 'module sync service deletes schemas set of each cm handle if it already exists'
            1 * mockModuleSyncService.deleteSchemaSetIfExists('cm-handle-1')
            1 * mockModuleSyncService.deleteSchemaSetIfExists('cm-handle-2')
        and: 'module sync service is invoked for each cm handle'
            1 * mockModuleSyncService.syncAndCreateSchemaSetAndAnchor(_) >> { args -> assert args[0].id == 'cm-handle-1' }
            1 * mockModuleSyncService.syncAndCreateSchemaSetAndAnchor(_) >> { args -> assert args[0].id == 'cm-handle-2' }
        and: 'the state handler is called for the both cm handles'
            1 * mockLcmEventsCmHandleStateHandler.updateCmHandleStateBatch(_) >> { args ->
                assertBatch(args, ['cm-handle-1', 'cm-handle-2'], CmHandleState.READY)
            }
        and: 'batch count is decremented by one'
            assert batchCount.get() == 4
    }

    def 'Handle CM handle failure during #scenario and log MODULE_UPGRADE lock reason'() {
        given: 'a CM handle in ADVISED state with a specific lock reason'
            def cmHandle = cmHandleByIdAndState('cm-handle', CmHandleState.ADVISED)
            cmHandle.compositeState.lockReason = CompositeState.LockReason.builder().lockReasonCategory(lockReasonCategory).details(lockReasonDetails).build()
            mockInventoryPersistence.getYangModelCmHandle('cm-handle') >> cmHandle
        and: 'module sync service attempts to sync/upgrade the CM handle and throws an exception'
            mockModuleSyncService.syncAndCreateSchemaSetAndAnchor(_) >> { throw new Exception('some exception') }
            mockModuleSyncService.syncAndUpgradeSchemaSet(_) >> { throw new Exception('some exception') }
        when: 'module sync is executed'
            objectUnderTest.performModuleSync(['cm-handle'], batchCount)
        then: 'lock reason is updated with number of attempts'
            1 * mockSyncUtils.updateLockReasonWithAttempts(_, expectedLockReasonCategory, 'some exception')
        and: 'the state handler is called to update the state to LOCKED'
            1 * mockLcmEventsCmHandleStateHandler.updateCmHandleStateBatch(_) >> { args ->
                assertBatch(args, ['cm-handle'], CmHandleState.LOCKED)
            }
        and: 'batch count is decremented by one'
            assert batchCount.get() == 4
        where:
            scenario         | lockReasonCategory    | lockReasonDetails                              || expectedLockReasonCategory
            'module sync'    | MODULE_SYNC_FAILED    | 'some lock details'                            || MODULE_SYNC_FAILED
            'module upgrade' | MODULE_UPGRADE_FAILED | 'Upgrade to ModuleSetTag: some-module-set-tag' || MODULE_UPGRADE_FAILED
            'module upgrade' | MODULE_UPGRADE        | 'Upgrade in progress'                          || MODULE_UPGRADE_FAILED
    }

    def 'Module sync succeeds even if a handle gets deleted during module sync.'() {
        given: 'a cm handle which has been deleted'
            mockInventoryPersistence.getYangModelCmHandle('cm-handle-1') >> { throw new DataNodeNotFoundException('dataspace', 'anchor', 'cm-handle-1') }
        and: 'a cm handle which is being deleted'
            mockInventoryPersistence.getYangModelCmHandle('cm-handle-2') >> cmHandleByIdAndState('cm-handle-2', CmHandleState.DELETING)
        and: 'a cm handle in advised state'
            mockInventoryPersistence.getYangModelCmHandle('cm-handle-3') >> cmHandleByIdAndState('cm-handle-3', CmHandleState.ADVISED)
        when: 'module sync poll is executed'
            objectUnderTest.performModuleSync(['cm-handle-1', 'cm-handle-2', 'cm-handle-3'], batchCount)
        then: 'no exception is thrown'
            noExceptionThrown()
        and: 'the deleted cm-handle did not sync'
            0 * mockModuleSyncService.syncAndCreateSchemaSetAndAnchor(_) >> { args -> assert args[0].id == 'cm-handle-1' }
        and: 'the deleting cm-handle did not sync'
            0 * mockModuleSyncService.syncAndCreateSchemaSetAndAnchor(_) >> { args -> assert args[0].id == 'cm-handle-2' }
        and: 'the advised cm-handle synced'
            1 * mockModuleSyncService.syncAndCreateSchemaSetAndAnchor(_) >> { args -> assert args[0].id == 'cm-handle-3' }
        and: 'the state handler called for only the advised handle'
            1 * mockLcmEventsCmHandleStateHandler.updateCmHandleStateBatch(_) >> { args ->
                assertBatch(args, ['cm-handle-3'], CmHandleState.READY)
            }
    }

    def 'Reset failed CM Handles #scenario.'() {
        given: 'cm handles in an locked state'
            def lockedState = new CompositeStateBuilder().withCmHandleState(CmHandleState.LOCKED)
                .withLockReason(MODULE_SYNC_FAILED, '').withLastUpdatedTimeNow().build()
            def yangModelCmHandle1 = new YangModelCmHandle(id: 'cm-handle-1', compositeState: lockedState)
            def yangModelCmHandle2 = new YangModelCmHandle(id: 'cm-handle-2', compositeState: lockedState)
            def expectedCmHandleStatePerCmHandle
                    = [(yangModelCmHandle1): CmHandleState.ADVISED, (yangModelCmHandle2): CmHandleState.ADVISED]
        and: 'clear in progress map'
            resetModuleSyncStartedOnCmHandles(moduleSyncStartedOnCmHandles)
        and: 'add cm handle entry into progress map'
            moduleSyncStartedOnCmHandles.put('cm-handle-1', 'started')
            moduleSyncStartedOnCmHandles.put('cm-handle-2', 'started')
        when: 'resetting failed cm handles'
            objectUnderTest.setCmHandlesToAdvised([yangModelCmHandle1, yangModelCmHandle2])
        then: 'updated to state "ADVISED" from "READY" is called as often as there are cm handles ready for retry'
            1 * mockLcmEventsCmHandleStateHandler.updateCmHandleStateBatch(expectedCmHandleStatePerCmHandle)
        and: 'after reset performed progress map is empty'
            assert moduleSyncStartedOnCmHandles.size() == 0
    }

    def 'Module Sync ADVISED cm handle without entry in progress map.'() {
        given: 'cm handles in an ADVISED state'
            def cmHandle1 = cmHandleByIdAndState('cm-handle-1', CmHandleState.ADVISED)
        and: 'the inventory persistence cm handle returns a ADVISED state for the any handle'
            mockInventoryPersistence.getYangModelCmHandle('cm-handle-1') >> cmHandle1
        and: 'entry in progress map for other cm handle'
            moduleSyncStartedOnCmHandles.put('other-cm-handle', 'started')
        when: 'module sync poll is executed'
            objectUnderTest.performModuleSync(['cm-handle-1'], batchCount)
        then: 'module sync service is invoked for cm handle'
            1 * mockModuleSyncService.syncAndCreateSchemaSetAndAnchor(_) >> { args -> assert args[0].id == 'cm-handle-1' }
        and: 'the entry for other cm handle is still in the progress map'
            assert moduleSyncStartedOnCmHandles.get('other-cm-handle') != null
    }

    def 'Remove already processed cm handle id from hazelcast map'() {
        given: 'hazelcast map contains cm handle id'
            moduleSyncStartedOnCmHandles.put('ch-1', 'started')
        when: 'remove cm handle entry'
            objectUnderTest.removeResetCmHandleFromModuleSyncMap('ch-1')
        then: 'an event is logged with level INFO'
            def loggingEvent = getLoggingEvent()
            assert loggingEvent.level == Level.INFO
        and: 'the log indicates the cm handle entry is removed successfully'
            assert loggingEvent.formattedMessage == 'ch-1 will be removed asynchronously from in progress map'
    }

    def 'Sync and upgrade CM handle if in upgrade state for #scenario'() {
        given: 'a CM handle in an upgrade state'
            def cmHandle = cmHandleByIdAndState('cm-handle', CmHandleState.ADVISED)
            cmHandle.compositeState.setLockReason(CompositeState.LockReason.builder().lockReasonCategory(lockReasonCategory).build())
            mockInventoryPersistence.getYangModelCmHandle('cm-handle') >> cmHandle
        when: 'module sync is executed'
            objectUnderTest.performModuleSync(['cm-handle'], batchCount)
        then: 'the module sync service should attempt to sync and upgrade the CM handle'
            1 * mockModuleSyncService.syncAndUpgradeSchemaSet(_) >> { args ->
                assert args[0].id == 'cm-handle'
            }
        where: 'the following lock reasons are used'
            scenario                | lockReasonCategory
            'module upgrade'        | MODULE_UPGRADE
            'module upgrade failed' | MODULE_UPGRADE_FAILED
    }

    def cmHandleByIdAndState(cmHandleId, cmHandleState) {
        return new YangModelCmHandle(id: cmHandleId, compositeState: new CompositeState(cmHandleState: cmHandleState))
    }

    def assertBatch(args, expectedCmHandleStatePerCmHandleIds, expectedCmHandleState) {
        {
            Map<YangModelCmHandle, CmHandleState> actualCmHandleStatePerCmHandle = args[0]
            assert actualCmHandleStatePerCmHandle.size() == expectedCmHandleStatePerCmHandleIds.size()
            actualCmHandleStatePerCmHandle.each {
                assert expectedCmHandleStatePerCmHandleIds.contains(it.key.id)
                assert it.value == expectedCmHandleState
            }
        }
        return true
    }

    def resetModuleSyncStartedOnCmHandles(moduleSyncStartedOnCmHandles) {
        moduleSyncStartedOnCmHandles.clear();
    }

    def getLoggingEvent() {
        return logger.list[0]
    }
}
