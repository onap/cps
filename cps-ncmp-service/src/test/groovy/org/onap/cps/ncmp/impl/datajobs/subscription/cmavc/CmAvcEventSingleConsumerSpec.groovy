/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.datajobs.subscription.cmavc

import static org.onap.cps.ncmp.utils.TestUtils.getResourceFileContent

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.nio.charset.Charset
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.common.TopicPartition
import org.onap.cps.events.EventProducer
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.events.avc1_0_0.AvcEvent
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import spock.lang.Specification

@SpringBootTest(classes = [CmAvcEventConsumer, ObjectMapper, JsonObjectMapper])
@DirtiesContext
class CmAvcEventSingleConsumerSpec extends Specification {

    def mockEventProducer = Mock(EventProducer)
    def mockCmAvcEventService = Mock(CmAvcEventService)
    def mockInventoryPersistence = Mock(InventoryPersistence)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def meterRegistry = new SimpleMeterRegistry()

    def objectUnderTest = new CmAvcEventConsumer(mockEventProducer, mockCmAvcEventService, mockInventoryPersistence, meterRegistry)

    def testEventKey = 'sample-key'
    def validAvcEventAsJson
    def onapDmiSourceSystem = 'ONAP-DMI-PLUGIN'

    def setup() {
        objectUnderTest.cmEventsTopicName = 'my-topic'
        validAvcEventAsJson = jsonObjectMapper.convertJsonString(getResourceFileContent('sampleAvcInputEvent.json'), AvcEvent.class)
    }

    def 'Consume and forward single avc event.'() {
        given: 'a cloud event wrapped in a ConsumerRecords batch of 1'
            def testCloudEvent = buildCloudEvent('sample-source', 'test-cmhandle1', validAvcEventAsJson)
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('', 0, 0, testEventKey, testCloudEvent)
            def topicPartition = new TopicPartition('', 0)
            def consumerRecordsBatch = new ConsumerRecords<String, CloudEvent>([(topicPartition): [consumerRecord]])
        when: 'the event is consumed and forwarded'
            objectUnderTest.consumeAndForward(consumerRecordsBatch)
        then: 'the event is sent to the target topic with the correct key'
            1 * mockEventProducer.sendCloudEventBatch('my-topic', _) >> { args ->
                def eventsToForward = args[1] as List<Map.Entry<String, CloudEvent>>
                assert eventsToForward.size() == 1
                assert eventsToForward[0].key == testEventKey
                assert eventsToForward[0].value == testCloudEvent
            }
        and: 'the events forwarded counter is incremented'
            assert meterRegistry.counter('cps.ncmp.cm.avc.events.forwarded').count() == 1
    }

    def 'Consume and process CM Avc Event with #scenario.'() {
        given: 'a cloud event'
            def testCloudEvent = buildCloudEvent(sourceSystem, 'some-cmhandle-id', validAvcEventAsJson)
        and: 'inventory persistence service has #scenario'
            def compositeState = new CompositeState(dataSyncEnabled: dataSyncEnabled)
            mockInventoryPersistence.getCmHandleState(_) >> compositeState
        and: 'event has source system header and is wrapped in a ConsumerRecords batch'
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('', 0, 0, testEventKey, testCloudEvent)
            if (sourceSystem != null) {
                consumerRecord.headers().add('ce_source', sourceSystem.getBytes(Charset.defaultCharset()))
            }
            def topicPartition = new TopicPartition('', 0)
            def consumerRecordsBatch = new ConsumerRecords<String, CloudEvent>([(topicPartition): [consumerRecord]])
        when: 'the event is consumed'
            objectUnderTest.consumeAndForward(consumerRecordsBatch)
        then: 'cm avc event is processed for updating the cached data'
            expectedCallToProcessCmAvcEvent * mockCmAvcEventService.processCmAvcEvent(testEventKey, _) >> { args ->
                { assert args[1] instanceof AvcEvent }
            }
        where: 'following scenarios are used'
            scenario                          | sourceSystem       | dataSyncEnabled || expectedCallToProcessCmAvcEvent
            'source ONAP, data sync enabled'  | 'ONAP-DMI-PLUGIN'  | true            || 1
            'source ONAP, data sync disabled' | 'ONAP-DMI-PLUGIN'  | false           || 0
            'other source, data sync enabled' | 'other'            | true            || 0
    }

    def 'Forward non-avc invalid event with source ONAP-DMI-PLUGIN.'() {
        given: 'a non-avc cloud event'
            def someJsonForOtherStructure = '{"some attribute":"for other event"}'
            def testCloudEvent = buildCloudEvent(onapDmiSourceSystem, 'some-cmhandle-id', someJsonForOtherStructure)
        and: 'inventory persistence service has data sync enabled'
            def compositeState = new CompositeState(dataSyncEnabled: true)
            mockInventoryPersistence.getCmHandleState(_) >> compositeState
        and: 'event has source system header and is wrapped in a ConsumerRecords batch'
            def consumerRecord = new ConsumerRecord<String, CloudEvent>('', 0, 0, testEventKey, testCloudEvent)
            consumerRecord.headers().add('ce_source', onapDmiSourceSystem.getBytes(Charset.defaultCharset()))
            def topicPartition = new TopicPartition('', 0)
            def consumerRecordsBatch = new ConsumerRecords<String, CloudEvent>([(topicPartition): [consumerRecord]])
        when: 'the event is consumed'
            objectUnderTest.consumeAndForward(consumerRecordsBatch)
        then: 'no AVC event processing takes place'
            0 * mockCmAvcEventService.processCmAvcEvent(testEventKey, _)
    }

    def buildCloudEvent(sourceSystem, cmHandleId, sourceEvent) {
        return CloudEventBuilder.v1()
            .withData(jsonObjectMapper.asJsonBytes(sourceEvent))
            .withId('sample-eventid')
            .withType('sample-test-type')
            .withSource(URI.create(sourceSystem as String))
            .withExtension('correlationid', cmHandleId).build()
    }
}
