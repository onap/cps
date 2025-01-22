/*
 *  ============LICENSE_START=======================================================
 *  Copyright (c) 2023-2024 Nordix Foundation.
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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.cmavc

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.kafka.CloudEventDeserializer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.onap.cps.events.EventsPublisher
import org.onap.cps.ncmp.events.avc1_0_0.AvcEvent
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.ncmp.utils.events.MessagingBaseSpec
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.spock.Testcontainers

import java.time.Duration

import static org.onap.cps.ncmp.utils.events.CloudEventMapper.toTargetEvent

@SpringBootTest(classes = [EventsPublisher, CmAvcEventConsumer, ObjectMapper, JsonObjectMapper])
@Testcontainers
@DirtiesContext
class CmAvcEventConsumerSpec extends MessagingBaseSpec {

    @SpringBean
    EventsPublisher eventsPublisher = new EventsPublisher<CloudEvent>(legacyEventKafkaTemplate, cloudEventKafkaTemplate)

    @SpringBean
    CmAvcEventConsumer acvEventConsumer = new CmAvcEventConsumer(eventsPublisher)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def cloudEventKafkaConsumer = new KafkaConsumer<>(eventConsumerConfigProperties('ncmp-group', CloudEventDeserializer))

    def 'Consume and forward valid message'() {
        given: 'consumer has a subscription on a topic'
            def cmEventsTopicName = 'cm-events'
            acvEventConsumer.cmEventsTopicName = cmEventsTopicName
            cloudEventKafkaConsumer.subscribe([cmEventsTopicName] as List<String>)
        and: 'an event is sent'
            def jsonData = TestUtils.getResourceFileContent('sampleAvcInputEvent.json')
            def testEventKey = 'sample-eventid-key'
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, AvcEvent.class)
            def testCloudEventSent = CloudEventBuilder.v1()
                .withData(jsonObjectMapper.asJsonBytes(testEventSent))
                .withId('sample-eventid')
                .withType('sample-test-type')
                .withSource(URI.create('sample-test-source'))
                .withExtension('correlationid', 'test-cmhandle1').build()
        and: 'event has header information'
            def consumerRecord = new ConsumerRecord<String, CloudEvent>(cmEventsTopicName, 0, 0, testEventKey, testCloudEventSent)
        when: 'the event is consumed and forwarded to target topic'
            acvEventConsumer.consumeAndForward(consumerRecord)
        and: 'the target topic is polled'
            def records = cloudEventKafkaConsumer.poll(Duration.ofMillis(1500))
        then: 'poll returns one record'
            assert records.size() == 1
        and: 'target record can be converted to AVC event'
            def record = records.iterator().next()
            def cloudEvent = record.value() as CloudEvent
            def convertedAvcEvent = toTargetEvent(cloudEvent, AvcEvent.class)
        and: 'the target event has the same key as the source event to maintain the ordering in a partition'
            assert record.key() == consumerRecord.key()
        and: 'we have correct headers forwarded where correlation id matches'
            assert KafkaHeaders.getParsedKafkaHeader(record.headers(), 'ce_correlationid') == 'test-cmhandle1'
        and: 'event id is same between consumed and forwarded'
            assert KafkaHeaders.getParsedKafkaHeader(record.headers(), 'ce_id') == 'sample-eventid'
        and: 'the event payload still matches'
            assert testEventSent == convertedAvcEvent
    }

}
