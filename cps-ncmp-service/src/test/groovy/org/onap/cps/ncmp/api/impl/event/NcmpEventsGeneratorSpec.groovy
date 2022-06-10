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

class NcmpEventsGeneratorSpec extends Specification {

    def objectUnderTest = new NcmpEventsGenerator()
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
            if (operation != DELETE) {
                assert result.event.cmhandleProperties.size() != 0
                assert result.event.cmhandleProperties[0].toString() == '[publicProperty1:value1, publicProperty2:value2]'
            } else {
                assert result.event.cmhandleProperties.size() == 0
            }
        where: 'the following operations are used'
            operation << [CREATE, UPDATE, DELETE]
    }
}
