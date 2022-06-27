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

import org.onap.cps.ncmp.api.impl.utils.YangDataConverter
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.inventory.CompositeStateBuilder
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import spock.lang.Specification

import static org.onap.cps.ncmp.api.impl.event.NcmpCmHandleStateTransition.ADVISED_TO_LOCKED
import static org.onap.cps.ncmp.api.impl.event.NcmpCmHandleStateTransition.ADVISED_TO_READY
import static org.onap.cps.ncmp.api.impl.event.NcmpCmHandleStateTransition.ANY_TO_DELETING
import static org.onap.cps.ncmp.api.impl.event.NcmpCmHandleStateTransition.DELETING_TO_DELETED
import static org.onap.cps.ncmp.api.impl.event.NcmpCmHandleStateTransition.NOTHING_TO_ADVISED
import static org.onap.cps.ncmp.api.inventory.CmHandleState.ADVISED
import static org.onap.cps.ncmp.api.inventory.CmHandleState.DELETED
import static org.onap.cps.ncmp.api.inventory.CmHandleState.DELETING
import static org.onap.cps.ncmp.api.inventory.CmHandleState.LOCKED
import static org.onap.cps.ncmp.api.inventory.CmHandleState.READY

class NcmpEventsCreatorSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)

    def objectUnderTest = new NcmpEventsCreator(mockInventoryPersistence)
    def cmHandleId = 'test-cm-handle'

    def 'Map the NCMP Event based on state transition #ncmpCmHandleStateTransition'() {
        given: 'mocked response to the inventory persistence'
            mockResponses(cmHandleState)
        when: 'the Ncmp Event is populated'
            def result = objectUnderTest.populateNcmpEvent(cmHandleId, ncmpCmHandleStateTransition)
        then: 'ncmp event header is mapped correctly'
            assert result.eventCorrelationId == cmHandleId
        and: 'correct Ncmp Event is generated'
            assert result.event.cmHandleId == cmHandleId
            assert result.event.cmhandleState.value() == eventCmHandleState
            assert (result.event.cmhandleProperties != null ? result.event.cmhandleProperties.size() : 0) == cmHandlePropertiesListSize
            assert (result.event.cmhandleProperties != null ? result.event.cmhandleProperties[0] : []) == cmHandleProperties
        where: 'the state transition events are as follows'
            ncmpCmHandleStateTransition | cmHandleState || eventCmHandleState | cmHandlePropertiesListSize | cmHandleProperties
            NOTHING_TO_ADVISED          | ADVISED       || 'ADVISED'          | 1                          | ['publicProperty1': 'publicValue1']
            ADVISED_TO_READY            | READY         || 'READY'            | 1                          | ['publicProperty1': 'publicValue1']
            ADVISED_TO_LOCKED           | LOCKED        || 'LOCKED'           | 1                          | ['publicProperty1': 'publicValue1']
            ANY_TO_DELETING             | DELETING      || 'DELETING'         | 0                          | []
            DELETING_TO_DELETED         | DELETED       || 'DELETED'          | 0                          | []
    }

    def mockResponses(cmHandleState) {
        def yangModelCmHandle = new YangModelCmHandle(id: cmHandleId,
            dmiProperties: [],
            publicProperties: [new YangModelCmHandle.Property('publicProperty1', 'publicValue1')],
            compositeState: new CompositeStateBuilder().withCmHandleState(cmHandleState).build())
        mockInventoryPersistence.getYangModelCmHandle(cmHandleId) >> yangModelCmHandle
        YangDataConverter.convertYangModelCmHandleToNcmpServiceCmHandle(yangModelCmHandle) >> new NcmpServiceCmHandle(cmHandleId: cmHandleId,
            dmiProperties: [:],
            publicProperties: ['name': 'publicProperty1', 'value': 'publicValue1'],
            compositeState: new CompositeStateBuilder().withCmHandleState(cmHandleState).build())
    }
}
