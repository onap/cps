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
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.onap.cps.ncmp.api.impl.config.kafka.KafkaConfig
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.kafka.CloudEventDeserializer
import org.onap.cps.ncmp.api.impl.events.EventsPublisher
import org.onap.cps.ncmp.api.kafka.MessagingBaseSpec
import org.onap.cps.ncmp.event.model.DmiAsyncRequestResponseEvent
import org.onap.cps.ncmp.event.model.ForwardedEvent
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

@SpringBootTest(classes =[NcmpAsyncDataOperationEventConsumer, NcmpAsyncRequestResponseEventConsumer, DataOperationAndDmiAsyncRecordFilterStrategy, KafkaConfig, EventsPublisher])
@DirtiesContext
@Testcontainers
@EnableAutoConfiguration
class NcmpAsyncDataOperationAndDmiRequestResponseEventConsumerIntegrationSpec extends MessagingBaseSpec<DmiAsyncRequestResponseEvent> {

    @Value('${app.ncmp.async-m2m.topic}')
    def topic
    def dataOperationEventTarget = 'cloud-Event-client-topic'
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
    NcmpAsyncRequestResponseEventMapper mapper = Stub() { toNcmpAsyncEvent(_) >> ncmpAsyncRequestResponse(legacyEventId, dmiAsyncEventTarget)}

    def cloudEventDataOperationKafkaConsumer = new KafkaConsumer<>(eventConsumerConfigProperties('ncmp-group', CloudEventDeserializer.class))
    def legacyDMIAsyncEventKafkaConsumer = new KafkaConsumer<>(eventConsumerConfigProperties('legacy-ncmp-group', StringDeserializer.class))

    def setup() {
        activateListeners()
    }

    def cleanup() {
        cloudEventDataOperationKafkaConsumer.close()
        legacyDMIAsyncEventKafkaConsumer.close()
    }
    
    def 'DataOperation related cloud events are consumed by data operation consumer and excluded by legacy(DMIAsyncResponseEvent) consumer.'() {
        given: 'a cloud event of type: #ceType'
            CloudEvent cloudEvent = cloudEvent(ceType)
        when: 'send the cloud event'
            cloudEventsPublisher.publishCloudEvent(topic, "122345", cloudEvent)
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
            DmiAsyncRequestResponseEvent legacyEvent =  dmiAsyncRequestResponse(legacyEventId, dmiAsyncEventTarget)
            legacyEventsPublisher.publishEvent(topic, '1234', legacyEvent)
        and: 'wait a little for async processing of message'
            TimeUnit.MILLISECONDS.sleep(300)
        and: 'consumer is subscribing to client topic to poll messages'
            legacyDMIAsyncEventKafkaConsumer.subscribe([dmiAsyncEventTarget] as List<String>)
        and: 'polling the consumer on the client topic'
            def dmiAsyncClientTopicRecords = legacyDMIAsyncEventKafkaConsumer.poll(Duration.ofMillis(15000))
        then: 'Verifying the produced records size'
            assert dmiAsyncClientTopicRecords.size() == 1
    }

    def dmiAsyncRequestResponse(String legacyEventId, String legacyEventTarget) {
        DmiAsyncRequestResponseEvent legacyEvent = new DmiAsyncRequestResponseEvent();
        legacyEvent.setEventId(legacyEventId)
        legacyEvent.setEventTarget(legacyEventTarget)
        return legacyEvent
    }

    def ncmpAsyncRequestResponse(String legacyEventId, String legacyEventTarget) {
        ForwardedEvent forwardedEvent = new ForwardedEvent()
        forwardedEvent.setEventId(legacyEventId)
        forwardedEvent.setEventTarget(legacyEventTarget)
        NcmpAsyncRequestResponseEvent ncmpAsyncRequestResponseEvent = new NcmpAsyncRequestResponseEvent()
        ncmpAsyncRequestResponseEvent.setEventId(legacyEventId)
        ncmpAsyncRequestResponseEvent.setEventTarget(legacyEventTarget)
        ncmpAsyncRequestResponseEvent.setForwardedEvent(forwardedEvent)
        return ncmpAsyncRequestResponseEvent
    }

    def cloudEvent(String ceType){
        final Response response = new Response();
        response.setOperationId("opID");
        response.setStatusMessage("test");
        final List<Response> responseList = new ArrayList<>();
        responseList.add(response);
        final Data data = new Data();
        data.setResponses(responseList);
        final DataOperationEvent dataOperationEvent = new DataOperationEvent();
        dataOperationEvent.setData(data);
        def cloudEvent = CloudEventBuilder.v1().withId('some id')
            .withType(ceType)
            .withSource(URI.create('some-source'))
            .withId("12345")
            .withExtension("destination", dataOperationEventTarget)
            .withData(objectMapper.writeValueAsBytes(dataOperationEvent))
            .build()
        return cloudEvent;
    }

    def activateListeners() {
        kafkaListenerEndpointRegistry.getListenerContainers().forEach(
            messageListenerContainer -> { ContainerTestUtils.waitForAssignment(messageListenerContainer, 1) }
        )
    }

}
