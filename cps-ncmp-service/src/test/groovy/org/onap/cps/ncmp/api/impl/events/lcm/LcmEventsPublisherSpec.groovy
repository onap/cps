/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.events.lcm

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.onap.cps.events.EventsPublisher
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.events.lcm.v1.Event
import org.onap.cps.ncmp.events.lcm.v1.LcmEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.util.SerializationUtils
import org.testcontainers.spock.Testcontainers

import java.time.Duration

@SpringBootTest(classes = [ObjectMapper, JsonObjectMapper])
@Testcontainers
@DirtiesContext
class LcmEventsPublisherSpec extends MessagingBaseSpec {

    def legacyEventKafkaConsumer = new KafkaConsumer<>(eventConsumerConfigProperties('ncmp-group', StringDeserializer))

    def testTopic = 'ncmp-events-test'

    @SpringBean
    EventsPublisher<LcmEvent> lcmEventsPublisher = new EventsPublisher(legacyEventKafkaTemplate, cloudEventKafkaTemplate)

    @Autowired
    JsonObjectMapper jsonObjectMapper


    def 'Produce and Consume Lcm Event'() {
        given: 'event key and event data'
            def eventKey = 'lcm'
            def eventId = 'test-uuid'
            def eventCorrelationId = 'cmhandle-test'
            def eventSource = 'org.onap.ncmp'
            def eventTime = '2022-12-31T20:30:40.000+0000'
            def eventType = 'org.onap.ncmp.cmhandle.lcm.event'
            def eventSchema = 'org.onap.ncmp.cmhandle.lcm.event'
            def eventSchemaVersion = 'v1'
            def eventData = new LcmEvent(
                eventId: eventId,
                eventCorrelationId: eventCorrelationId,
                eventSource: eventSource,
                eventTime: eventTime,
                eventType: eventType,
                eventSchema: eventSchema,
                eventSchemaVersion: eventSchemaVersion,
                event: new Event(cmHandleId: 'cmhandle-test'))
        and: 'we have a event header'
            def eventHeader = [
                eventId           : eventId,
                eventCorrelationId: eventCorrelationId,
                eventSource       : eventSource,
                eventTime         : eventTime,
                eventType         : eventType,
                eventSchema       : eventSchema,
                eventSchemaVersion: eventSchemaVersion]
        and: 'consumer has a subscription'
            legacyEventKafkaConsumer.subscribe([testTopic] as List<String>)
        when: 'an event is published'
            lcmEventsPublisher.publishEvent(testTopic, eventKey, eventHeader, eventData)
        and: 'topic is polled'
            def records = legacyEventKafkaConsumer.poll(Duration.ofMillis(1500))
        then: 'poll returns one record'
            assert records.size() == 1
        and: 'record key matches the expected event key'
            def record = records.iterator().next()
            assert eventKey == record.key
        and: 'record matches the expected event'
            def expectedJsonString = TestUtils.getResourceFileContent('expectedLcmEvent.json')
            def expectedLcmEvent = jsonObjectMapper.convertJsonString(expectedJsonString, LcmEvent.class)
            assert expectedLcmEvent == jsonObjectMapper.convertJsonString(record.value, LcmEvent.class)
        and: 'record header matches the expected parameters'
            assert SerializationUtils.deserialize(record.headers().lastHeader('eventId').value()) == eventId
            assert SerializationUtils.deserialize(record.headers().lastHeader('eventCorrelationId').value()) == eventCorrelationId
    }
}
