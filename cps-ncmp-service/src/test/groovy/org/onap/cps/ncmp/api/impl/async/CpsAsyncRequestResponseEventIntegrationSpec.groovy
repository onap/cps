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
import org.onap.cps.ncmp.event.model.DmiAsyncRequestResponseEvent
import org.onap.cps.ncmp.event.model.NcmpAsyncRequestResponseEvent
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
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

    static kafkaTestContainer = new KafkaContainer(
        DockerImageName.parse('confluentinc/cp-kafka:6.2.1')
    )

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(kafkaTestContainer::stop))
    }

    def setupSpec() {
        kafkaTestContainer.start()
    }

    def producerConfigProperties = [
        (ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)      : kafkaTestContainer.getBootstrapServers().split(',')[0],
        (ProducerConfig.RETRIES_CONFIG)                : 0,
        (ProducerConfig.BATCH_SIZE_CONFIG)             : 16384,
        (ProducerConfig.LINGER_MS_CONFIG)              : 1,
        (ProducerConfig.BUFFER_MEMORY_CONFIG)          : 33554432,
        (ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG)   : StringSerializer,
        (ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG) : JsonSerializer
    ]

    def consumerConfigProperties = [
        (ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG)       : kafkaTestContainer.getBootstrapServers().split(',')[0],
        (ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG)  : StringDeserializer,
        (ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG): StringDeserializer,
        (ConsumerConfig.AUTO_OFFSET_RESET_CONFIG)       : 'earliest',
        (ConsumerConfig.GROUP_ID_CONFIG)                : 'test'
    ]

    def kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<Integer, String>(producerConfigProperties))

    @SpringBean
    NcmpAsyncRequestResponseEventProducer cpsAsyncRequestResponseEventProducerService =
        new NcmpAsyncRequestResponseEventProducer(kafkaTemplate);

    @SpringBean
    NcmpAsyncRequestResponseEventMapper ncmpAsyncRequestResponseEventMapper =
            Mappers.getMapper(NcmpAsyncRequestResponseEventMapper.class)

    @SpringBean
    NcmpAsyncRequestResponseEventConsumer ncmpAsyncRequestResponseEventConsumer =
            new NcmpAsyncRequestResponseEventConsumer(cpsAsyncRequestResponseEventProducerService,
                    ncmpAsyncRequestResponseEventMapper)

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    def kafkaConsumer = new KafkaConsumer<>(getConsumerConfigProperties())

    def 'Consume and forward valid message'() {
        given: 'an event is sent'
            def jsonData = TestUtils.getResourceFileContent('dmiAsyncRequestResponseEvent.json')
            def testEventSent = jsonObjectMapper.convertJsonString(jsonData, DmiAsyncRequestResponseEvent.class)
        and: 'consumer has a subscription'
            kafkaConsumer.subscribe(['test-topic'] as List<String>)
        when: 'the event is consumed'
            ncmpAsyncRequestResponseEventConsumer.consume(testEventSent)
        and: 'the topic is polled'
            def records = kafkaConsumer.poll(Duration.ofMillis(500))
        then: 'poll returns one record'
            assert records.size() == 1
        and: 'consumed forwarded event id is the same as sent event id'
            def record = records.iterator().next()
            assert testEventSent.eventId.equalsIgnoreCase(jsonObjectMapper.convertJsonString(record.value(),
                    NcmpAsyncRequestResponseEvent).getForwardedEvent().getEventId())
    }

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add('spring.kafka.bootstrap-servers', kafkaTestContainer::getBootstrapServers)
    }

}

@Configuration
class TopicConfig {
    @Bean
    NewTopic newTopic() {
        return new NewTopic('test-topic', 1, (short) 1);
    }
}
