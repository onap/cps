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

import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeStateBuilder
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import spock.lang.Specification

import static org.onap.ncmp.cmhandle.lcm.event.Event.Operation.CREATE
import static org.onap.ncmp.cmhandle.lcm.event.Event.Operation.DELETE
import static org.onap.ncmp.cmhandle.lcm.event.Event.Operation.UPDATE

class NcmpEventsCreatorSpec extends Specification {

    def objectUnderTest = new NcmpEventsCreator()
    def cmHandleId = 'test-cm-handle'

    def 'Map the NcmpEvent for operation #operation'() {
        given: 'NCMP cm handle details'
            def ncmpServiceCmHandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, compositeState: new CompositeStateBuilder().withCmHandleState(CmHandleState.READY).build(),
                publicProperties: ['publicProperty1': 'value1', 'publicProperty2': 'value2'])
        when: 'the event is populated'
            def result = objectUnderTest.populateNcmpEvent(cmHandleId, operation, ncmpServiceCmHandle)
        then: 'event header is mapped correctly'
            assert result.eventSource == 'org.onap.ncmp'
            assert result.eventCorrelationId == cmHandleId
        and: 'event payload is mapped correctly'
            assert result.event.operation == operation
            assert (result.event.cmhandleProperties != null) ? result.event.cmhandleProperties.size() : 0 == cmHandlePropertiesListSize
            assert (result.event.cmhandleProperties != null) ? result.event.cmhandleProperties[0] : null == cmHandleProperties
        where: 'the following operations are used'
            operation | cmHandlePropertiesListSize | cmHandleProperties
            CREATE    | 1                          | ['publicProperty1': 'value1', 'publicProperty2': 'value2']
            UPDATE    | 1                          | ['publicProperty1': 'value1', 'publicProperty2': 'value2']
            DELETE    | 0                          | null

    }
}
