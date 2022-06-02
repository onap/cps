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

package org.onap.cps.ncmp.api.utils

import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.kafka.support.serializer.JsonSerializer
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName
import spock.lang.Specification

class MessagingSpec extends Specification {

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(kafkaTestContainer::stop))
    }

    def setupSpec() {
        kafkaTestContainer.start()
    }

    static kafkaTestContainer = new KafkaContainer(DockerImageName.parse('confluentinc/cp-kafka:6.2.1'))

    def producerConfigProperties() {
        return [('bootstrap.servers'): kafkaTestContainer.getBootstrapServers().split(',')[0],
                ('retries')          : 0,
                ('batch-size')       : 16384,
                ('linger.ms')        : 1,
                ('buffer.memory')    : 33554432,
                ('key.serializer')   : StringSerializer,
                ('value.serializer') : JsonSerializer]
    }

    def consumerConfigProperties(consumerGroupId) {
        return [('bootstrap.servers') : kafkaTestContainer.getBootstrapServers().split(',')[0],
                ('key.deserializer')  : StringDeserializer,
                ('value.deserializer'): StringDeserializer,
                ('auto.offset.reset') : 'earliest',
                ('group.id')          : consumerGroupId
        ]
    }

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry dynamicPropertyRegistry) {
        dynamicPropertyRegistry.add('spring.kafka.bootstrap-servers', kafkaTestContainer::getBootstrapServers)
    }
}
