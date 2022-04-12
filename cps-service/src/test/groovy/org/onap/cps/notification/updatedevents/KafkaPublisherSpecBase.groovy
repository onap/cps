/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Bell Canada. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.notification.updatedevents

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.config.TopicBuilder
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaAdmin
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.listener.MessageListener
import org.springframework.kafka.test.utils.ContainerTestUtils
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import spock.lang.Shared
import spock.lang.Specification

@ContextConfiguration(classes = [KafkaAutoConfiguration, CpsUpdatedEventProducerListener, CpsUpdatedEventNotificationErrorHandler])
@SpringBootTest
class KafkaPublisherSpecBase extends Specification {

    @Autowired
    KafkaTemplate kafkaTemplate

    @Autowired
    KafkaAdmin kafkaAdmin

    @Autowired
    ConsumerFactory consumerFactory

    @Shared volatile topicCreated = false
    @Shared consumedMessages = new ArrayList<>()

    def cpsEventTopic = 'cps-events'

    @DynamicPropertySource
    static void registerKafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", KafkaTestContainerConfig::getBootstrapServers)
    }

    def setup() {
        // Kafka listener and topic should be created only once for a test-suite.
        // We are also dependent on sprint context to achieve it, and can not execute it in setupSpec
        if (!topicCreated) {
            kafkaAdmin.createOrModifyTopics(TopicBuilder.name(cpsEventTopic).partitions(1).replicas(1).build())
            startListeningToTopic()
            topicCreated = true
        }
        /* kafka message listener stores the messages to consumedMessages.
            It is important to clear the list before each test case so that test cases can fetch the message from index '0'.
         */
        consumedMessages.clear()
    }

    def startListeningToTopic() {
        ContainerProperties containerProperties = new ContainerProperties(cpsEventTopic)
        containerProperties.setMessageListener([
                onMessage: {
                    record ->
                        consumedMessages.add(record.value())
                }] as MessageListener)

        ConcurrentMessageListenerContainer container =
                new ConcurrentMessageListenerContainer<>(
                        consumerFactory,
                        containerProperties)

        container.start()
        ContainerTestUtils.waitForAssignment(container, 1)
    }

}
