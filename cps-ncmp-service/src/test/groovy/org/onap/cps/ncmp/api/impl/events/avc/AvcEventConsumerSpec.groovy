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

package org.onap.cps.ncmp.api.impl.events.avc

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.events.avc.v1.AvcEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.messaging.MessageHeaders
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
    EventsPublisher eventsPublisher = new EventsPublisher<AvcEvent>(kafkaTemplate)

    @SpringBean
    AvcEventConsumer acvEventConsumer = new AvcEventConsumer(eventsPublisher, avcEventMapper)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def kafkaConsumer = new KafkaConsumer<>(consumerConfigProperties('ncmp-group'))

    def 'Consume and forward valid message'() {
        given: 'consumer has a subscription on a topic'
            def cmEventsTopicName = 'cm-events'
            acvEventConsumer.cmEventsTopicName = cmEventsTopicName
            kafkaConsumer.subscribe([cmEventsTopicName] as List<String>)
        and: 'an event is sent'
            def jsonData = TestUtils.getResourceFileContent('sampleAvcInputEvent.json')
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, AvcEvent.class)
        and: 'event has header information'
            def testEventMessageHeader = new MessageHeaders(['eventId': 'sample-eventid', 'eventCorrelationId': 'cmhandle1'])
        when: 'the event is consumed'
            acvEventConsumer.consumeAndForward(testEventSent, testEventMessageHeader)
        and: 'the topic is polled'
            def records = kafkaConsumer.poll(Duration.ofMillis(1500))
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
        and: 'event id differs between consumed and forwarded'
            record.headers().forEach(header -> {
                if (header.key().equals('eventId')) {
                    assert SerializationUtils.deserialize(header.value()) != 'sample-eventid'
                }
            })
        and: 'the event payload still matches'
            assert testEventSent == convertedAvcEvent
    }

}