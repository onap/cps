/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2022 Nordix Foundation.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
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

import org.onap.cps.event.model.DmiAsyncRequestResponseEvent
import org.onap.cps.event.model.NcmpAsyncRequestResponseEvent
import org.springframework.kafka.core.KafkaTemplate
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
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.spockframework.spring.SpringBean
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import spock.lang.Specification

@SpringBootTest(classes = [NcmpAsyncRequestResponseEventProducer, NcmpAsyncRequestResponseEventConsumer])
@Testcontainers
@DirtiesContext
class NcmpAsyncRequestResponseEventProducerSpec extends Specification {

    static kafkaTestContainer = new KafkaContainer(
        DockerImageName.parse('confluentinc/cp-kafka:6.1.1')
    ).withExposedPorts(49225, 49227, 9093);

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

    def 'Consume and forward valid message'() {
        given: 'an event is sent'
            def jsonData = TestUtils.getResourceFileContent('dmiAsyncRequestResponseEvent.json')
            def testEventSent = mapper.convertJsonString(jsonData, DmiAsyncRequestResponseEvent.class)
        and: 'consumer has a subscription'
            consumer.subscribe(['topic-for-test'] as List<String>)
        when: 'the event is consumed'
            ncmpAsyncRequestResponseEventConsumer.consume(testEventSent)
        and: 'the topic is polled'
            def records = consumer.poll(Duration.ofMillis(250))
        then: 'poll returns one record'
            assert records.size() == 1
        and: 'the record received is the event sent'
            def record = records.iterator().next()
            assert testEventSent == record.value() as NcmpAsyncRequestResponseEvent
    }

    def consumerConfig() {
        return [
            'key.deserializer' : StringDeserializer.name,
            'value.deserializer' : JsonDeserializer.name,
            'bootstrap.servers' : kafkaTestContainer.getBootstrapServers().split(',')[0],
            'group.id' : 'test']
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
        return new NewTopic('topic-for-test', 1, (short) 1);
    }
}
