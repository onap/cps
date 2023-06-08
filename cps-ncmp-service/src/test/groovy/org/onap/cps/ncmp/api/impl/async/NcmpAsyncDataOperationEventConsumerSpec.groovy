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
import org.apache.kafka.common.serialization.StringDeserializer
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent1_0_0
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.listener.adapter.RecordFilterStrategy
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.spock.Testcontainers
import java.time.Duration

@SpringBootTest(classes = [EventsPublisher, NcmpAsyncDataOperationEventConsumer, DataOperationRecordFilterStrategy,JsonObjectMapper, ObjectMapper])
@Testcontainers
@DirtiesContext
class NcmpAsyncDataOperationEventConsumerSpec extends MessagingBaseSpec {

    @SpringBean
    EventsPublisher asyncDataOperationEventPublisher = new EventsPublisher<DataOperationEvent1_0_0>(legacyEventKafkaTemplate, cloudEventKafkaTemplate)

    @SpringBean
    NcmpAsyncDataOperationEventConsumer asyncDataOperationEventConsumer = new NcmpAsyncDataOperationEventConsumer(asyncDataOperationEventPublisher)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    RecordFilterStrategy<String, DataOperationEvent1_0_0> recordFilterStrategy

    def legacyEventKafkaConsumer = new KafkaConsumer<>(eventConsumerConfigProperties('test', StringDeserializer))
    def static clientTopic = 'client-topic'
    def static dataOperationType = 'org.onap.cps.ncmp.events.async.DataOperationEvent1_0_0'

    def 'Consume and publish event to client specified topic'() {
        given: 'consumer subscribing to client topic'
            legacyEventKafkaConsumer.subscribe([clientTopic])
        and: 'consumer record for data operation event'
            def consumerRecordIn = createConsumerRecord(dataOperationType)
        when: 'the data operation event is consumed and published to client specified topic'
            asyncDataOperationEventConsumer.consumeAndPublish(consumerRecordIn)
        and: 'the client specified topic is polled'
            def consumerRecordOut = legacyEventKafkaConsumer.poll(Duration.ofMillis(1500))[0]
        then: 'verifying consumed event operationID is same as published event operationID'
            def operationIdIn = consumerRecordIn.value.data.responses[0].operationId
            def operationIdOut = jsonObjectMapper.convertJsonString((String)consumerRecordOut.value(), DataOperationEvent1_0_0.class).data.responses[0].operationId
            assert operationIdIn == operationIdOut
    }

    def 'Filter an event with type #eventType'() {
        given: 'consumer record for event with type #eventType'
            def consumerRecord = createConsumerRecord(eventType)
        when: 'while consuming the topic ncmp-async-m2m it executes the filter strategy'
            def result = recordFilterStrategy.filter(consumerRecord)
        then: 'the event is #description'
            assert result == expectedResult
        where: 'filter the event based on the eventType #eventType'
            description                                     | eventType       || expectedResult
            'not filtered(the consumer will see the event)' | dataOperationType  || false
            'filtered(the consumer will not see the event)' | 'wrongType'     || true
    }

    def createConsumerRecord(eventTypeAsString) {
        def jsonData = TestUtils.getResourceFileContent('dataOperationEvent.json')
        def testEventSent = jsonObjectMapper.convertJsonString(jsonData, DataOperationEvent1_0_0.class)
        def eventTarget = SerializationUtils.serialize(clientTopic)
        def eventType = SerializationUtils.serialize(eventTypeAsString)
        def eventId = SerializationUtils.serialize('12345')
        def consumerRecord = new ConsumerRecord<String, Object>(clientTopic, 1, 1L, '123', testEventSent)
        consumerRecord.headers().add(new RecordHeader('eventId', eventId))
        consumerRecord.headers().add(new RecordHeader('eventTarget', eventTarget))
        consumerRecord.headers().add(new RecordHeader('eventType', eventType))
        return consumerRecord
    }
}
