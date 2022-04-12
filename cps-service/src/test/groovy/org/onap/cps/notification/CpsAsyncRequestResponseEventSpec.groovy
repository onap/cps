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

import org.onap.cps.event.model.DmiAsyncRequestResponseEvent
import org.onap.cps.event.model.NcmpAsyncRequestResponseEvent
import org.onap.cps.notification.async.NcmpAsyncRequestResponseEventConsumer
import org.onap.cps.notification.async.NcmpAsyncRequestResponseEventProducer
import org.onap.cps.notification.async.NcmpAsyncRequestResponseEventUtil
import org.springframework.kafka.core.KafkaTemplate

import java.time.Duration
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.utils.JsonObjectMapper
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.onap.cps.TestUtils
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

@SpringBootTest(classes = [NcmpAsyncRequestResponseEventProducer, NcmpAsyncRequestResponseEventConsumer])
@Testcontainers
@DirtiesContext
class NcmpAsyncRequestResponseEventProducerSpec extends Specification {

    static kafkaTestContainer = new KafkaContainer()

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(kafkaTestContainer::stop))
    }

    def setupSpec() {
        kafkaTestContainer.start()
    }

    KafkaTemplate<String, NcmpAsyncRequestResponseEvent> kafkaTemplate = Mock()

    @SpringBean
    NcmpAsyncRequestResponseEventProducer cpsAsyncRequestResponseEventProducerService =
        new NcmpAsyncRequestResponseEventProducer(kafkaTemplate);

    @SpringBean
    NcmpAsyncRequestResponseEventConsumer ncmpAsyncRequestResponseEventConsumer =
        new NcmpAsyncRequestResponseEventConsumer(cpsAsyncRequestResponseEventProducerService)

    @SpringBean
    NcmpAsyncRequestResponseEventUtil cpsAsyncRequestResponseEventUtil = Mock()

    @SpringBean
    ObjectMapper objectMapper = Mock()

    JsonObjectMapper mapper = new JsonObjectMapper(new ObjectMapper())

    KafkaConsumer<String, Object> consumer = new KafkaConsumer<>(consumerConfig())

    def 'Consume and Publish valid message'() {
        given: 'a valid event'
            def jsonData = TestUtils.getResourceFileContent('dmiAsyncRequestResponseEvent.json')
            def event = mapper.convertJsonString(jsonData, DmiAsyncRequestResponseEvent.class)
        when: 'the event is consumed'
            ncmpAsyncRequestResponseEventConsumer.consume(event)
        then: 'an event is placed on topic with correct information'
            consumer.subscribe(["my-topic-99"] as List<String>)
            def records = consumer.poll(Duration.ofMillis(250))
            for (record in records) {
                assert event == record.value() as NcmpAsyncRequestResponseEvent
            }
    }

    def consumerConfig() {
        return [
            'key.deserializer' : StringDeserializer.name,
            'value.deserializer' : JsonDeserializer.name,
            'bootstrap.servers' : kafkaTestContainer.getBootstrapServers().split(",")[0],
            'group.id' : "test"]
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
