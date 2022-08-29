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
import org.onap.cps.ncmp.api.inventory.CompositeStateBuilder
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.inventory.LockReasonCategory
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification

class ModuleSyncTasksSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)

    def mockSyncUtils = Mock(SyncUtils)

    def mockModuleSyncService = Mock(ModuleSyncService)

    def mockLcmEventsCmHandleStateHandler = Mock(LcmEventsCmHandleStateHandler)

    def objectUnderTest = new ModuleSyncTasks(mockInventoryPersistence, mockSyncUtils, mockModuleSyncService, mockLcmEventsCmHandleStateHandler)

    def 'Module Sync ADVISED cm handles.'() {
        given: 'cm handles in an ADVISED state'
            def cmHandle1 = advisedCmHandleAsDataNode('cm-handle-1')
            def cmHandle2 = advisedCmHandleAsDataNode('cm-handle-2')
        and: 'the inventory persistence cm handle returns a ADVISED state for the any handle'
            mockInventoryPersistence.getCmHandleState(_) >> new CompositeState(cmHandleState: CmHandleState.ADVISED)
        when: 'module sync poll is executed'
            objectUnderTest.performModuleSync([cmHandle1, cmHandle2])
        then: 'module sync service deletes schemas set of each cm handle if it already exists'
            1 * mockModuleSyncService.deleteSchemaSetIfExists('cm-handle-1')
            1 * mockModuleSyncService.deleteSchemaSetIfExists('cm-handle-2')
        and: 'module sync service is invoked for each cm handle'
            1 * mockModuleSyncService.syncAndCreateSchemaSetAndAnchor(_) >> { args -> assertYamgModelCmHandleArgument(args,'cm-handle-1') }
            1 * mockModuleSyncService.syncAndCreateSchemaSetAndAnchor(_) >> { args -> assertYamgModelCmHandleArgument(args,'cm-handle-2') }
        and: 'the state handler is called for the both cm handles'
            2 * mockLcmEventsCmHandleStateHandler.updateCmHandleState(_, CmHandleState.READY)
    }

    def 'Module Sync ADVISED cm handle with failure during sync.'() {
        given: 'a cm handle in an ADVISED state'
            def cmHandle = advisedCmHandleAsDataNode('cm-handle')
        and: 'the inventory persistence cm handle returns a ADVISED state for the cm handle'
            def cmHandleState = new CompositeState(cmHandleState: CmHandleState.ADVISED)
            1 * mockInventoryPersistence.getCmHandleState('cm-handle') >> cmHandleState
        and: 'module sync service attempts to sync the cm handle and throws an exception'
            1 * mockModuleSyncService.syncAndCreateSchemaSetAndAnchor(*_) >> { throw new Exception('some exception') }
        when: 'module sync is executed'
            objectUnderTest.performModuleSync([cmHandle])
        then: 'update lock reason, details and attempts is invoked'
            1 * mockSyncUtils.updateLockReasonDetailsAndAttempts(cmHandleState, LockReasonCategory.LOCKED_MODULE_SYNC_FAILED ,'some exception')
        and: 'the state handler is called to update the state to LOCKED'
            1 * mockLcmEventsCmHandleStateHandler.updateCmHandleState(_, CmHandleState.LOCKED)
    }

    def 'Reset failed CM Handles #scenario.'() {
        given: 'cm handles in an locked state'
            def lockedState = new CompositeStateBuilder().withCmHandleState(CmHandleState.LOCKED)
                    .withLockReason(LockReasonCategory.LOCKED_MODULE_SYNC_FAILED, '').withLastUpdatedTimeNow().build()
            def yangModelCmHandle1 = new YangModelCmHandle(id: 'cm-handle-1', compositeState: lockedState)
            def yangModelCmHandle2 = new YangModelCmHandle(id: 'cm-handle-2', compositeState: lockedState)
        and: 'sync utils retry locked cm handle returns #isReadyForRetry'
            mockSyncUtils.isReadyForRetry(lockedState) >>> isReadyForRetry
        when: 'resetting failed cm handles'
            objectUnderTest.resetFailedCmHandles([yangModelCmHandle1, yangModelCmHandle2])
        then: 'updated to state "ADVISED" from "READY" is called as often as there are cm handles ready for retry'
            expectedNumberOfInvocationsToSaveCmHandleState * mockLcmEventsCmHandleStateHandler.updateCmHandleState(_, CmHandleState.ADVISED)
        where:
            scenario                        | isReadyForRetry         || expectedNumberOfInvocationsToSaveCmHandleState
            'retry locked cm handle once'   | [true, false]           || 1
            'retry locked cm handle twice'  | [true, true]            || 2
            'do not retry locked cm handle' | [false, false]          || 0
    }

    def advisedCmHandleAsDataNode(cmHandleId) {
        return new DataNode(anchorName:cmHandleId, leaves:['id':cmHandleId, 'cm-handle-state':'ADVISED'])
    }

    def assertYamgModelCmHandleArgument(args, expectedCmHandleId) {
        {
            def yangModelCmHandle = args[0]
            assert yangModelCmHandle.id == expectedCmHandleId
        }
        return true
    }
}
