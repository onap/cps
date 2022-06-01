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

import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeState
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.inventory.LockReasonCategory
import spock.lang.Specification

class ModuleSyncSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)

    def mockSyncUtils = Mock(SyncUtils)

    def mockModuleSyncService = Mock(ModuleSyncService)

    def cmHandleState = CmHandleState.ADVISED

    def objectUnderTest = new ModuleSyncWatchdog(mockInventoryPersistence, mockSyncUtils, mockModuleSyncService)

    def 'Schedule a Cm-Handle Sync for ADVISED Cm-Handles'() {
        given: 'cm handles in an advised state'
            def compositeState1 = new CompositeState(cmHandleState: cmHandleState)
            def compositeState2 = new CompositeState(cmHandleState: cmHandleState)
            def yangModelCmHandle1 = new YangModelCmHandle(id: 'some-cm-handle', compositeState: compositeState1)
            def yangModelCmHandle2 = new YangModelCmHandle(id: 'some-cm-handle-2', compositeState: compositeState2)
        and: 'sync utilities return a cm handle twice'
            mockSyncUtils.getAnAdvisedCmHandle() >>> [yangModelCmHandle1, yangModelCmHandle2, null]
        when: 'module sync poll is executed'
            objectUnderTest.executeAdvisedCmHandlePoll()
        then: 'the inventory persistence cm handle returns a composite state for the first cm handle'
            1 * mockInventoryPersistence.getCmHandleState('some-cm-handle') >> compositeState1
        and: 'module sync service syncs the first cm handle and creates a schema set'
            1 * mockModuleSyncService.syncAndCreateSchemaSet(yangModelCmHandle1)
        and: 'the composite state cm handle state is now READY'
            assert compositeState1.getCmHandleState() == CmHandleState.READY
        and: 'the first cm handle state is updated'
            1 * mockInventoryPersistence.saveCmHandleState('some-cm-handle', compositeState1)
        then: 'the inventory persistence cm handle returns a composite state for the second cm handle'
            mockInventoryPersistence.getCmHandleState('some-cm-handle-2') >> compositeState2
        and: 'module sync service syncs the second cm handle and creates a schema set'
            1 * mockModuleSyncService.syncAndCreateSchemaSet(yangModelCmHandle2)
        and: 'the composite state cm handle state is now READY'
            assert compositeState2.getCmHandleState() == CmHandleState.READY
        and: 'the second cm handle state is updated'
            1 * mockInventoryPersistence.saveCmHandleState('some-cm-handle-2', compositeState2)
    }

    def 'Schedule a Cm-Handle Sync for ADVISED Cm-Handle with failure'() {
        given: 'cm handles in an advised state'
            def compositeState = new CompositeState(cmHandleState: cmHandleState)
            def yangModelCmHandle = new YangModelCmHandle(id: 'some-cm-handle', compositeState: compositeState)
        and: 'sync utilities return a cm handle'
            mockSyncUtils.getAnAdvisedCmHandle() >>> [yangModelCmHandle, null]
        when: 'module sync poll is executed'
            objectUnderTest.executeAdvisedCmHandlePoll()
        then: 'the inventory persistence cm handle returns a composite state for the cm handle'
            1 * mockInventoryPersistence.getCmHandleState('some-cm-handle') >> compositeState
        and: 'module sync service attempts to sync the cm handle and throws an exception'
            1 * mockModuleSyncService.syncAndCreateSchemaSet(*_) >> { throw new Exception('some exception') }
        and: 'the composite state cm handle state is now LOCKED'
            assert compositeState.getCmHandleState() == CmHandleState.LOCKED
        and: 'update lock reason, details and attempts is invoked'
            1 * mockSyncUtils.updateLockReasonDetailsAndAttempts(compositeState, LockReasonCategory.LOCKED_MISBEHAVING ,'some exception')
        and: 'the cm handle state is updated'
            1 * mockInventoryPersistence.saveCmHandleState('some-cm-handle', compositeState)

    }

}
