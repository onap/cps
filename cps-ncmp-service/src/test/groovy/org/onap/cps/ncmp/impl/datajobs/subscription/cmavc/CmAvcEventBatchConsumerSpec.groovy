/*
 *  ============LICENSE_START=======================================================
 *  Copyright (c) 2026 OpenInfra Foundation Europe. All rights reserved.
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

@SpringBootTest(classes = [CmAvcEventBatchConsumer, ObjectMapper, JsonObjectMapper])
@DirtiesContext
class CmAvcEventBatchConsumerSpec extends Specification {

    def mockEventProducer = Mock(EventProducer)
    def mockCmAvcEventService = Mock(CmAvcEventService)
    def mockInventoryPersistence = Mock(InventoryPersistence)
    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())
    def meterRegistry = new SimpleMeterRegistry()

    def objectUnderTest = new CmAvcEventBatchConsumer(mockEventProducer, mockCmAvcEventService, mockInventoryPersistence, meterRegistry)

    def validAvcEventAsJson

    def setup() {
        objectUnderTest.cmEventsTopicName = 'my-topic'
        validAvcEventAsJson = jsonObjectMapper.convertJsonString(getResourceFileContent('sampleAvcInputEvent.json'), AvcEvent.class)
    }

    def 'Consume and forward batch of avc events.'() {
        given: 'multiple cloud events'
            def testCloudEvent1 = buildCloudEvent('', '', validAvcEventAsJson)
            def testCloudEvent2 = buildCloudEvent('', '', validAvcEventAsJson)
        and: 'events are wrapped in consumer records and collected into ConsumerRecords batch'
            def consumerRecord1 = new ConsumerRecord<String, CloudEvent>('', 0, 0, 'key1', testCloudEvent1)
            def consumerRecord2 = new ConsumerRecord<String, CloudEvent>('', 0, 1, 'key2', testCloudEvent2)
            def topicPartition = new TopicPartition('', 0)
            def consumerRecordsBatch = new ConsumerRecords<String, CloudEvent>([(topicPartition): [consumerRecord1, consumerRecord2]])
        when: 'the batch is consumed and forwarded to target topic'
            objectUnderTest.consumeAndForwardBatch(consumerRecordsBatch)
        then: 'the batch is sent to the target topic with correct key-value pairs'
            1 * mockEventProducer.sendCloudEventBatch('my-topic', _) >> { args ->
                def eventsToForward = args[1] as List<Map.Entry<String, CloudEvent>>
                assert eventsToForward.size() == 2
                assert eventsToForward[0].key == 'key1'
                assert eventsToForward[0].value == testCloudEvent1
                assert eventsToForward[1].key == 'key2'
                assert eventsToForward[1].value == testCloudEvent2
            }
        and: 'the events forwarded counter is incremented by the batch size'
            assert meterRegistry.counter('cps.ncmp.cm.avc.events.forwarded.batch').count() == 2
    }

    def 'Consume and process batch of CM Avc Events with #scenario.'() {
        given: 'multiple cloud events with different source systems'
            def testCloudEvent1 = buildCloudEvent(sourceForCloudEvent1, '', validAvcEventAsJson)
            def testCloudEvent2 = buildCloudEvent(sourceForCloudEvent2, '', validAvcEventAsJson)
        and: 'inventory persistence service has data sync enabled for both handles'
            def compositeState = new CompositeState(dataSyncEnabled: true)
            mockInventoryPersistence.getCmHandleState(_) >> compositeState
        and: 'events have source systems and are wrapped in consumer records'
            def consumerRecord1 = new ConsumerRecord<String, CloudEvent>('', 0, 0, '', testCloudEvent1)
            def consumerRecord2 = new ConsumerRecord<String, CloudEvent>('', 0, 1, '', testCloudEvent2)
            consumerRecord1.headers().add('ce_source', sourceForCloudEvent1.getBytes(Charset.defaultCharset()))
            consumerRecord2.headers().add('ce_source', sourceForCloudEvent2.getBytes(Charset.defaultCharset()))
            def topicPartition = new TopicPartition('', 0)
            def consumerRecordsBatch = new ConsumerRecords<String, CloudEvent>([(topicPartition): [consumerRecord1, consumerRecord2]])
        when: 'the batch is consumed'
            objectUnderTest.consumeAndForwardBatch(consumerRecordsBatch)
        then: 'cm avc events are processed based on cloud event source'
            expectedCallsToProcessCmAvcEvent * mockCmAvcEventService.processCmAvcEvent(_, _) >> { args ->
                assert args[1] instanceof AvcEvent
            }
        and: 'the batch is forwarded to the target topic'
            1 * mockEventProducer.sendCloudEventBatch('my-topic', _) >> { args ->
                def eventsToForward = args[1] as List<Map.Entry<String, CloudEvent>>
                assert eventsToForward.size() == 2
            }
        where: 'following scenarios are used'
            scenario                    | sourceForCloudEvent1| sourceForCloudEvent2|| expectedCallsToProcessCmAvcEvent
            'both ONAP sources'         | 'ONAP-DMI-PLUGIN'   | 'ONAP-DMI-PLUGIN'   || 2
            'one ONAP, one other source'| 'ONAP-DMI-PLUGIN'   | 'other'             || 1
            'both other sources'        | 'other'             | 'another'           || 0
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
