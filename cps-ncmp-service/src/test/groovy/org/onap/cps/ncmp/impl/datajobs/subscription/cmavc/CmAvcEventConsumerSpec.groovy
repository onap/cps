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

package org.onap.cps.ncmp.impl.datajobs.subscription.cmavc

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
import org.onap.cps.ncmp.utils.events.MessagingBaseSpec
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.spock.Testcontainers

import java.nio.charset.Charset
import java.time.Duration

import static org.onap.cps.ncmp.utils.TestUtils.getResourceFileContent
import static org.onap.cps.ncmp.utils.events.CloudEventMapper.toTargetEvent

@SpringBootTest(classes = [EventsProducer, CmAvcEventConsumer, ObjectMapper, JsonObjectMapper])
@Testcontainers
@DirtiesContext
class CmAvcEventConsumerSpec extends MessagingBaseSpec {

    @SpringBean
    EventsProducer eventsProducer = new EventsProducer(legacyEventKafkaTemplate, cloudEventKafkaTemplate)

    def mockCmAvcEventService = Mock(CmAvcEventService)
    def mockInventoryPersistence = Mock(InventoryPersistence)

    @SpringBean
    CmAvcEventConsumer cmAvcEventConsumer = new CmAvcEventConsumer(eventsProducer, mockCmAvcEventService, mockInventoryPersistence)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    def cloudEventKafkaConsumer = new KafkaConsumer<>(eventConsumerConfigProperties('group for Test A', CloudEventDeserializer))

    def testAvcEvent
    def testEventKey

    def setup() {
        testEventKey = 'sample-key'
        testAvcEvent = jsonObjectMapper.convertJsonString(getResourceFileContent('sampleAvcInputEvent.json'), AvcEvent.class)
    }

    def 'Test A : Consume and forward valid message'() {
        given: 'a cloud event'
            def testCloudEventSent = buildCloudEvent('sample-source', 'test-cmhandle1')
        and: 'consumer has a subscription on the target topic for this test'
            cmAvcEventConsumer.cmEventsTopicName = 'target-topic-for-Test-A'
            cloudEventKafkaConsumer.subscribe([cmAvcEventConsumer.cmEventsTopicName])
        and: 'event is wrapped in a consumer record with message key(cmHandleId) and value as cloud event'
            def consumerRecordReceived = new ConsumerRecord<String, CloudEvent>(cmAvcEventConsumer.cmEventsTopicName, 0, 0, testEventKey, testCloudEventSent)
        when: 'the event is consumed and forwarded to target topic'
            cmAvcEventConsumer.consumeAndForward(consumerRecordReceived)
        then: 'the consumer record can be read from the target topic within 2 seconds'
            def consumerRecordOnTargetTopic = cloudEventKafkaConsumer.poll(Duration.ofMillis(2000)).iterator().next()
        and: 'the target event has the same key as the source event to maintain the ordering in a partition'
            def cloudEventFromTargetTopic = consumerRecordOnTargetTopic.value() as CloudEvent
            def avcEventFromTargetTopic = toTargetEvent(cloudEventFromTargetTopic, AvcEvent.class)
            assert consumerRecordOnTargetTopic.key() == consumerRecordReceived.key()
        and: 'we have correct headers forwarded where correlation id matches'
            assert KafkaHeaders.getParsedKafkaHeader(consumerRecordOnTargetTopic.headers(), 'ce_correlationid') == 'test-cmhandle1'
        and: 'event id is same between consumed and forwarded'
            assert KafkaHeaders.getParsedKafkaHeader(consumerRecordOnTargetTopic.headers(), 'ce_id') == 'sample-eventid'
        and: 'the event payload still matches'
            assert avcEventFromTargetTopic == testAvcEvent
    }

    def 'Test B : Consume and process CM Avc Event when #scenario'() {
        given: 'a cloud event is created(what we get from ONAP-DMI-PLUGIN)'
            def sourceSystem = 'ONAP-DMI-PLUGIN'
            def testCloudEventSent = buildCloudEvent(sourceSystem, 'some-cmhandle-id')
        and: 'a separate topic for this test'
            cmAvcEventConsumer.cmEventsTopicName = 'some-topic-for-Test-B'
        and: 'inventory persistence service has #scenario'
            def compositeState = new CompositeState(dataSyncEnabled: dataSyncFlag)
            1 * mockInventoryPersistence.getCmHandleState(_) >> compositeState
        and: 'event has source system as ONAP-DMI-PLUGIN and key(cmHandleId) and value as cloud event'
            def consumerRecord = new ConsumerRecord<String, CloudEvent>(cmAvcEventConsumer.cmEventsTopicName, 0, 0, testEventKey, testCloudEventSent)
            consumerRecord.headers().add('ce_source', sourceSystem.getBytes(Charset.defaultCharset()))
        when: 'the event is consumed'
            cmAvcEventConsumer.consumeAndForward(consumerRecord)
        then: 'cm avc event is processed for updating the cached data'
            expectedCallToProcessCmAvcEvent * mockCmAvcEventService.processCmAvcEvent(testEventKey, _) >> {args ->
                {
                    assert args[1] instanceof AvcEvent
                }
            }
        where: 'following scenarios are used'
            scenario                  | dataSyncFlag || expectedCallToProcessCmAvcEvent
            'data sync flag enabled'  | true         || 1
            'data sync flag disabled' | false        || 0

    }

    def buildCloudEvent(sourceSystem, cmHandleId) {

        return CloudEventBuilder.v1()
            .withData(jsonObjectMapper.asJsonBytes(testAvcEvent))
            .withId('sample-eventid')
            .withType('sample-test-type')
            .withSource(URI.create(sourceSystem as String))
            .withExtension('correlationid', cmHandleId).build()

    }
}
