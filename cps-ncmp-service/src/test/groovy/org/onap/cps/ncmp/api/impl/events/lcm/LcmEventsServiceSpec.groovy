/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.events.lcm

import static org.onap.cps.ncmp.api.impl.events.mapper.CloudEventMapper.toTargetEvent

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.impl.utils.context.CpsApplicationContext
import org.onap.cps.ncmp.events.lcm.v1.Data
import org.onap.cps.ncmp.events.lcm.v1.LcmEvent
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.kafka.KafkaException
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@ContextConfiguration(classes = [CpsApplicationContext, ObjectMapper, JsonObjectMapper])
class LcmEventsServiceSpec extends Specification {

    def mockLcmEventsPublisher = Mock(EventsPublisher)

    def objectUnderTest = new LcmEventsService(mockLcmEventsPublisher)

    def 'Create and Publish lcm event where events are #scenario'() {
        given: 'an lcm event'
            def payload = new Data(cmHandleId: 'someCmHandleId', alternateId: 'someAlternateId')
            def lcmEvent = new LcmEvent(data: payload)
        and: 'notificationsEnabled is #notificationsEnabled and it will be true as default'
            objectUnderTest.notificationsEnabled = notificationsEnabled
        when: 'service is called to publish lcm event'
            objectUnderTest.publishLcmEvent('someCmHandleId', lcmEvent)
        then: 'publisher is called #expectedTimesMethodCalled times'
            expectedTimesMethodCalled * mockLcmEventsPublisher.publishCloudEvent(_, 'someCmHandleId',
                cloudEvent -> {
                    assert cloudEvent.extensions.containsKey('correlationid')
                    assert cloudEvent.extensions.get('correlationid') == 'someCmHandleId'

                    def actualLcmEventPayload= toTargetEvent(cloudEvent, LcmEvent.class).data
                    def expectedLcmEventPayload = new Data(cmHandleId: 'someCmHandleId', alternateId: 'someAlternateId')
                    assert [actualLcmEventPayload] == [expectedLcmEventPayload]

                })
        where: 'the following values are used'
            scenario   | notificationsEnabled || expectedTimesMethodCalled
            'enabled'  | true                 || 1
            'disabled' | false                || 0
    }

    def 'Unable to send message'(){
        given: 'a cm handle id and Lcm Event and notification enabled'
            def cmHandleId = 'test-cm-handle-id'
            def lcmEvent = new LcmEvent()
            objectUnderTest.notificationsEnabled = true
        when: 'publisher set to throw an exception'
            mockLcmEventsPublisher.publishCloudEvent(_, _, _) >> { throw new KafkaException('publishing failed')}
        and: 'an event is published'
            objectUnderTest.publishLcmEvent(cmHandleId, lcmEvent)
        then: 'the exception is just logged and not bubbled up'
            noExceptionThrown()
    }

}
