/*
 *  ============LICENSE_START=======================================================
 *  Copyright (c) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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
import io.cloudevents.kafka.impl.KafkaHeaders
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.onap.cps.events.EventsProducer
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.events.avc1_0_0.AvcEvent
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.ncmp.utils.events.MessagingBaseSpec
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.spock.Testcontainers

import java.nio.charset.Charset
import java.time.Duration

import static org.onap.cps.ncmp.utils.events.CloudEventMapper.toTargetEvent

@SpringBootTest(classes = [EventsProducer, CmAvcEventConsumer, ObjectMapper, JsonObjectMapper])
@Testcontainers
@DirtiesContext
class CmAvcEventConsumerSpec extends MessagingBaseSpec {

    @SpringBean
    EventsProducer eventsProducer = new EventsProducer<CloudEvent>(legacyEventKafkaTemplate, cloudEventKafkaTemplate)

    def mockCmAvcEventService = Mock(CmAvcEventService)
    def mockInventoryPersistence = Mock(InventoryPersistence)

    @SpringBean
    CmAvcEventConsumer cmAvcEventConsumer = new CmAvcEventConsumer(eventsProducer, mockCmAvcEventService, mockInventoryPersistence)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def cloudEventKafkaConsumer

    def 'Consume and forward valid message'() {
        given: 'consumer has a subscription on a topic'
            cloudEventKafkaConsumer = new KafkaConsumer<>(eventConsumerConfigProperties('test-group-1', CloudEventDeserializer))
            def cmEventsTopicName = 'test-topic-1'
            cmAvcEventConsumer.cmEventsTopicName = cmEventsTopicName
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
            cmAvcEventConsumer.consumeAndForward(consumerRecord)
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

    def 'Consume and process CM Avc event when #scenario'() {
        given: 'consumer has a subscription on a topic'
            cloudEventKafkaConsumer = new KafkaConsumer<>(eventConsumerConfigProperties('test-group-2', CloudEventDeserializer))
            def cmEventsTopicName = 'test-topic-2'
            cmAvcEventConsumer.cmEventsTopicName = cmEventsTopicName
            cloudEventKafkaConsumer.subscribe([cmEventsTopicName] as List<String>)
        and: 'a composite state with the data sync flag'
            def compositeState = new CompositeState(dataSyncEnabled: dataSyncFlag)
            1 * mockInventoryPersistence.getCmHandleState(_) >> compositeState
        and: 'ONAP DMI PLUGIN sends an event'
            def jsonData = TestUtils.getResourceFileContent('sampleAvcInputEvent.json')
            def testEventKey = 'sample-key'
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, AvcEvent.class)
            def source = 'ONAP-DMI-PLUGIN'
            def testCloudEventSent = CloudEventBuilder.v1()
                .withData(jsonObjectMapper.asJsonBytes(testEventSent))
                .withId('sample-id')
                .withType('sample-test-type')
                .withSource(URI.create(source)).build()
        and: 'event has source system information'
            def consumerRecord = new ConsumerRecord<String, CloudEvent>(cmEventsTopicName, 0, 0, testEventKey, testCloudEventSent)
            consumerRecord.headers().add('ce_source', source.getBytes(Charset.defaultCharset()))
        when: 'the event is consumed'
            cmAvcEventConsumer.consumeAndForward(consumerRecord)
        then: 'cm avc event is processed for caching'
            expectedCallToProcessCmAvcEvent * mockCmAvcEventService.processCmAvcEvent(testEventKey, _) >> { args ->
                {
                    assert args[1] instanceof AvcEvent
                }
            }
        where: 'following scenarios are used'
            scenario                     | dataSyncFlag || expectedCallToProcessCmAvcEvent
            'data sync flag is enabled'  | true         || 1
            'data sync flag is disabled' | false        || 0

    }
}
