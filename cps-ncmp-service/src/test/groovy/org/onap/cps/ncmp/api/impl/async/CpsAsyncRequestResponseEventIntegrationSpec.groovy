/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2022 Nordix Foundation.
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

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.mapstruct.factory.Mappers
import org.onap.cps.event.model.DmiAsyncRequestResponseEvent
import org.onap.cps.event.model.NcmpAsyncRequestResponseEvent
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.serializer.JsonSerializer
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.spock.Testcontainers
import org.testcontainers.utility.DockerImageName

import java.time.Duration
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.utils.JsonObjectMapper
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.onap.cps.ncmp.utils.TestUtils;
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.spockframework.spring.SpringBean
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import spock.lang.Specification

@SpringBootTest(classes = [NcmpAsyncRequestResponseEventProducer, NcmpAsyncRequestResponseEventConsumer])
@Testcontainers
@DirtiesContext
class NcmpAsyncRequestResponseEventProducerIntegrationSpec extends Specification {

    private static final String CPS_ASYNC_EVENT_TOPIC_NAME = 'my-topic-999'

    static kafkaTestContainer = new KafkaContainer(
        DockerImageName.parse('confluentinc/cp-kafka:6.2.1')
    )

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(kafkaTestContainer::stop))
    }

    def setupSpec() {
        kafkaTestContainer.start()
    }

    KafkaTemplate<String, NcmpAsyncRequestResponseEvent> spiedKafkaTemplate = Spy(createTemplate())

    @SpringBean
    NcmpAsyncRequestResponseEventProducer cpsAsyncRequestResponseEventProducerService =
        new NcmpAsyncRequestResponseEventProducer(spiedKafkaTemplate);

    @SpringBean
    NcmpAsyncRequestResponseEventMapper ncmpAsyncRequestResponseEventMapper =
            Mappers.getMapper(NcmpAsyncRequestResponseEventMapper.class)

    @SpringBean
    NcmpAsyncRequestResponseEventConsumer ncmpAsyncRequestResponseEventConsumer =
            new NcmpAsyncRequestResponseEventConsumer(cpsAsyncRequestResponseEventProducerService,
                    ncmpAsyncRequestResponseEventMapper)

    @SpringBean
    ObjectMapper objectMapper = Mock()

    JsonObjectMapper mapper = new JsonObjectMapper(new ObjectMapper())

    KafkaConsumer<String, Object> consumer = new KafkaConsumer<>(getConsumerConfig())

    def 'Consume and forward valid message'() {
        given: 'an event is sent'
            def jsonData = TestUtils.getResourceFileContent('dmiAsyncRequestResponseEvent.json')
            def testEventSent = mapper.convertJsonString(jsonData, DmiAsyncRequestResponseEvent.class)
        and: 'consumer has a subscription'
            consumer.subscribe([CPS_ASYNC_EVENT_TOPIC_NAME] as List<String>)
        when: 'the event is consumed'
            ncmpAsyncRequestResponseEventConsumer.consume(testEventSent)
        and: 'the topic is polled'
            def records = consumer.poll(Duration.ofMillis(500))
        then: 'poll returns one record'
            assert records.size() == 1
        and: 'consumed forwarded event id is the same as sent event id'
            def record = records.iterator().next()
            assert testEventSent.eventId.equalsIgnoreCase(mapper.convertJsonString(record.value(),
                    NcmpAsyncRequestResponseEvent).getForwardedEvent().getEventId())
    }

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add('spring.kafka.bootstrap-servers', kafkaTestContainer::getBootstrapServers)
    }

    private KafkaTemplate<Integer, String> createTemplate() {
        Map<String, Object> producerProps = producerConfigProps();
        ProducerFactory<Integer, String> producerFactory =
                new DefaultKafkaProducerFactory<Integer, String>(producerProps);
        KafkaTemplate<Integer, String> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        return kafkaTemplate;
    }

    def producerConfigProps() {
        Map<String, Object> producerConfigProps = new HashMap<>();
        producerConfigProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaTestContainer.getBootstrapServers().split(',')[0]);
        producerConfigProps.put(ProducerConfig.RETRIES_CONFIG, 0);
        producerConfigProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        producerConfigProps.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        producerConfigProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        producerConfigProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer);
        producerConfigProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer);
        return producerConfigProps;
    }

    def getConsumerConfig() {
        Map<String, Object> consumerConfigProps = new HashMap<>();
        consumerConfigProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaTestContainer.getBootstrapServers().split(',')[0]);
        consumerConfigProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer);
        consumerConfigProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer);
        consumerConfigProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, 'earliest');
        consumerConfigProps.put(ConsumerConfig.GROUP_ID_CONFIG, 'test');
        return consumerConfigProps;
    }
}

@Configuration
class TopicConfig {
    @Bean
    NewTopic newTopic() {
        return new NewTopic(NcmpAsyncRequestResponseEventProducerIntegrationSpec.CPS_ASYNC_EVENT_TOPIC_NAME, 1, (short) 1);
    }
}
