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

package org.onap.cps.ncmp.api.impl.async

import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.kafka.impl.KafkaHeaders
import org.onap.cps.ncmp.api.impl.config.kafka.KafkaConfig
import io.cloudevents.core.builder.CloudEventBuilder
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.event.model.DmiAsyncRequestResponseEvent
import org.onap.cps.ncmp.event.model.NcmpAsyncRequestResponseEvent
import org.onap.cps.ncmp.events.async1_0_0.Data
import org.onap.cps.ncmp.events.async1_0_0.DataOperationEvent
import org.onap.cps.ncmp.events.async1_0_0.Response
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.test.annotation.DirtiesContext
import org.testcontainers.spock.Testcontainers
import java.time.Duration
import java.util.concurrent.TimeUnit

@SpringBootTest(classes =[NcmpAsyncDataOperationEventConsumer, NcmpAsyncRequestResponseEventConsumer, RecordFilterStrategies, KafkaConfig, EventsPublisher])
@DirtiesContext
@Testcontainers
@EnableAutoConfiguration
class KafkaListenersIntegrationSpec extends MessagingBaseSpec<DmiAsyncRequestResponseEvent> {

    @Value('${app.ncmp.async-m2m.topic}')
    def topic
    def dataOperationEventTarget = 'cloud-event-client-topic'
    def legacyEventId = 'sampleId'
    def dmiAsyncEventTarget = 'legacy-event-client-topic'

    @Autowired
    EventsPublisher cloudEventsPublisher

    @Autowired
    EventsPublisher<DmiAsyncRequestResponseEvent> legacyEventsPublisher

    @Autowired
    private KafkaListenerEndpointRegistry kafkaListenerEndpointRegistry

    @Autowired
    private ObjectMapper objectMapper;

    @SpringBean
    NcmpAsyncRequestResponseEventMapper mapper = Stub() { toNcmpAsyncEvent(_) >> new NcmpAsyncRequestResponseEvent(eventId: legacyEventId, eventTarget: dmiAsyncEventTarget)}

    def setup() {
        activateListeners()
    }

    def 'DataOperation related cloud events are consumed by data operation consumer and excluded by legacy(DMIAsyncResponseEvent) consumer.'() {
        given: 'a cloud event of type: #ceType'
            CloudEvent cloudEvent = cloudEvent(ceType)
        when: 'send the cloud event'
            cloudEventsPublisher.publishCloudEvent(topic, 'someKey', cloudEvent)
        then: 'wait a little for async processing of message'
            TimeUnit.MILLISECONDS.sleep(300)
        and: 'consumer is subscribing to client topic to poll messages'
            cloudEventDataOperationKafkaConsumer.subscribe([dataOperationEventTarget] as List<String>)
            legacyDMIAsyncEventKafkaConsumer.subscribe([dmiAsyncEventTarget] as List<String>)
        and: 'polling the consumer on the client-topic'
            def dataOperationClientTopicRecords = cloudEventDataOperationKafkaConsumer.poll(Duration.ofMillis(3000))
            def dmiAsyncClientTopicRecords = legacyDMIAsyncEventKafkaConsumer.poll(Duration.ofMillis(3000))
        then: 'Verifying the produced records size both in the data operation & DMIAsync client topic'
            assert dataOperationClientTopicRecords.size() == expectedNoOfRecordsProducedAtDataOperationClientTopicWithFilter
            assert dmiAsyncClientTopicRecords.size() == expectedNoOfRecordsProducedAtDMIAsyncEventClientTopicWithFilter
        then: 'Verifying the ceType header is forwarded to the data operation client topic'
            dataOperationClientTopicRecords.forEach { consumerRecord ->
                    assert (KafkaHeaders.getParsedKafkaHeader(consumerRecord.headers(), "ce_type")) == ceType }
        where: 'the following event types are used'
            ceType                                           || expectedNoOfRecordsProducedAtDataOperationClientTopicWithFilter || expectedNoOfRecordsProducedAtDMIAsyncEventClientTopicWithFilter
            'DataOperationEvent'                             || 1                                                               || 0
            'Other Type'                                     || 0                                                               || 0
    }

    def 'DMIAsyncRequestResponse events consumed by only DMIAsyncRequestResponse consumer.'() {
        when: 'sending a DMIAsyncRequestResponse event on the same topic'
            DmiAsyncRequestResponseEvent legacyEvent =  new DmiAsyncRequestResponseEvent(eventId: legacyEventId, eventTarget: dmiAsyncEventTarget)
            legacyEventsPublisher.publishEvent(topic, 'someKey', legacyEvent)
        and: 'wait a little for async processing of message'
            TimeUnit.MILLISECONDS.sleep(300)
        and: 'consumer is subscribing to client topic to poll messages'
            legacyDMIAsyncEventKafkaConsumer.subscribe([dmiAsyncEventTarget] as List<String>)
        and: 'polling the consumer on the client topic'
            def dmiAsyncClientTopicRecords = legacyDMIAsyncEventKafkaConsumer.poll(Duration.ofMillis(3000))
        then: 'Verifying the produced records size'
            assert dmiAsyncClientTopicRecords.size() == 1
    }

    def cloudEvent(ceType){
        def cloudEvent = CloudEventBuilder.v1()
            .withId('someId')
            .withType(ceType)
            .withSource(URI.create('someSource'))
            .withExtension("destination", dataOperationEventTarget)
            .withData(objectMapper.writeValueAsBytes(new DataOperationEvent(data: new Data(responses: [new Response(operationId: 'opID', statusMessage: 'test')]))))
            .build()
        return cloudEvent;
    }

    def activateListeners() {
        kafkaListenerEndpointRegistry.getListenerContainers().forEach(
            messageListenerContainer -> { ContainerTestUtils.waitForAssignment(messageListenerContainer, 1) }
        )
    }

}
