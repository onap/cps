/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.inventory.sync


import org.onap.cps.ncmp.api.impl.event.lcm.LcmEventsCmHandleStateHandler
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeState
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.inventory.LockReasonCategory
import org.onap.cps.ncmp.api.inventory.CompositeStateBuilder
import org.onap.cps.ncmp.api.inventory.sync.executor.AsyncTaskExecutor
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.BlockingQueue

class ModuleSyncWatchdogSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockSyncUtils = Mock(SyncUtils)
    def mockModuleSyncService = Mock(ModuleSyncService)
    def stubbedMap = Stub(ConcurrentHashMap)
    def mockLcmEventsCmHandleStateHandler = Mock(LcmEventsCmHandleStateHandler)
    def blockingQueue = Mock(BlockingQueue<DataNode>)
    def asyncTaskExecutor = Mock(AsyncTaskExecutor)
    def cmHandleState = CmHandleState.ADVISED

    def objectUnderTest = new ModuleSyncWatchdog(mockInventoryPersistence,
        mockSyncUtils, mockModuleSyncService, stubbedMap , mockLcmEventsCmHandleStateHandler, blockingQueue, asyncTaskExecutor)

    def 'Schedule a Cm-Handle Sync for ADVISED Cm-Handles'() {
        given: 'cm handles in an advised state and a data sync state'
            def compositeState1 = new CompositeState(cmHandleState: cmHandleState)
            def compositeState2 = new CompositeState(cmHandleState: cmHandleState)
            def yangModelCmHandle1 = new YangModelCmHandle(id: 'some-cm-handle', compositeState: compositeState1)
            def yangModelCmHandle2 = new YangModelCmHandle(id: 'some-cm-handle-2', compositeState: compositeState2)
        and: 'sync utilities return a cm handle twice'
            mockSyncUtils.getAdvisedCmHandles(_) >> [yangModelCmHandle1, yangModelCmHandle2]
        when: 'module sync poll is executed'
            objectUnderTest.moduleSyncAdvisedCmHandles()
        then: ''
            1 * asyncTaskExecutor.executeTask(*_)
        //TODO Add test for actual process

    }

    def 'Perform state transition '() {
        given: 'cm handles in an advised state and a data sync state'
            def compositeState1 = new CompositeState(cmHandleState: cmHandleState)
            def compositeState2 = new CompositeState(cmHandleState: cmHandleState)
            def yangModelCmHandle1 = new YangModelCmHandle(id: 'some-cm-handle', compositeState: compositeState1)
            def yangModelCmHandle2 = new YangModelCmHandle(id: 'some-cm-handle-2', compositeState: compositeState2)
        when: 'module sync poll is executed'
            objectUnderTest.performModuleStateTransition([yangModelCmHandle1, yangModelCmHandle2])
            1 * mockInventoryPersistence.getCmHandleState('some-cm-handle') >> compositeState1
        and: 'module sync service deletes schema set of cm handle if it exists'
            1 * mockModuleSyncService.deleteSchemaSetIfExists(yangModelCmHandle1)
        and: 'module sync service syncs the first cm handle and creates a schema set'
            1 * mockModuleSyncService.syncAndCreateSchemaSetAndAnchor(yangModelCmHandle1)
        then: 'the state handler is called for the first cm handle'
            1 * mockLcmEventsCmHandleStateHandler.updateCmHandlesStateBatch(yangModelCmHandle1, CmHandleState.READY)
        and: 'the inventory persistence cm handle returns a composite state for the second cm handle'
            mockInventoryPersistence.getCmHandleState('some-cm-handle-2') >> compositeState2
        and: 'module sync service syncs the second cm handle and creates a schema set'
            1 * mockModuleSyncService.syncAndCreateSchemaSetAndAnchor(yangModelCmHandle2)
        then: 'the state handler is called for the second cm handle'
            1 * mockLcmEventsCmHandleStateHandler.updateCmHandlesStateBatch(yangModelCmHandle2, CmHandleState.READY)
    }

    def 'Perform state transition with failure'() {
        given: 'cm handles in an advised state'
            def compositeState = new CompositeState(cmHandleState: cmHandleState)
            def yangModelCmHandle = new YangModelCmHandle(id: 'some-cm-handle', compositeState: compositeState)
        when: 'module sync poll is executed'
            objectUnderTest.performModuleStateTransition([yangModelCmHandle])
        then: 'the inventory persistence cm handle returns a composite state for the cm handle'
            1 * mockInventoryPersistence.getCmHandleState('some-cm-handle') >> compositeState
        and: 'module sync service attempts to sync the cm handle and throws an exception'
            1 * mockModuleSyncService.syncAndCreateSchemaSetAndAnchor(*_) >> { throw new Exception('some exception') }
        and: 'update lock reason, details and attempts is invoked'
            1 * mockSyncUtils.updateLockReasonDetailsAndAttempts(compositeState, LockReasonCategory.LOCKED_MODULE_SYNC_FAILED ,'some exception')
        and: 'the state handler is called to update the state to LOCKED'
            1 * mockLcmEventsCmHandleStateHandler.updateCmHandlesStateBatch(yangModelCmHandle, CmHandleState.LOCKED)
    }


    def 'Schedule a Cm-Handle Sync with condition #scenario '() {
        given: 'cm handles in an locked state'
            def compositeState = new CompositeStateBuilder().withCmHandleState(CmHandleState.LOCKED)
                    .withLockReason(LockReasonCategory.LOCKED_MODULE_SYNC_FAILED, '').withLastUpdatedTimeNow().build()
            def yangModelCmHandle = new YangModelCmHandle(id: 'some-cm-handle', compositeState: compositeState)
        and: 'inventory persistence returns the composite state of the cm handle'
            mockInventoryPersistence.getCmHandleState(yangModelCmHandle.getId()) >> compositeState
        and: 'sync utils retry locked cm handle returns #isReadyForRetry'
            mockSyncUtils.isReadyForRetry(compositeState) >>> isReadyForRetry
        when: 'module sync poll is executed'
            objectUnderTest.performModuleStateTransition([yangModelCmHandle, yangModelCmHandle])
        then: 'the first cm handle is updated to state "ADVISED" from "READY"'
            expectedNumberOfInvocationsToSaveCmHandleState * mockLcmEventsCmHandleStateHandler.updateCmHandlesStateBatch(yangModelCmHandle, CmHandleState.ADVISED)
        where:
            scenario                        | isReadyForRetry         || expectedNumberOfInvocationsToSaveCmHandleState
            'retry locked cm handle once'   | [true, false]           || 1
            'retry locked cm handle twice'  | [true, true]            || 2
            'do not retry locked cm handle' | [false, false]          || 0
    }
}
