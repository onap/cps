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

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.ObjectMapper
import io.cloudevents.CloudEvent
import io.cloudevents.core.builder.CloudEventBuilder
import io.cloudevents.kafka.CloudEventDeserializer
import io.cloudevents.kafka.CloudEventSerializer
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import java.time.Duration
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.onap.cps.events.EventProducer
import org.onap.cps.ncmp.config.ExactlyOnceSemanticsKafkaConfig
import org.onap.cps.ncmp.events.avc1_0_0.AvcEvent
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.utils.events.ConsumerBaseSpec
import org.onap.cps.utils.JsonObjectMapper
import org.slf4j.LoggerFactory
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.KafkaException
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import org.testcontainers.spock.Testcontainers

@SpringBootTest(classes = [CmAvcEventBatchConsumer, ExactlyOnceSemanticsKafkaConfig,
        KafkaProperties, ObjectMapper, JsonObjectMapper, SimpleMeterRegistry])
@EnableConfigurationProperties
@Testcontainers
@DirtiesContext
@TestPropertySource(properties = [
        'ncmp.kafka.eos.enabled=true',
        'ncmp.notifications.avc-event-producer.transaction-id-prefix=tx-batch-int-test-',
        'ncmp.notifications.avc-event-consumer.batch-enabled=true',
        'ncmp.notifications.avc-event-consumer.concurrency=1',
        'ncmp.notifications.avc-event-consumer.max-poll-records=500',
        'app.dmi.cm-events.topic=dmi-cm-events-batch-int',
        'app.ncmp.avc.cm-events-topic=cm-events-output-batch-int',
        'notification.enabled=true',
        'spring.kafka.consumer.group-id=batch-int-test-listener-group'
])
class CmAvcEventExactlyOnceIntegrationSpec extends ConsumerBaseSpec {

    static final INPUT_TOPIC = 'dmi-cm-events-batch-int'
    static final OUTPUT_TOPIC = 'cm-events-output-batch-int'
    static final BATCH_SIZE = 500

    @SpringBean
    InventoryPersistence mockInventoryPersistence = Mock()

    @SpringBean
    CmAvcEventService mockCmAvcEventService = Mock()

    @SpringBean
    EventProducer failOnceEventProducer = new EventProducerSimulatingFailureOnFirstAttempt(legacyEventKafkaTemplate, cloudEventKafkaTemplate, meterRegistry)

    @Autowired
    JsonObjectMapper jsonObjectMapper

    @Autowired
    @Qualifier('cloudEventKafkaTemplateForExactlyOnceSemantics')
    KafkaTemplate eosKafkaTemplate

    def validAvcEventAsJson
    def testProducer = new KafkaProducer<String, CloudEvent>(producerConfigProperties())
    def testOutputConsumer = new KafkaConsumer<String, CloudEvent>(readCommittedConsumerConfigProperties())
    def logAppender = new ListAppender<ILoggingEvent>()

    def setup() {
        testOutputConsumer.subscribe([OUTPUT_TOPIC])
        failOnceEventProducer.setCloudEventKafkaTemplateForExactlyOnceSemantics(eosKafkaTemplate)
        validAvcEventAsJson = jsonObjectMapper.convertJsonString(getResourceFileContent('sampleAvcInputEvent.json'), AvcEvent.class)
        def batchConsumerLogger = (Logger) LoggerFactory.getLogger(CmAvcEventBatchConsumer)
        batchConsumerLogger.setLevel(Level.DEBUG)
        def kafkaContainerLogger = (Logger) LoggerFactory.getLogger('org.springframework.kafka.listener.KafkaMessageListenerContainer')
        logAppender.start()
        batchConsumerLogger.addAppender(logAppender)
        kafkaContainerLogger.addAppender(logAppender)
    }

    def 'Send a batch of event with failure.'() {
        given: 'a batch of 500 event is sent'
            BATCH_SIZE.times { index ->
                def key = 'key-' + index
                testProducer.send(new ProducerRecord<>(INPUT_TOPIC, key, buildCloudEvent(key, validAvcEventAsJson)))
            }
        when: 'polling events from the output topic'
            def result = pollUntilExpectedCount(testOutputConsumer, BATCH_SIZE, 30000)
        then: 'the client has received all events'
            assert result.size() == BATCH_SIZE
        and: 'the batch event was processed more than once'
            assert logAppender.list.count { it.formattedMessage.contains('Processing batch of') } > 1
        and: 'no additional events are produced after the batch (confirming exactly once behaviour)'
            def additionalEvents = testOutputConsumer.poll(Duration.ofMillis(3000))
            assert additionalEvents.count() == 0
    }

    def pollUntilExpectedCount(consumer, expectedCount, timeoutMs) {
        def result = []
        def deadline = System.currentTimeMillis() + timeoutMs
        while (result.size() < expectedCount && System.currentTimeMillis() < deadline) {
            consumer.poll(Duration.ofMillis(1000)).each { result.add(it) }
        }
        return result
    }

    def buildCloudEvent(String key, sourceEvent) {
        return CloudEventBuilder.v1()
                .withData(jsonObjectMapper.asJsonBytes(sourceEvent))
                .withId('event-' + key)
                .withType('sample-test-type')
                .withSource(URI.create('test-source'))
                .withExtension('correlationid', key).build()
    }

    def readCommittedConsumerConfigProperties() {
        return [('bootstrap.servers')  : kafkaTestContainer.getBootstrapServers().split(',')[0],
                ('key.deserializer')   : StringDeserializer,
                ('value.deserializer') : CloudEventDeserializer,
                ('auto.offset.reset')  : 'earliest',
                ('group.id')           : 'batch-int-test-consumer-group',
                ('isolation.level')    : 'read_committed']
    }

    def producerConfigProperties() {
        return [('bootstrap.servers'): kafkaTestContainer.getBootstrapServers().split(',')[0],
                ('key.serializer')   : StringSerializer,
                ('value.serializer') : CloudEventSerializer]
    }

    def cleanup() {
        testProducer.flush()
        testProducer.close()
        testOutputConsumer.close()
        ((Logger) LoggerFactory.getLogger(CmAvcEventBatchConsumer)).detachAndStopAllAppenders()
        ((Logger) LoggerFactory.getLogger('org.springframework.kafka.listener.KafkaMessageListenerContainer')).detachAndStopAllAppenders()
    }
}

/**
 * EventProducer that can simulate a KafkaException on the first sendCloudEventBatch call,
 * then delegates to the real EventProducer on subsequent calls.
 */
class EventProducerSimulatingFailureOnFirstAttempt extends EventProducer {

    boolean simulatedKafkaException = false

    EventProducerSimulatingFailureOnFirstAttempt(KafkaTemplate legacyEventKafkaTemplate,
                                                 KafkaTemplate cloudEventKafkaTemplate,
                                                 MeterRegistry meterRegistry) {
        super(legacyEventKafkaTemplate, cloudEventKafkaTemplate, meterRegistry)
    }

    @Override
    void sendCloudEventBatch(String topicName, List<Map.Entry<String, CloudEvent>> events) {
        if (!simulatedKafkaException) {
            simulatedKafkaException = true
            throw new KafkaException('Simulated batch send failure')
        }
        super.sendCloudEventBatch(topicName, events)
    }
}
