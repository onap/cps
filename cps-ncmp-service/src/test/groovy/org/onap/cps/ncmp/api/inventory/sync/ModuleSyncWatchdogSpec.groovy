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

import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeState
import org.onap.cps.ncmp.api.inventory.DataStoreSyncState
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.inventory.LockReasonCategory
import org.onap.cps.ncmp.api.inventory.CompositeStateBuilder
import spock.lang.Specification

import java.util.concurrent.ConcurrentHashMap

class ModuleSyncWatchdogSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)

    def mockSyncUtils = Mock(SyncUtils)

    def mockModuleSyncService = Mock(ModuleSyncService)

    def cmHandleState = CmHandleState.ADVISED

    def objectUnderTest = new ModuleSyncWatchdog(mockInventoryPersistence, mockSyncUtils, mockModuleSyncService,  new ConcurrentHashMap<>())

    def 'Schedule a Cm-Handle Sync for ADVISED Cm-Handles where #scenario'() {
        given: 'cm handles in an advised state and a data sync state'
            def compositeState1 = new CompositeState(cmHandleState: cmHandleState)
            def compositeState2 = new CompositeState(cmHandleState: cmHandleState)
            def yangModelCmHandle1 = new YangModelCmHandle(id: 'some-cm-handle', compositeState: compositeState1)
            def yangModelCmHandle2 = new YangModelCmHandle(id: 'some-cm-handle-2', compositeState: compositeState2)
            objectUnderTest.isGlobalDataSyncCacheEnabled = dataSyncCacheEnabled
        and: 'sync utilities return a cm handle twice'
            mockSyncUtils.getAdvisedCmHandles() >> [yangModelCmHandle1, yangModelCmHandle2]
        when: 'module sync poll is executed'
            objectUnderTest.executeAdvisedCmHandlePoll()
        then: 'the inventory persistence cm handle returns a composite state for the first cm handle'
            1 * mockInventoryPersistence.getCmHandleState('some-cm-handle') >> compositeState1
        and: 'module sync service deletes schema set of cm handle if it exists'
            1 * mockModuleSyncService.deleteSchemaSetIfExists(yangModelCmHandle1)
        and: 'module sync service syncs the first cm handle and creates a schema set'
            1 * mockModuleSyncService.syncAndCreateSchemaSetAndAnchor(yangModelCmHandle1)
        then: 'the composite state cm handle state is now READY'
            assert compositeState1.getCmHandleState() == CmHandleState.READY
        and: 'the data store sync state returns the expected state'
            compositeState1.getDataStores().operationalDataStore.dataStoreSyncState == expectedDataStoreSyncState
        and: 'the first cm handle state is updated'
            1 * mockInventoryPersistence.saveCmHandleState('some-cm-handle', compositeState1)
        then: 'the inventory persistence cm handle returns a composite state for the second cm handle'
            mockInventoryPersistence.getCmHandleState('some-cm-handle-2') >> compositeState2
        and: 'module sync service syncs the second cm handle and creates a schema set'
            1 * mockModuleSyncService.syncAndCreateSchemaSetAndAnchor(yangModelCmHandle2)
        and: 'the composite state cm handle state is now READY'
            assert compositeState2.getCmHandleState() == CmHandleState.READY
        and: 'the second cm handle state is updated'
            1 * mockInventoryPersistence.saveCmHandleState('some-cm-handle-2', compositeState2)
        where:
            scenario                         | dataSyncCacheEnabled  || expectedDataStoreSyncState
            'data sync cache enabled'        | true                  || DataStoreSyncState.UNSYNCHRONIZED
            'data sync cache is not enabled' | false                 || DataStoreSyncState.NONE_REQUESTED
    }

    def 'Schedule a Cm-Handle Sync for ADVISED Cm-Handle with failure'() {
        given: 'cm handles in an advised state'
            def compositeState = new CompositeState(cmHandleState: cmHandleState)
            def yangModelCmHandle = new YangModelCmHandle(id: 'some-cm-handle', compositeState: compositeState)
        and: 'sync utilities return a cm handle'
            mockSyncUtils.getAdvisedCmHandles() >> [yangModelCmHandle]
        when: 'module sync poll is executed'
            objectUnderTest.executeAdvisedCmHandlePoll()
        then: 'the inventory persistence cm handle returns a composite state for the cm handle'
            1 * mockInventoryPersistence.getCmHandleState('some-cm-handle') >> compositeState
        and: 'module sync service attempts to sync the cm handle and throws an exception'
            1 * mockModuleSyncService.syncAndCreateSchemaSetAndAnchor(*_) >> { throw new Exception('some exception') }
        and: 'the composite state cm handle state is now LOCKED'
            assert compositeState.getCmHandleState() == CmHandleState.LOCKED
        and: 'update lock reason, details and attempts is invoked'
            1 * mockSyncUtils.updateLockReasonDetailsAndAttempts(compositeState, LockReasonCategory.LOCKED_MODULE_SYNC_FAILED ,'some exception')
        and: 'the cm handle state is updated'
            1 * mockInventoryPersistence.saveCmHandleState('some-cm-handle', compositeState)

    }

    def 'Schedule a Cm-Handle Sync with condition #scenario '() {
        given: 'cm handles in an locked state'
            def compositeState = new CompositeStateBuilder().withCmHandleState(CmHandleState.LOCKED)
                    .withLockReason(LockReasonCategory.LOCKED_MODULE_SYNC_FAILED, '').withLastUpdatedTimeNow().build()
            def yangModelCmHandle = new YangModelCmHandle(id: 'some-cm-handle', compositeState: compositeState)
        and: 'sync utilities return a cm handle twice'
            mockSyncUtils.getModuleSyncFailedCmHandles() >> [yangModelCmHandle, yangModelCmHandle]
        and: 'inventory persistence returns the composite state of the cm handle'
            mockInventoryPersistence.getCmHandleState(yangModelCmHandle.getId()) >> compositeState
        and: 'sync utils retry locked cm handle returns #isReadyForRetry'
            mockSyncUtils.isReadyForRetry(compositeState) >>> isReadyForRetry
        when: 'module sync poll is executed'
            objectUnderTest.executeLockedCmHandlePoll()
        then: 'the first cm handle is updated to state "ADVISED" from "READY"'
            expectedNumberOfInvocationsToSaveCmHandleState * mockInventoryPersistence.saveCmHandleState(yangModelCmHandle.id, compositeState)
        where:
            scenario                        | isReadyForRetry         || expectedNumberOfInvocationsToSaveCmHandleState
            'retry locked cm handle once'   | [true, false]           || 1
            'retry locked cm handle twice'  | [true, true]            || 2
            'do not retry locked cm handle' | [false, false]          || 0
    }
}
