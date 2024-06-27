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

import org.onap.cps.events.EventsPublisher
import org.onap.cps.ncmp.events.lcm.v1.LcmEvent
import org.onap.cps.ncmp.events.lcm.v1.LcmEventHeader
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.kafka.KafkaException
import spock.lang.Specification

class LcmEventsServiceSpec extends Specification {

    def mockLcmEventsPublisher = Mock(EventsPublisher)
    def mockJsonObjectMapper = Mock(JsonObjectMapper)

    def objectUnderTest = new LcmEventsService(mockLcmEventsPublisher, mockJsonObjectMapper)

    def 'Create and Publish lcm event where events are #scenario'() {
        given: 'a cm handle id, Lcm Event, and headers'
            def cmHandleId = 'test-cm-handle-id'
            def eventId = UUID.randomUUID().toString()
            def lcmEvent = new LcmEvent(eventId: eventId, eventCorrelationId: cmHandleId)
        and: 'we also have a lcm event header'
            def lcmEventHeader = new LcmEventHeader(eventId: eventId, eventCorrelationId: cmHandleId)
        and: 'notificationsEnabled is #notificationsEnabled and it will be true as default'
            objectUnderTest.notificationsEnabled = notificationsEnabled
        and: 'lcm event header is transformed to headers map'
            mockJsonObjectMapper.convertToValueType(lcmEventHeader, Map.class) >> ['eventId': eventId, 'eventCorrelationId': cmHandleId]
        when: 'service is called to publish lcm event'
            objectUnderTest.publishLcmEvent('test-cm-handle-id', lcmEvent, lcmEventHeader)
        then: 'publisher is called #expectedTimesMethodCalled times'
            expectedTimesMethodCalled * mockLcmEventsPublisher.publishEvent(_, cmHandleId, _, lcmEvent) >> {
                args -> {
                    def eventHeaders = (args[2] as Map<String,Object>)
                    assert eventHeaders.containsKey('eventId')
                    assert eventHeaders.containsKey('eventCorrelationId')
                    assert eventHeaders.get('eventId') == eventId
                    assert eventHeaders.get('eventCorrelationId') == cmHandleId
                }
            }
        where: 'the following values are used'
            scenario   | notificationsEnabled || expectedTimesMethodCalled
            'enabled'  | true                 || 1
            'disabled' | false                || 0
    }

    def 'Unable to send message'(){
        given: 'a cm handle id and Lcm Event and notification enabled'
            def cmHandleId = 'test-cm-handle-id'
            def eventId = UUID.randomUUID().toString()
            def lcmEvent = new LcmEvent(eventId: eventId, eventCorrelationId: cmHandleId)
            def lcmEventHeader = new LcmEventHeader(eventId: eventId, eventCorrelationId: cmHandleId)
            objectUnderTest.notificationsEnabled = true
        when: 'publisher set to throw an exception'
            mockLcmEventsPublisher.publishEvent(_, _, _, _) >> { throw new KafkaException('publishing failed')}
        and: 'an event is publised'
            objectUnderTest.publishLcmEvent(cmHandleId, lcmEvent, lcmEventHeader)
        then: 'the exception is just logged and not bubbled up'
            noExceptionThrown()
    }

}
