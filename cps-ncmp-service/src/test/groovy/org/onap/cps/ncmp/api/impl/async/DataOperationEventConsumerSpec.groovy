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
import io.cloudevents.CloudEvent
import io.cloudevents.kafka.CloudEventDeserializer
import io.cloudevents.kafka.CloudEventSerializer
import io.cloudevents.kafka.impl.KafkaHeaders
import io.cloudevents.core.builder.CloudEventBuilder
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.header.internals.RecordHeaders
import org.onap.cps.events.EventsPublisher
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.listener.adapter.RecordFilterStrategy
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.spock.Testcontainers
import java.time.Duration

import static org.onap.cps.events.mapper.CloudEventMapper.toTargetEvent

@SpringBootTest(classes = [EventsPublisher, DataOperationEventConsumer, RecordFilterStrategies, JsonObjectMapper, ObjectMapper])
@Testcontainers
@DirtiesContext
class DataOperationEventConsumerSpec extends MessagingBaseSpec {

    @SpringBean
    EventsPublisher asyncDataOperationEventPublisher = new EventsPublisher<CloudEvent>(legacyEventKafkaTemplate, cloudEventKafkaTemplate)

    @SpringBean
    DataOperationEventConsumer objectUnderTest = new DataOperationEventConsumer(asyncDataOperationEventPublisher)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    RecordFilterStrategy<String, CloudEvent> dataOperationRecordFilterStrategy

    def cloudEventKafkaConsumer = new KafkaConsumer<>(eventConsumerConfigProperties('test', CloudEventDeserializer))
    def static clientTopic = 'client-topic'
    def static dataOperationType = 'org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent'

    def 'Consume and publish event to client specified topic'() {
        given: 'consumer subscribing to client topic'
            cloudEventKafkaConsumer.subscribe([clientTopic])
        and: 'consumer record for data operation event'
            def consumerRecordIn = createConsumerRecord(dataOperationType)
        when: 'the data operation event is consumed and published to client specified topic'
            objectUnderTest.consumeAndPublish(consumerRecordIn)
        and: 'the client specified topic is polled'
            def consumerRecordOut = cloudEventKafkaConsumer.poll(Duration.ofMillis(1500))[0]
        then: 'verify cloud compliant headers'
            def consumerRecordOutHeaders = consumerRecordOut.headers()
            assert KafkaHeaders.getParsedKafkaHeader(consumerRecordOutHeaders, 'ce_correlationid') == 'request-id'
            assert KafkaHeaders.getParsedKafkaHeader(consumerRecordOutHeaders, 'ce_id') == 'some-uuid'
            assert KafkaHeaders.getParsedKafkaHeader(consumerRecordOutHeaders, 'ce_type') == dataOperationType
        and: 'verify that extension is included into header'
            assert KafkaHeaders.getParsedKafkaHeader(consumerRecordOutHeaders, 'ce_destination') == clientTopic
        and: 'map consumer record to expected event type'
            def dataOperationResponseEvent = toTargetEvent(consumerRecordOut.value(), DataOperationEvent.class)
        and: 'verify published response data properties'
            def response = dataOperationResponseEvent.data.responses[0]
            response.operationId == 'some-operation-id'
            response.statusCode == 'any-success-status-code'
            response.statusMessage == 'Successfully applied changes'
            response.result as String == '[some-key:some-value]'
    }

    def 'Filter an event with type #eventType'() {
        given: 'consumer record for event with type #eventType'
            def consumerRecord = createConsumerRecord(eventType)
        when: 'while consuming the topic ncmp-async-m2m it executes the filter strategy'
            def result = dataOperationRecordFilterStrategy.filter(consumerRecord)
        then: 'the event is #description'
            assert result == expectedResult
        where: 'filter the event based on the eventType #eventType'
            description                                     | eventType         || expectedResult
            'not filtered(the consumer will see the event)' | dataOperationType || false
            'filtered(the consumer will not see the event)' | 'wrongType'       || true
    }

    def createConsumerRecord(eventTypeAsString) {
        def jsonData = TestUtils.getResourceFileContent('dataOperationEvent.json')
        def testEventSentAsBytes = jsonObjectMapper.asJsonBytes(jsonObjectMapper.convertJsonString(jsonData, DataOperationEvent.class))

        CloudEvent cloudEvent = getCloudEvent(eventTypeAsString, testEventSentAsBytes)

        def headers = new RecordHeaders()
        def cloudEventSerializer = new CloudEventSerializer()
        cloudEventSerializer.serialize(clientTopic, headers, cloudEvent)

        def consumerRecord = new ConsumerRecord<String, CloudEvent>(clientTopic, 0, 0L, 'sample-message-key', cloudEvent)
        headers.forEach(header -> consumerRecord.headers().add(header))
        return consumerRecord
    }

    def getCloudEvent(eventTypeAsString, byte[] testEventSentAsBytes) {
        return CloudEventBuilder.v1()
                .withId("some-uuid")
                .withType(eventTypeAsString)
                .withSource(URI.create("sample-test-source"))
                .withData(testEventSentAsBytes)
                .withExtension("correlationid", "request-id")
                .withExtension("destination", clientTopic)
                .build();
    }
}
