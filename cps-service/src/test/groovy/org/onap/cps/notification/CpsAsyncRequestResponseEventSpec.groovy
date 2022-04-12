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
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.notification

import org.onap.cps.event.model.CpsAsyncRequestResponseEventWithOrigin
import org.springframework.kafka.core.KafkaTemplate

import java.time.Duration
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.utils.JsonObjectMapper
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.onap.cps.TestUtils
import org.onap.cps.event.model.CpsAsyncRequestResponseEvent
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.spockframework.spring.SpringBean
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.spock.Testcontainers
import spock.lang.Specification

import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG
import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG

@SpringBootTest(classes = [CpsAsyncRequestResponseEventProducer, CpsAsyncRequestResponseEventConsumer])
@Testcontainers
@DirtiesContext
class CpsAsyncRequestResponseEventProducerSpec extends Specification {

    static kafkaTestContainer = new KafkaContainer()

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(kafkaTestContainer::stop))
    }

    def setupSpec() {
        kafkaTestContainer.start()
    }

    KafkaTemplate<String, CpsAsyncRequestResponseEventWithOrigin> kafkaTemplate = Mock()

    @SpringBean
    CpsAsyncRequestResponseEventProducer cpsAsyncRequestResponseEventProducerService =
        new CpsAsyncRequestResponseEventProducer(kafkaTemplate);

    @SpringBean
    CpsAsyncRequestResponseEventConsumer cpsAsyncRequestResponseEventConsumer =
        new CpsAsyncRequestResponseEventConsumer(cpsAsyncRequestResponseEventProducerService)

    @SpringBean
    CpsAsyncRequestResponseEventUtil cpsAsyncRequestResponseEventUtil = Spy()

    @SpringBean
    ObjectMapper objectMapper = Spy()

    JsonObjectMapper mapper = new JsonObjectMapper(new ObjectMapper())

    KafkaConsumer<String, Object> consumer = new KafkaConsumer<>(consumerConfig())

    def 'Consume and Publish valid message'() {
        given: 'a valid event'
            def jsonData = TestUtils.getResourceFileContent('cpsAsyncRequestResponseEvent.json')
            def event = mapper.convertJsonString(jsonData, CpsAsyncRequestResponseEvent.class)
        when: 'the event is consumed'
            cpsAsyncRequestResponseEventConsumer.consume(event)
        then: 'no exception is thrown'
            noExceptionThrown()
        and: 'an event is placed on topic with correct information'
            consumer.subscribe(["my-topic-99"] as List<String>)
            def records = consumer.poll(Duration.ofMillis(250))
            for (record in records) {
                assert event == record.value() as CpsAsyncRequestResponseEventWithOrigin
            }
    }

    def consumerConfig() {
        def configs = [:]
        configs.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.name)
        configs.put(VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.name)
        configs.put(AUTO_OFFSET_RESET_CONFIG, "earliest")
        configs.put(BOOTSTRAP_SERVERS_CONFIG, kafkaTestContainer.getBootstrapServers().split(",")[0])
        configs.put(GROUP_ID_CONFIG, "test")
        return configs
    }

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add('spring.kafka.bootstrap-servers', kafkaTestContainer::getBootstrapServers)
    }
}

@Configuration
class TopicConfig {
    @Bean
    NewTopic newTopic() {
        return new NewTopic("my-topic-99", 1, (short) 1);
    }
}
