/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.event.lcm

import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.inventory.CompositeState
import org.onap.cps.ncmp.api.inventory.DataStoreSyncState
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import spock.lang.Specification

import static org.onap.cps.ncmp.api.inventory.CmHandleState.ADVISED
import static org.onap.cps.ncmp.api.inventory.CmHandleState.DELETED
import static org.onap.cps.ncmp.api.inventory.CmHandleState.DELETING
import static org.onap.cps.ncmp.api.inventory.CmHandleState.LOCKED
import static org.onap.cps.ncmp.api.inventory.CmHandleState.READY
import static org.onap.cps.ncmp.api.inventory.LockReasonCategory.LOCKED_MODULE_SYNC_FAILED

class LcmEventsCmHandleStateHandlerImplSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockLcmEventsCreator = Mock(LcmEventsCreator)
    def mockLcmEventsService = Mock(LcmEventsService)

    def objectUnderTest = new LcmEventsCmHandleStateHandlerImpl(mockInventoryPersistence, mockLcmEventsCreator, mockLcmEventsService)

    def cmHandleId = 'cmhandle-id-1'
    def compositeState
    def yangModelCmHandle

    def 'Update and Publish Events on State Change #stateChange'() {
        given: 'Cm Handle represented as YangModelCmHandle'
            compositeState = new CompositeState(cmHandleState: fromCmHandleState)
            yangModelCmHandle = new YangModelCmHandle(id: cmHandleId, dmiProperties: [], publicProperties: [], compositeState: compositeState)
        when: 'update state is invoked'
            objectUnderTest.updateCmHandleState(yangModelCmHandle, toCmHandleState)
        then: 'state is saved using inventory persistence'
            expectedCallsToInventoryPersistence * mockInventoryPersistence.saveCmHandleState(cmHandleId, _)
        and: 'event service is called to publish event'
            expectedCallsToEventService * mockLcmEventsService.publishLcmEvent(cmHandleId, _)
        where: 'state change parameters are provided'
            stateChange          | fromCmHandleState | toCmHandleState || expectedCallsToInventoryPersistence | expectedCallsToEventService
            'ADVISED to READY'   | ADVISED           | READY           || 1                                   | 1
            'READY to LOCKED'    | READY             | LOCKED          || 1                                   | 1
            'ADVISED to ADVISED' | ADVISED           | ADVISED         || 0                                   | 0
            'READY to READY'     | READY             | READY           || 0                                   | 0
            'LOCKED to LOCKED'   | LOCKED            | LOCKED          || 0                                   | 0

    }

    def 'Update and Publish Events on State Change from NO_EXISTING state to ADVISED'() {
        given: 'Cm Handle represented as YangModelCmHandle'
            yangModelCmHandle = new YangModelCmHandle(id: cmHandleId, dmiProperties: [], publicProperties: [])
        when: 'update state is invoked'
            objectUnderTest.updateCmHandleState(yangModelCmHandle, ADVISED)
        then: 'state is saved using inventory persistence'
            1 * mockInventoryPersistence.saveCmHandle(yangModelCmHandle)
        and: 'event service is called to publish event'
            1 * mockLcmEventsService.publishLcmEvent(cmHandleId, _)
    }

    def 'Update and Publish Events on State Change from LOCKED to ADVISED'() {
        given: 'Cm Handle represented as YangModelCmHandle in LOCKED state'
            compositeState = new CompositeState(cmHandleState: LOCKED,
                lockReason: CompositeState.LockReason.builder().lockReasonCategory(LOCKED_MODULE_SYNC_FAILED).details('some lock details').build())
            yangModelCmHandle = new YangModelCmHandle(id: cmHandleId, dmiProperties: [], publicProperties: [], compositeState: compositeState)
        when: 'update state is invoked'
            objectUnderTest.updateCmHandleState(yangModelCmHandle, ADVISED)
        then: 'state is saved using inventory persistence and old lock reason details are retained'
            1 * mockInventoryPersistence.saveCmHandleState(cmHandleId, _) >> {
                args -> {
                    assert (args[1] as CompositeState).lockReason.details == 'some lock details'
                }
            }
        and: 'event service is called to publish event'
            1 * mockLcmEventsService.publishLcmEvent(cmHandleId, _)
    }

    def 'Update and Publish Events on State Change to READY with #scenario'() {
        given: 'Cm Handle represented as YangModelCmHandle'
            compositeState = new CompositeState(cmHandleState: ADVISED)
            yangModelCmHandle = new YangModelCmHandle(id: cmHandleId, dmiProperties: [], publicProperties: [], compositeState: compositeState)
        and: 'global sync flag is set'
            objectUnderTest.isGlobalDataSyncCacheEnabled = dataSyncCacheEnabled
        when: 'update cmhandle state is invoked'
            objectUnderTest.updateCmHandleState(yangModelCmHandle, READY)
        then: 'state is saved using inventory persistence with expected dataSyncState'
            1 * mockInventoryPersistence.saveCmHandleState(cmHandleId, _) >> {
                args-> {
                    def result = (args[1] as CompositeState)
                    assert result.dataSyncEnabled == dataSyncCacheEnabled
                    assert result.dataStores.operationalDataStore.dataStoreSyncState == expectedDataStoreSyncState

                }
            }
        and: 'event service is called to publish event'
            1 * mockLcmEventsService.publishLcmEvent(cmHandleId, _)
        where:
            scenario                         | dataSyncCacheEnabled || expectedDataStoreSyncState
            'data sync cache enabled'        | true                 || DataStoreSyncState.UNSYNCHRONIZED
            'data sync cache is not enabled' | false                || DataStoreSyncState.NONE_REQUESTED
    }

    def 'Update cmHandle state to "DELETING"' (){
        given: 'cm Handle as Yang model'
            compositeState = new CompositeState(cmHandleState: READY)
            yangModelCmHandle = new YangModelCmHandle(id: cmHandleId, dmiProperties: [], publicProperties: [], compositeState: compositeState)
        when: 'updating cm handle state to "DELETING"'
            objectUnderTest.updateCmHandleState(yangModelCmHandle, DELETING)
        then: 'the cm handle state is as expected'
            yangModelCmHandle.getCompositeState().getCmHandleState() == DELETING
        and: 'method to persist cm handle state is called once'
            1 * mockInventoryPersistence.saveCmHandleState(yangModelCmHandle.getId(), yangModelCmHandle.getCompositeState())
        and: 'the method to publish Lcm event is called once'
            1 * mockLcmEventsService.publishLcmEvent(cmHandleId, _)
    }

    def 'Update cmHandle state to "DELETED"' (){
        given: 'cm Handle with state "DELETING" as Yang model '
            compositeState = new CompositeState(cmHandleState: DELETING)
            yangModelCmHandle = new YangModelCmHandle(id: cmHandleId, dmiProperties: [], publicProperties: [], compositeState: compositeState)
        when: 'updating cm handle state to "DELETED"'
            objectUnderTest.updateCmHandleState(yangModelCmHandle, DELETED)
        then: 'the cm handle state is as expected'
            yangModelCmHandle.getCompositeState().getCmHandleState() == DELETED
        and: 'the method to publish Lcm event is called once'
            1 * mockLcmEventsService.publishLcmEvent(cmHandleId, _)
    }
}