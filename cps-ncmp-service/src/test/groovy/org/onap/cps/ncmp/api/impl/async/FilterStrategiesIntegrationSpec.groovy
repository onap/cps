/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2023 Nordix Foundation.
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

import io.cloudevents.core.builder.CloudEventBuilder
import org.onap.cps.events.EventsPublisher
import org.onap.cps.ncmp.api.impl.config.kafka.KafkaConfig

import org.onap.cps.ncmp.api.kafka.ConsumerBaseSpec
import org.onap.cps.ncmp.event.model.DmiAsyncRequestResponseEvent
import org.spockframework.spring.SpringBean
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
class FilterStrategiesIntegrationSpec extends ConsumerBaseSpec {

    @SpringBean
    EventsPublisher mockEventsPublisher = Mock()

    @SpringBean
    NcmpAsyncRequestResponseEventMapper mapper = Stub()

    @Value('${app.ncmp.async-m2m.topic}')
    def topic

    def 'Legacy event consumer with cloud event.'() {
        given: 'a cloud event of type: #eventType'
            def cloudEvent = CloudEventBuilder.v1().withId('some id')
                .withType('DataOperationEvent')
                .withSource(URI.create('some-source'))
                .build()
        when: 'send the cloud event'
            cloudEventKafkaTemplate.send(topic, cloudEvent)
        and: 'wait a little for async processing of message'
            TimeUnit.MILLISECONDS.sleep(300)
        then: 'event is not consumed'
            0 * mockEventsPublisher.publishEvent(*_)
    }

    def 'Legacy event consumer with valid legacy event.'() {
        given: 'a cloud event of type: #eventType'
            DmiAsyncRequestResponseEvent legacyEvent = new DmiAsyncRequestResponseEvent(eventId:'legacyEventId', eventTarget:'legacyEventTarget')
        when: 'send the cloud event'
            legacyEventKafkaTemplate.send(topic, legacyEvent)
        and: 'wait a little for async processing of message'
            TimeUnit.MILLISECONDS.sleep(300)
        then: 'the event is consumed by the (legacy) AsynRestRequest consumer'
            1 * mockEventsPublisher.publishEvent(*_)
    }

    def 'Filtering Cloud Events on Type.'() {
        given: 'a cloud event of type: #eventType'
            def cloudEvent = CloudEventBuilder.v1().withId('some id')
                .withType(eventType)
                .withSource(URI.create('some-source'))
                .build()
        when: 'send the cloud event'
            cloudEventKafkaTemplate.send(topic, cloudEvent)
        and: 'wait a little for async processing of message'
            TimeUnit.MILLISECONDS.sleep(300)
        then: 'the event has only been forwarded for the correct type'
            expectedNUmberOfCallsToPublishForwardedEvent * mockEventsPublisher.publishCloudEvent(*_)
        where: 'the following event types are used'
            eventType                                        || expectedNUmberOfCallsToPublishForwardedEvent
            'DataOperationEvent'                             || 1
            'other type'                                     || 0
            'any type contain the word "DataOperationEvent"' || 1
    }

    //TODO Toine, add positive test with data to prove event is converted correctly (using correct factory)

    def 'Non cloud events on same Topic.'() {
        when: 'sending a non-cloud event on the same topic'
            legacyEventKafkaTemplate.send(topic, 'simple string event')
        and: 'wait a little for async processing of message'
            TimeUnit.MILLISECONDS.sleep(300)
        then: 'the event is not processed by this consumer'
            0 * mockEventsPublisher.publishCloudEvent(*_)
    }

}
