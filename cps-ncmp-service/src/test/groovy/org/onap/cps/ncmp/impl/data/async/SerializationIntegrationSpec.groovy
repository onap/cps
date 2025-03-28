/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.data.async

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.core.builder.CloudEventBuilder
import org.onap.cps.events.EventsProducer
import org.onap.cps.ncmp.config.KafkaConfig
import org.onap.cps.ncmp.event.model.DmiAsyncRequestResponseEvent
import org.onap.cps.ncmp.event.model.NcmpAsyncRequestResponseEvent
import org.onap.cps.ncmp.events.async1_0_0.Data
import org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent
import org.onap.cps.ncmp.events.async1_0_0.Response
import org.onap.cps.ncmp.utils.events.ConsumerBaseSpec
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.spock.Testcontainers
import spock.util.concurrent.PollingConditions

@SpringBootTest(classes =[DataOperationEventConsumer, DmiAsyncRequestResponseEventConsumer, RecordFilterStrategies, KafkaConfig])
@DirtiesContext
@Testcontainers
@EnableAutoConfiguration
class SerializationIntegrationSpec extends ConsumerBaseSpec {

    @SpringBean
    EventsProducer mockEventsProducer = Mock()

    @SpringBean
    NcmpAsyncRequestResponseEventMapper mapper = Stub() { toNcmpAsyncEvent(_) >> new NcmpAsyncRequestResponseEvent(eventId: 'my-event-id', eventTarget: 'some client topic')}

    @Autowired
    private ObjectMapper objectMapper

    @Value('${app.ncmp.async-m2m.topic}')
    def topic

    def 'Forwarding DataOperation Event Data.'() {
        given: 'a data operation cloud event'
            def cloudEvent = createCloudEvent()
        and: 'a flag to track the send cloud event call'
            def sendCloudEventMethodCalled = false
        and: 'the (mocked) events producer will use the flag to indicate if it is called and will capture the cloud event'
            mockEventsProducer.sendCloudEvent('some client topic', 'some-correlation-id', cloudEvent) >> {
                sendCloudEventMethodCalled = true
            }
        when: 'send the event'
            cloudEventKafkaTemplate.send(topic, cloudEvent)
        then: 'the event has been forwarded'
            new PollingConditions().within(1) {
                assert sendCloudEventMethodCalled == true
            }
    }

    def 'Forwarding AsyncRestRequestResponse Event Data.'() {
        given: 'async request response legacy event'
            def dmiAsyncRequestResponseEvent = new DmiAsyncRequestResponseEvent(eventId: 'my-event-id',eventTarget: 'some client topic')
        and: 'a flag to track the send event call'
            def sendEventMethodCalled = false
        and: 'the (mocked) events producer will use the flag to indicate if it is called and will capture the event'
            mockEventsProducer.sendEvent(*_) >> {
                sendEventMethodCalled = true
            }
        when: 'send the event'
            legacyEventKafkaTemplate.send(topic, dmiAsyncRequestResponseEvent)
        then: 'the event has been forwarded'
            new PollingConditions().within(1) {
                assert sendEventMethodCalled == true
            }
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
