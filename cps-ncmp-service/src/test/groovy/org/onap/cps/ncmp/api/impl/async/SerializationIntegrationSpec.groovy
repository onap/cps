/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2023-2024 Nordix Foundation.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.async

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.core.builder.CloudEventBuilder
import org.onap.cps.events.EventsPublisher
import org.onap.cps.ncmp.api.impl.config.kafka.KafkaConfig

import org.onap.cps.ncmp.api.kafka.ConsumerBaseSpec
import org.onap.cps.ncmp.event.model.DmiAsyncRequestResponseEvent
import org.onap.cps.ncmp.event.model.NcmpAsyncRequestResponseEvent
import org.onap.cps.ncmp.events.async1_0_0.Data
import org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent
import org.onap.cps.ncmp.events.async1_0_0.Response
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.spock.Testcontainers

import java.util.concurrent.TimeUnit

@SpringBootTest(classes =[DataOperationEventConsumer, AsyncRestRequestResponseEventConsumer, RecordFilterStrategies, KafkaConfig])
@DirtiesContext
@Testcontainers
@EnableAutoConfiguration
class SerializationIntegrationSpec extends ConsumerBaseSpec {

    @SpringBean
    EventsPublisher mockEventsPublisher = Mock()

    @SpringBean
    NcmpAsyncRequestResponseEventMapper mapper = Stub() { toNcmpAsyncEvent(_) >> new NcmpAsyncRequestResponseEvent(eventId: 'my-event-id', eventTarget: 'some client topic')}

    @Autowired
    private ObjectMapper objectMapper

    @Value('${app.ncmp.async-m2m.topic}')
    def topic

    def capturedForwardedEvent

    def 'Forwarding DataOperation Event Data.'() {
        given: 'a data operation cloud event'
            def cloudEvent = createCloudEvent()
        when: 'send the event'
            cloudEventKafkaTemplate.send(topic, cloudEvent)
        and: 'wait a little for async processing of message'
            TimeUnit.MILLISECONDS.sleep(300)
        then: 'the event has been forwarded'
            1 * mockEventsPublisher.publishCloudEvent('some client topic', 'some-correlation-id', _) >> { args -> { capturedForwardedEvent = args[2] } }
        and: 'the forwarded event is identical to the event that was sent'
            assert capturedForwardedEvent == cloudEvent
    }

    def 'Forwarding AsyncRestRequestResponse Event Data.'() {
        given: 'async request response legacy event'
            def dmiAsyncRequestResponseEvent = new DmiAsyncRequestResponseEvent(eventId: 'my-event-id',eventTarget: 'some client topic')
        when: 'send the event'
            legacyEventKafkaTemplate.send(topic, dmiAsyncRequestResponseEvent)
        and: 'wait a little for async processing of message'
            TimeUnit.MILLISECONDS.sleep(300)
        then: 'the event has been forwarded'
            1 * mockEventsPublisher.publishEvent('some client topic', 'my-event-id', _) >> { args -> { capturedForwardedEvent = args[2] } }
        and: 'the captured id and target of the forwarded event is same as the one that was sent'
            assert capturedForwardedEvent.eventId == dmiAsyncRequestResponseEvent.eventId
            assert capturedForwardedEvent.eventTarget == dmiAsyncRequestResponseEvent.eventTarget
    }

    def createCloudEvent() {
        def dataOperationEvent = new DataOperationEvent(data: new Data(responses: [new Response()]))
        return CloudEventBuilder.v1()
            .withId('my-event-id')
            .withType('DataOperationEvent')
            .withSource(URI.create('some-source'))
            .withExtension('destination','some client topic')
            .withExtension('correlationid','some-correlation-id')
            .withData(objectMapper.writeValueAsBytes(dataOperationEvent))
            .build()
    }

}
