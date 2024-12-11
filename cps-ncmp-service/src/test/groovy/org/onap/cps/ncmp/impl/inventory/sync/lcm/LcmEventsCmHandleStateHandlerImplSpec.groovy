/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2024 Nordix Foundation
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

package org.onap.cps.ncmp.impl.inventory.sync.lcm

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.impl.inventory.DataStoreSyncState
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.slf4j.LoggerFactory
import spock.lang.Specification

import static java.util.Collections.EMPTY_LIST
import static java.util.Collections.EMPTY_MAP
import static org.onap.cps.ncmp.impl.inventory.models.CmHandleState.ADVISED
import static org.onap.cps.ncmp.impl.inventory.models.CmHandleState.DELETED
import static org.onap.cps.ncmp.impl.inventory.models.CmHandleState.DELETING
import static org.onap.cps.ncmp.impl.inventory.models.CmHandleState.LOCKED
import static org.onap.cps.ncmp.impl.inventory.models.CmHandleState.READY
import static org.onap.cps.ncmp.impl.inventory.models.LockReasonCategory.MODULE_SYNC_FAILED

class LcmEventsCmHandleStateHandlerImplSpec extends Specification {

    def logger = Spy(ListAppender<ILoggingEvent>)

    void setup() {
        ((Logger) LoggerFactory.getLogger(LcmEventsCmHandleStateHandlerImpl.class)).addAppender(logger)
        logger.start()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(LcmEventsCmHandleStateHandlerImpl.class)).detachAndStopAllAppenders()
    }

    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockLcmEventsCreator = Mock(LcmEventsCreator)
    def mockLcmEventsService = Mock(LcmEventsService)
    def mockCmHandleStateGaugeManager = Mock(CmHandleStateGaugeManager)

    def lcmEventsCmHandleStateHandlerAsyncHelper = new LcmEventsCmHandleStateHandlerAsyncHelper(mockLcmEventsCreator, mockLcmEventsService)
    def objectUnderTest = new LcmEventsCmHandleStateHandlerImpl(mockInventoryPersistence, lcmEventsCmHandleStateHandlerAsyncHelper, mockCmHandleStateGaugeManager)

    def cmHandleId = 'cmhandle-id-1'
    def compositeState
    def yangModelCmHandle

    def 'Update and Publish Events on State Change #stateChange'() {
        given: 'Cm Handle represented as YangModelCmHandle'
            compositeState = new CompositeState(cmHandleState: fromCmHandleState)
            yangModelCmHandle = new YangModelCmHandle(id: cmHandleId, dmiProperties: [], publicProperties: [], compositeState: compositeState)
        when: 'update state is invoked'
            objectUnderTest.updateCmHandleStateBatch(Map.of(yangModelCmHandle, toCmHandleState))
        then: 'state is saved using inventory persistence'
            1 * mockInventoryPersistence.saveCmHandleStateBatch(_) >> {
                args -> {
                    def cmHandleStatePerCmHandleId = args[0] as Map<String, CompositeState>
                    assert cmHandleStatePerCmHandleId.get(cmHandleId).cmHandleState == toCmHandleState
                }
            }
        and: 'log message shows state change at INFO level'
            def loggingEvent = (ILoggingEvent) logger.list[0]
            assert loggingEvent.level == Level.INFO
            assert loggingEvent.formattedMessage == "${cmHandleId} is now in ${toCmHandleState} state"
        and: 'event service is called to publish event'
            1 * mockLcmEventsService.publishLcmEvent(cmHandleId, _, _)
        where: 'state change parameters are provided'
            stateChange           | fromCmHandleState | toCmHandleState
            'ADVISED to READY'    | ADVISED           | READY
            'READY to LOCKED'     | READY             | LOCKED
            'ADVISED to LOCKED'   | ADVISED           | LOCKED
            'ADVISED to DELETING' | ADVISED           | DELETING
    }

    def 'Update and Publish Events on State Change from non-existing to ADVISED'() {
        given: 'Cm Handle represented as YangModelCmHandle'
            yangModelCmHandle = new YangModelCmHandle(id: cmHandleId, dmiProperties: [], publicProperties: [])
        when: 'update state is invoked'
            objectUnderTest.updateCmHandleStateBatch(Map.of(yangModelCmHandle, ADVISED))
        then: 'CM-handle is saved using inventory persistence'
            1 * mockInventoryPersistence.saveCmHandleBatch(List.of(yangModelCmHandle))
        and: 'event service is called to publish event'
            1 * mockLcmEventsService.publishLcmEvent(cmHandleId, _, _)
        and: 'a log entry is written'
            assert getLogMessage(0) == "${cmHandleId} is now in ADVISED state"
    }

    def 'Update and Publish Events on State Change from LOCKED to ADVISED'() {
        given: 'Cm Handle represented as YangModelCmHandle in LOCKED state'
            compositeState = new CompositeState(cmHandleState: LOCKED,
                lockReason: CompositeState.LockReason.builder().lockReasonCategory(MODULE_SYNC_FAILED).details('some lock details').build())
            yangModelCmHandle = new YangModelCmHandle(id: cmHandleId, dmiProperties: [], publicProperties: [], compositeState: compositeState)
        when: 'update state is invoked'
            objectUnderTest.updateCmHandleStateBatch(Map.of(yangModelCmHandle, ADVISED))
        then: 'state is saved using inventory persistence and old lock reason details are retained'
            1 * mockInventoryPersistence.saveCmHandleStateBatch(_) >> {
                args -> {
                    def cmHandleStatePerCmHandleId = args[0] as Map<String, CompositeState>
                    assert cmHandleStatePerCmHandleId.get(cmHandleId).lockReason.details == 'some lock details'
                }
            }
        and: 'event service is called to publish event'
            1 * mockLcmEventsService.publishLcmEvent(cmHandleId, _, _)
        and: 'a log entry is written'
            assert getLogMessage(0) == "${cmHandleId} is now in ADVISED state"
    }

    def 'Update and Publish Events on State Change to from ADVISED to READY'() {
        given: 'Cm Handle represented as YangModelCmHandle'
            compositeState = new CompositeState(cmHandleState: ADVISED)
            yangModelCmHandle = new YangModelCmHandle(id: cmHandleId, dmiProperties: [], publicProperties: [], compositeState: compositeState)
        and: 'global sync flag is set'
            compositeState.setDataSyncEnabled(false)
        when: 'update cmhandle state is invoked'
            objectUnderTest.updateCmHandleStateBatch(Map.of(yangModelCmHandle, READY))
        then: 'state is saved using inventory persistence with expected dataSyncState'
            1 * mockInventoryPersistence.saveCmHandleStateBatch(_) >> {
                args-> {
                    def cmHandleStatePerCmHandleId = args[0] as Map<String, CompositeState>
                    assert cmHandleStatePerCmHandleId.get(cmHandleId).dataSyncEnabled == false
                    assert cmHandleStatePerCmHandleId.get(cmHandleId).dataStores.operationalDataStore.dataStoreSyncState == DataStoreSyncState.NONE_REQUESTED
                }
            }
        and: 'event service is called to publish event'
            1 * mockLcmEventsService.publishLcmEvent(cmHandleId, _, _)
        and: 'a log entry is written'
            assert getLogMessage(0) == "${cmHandleId} is now in READY state"
    }

    def 'Update cmHandle state from READY to DELETING' (){
        given: 'cm Handle as Yang model'
            compositeState = new CompositeState(cmHandleState: READY)
            yangModelCmHandle = new YangModelCmHandle(id: cmHandleId, dmiProperties: [], publicProperties: [], compositeState: compositeState)
        when: 'updating cm handle state to "DELETING"'
            objectUnderTest.updateCmHandleStateBatch(Map.of(yangModelCmHandle, DELETING))
        then: 'the cm handle state is as expected'
            yangModelCmHandle.getCompositeState().getCmHandleState() == DELETING
        and: 'method to persist cm handle state is called once'
            1 * mockInventoryPersistence.saveCmHandleStateBatch(Map.of(yangModelCmHandle.getId(), yangModelCmHandle.getCompositeState()))
        and: 'the method to publish Lcm event is called once'
            1 * mockLcmEventsService.publishLcmEvent(cmHandleId, _, _)
    }

    def 'Update cmHandle state to DELETING to DELETED' (){
        given: 'cm Handle with state "DELETING" as Yang model '
            compositeState = new CompositeState(cmHandleState: DELETING)
            yangModelCmHandle = new YangModelCmHandle(id: cmHandleId, dmiProperties: [], publicProperties: [], compositeState: compositeState)
        when: 'updating cm handle state to "DELETED"'
            objectUnderTest.updateCmHandleStateBatch(Map.of(yangModelCmHandle, DELETED))
        then: 'the cm handle state is as expected'
            yangModelCmHandle.getCompositeState().getCmHandleState() == DELETED
        and: 'the method to publish Lcm event is called once'
            1 * mockLcmEventsService.publishLcmEvent(cmHandleId, _, _)
    }

    def 'No state change and no event to be published'() {
        given: 'Cm Handle batch with same state transition as before'
            def cmHandleStateMap = setupBatch('NO_CHANGE')
        when: 'updating a batch of changes'
            objectUnderTest.updateCmHandleStateBatch(cmHandleStateMap)
        then: 'no changes are persisted'
            1 * mockInventoryPersistence.saveCmHandleBatch(EMPTY_LIST)
            1 * mockInventoryPersistence.saveCmHandleStateBatch(EMPTY_MAP)
        and: 'no event will be published'
            0 * mockLcmEventsService.publishLcmEvent(*_)
        and: 'no log entries are written'
            assert logger.list.empty
    }

    def 'Batch of new cm handles provided'() {
        given: 'A batch of new cm handles'
            def yangModelCmHandlesToBeCreated = setupBatch('NEW')
        when: 'instantiating a batch of new cm handles'
            objectUnderTest.initiateStateAdvised(yangModelCmHandlesToBeCreated)
        then: 'new cm handles are saved using inventory persistence'
            1 * mockInventoryPersistence.saveCmHandleBatch(_) >> {
                args -> {
                    assert (args[0] as Collection<YangModelCmHandle>).id.containsAll('cmhandle1', 'cmhandle2')
                }
            }
        and: 'no state updates are persisted'
            1 * mockInventoryPersistence.saveCmHandleStateBatch(EMPTY_MAP)
        and: 'event service is called to publish events'
            2 * mockLcmEventsService.publishLcmEvent(_, _, _)
        and: 'two log entries are written'
            assert getLogMessage(0) == 'cmhandle1 is now in ADVISED state'
            assert getLogMessage(1) == 'cmhandle2 is now in ADVISED state'
    }

    def 'Batch of existing cm handles is updated'() {
        given: 'A batch of updated cm handles'
            def cmHandleStateMap = setupBatch('UPDATE')
        when: 'updating a batch of changes'
            objectUnderTest.updateCmHandleStateBatch(cmHandleStateMap)
        then: 'existing cm handles composite states are persisted'
            1 * mockInventoryPersistence.saveCmHandleStateBatch(_) >> {
                args -> {
                    assert (args[0] as Map<String, CompositeState>).keySet().containsAll(['cmhandle1', 'cmhandle2'])
                }
            }
        and: 'no new handles are persisted'
            1 * mockInventoryPersistence.saveCmHandleBatch(EMPTY_LIST)
        and: 'event service is called to publish events'
            2 * mockLcmEventsService.publishLcmEvent(_, _, _)
        and: 'two log entries are written'
            assert getLogMessage(0) == 'cmhandle1 is now in READY state'
            assert getLogMessage(1) == 'cmhandle2 is now in DELETING state'
    }

    def 'Batch of existing cm handles is deleted'() {
        given: 'A batch of deleted cm handles'
            def cmHandleStateMap = setupBatch('DELETED')
        when: 'updating a batch of changes'
            objectUnderTest.updateCmHandleStateBatch(cmHandleStateMap)
        then: 'state of deleted handles is not persisted'
            1 * mockInventoryPersistence.saveCmHandleStateBatch(EMPTY_MAP)
        and: 'no new handles are persisted'
            1 * mockInventoryPersistence.saveCmHandleBatch(EMPTY_LIST)
        and: 'event service is called to publish events'
            2 * mockLcmEventsService.publishLcmEvent(_, _, _)
        and: 'two log entries are written'
            assert getLogMessage(0) == 'cmhandle1 is now in DELETED state'
            assert getLogMessage(1) == 'cmhandle2 is now in DELETED state'
    }

    def 'Log entries and events are not sent when an error occurs during persistence'() {
        given: 'A batch of updated cm handles'
            def cmHandleStateMap = setupBatch('UPDATE')
        and: 'an error will be thrown when trying to persist'
            mockInventoryPersistence.saveCmHandleStateBatch(_) >> { throw new RuntimeException() }
        when: 'updating a batch of changes'
            objectUnderTest.updateCmHandleStateBatch(cmHandleStateMap)
        then: 'the exception is not handled'
            thrown(RuntimeException)
        and: 'no events are published'
            0 * mockLcmEventsService.publishLcmEvent(_, _, _)
        and: 'no log entries are written'
            assert logger.list.empty
    }

    def setupBatch(type) {

        def yangModelCmHandle1 = new YangModelCmHandle(id: 'cmhandle1', dmiProperties: [], publicProperties: [])
        def yangModelCmHandle2 = new YangModelCmHandle(id: 'cmhandle2', dmiProperties: [], publicProperties: [])

        switch (type) {
            case 'NEW':
                return [yangModelCmHandle1, yangModelCmHandle2]

            case 'DELETED':
                yangModelCmHandle1.compositeState = new CompositeState(cmHandleState: READY)
                yangModelCmHandle2.compositeState = new CompositeState(cmHandleState: READY)
                return [(yangModelCmHandle1): DELETED, (yangModelCmHandle2): DELETED]

            case 'UPDATE':
                yangModelCmHandle1.compositeState = new CompositeState(cmHandleState: ADVISED)
                yangModelCmHandle2.compositeState = new CompositeState(cmHandleState: READY)
                return [(yangModelCmHandle1): READY, (yangModelCmHandle2): DELETING]

            case 'NO_CHANGE':
                yangModelCmHandle1.compositeState = new CompositeState(cmHandleState: ADVISED)
                yangModelCmHandle2.compositeState = new CompositeState(cmHandleState: READY)
                return [(yangModelCmHandle1): ADVISED, (yangModelCmHandle2): READY]

            default:
                throw new IllegalArgumentException("batch type '${type}' not recognized")
        }
    }

    def getLogMessage(index) {
        return logger.list[index].formattedMessage
    }
}
