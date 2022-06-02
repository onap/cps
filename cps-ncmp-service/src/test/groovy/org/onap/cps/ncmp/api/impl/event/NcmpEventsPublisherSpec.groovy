/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.event

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.onap.ncmp.cmhandle.lcm.event.Event
import org.onap.ncmp.cmhandle.lcm.event.NcmpEvent
import org.spockframework.spring.SpringBean
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.spock.Testcontainers
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

import java.time.Duration

@SpringBootTest(classes = [NcmpEventsPublisher])
@Testcontainers
@DirtiesContext
class NcmpEventsPublisherSpec extends Specification {

    static kafkaTestContainer = new KafkaContainer(DockerImageName.parse('confluentinc/cp-kafka:6.2.1'))

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(kafkaTestContainer::stop))
    }

    def setupSpec() {
        kafkaTestContainer.start()
    }

    def producerConfigProperties = [("bootstrap.servers"): kafkaTestContainer.getBootstrapServers().split(',')[0],
                                    ("key.serializer")   : StringSerializer,
                                    ("value.serializer") : JsonSerializer]

    def consumerConfigProperties = [("bootstrap.servers") : kafkaTestContainer.getBootstrapServers().split(',')[0],
                                    ("key.deserializer")  : StringDeserializer,
                                    ("value.deserializer"): StringDeserializer,
                                    ("auto.offset.reset") : 'earliest',
                                    ("group.id")          : 'test']

    def kafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<Integer, String>(producerConfigProperties))

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    def kafkaConsumer = new KafkaConsumer<>(getConsumerConfigProperties())

    def testTopic = 'ncmp-events-test'

    @SpringBean
    NcmpEventsPublisher ncmpEventsPublisher = new NcmpEventsPublisher(kafkaTemplate, testTopic)


    def 'Produce and Consume Ncmp Event'() {
        given: 'event key and data'
            def messageKey = 'ncmp'
            def eventData = new NcmpEvent(eventId: 'test-uuid',
                eventCorrelationId: 'cmhandle-test',
                eventSchema: URI.create('org.onap.ncmp.cmhandle.lcm.event:v1'),
                eventSource: URI.create('org.onap.ncmp'),
                eventTime: '2022-12-31T20:30:40.000+0000',
                eventType: 'org.onap.ncmp.cmhandle.lcm.event',
                event: new Event(cmHandleId: 'cmhandle-test', cmhandleState: 'READY', operation: 'CREATE', cmhandleProperties: [['publicprop1': 'value1'], ['publicprop2': 'value2']]))
        and: 'we have an expected NcmpEvent'
            def expectedJsonString = TestUtils.getResourceFileContent('expectedNcmpEvent.json')
            def expectedNcmpEvent = jsonObjectMapper.convertJsonString(expectedJsonString, NcmpEvent.class)
        and: 'consumer has a subscription'
            kafkaConsumer.subscribe([testTopic] as List<String>)
        when: 'an event is published'
            ncmpEventsPublisher.publishEvent(messageKey, eventData)
        and: 'topic is polled'
            def records = kafkaConsumer.poll(Duration.ofMillis(1500))
        then: 'no exception is thrown'
            noExceptionThrown()
        and: 'poll returns one record'
            assert records.size() == 1
        and: 'record matches the expected event'
            def record = records.iterator().next()
            assert messageKey == record.key
            assert expectedNcmpEvent == jsonObjectMapper.convertJsonString(record.value, NcmpEvent.class)

    }

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add('spring.kafka.bootstrap-servers', kafkaTestContainer::getBootstrapServers)
    }
}
