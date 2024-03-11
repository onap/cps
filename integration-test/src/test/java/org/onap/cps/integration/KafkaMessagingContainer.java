/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
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

package org.onap.cps.integration;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

public class KafkaMessagingContainer extends KafkaContainer {

    private static final String IMAGE_NAME = "registry.nordix.org/onaptest/confluentinc/cp-kafka:6.2.1";

    private static KafkaMessagingContainer kafkaMessagingContainer;

    private KafkaMessagingContainer() {
        super(DockerImageName.parse(IMAGE_NAME).asCompatibleSubstituteFor("confluentinc/cp-kafka"));
    }

    /**
     * Kafka messaging test container wrapper.
     * Singleton.
     *
     * @return kafka messaging container
     */
    public static KafkaMessagingContainer getInstance() {
        if (kafkaMessagingContainer == null) {
            kafkaMessagingContainer = new KafkaMessagingContainer();
            Runtime.getRuntime().addShutdownHook(new Thread(kafkaMessagingContainer::close));
        }
        return kafkaMessagingContainer;
    }

    @Override
    public void start() {
        super.start();
        System.setProperty("spring.kafka.bootstrap-servers", kafkaMessagingContainer.getBootstrapServers());
    }

    @Override
    public void stop() {
        // never stop otherwise the data in topics will be lost
    }


    private static Map<String, Object> consumerProperties() {
        final Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaMessagingContainer.getBootstrapServers());
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "ncmp-group");
        return configProps;
    }

    public static KafkaConsumer getConsumer() {
        return new KafkaConsumer<>(consumerProperties());
    }

}
