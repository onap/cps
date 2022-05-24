/*
 * ============LICENSE_START=======================================================
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
import spock.lang.Specification

class ModuleSyncSpec extends Specification {

    def mockSyncUtils = Mock(SyncUtils)

    def mockModuleSyncService = Mock(ModuleSyncService)

    def cmHandleState = CmHandleState.ADVISED

    def compositeState = new CompositeState()

    def objectUnderTest = new ModuleSyncWatchdog(mockSyncUtils, mockModuleSyncService)

    def 'Schedule a Cm-Handle Sync for ADVISED Cm-Handles'() {
        given: 'cm handles in an advised state'
            compositeState.cmhandleState = cmHandleState
            def yangModelCmHandle1 = new YangModelCmHandle(compositeState: compositeState)
            def yangModelCmHandle2 = new YangModelCmHandle(compositeState: compositeState)
        and: 'sync utilities return a cm handle twice'
            mockSyncUtils.getAnAdvisedCmHandle() >>> [yangModelCmHandle1, yangModelCmHandle2, null]
        when: 'module sync poll is executed'
            objectUnderTest.executeAdvisedCmHandlePoll()
        then: 'module sync service syncs the first cm handle and creates a schema set'
            1 * mockModuleSyncService.syncAndCreateSchemaSet(yangModelCmHandle1)
        and: 'the first cm handle is updated to state "READY" from "ADVISED"'
            1 * mockSyncUtils.updateCmHandleState(yangModelCmHandle1, CmHandleState.READY)
        then: 'module sync service syncs the second cm handle and creates a schema set'
            1 * mockModuleSyncService.syncAndCreateSchemaSet(yangModelCmHandle2)
        then: 'the second cm handle is updated to state "READY" from "ADVISED"'
            1 * mockSyncUtils.updateCmHandleState(yangModelCmHandle2, CmHandleState.READY)
    }

    def 'Schedule a Cm-Handle Sync for LOCKED with reason LOCKED-MISBEHAVING Cm-Handles '() {
        given: 'cm handles in an advised state'
            compositeState.cmhandleState = CmHandleState.LOCKED
            CompositeState.LockReason lockReason = new CompositeState.LockReason('LOCKED-MISBEHAVING', "")
            compositeState.setLockReason(lockReason);
            def yangModelCmHandle1 = new YangModelCmHandle(compositeState: compositeState)
            def yangModelCmHandle2 = new YangModelCmHandle(compositeState: compositeState)
        and: 'sync utilities return a cm handle twice'
            mockSyncUtils.getLockedMisbehavingCmHandles() >> [yangModelCmHandle1, yangModelCmHandle2]
        when: 'module sync poll is executed'
            objectUnderTest.executeLockedMisbehavingCmHandlePoll()
        then: 'the first cm handle is updated to state "ADVISED" from "READY"'
            1 * mockSyncUtils.updateCmHandleState(yangModelCmHandle1, CmHandleState.ADVISED)
    }
}
