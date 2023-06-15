/*
 *  ============LICENSE_START=======================================================
 *  Copyright (c) 2023 Nordix Foundation.
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

package org.onap.cps.ncmp.api.impl.events.avc

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.header.internals.RecordHeader
import org.apache.kafka.common.serialization.StringDeserializer
import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.events.avc.v1.AvcEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.util.SerializationUtils
import org.testcontainers.spock.Testcontainers

import java.time.Duration

@SpringBootTest(classes = [EventsPublisher, AvcEventConsumer, ObjectMapper, JsonObjectMapper])
@Testcontainers
@DirtiesContext
class AvcEventConsumerSpec extends MessagingBaseSpec {

    @SpringBean
    AvcEventMapper avcEventMapper = Mappers.getMapper(AvcEventMapper.class)

    @SpringBean
    EventsPublisher eventsPublisher = new EventsPublisher<AvcEvent>(legacyEventKafkaTemplate, cloudEventKafkaTemplate)

    @SpringBean
    AvcEventConsumer acvEventConsumer = new AvcEventConsumer(eventsPublisher, avcEventMapper)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def legacyEventKafkaConsumer = new KafkaConsumer<>(eventConsumerConfigProperties('ncmp-group', StringDeserializer))

    def 'Consume and forward valid message'() {
        given: 'consumer has a subscription on a topic'
            def cmEventsTopicName = 'cm-events'
            acvEventConsumer.cmEventsTopicName = cmEventsTopicName
            legacyEventKafkaConsumer.subscribe([cmEventsTopicName] as List<String>)
        and: 'an event is sent'
            def jsonData = TestUtils.getResourceFileContent('sampleAvcInputEvent.json')
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, AvcEvent.class)
        and: 'event has header information'
            def consumerRecord = new ConsumerRecord<String,AvcEvent>(cmEventsTopicName,0, 0, 'sample-eventid', testEventSent)
            consumerRecord.headers().add(new RecordHeader('eventId', SerializationUtils.serialize('sample-eventid')))
            consumerRecord.headers().add(new RecordHeader('eventCorrelationId', SerializationUtils.serialize('cmhandle1')))
        when: 'the event is consumed'
            acvEventConsumer.consumeAndForward(consumerRecord)
        and: 'the topic is polled'
            def records = legacyEventKafkaConsumer.poll(Duration.ofMillis(1500))
        then: 'poll returns one record'
            assert records.size() == 1
        and: 'record can be converted to AVC event'
            def record = records.iterator().next()
            def convertedAvcEvent = jsonObjectMapper.convertJsonString(record.value(), AvcEvent.class)
        and: 'we have correct headers forwarded where correlation id matches'
            record.headers().forEach(header -> {
                if (header.key().equals('eventCorrelationId')) {
                    assert SerializationUtils.deserialize(header.value()) == 'cmhandle1'
                }
            })
        and: 'event id differs(as per requirement) between consumed and forwarded'
            record.headers().forEach(header -> {
                if (header.key().equals('eventId')) {
                    assert SerializationUtils.deserialize(header.value()) != 'sample-eventid'
                }
            })
        and: 'the event payload still matches'
            assert testEventSent == convertedAvcEvent
    }

}