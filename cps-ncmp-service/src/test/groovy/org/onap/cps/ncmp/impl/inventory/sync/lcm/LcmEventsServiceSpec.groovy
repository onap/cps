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

import static org.onap.cps.ncmp.events.lcm.v1.Values.CmHandleState.ADVISED
import static org.onap.cps.ncmp.events.lcm.v1.Values.CmHandleState.READY

import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.onap.cps.events.EventsPublisher
import org.onap.cps.ncmp.events.lcm.v1.Event
import org.onap.cps.ncmp.events.lcm.v1.LcmEvent
import org.onap.cps.ncmp.events.lcm.v1.LcmEventHeader
import org.onap.cps.ncmp.events.lcm.v1.Values
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.kafka.KafkaException
import spock.lang.Specification

class LcmEventsServiceSpec extends Specification {

    def mockLcmEventsPublisher = Mock(EventsPublisher)
    def mockJsonObjectMapper = Mock(JsonObjectMapper)
    def meterRegistry = new SimpleMeterRegistry()

    def objectUnderTest = new LcmEventsService(mockLcmEventsPublisher, mockJsonObjectMapper, meterRegistry)

    def 'Create and Publish lcm event where events are #scenario'() {
        given: 'a cm handle id, Lcm Event, and headers'
            def cmHandleId = 'test-cm-handle-id'
            def eventId = UUID.randomUUID().toString()
            def event = getEventWithCmHandleState(ADVISED, READY)
            def lcmEvent = new LcmEvent(event: event, eventId: eventId, eventCorrelationId: cmHandleId)
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
        and: 'metrics are recorded with correct tags'
            def timer = meterRegistry.find('cps.ncmp.lcm.events.publish').timer()
            if (notificationsEnabled) {
                assert timer != null
                assert timer.count() == expectedTimesMethodCalled
                def tags = timer.getId().getTags()
                assert tags.containsAll(Tag.of('oldCmHandleState', ADVISED.value()), Tag.of('newCmHandleState', READY.value()))
            } else {
                assert timer == null
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
        and: 'event #event'
            def lcmEvent = new LcmEvent(event: event, eventId: eventId, eventCorrelationId: cmHandleId)
            def lcmEventHeader = new LcmEventHeader(eventId: eventId, eventCorrelationId: cmHandleId)
            objectUnderTest.notificationsEnabled = true
        when: 'publisher set to throw an exception'
            mockLcmEventsPublisher.publishEvent(_, _, _, _) >> { throw new KafkaException('publishing failed')}
        and: 'an event is publised'
            objectUnderTest.publishLcmEvent(cmHandleId, lcmEvent, lcmEventHeader)
        then: 'the exception is just logged and not bubbled up'
            noExceptionThrown()
        and: 'metrics are recorded with error tags'
            def timer = meterRegistry.find('cps.ncmp.lcm.events.publish').timer()
            assert timer != null
            assert timer.count() == 1
            def expectedTags = [Tag.of('oldCmHandleState', 'N/A'), Tag.of('newCmHandleState', 'N/A')]
            def tags = timer.getId().getTags()
            assert tags.containsAll(expectedTags)
        where: 'the following values are used'
            scenario                  | event
            'without values'          | new Event()
            'without cm handle state' | getEvent()
    }

    def getEvent() {
        def event = new Event()
        def values = new Values()
        event.setOldValues(values)
        event.setNewValues(values)
        event
    }

    def getEventWithCmHandleState(oldCmHandleState, newCmHandleState) {
        def event = new Event()
        def advisedCmHandleStateValues = new Values()
        advisedCmHandleStateValues.setCmHandleState(oldCmHandleState)
        event.setOldValues(advisedCmHandleStateValues)
        def readyCmHandleStateValues = new Values()
        readyCmHandleStateValues.setCmHandleState(newCmHandleState)
        event.setNewValues(readyCmHandleStateValues)
        return event
    }
}
