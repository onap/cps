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

import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.kafka.CloudEventSerializer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.spock.Testcontainers
import java.util.concurrent.TimeUnit

@SpringBootTest(classes =[NcmpAsyncDataOperationEventConsumer, DataOperationRecordFilterStrategy])
@DirtiesContext
@Testcontainers
@EnableAutoConfiguration
class NcmpAsyncDataOperationEventConsumerIntegrationSpec extends MessagingBaseSpec {

    @SpringBean
    EventsPublisher mockEventsPublisher = Mock()

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry

    @Value('${app.ncmp.async-m2m.topic}')
    def topic

    def setup() {
        activateListeners()
    }

    def 'Filtering Cloud Events on Type.'() {
        given: 'a cloud event of type: #eventType'
            def cloudEvent = CloudEventBuilder.v1().withId('some id')
                    .withType(eventType)
                    .withSource(URI.create('some-source'))
                    .build()
        when: 'send the cloud event'
            ProducerRecord<String, CloudEvent> record = new ProducerRecord<>(topic, cloudEvent)
            KafkaProducer<String, CloudEvent> producer = new KafkaProducer<>(eventProducerConfigProperties(CloudEventSerializer))
            producer.send(record)
        and: 'wait a little for async processing of message'
            TimeUnit.MILLISECONDS.sleep(100)
        then: 'the event has only been forwarded for the correct type'
            expectedNUmberOfCallsToPublishForwardedEvent * mockEventsPublisher.publishCloudEvent(*_)
        where: 'the following event types are used'
            eventType                                        || expectedNUmberOfCallsToPublishForwardedEvent
            'DataOperationEvent'                             || 1
            'other type'                                     || 0
            'any type contain the word "DataOperationEvent"' || 1
    }

    def 'Non cloud events on same Topic.'() {
        when: 'sending a non-cloud event on the same topic'
            ProducerRecord<String, String> record = new ProducerRecord<>(topic, 'simple string event')
            KafkaProducer<String, String> producer = new KafkaProducer<>(eventProducerConfigProperties(StringSerializer))
            producer.send(record)
        and: 'wait a little for async processing of message'
            TimeUnit.MILLISECONDS.sleep(100)
        then: 'the event is not processed by this consumer'
            0 * mockEventsPublisher.publishCloudEvent(*_)
    }

    def activateListeners() {
        kafkaListenerEndpointRegistry.getListenerContainers().forEach(
            messageListenerContainer -> { ContainerTestUtils.waitForAssignment(messageListenerContainer, 1) }
        )
    }

}
