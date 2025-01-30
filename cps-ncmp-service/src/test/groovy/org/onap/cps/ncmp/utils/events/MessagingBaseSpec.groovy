/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2023-2025 Nordix Foundation.
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

package org.onap.cps.ncmp.utils.events

import io.cloudevents.CloudEvent
import io.cloudevents.kafka.CloudEventSerializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.kafka.ConfluentKafkaContainer
import spock.lang.Specification

class MessagingBaseSpec extends Specification {

    def setupSpec() {
        kafkaTestContainer.start()
    }

    def cleanupSpec() {
        kafkaTestContainer.stop()
    }

    static kafkaTestContainer = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.8.0")

    def legacyEventKafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<String, String>(eventProducerConfigProperties(JsonSerializer)))

    def cloudEventKafkaTemplate = new KafkaTemplate<>(new DefaultKafkaProducerFactory<String, CloudEvent>(eventProducerConfigProperties(CloudEventSerializer)))

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add('spring.kafka.bootstrap-servers', kafkaTestContainer::getBootstrapServers)
    }

    def eventProducerConfigProperties(valueSerializer) {
        return [('bootstrap.servers'): kafkaTestContainer.getBootstrapServers().split(',')[0],
                ('retries')          : 0,
                ('batch-size')       : 16384,
                ('linger.ms')        : 1,
                ('buffer.memory')    : 33554432,
                ('key.serializer')   : StringSerializer,
                ('value.serializer') : valueSerializer]
    }

    def eventConsumerConfigProperties(consumerGroupId, valueSerializer) {
        return [('bootstrap.servers') : kafkaTestContainer.getBootstrapServers().split(',')[0],
                ('key.deserializer')  : StringDeserializer,
                ('value.deserializer'): valueSerializer,
                ('auto.offset.reset') : 'earliest',
                ('group.id')          : consumerGroupId
        ]
    }
}
