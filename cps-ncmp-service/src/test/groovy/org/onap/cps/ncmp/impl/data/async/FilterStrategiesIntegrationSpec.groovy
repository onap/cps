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

package org.onap.cps.ncmp.impl.data.async

import io.cloudevents.core.builder.CloudEventBuilder
import org.onap.cps.events.EventsPublisher
import org.onap.cps.ncmp.config.KafkaConfig
import org.onap.cps.ncmp.event.model.ncmp.asyncm2m.DmiAsyncRequestResponseEvent
import org.onap.cps.ncmp.utils.events.ConsumerBaseSpec
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.spock.Testcontainers
import spock.util.concurrent.PollingConditions

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
        given: 'a data operation cloud event type'
            def cloudEvent = CloudEventBuilder.v1().withId('some id')
                .withType('DataOperationEvent')
                .withSource(URI.create('some-source'))
                .build()
        when: 'send the cloud event'
            cloudEventKafkaTemplate.send(topic, cloudEvent)
        then: 'wait a little for async processing of message (must wait to try to avoid false positives)'
            TimeUnit.MILLISECONDS.sleep(300)
        and: 'event is not consumed'
            0 * mockEventsPublisher.publishEvent(*_)
    }

    def 'Legacy event consumer with valid legacy event.'() {
        given: 'a legacy event'
            DmiAsyncRequestResponseEvent legacyEvent = new DmiAsyncRequestResponseEvent(eventId:'legacyEventId', eventTarget:'legacyEventTarget')
        and: 'a flag to track the publish event call'
            def publishEventMethodCalled = false
        and: 'the (mocked) events publisher will use the flag to indicate if it is called'
            mockEventsPublisher.publishEvent(*_) >> {
                publishEventMethodCalled = true
            }
        when: 'send the cloud event'
            legacyEventKafkaTemplate.send(topic, legacyEvent)
        then: 'the event is consumed by the (legacy) AsynRestRequest consumer'
            new PollingConditions().within(1) {
                assert publishEventMethodCalled == true
            }
    }

    def 'Filtering Cloud Events on Type.'() {
        given: 'a cloud event of type: #eventType'
            def cloudEvent = CloudEventBuilder.v1().withId('some id')
                .withType(eventType)
                .withSource(URI.create('some-source'))
                .build()
        and: 'a flag to track the publish event call'
            def publishEventMethodCalled = false
        and: 'the (mocked) events publisher will use the flag to indicate if it is called'
            mockEventsPublisher.publishCloudEvent(*_) >> {
                publishEventMethodCalled = true
            }
        when: 'send the cloud event'
            cloudEventKafkaTemplate.send(topic, cloudEvent)
        then: 'the event has only been forwarded for the correct type'
            new PollingConditions(initialDelay: 0.3).within(1) {
                assert publishEventMethodCalled == expectCallToPublishEventMethod
            }
        where: 'the following event types are used'
            eventType                                        || expectCallToPublishEventMethod
            'DataOperationEvent'                             || true
            'other type'                                     || false
            'any type contain the word "DataOperationEvent"' || true
    }

    //TODO Toine, add positive test with data to prove event is converted correctly (using correct factory)

    def 'Non cloud events on same Topic.'() {
        when: 'sending a non-cloud event on the same topic'
            legacyEventKafkaTemplate.send(topic, 'simple string event')
        then: 'wait a little for async processing of message (must wait to try to avoid false positives)'
            TimeUnit.MILLISECONDS.sleep(300)
        and: 'the event is not processed by this consumer'
            0 * mockEventsPublisher.publishCloudEvent(*_)
    }

}
