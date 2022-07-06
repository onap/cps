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

package org.onap.cps.ncmp.api.impl.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.inventory.CompositeState
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

import static org.onap.cps.ncmp.api.inventory.CmHandleState.ADVISED
import static org.onap.cps.ncmp.api.inventory.CmHandleState.LOCKED
import static org.onap.cps.ncmp.api.inventory.CmHandleState.READY
import static org.onap.cps.ncmp.api.inventory.LockReasonCategory.LOCKED_MODULE_SYNC_FAILED

class NcmpEventsCmHandleStateHandlerImplSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockNcmpEventsCreator = Mock(NcmpEventsCreator)
    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))
    def mockNcmpEventsService = Mock(NcmpEventsService)

    def objectUnderTest = new NcmpEventsCmHandleStateHandlerImpl(mockInventoryPersistence, mockNcmpEventsCreator, spiedJsonObjectMapper, mockNcmpEventsService)

    def 'Update and Publish Events on State Change #stateChange'() {
        given: 'Cm Handle represented as YangModelCmHandle'
            def cmHandleId = 'cmhandle-id-1'
            def compositeState = new CompositeState(cmHandleState: fromCmHandleState)
            def yangModelCmHandle = new YangModelCmHandle(id: cmHandleId, dmiProperties: [], publicProperties: [], compositeState: compositeState)
        when: 'update state is invoked'
            objectUnderTest.updateState(yangModelCmHandle, toCmHandleState)
        then: 'state is saved using inventory persistence'
            expectedCallsToInventoryPersistence * mockInventoryPersistence.saveCmHandleState(cmHandleId, _)
        and: 'event service is called to publish event'
            expectedCallsToEventService * mockNcmpEventsService.publishNcmpEvent(cmHandleId, _)
        where: 'state change parameters are provided'
            stateChange          | fromCmHandleState | toCmHandleState || expectedCallsToInventoryPersistence | expectedCallsToEventService
            'ADVISED to READY'   | ADVISED           | READY           || 1                                   | 1
            'READY to LOCKED'    | READY             | LOCKED          || 1                                   | 1
            'ADVISED to ADVISED' | ADVISED           | ADVISED         || 0                                   | 0
            'READY to READY'     | READY             | READY           || 0                                   | 0
            'LOCKED to LOCKED'   | LOCKED            | LOCKED          || 0                                   | 0

    }

    def 'Update and Publish Events on State Change from READY to ADVISED'() {
        given: 'Cm Handle represented as YangModelCmHandle'
            def cmHandleId = 'cmhandle-id-1'
            def compositeState = new CompositeState(cmHandleState: READY)
            def yangModelCmHandle = new YangModelCmHandle(id: cmHandleId, dmiProperties: [], publicProperties: [], compositeState: compositeState)
        when: 'update state is invoked'
            objectUnderTest.updateState(yangModelCmHandle, ADVISED)
        then: 'state is saved using inventory persistence'
            1 * mockInventoryPersistence.saveListElements(_)
        and: 'event service is called to publish event'
            1 * mockNcmpEventsService.publishNcmpEvent(cmHandleId, _)
    }

    def 'Update and Publish Events on State Change from LOCKED to ADVISED'() {
        given: 'Cm Handle represented as YangModelCmHandle'
            def cmHandleId = 'cmhandle-id-1'
            def compositeState = new CompositeState(cmHandleState: LOCKED,
                lockReason: CompositeState.LockReason.builder().lockReasonCategory(LOCKED_MODULE_SYNC_FAILED).details('some lock details').build())
            def yangModelCmHandle = new YangModelCmHandle(id: cmHandleId, dmiProperties: [], publicProperties: [], compositeState: compositeState)
        when: 'update state is invoked'
            objectUnderTest.updateState(yangModelCmHandle, ADVISED)
        then: 'state is saved using inventory persistence and old lock reason details are retained'
            1 * mockInventoryPersistence.saveCmHandleState(cmHandleId, _) >> {
                args ->
                    {
                        assert (args[1] as CompositeState).lockReason.details == 'some lock details'
                    }
            }
        and: 'event service is called to publish event'
            1 * mockNcmpEventsService.publishNcmpEvent(cmHandleId, _)
    }
}
