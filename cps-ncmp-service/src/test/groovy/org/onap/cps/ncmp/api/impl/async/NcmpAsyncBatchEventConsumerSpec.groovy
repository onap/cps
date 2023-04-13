/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.async

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.commons.lang3.SerializationUtils
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.header.internals.RecordHeader
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.events.async.v1.BatchDataResponseEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.listener.adapter.RecordFilterStrategy
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.spock.Testcontainers

import java.time.Duration

@SpringBootTest(classes = [EventsPublisher, NcmpAsyncBatchEventConsumer, BatchRecordFilterStrategy,JsonObjectMapper,
                ObjectMapper])
@Testcontainers
@DirtiesContext
class NcmpAsyncBatchEventConsumerSpec extends MessagingBaseSpec {

    @SpringBean
    EventsPublisher asyncBatchEventPublisher = new EventsPublisher<BatchDataResponseEvent>(kafkaTemplate)

    @SpringBean
    NcmpAsyncBatchEventConsumer asyncBatchEventConsumer = new NcmpAsyncBatchEventConsumer(asyncBatchEventPublisher)

    @Autowired
    JsonObjectMapper jsonObjectMapper
    @Autowired
    RecordFilterStrategy<Object, Object> filterByeventType

    def kafkaConsumer = new KafkaConsumer<>(consumerConfigProperties('test'))
    def final clientTopic = "client-topic"
    ConsumerRecord<String, Object> consumerRecord
    def jsonData
    def testEventSent
    def eventTarget
    def eventType
    def eventId

    def setup() {
        jsonData = TestUtils.getResourceFileContent('asyncBatchDataevent.json')
        testEventSent = jsonObjectMapper.convertJsonString(jsonData, BatchDataResponseEvent.class)
        eventTarget = SerializationUtils.serialize(clientTopic)
        eventType = SerializationUtils.serialize("org.onap.cps.ncmp.events.async.v1.BatchDataResponseEvent")
        eventId = SerializationUtils.serialize("12345")
        consumerRecord = new ConsumerRecord<String, Object>(clientTopic, 1, 1L, "123", testEventSent)
        consumerRecord.headers().add(new RecordHeader("eventId", eventId))
        consumerRecord.headers().add(new RecordHeader("eventTarget", eventTarget))
        consumerRecord.headers().add(new RecordHeader("eventType", eventType))
    }

    def 'Consume and publish event to client specified topic'() {
        given: 'consumer subscribing to client topic'
            kafkaConsumer.subscribe([clientTopic])
        when: 'the batch event is consumed and published to client specified topic'
            asyncBatchEventConsumer.consumeAndForward(consumerRecord)
        and: 'the client specified topic is polled'
            def recordsConsumed = kafkaConsumer.poll(Duration.ofMillis(1500))
        then: 'consumer poll returns one record'
            assert recordsConsumed.size() == 1
        and: 'verifying consumed event data is same as published event data'
            def recordConsumed = recordsConsumed.iterator().next()
            assert testEventSent.getEvent().getBatchResponses().get(0).getStatusCode() ==
                (jsonObjectMapper.convertJsonString(recordConsumed.value(), BatchDataResponseEvent.class).getEvent()
                .getBatchResponses().get(0).getStatusCode())
    }

    def 'Non filtering the event'() {
        when: 'while consuming the topic ncmp-async-m2m it executes the filter strategy'
            def filterResult = filterByeventType.filter(consumerRecord)
        then: 'Non filtering returns false'
            assert filterResult == false
    }

    def 'Filtering the event'() {
        given: 'Sending consumer record with some other eventType'
            consumerRecord.headers().remove("eventType")
            consumerRecord.headers().add(new RecordHeader("eventType", SerializationUtils.serialize("wrongType")))
        when: 'while consuming the topic ncmp-async-m2m it executes the filter strategy'
            def filterResult = filterByeventType.filter(consumerRecord)
        then: 'filtering returns true if the header has wrong eventType'
            assert filterResult == true
    }
}
