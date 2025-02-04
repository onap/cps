/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 Nordix Foundation.
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
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.testcontainers.kafka.ConfluentKafkaContainer;

/**
 * The Apache Kafka test container wrapper.
 * Allow to use specific image and version with Singleton design pattern.
 * This ensures only one instance of Kafka container across the integration tests.
 * Avoid unnecessary resource and time consumption.
 */
@Slf4j
public class KafkaTestContainer extends ConfluentKafkaContainer {

    private static final String IMAGE_NAME_AND_VERSION = "confluentinc/cp-kafka:7.8.0";

    private static volatile KafkaTestContainer kafkaTestContainer;

    private KafkaTestContainer() {
        super(IMAGE_NAME_AND_VERSION);
    }

    /**
     * Provides an instance of Kafka test container wrapper.
     * This will allow to initialize Kafka messaging support before any integration test run.
     *
     * @return KafkaTestContainer the unique Kafka instance
     */
    public static KafkaTestContainer getInstance() {
        if (kafkaTestContainer == null) {
            synchronized (KafkaTestContainer.class) {
                if (kafkaTestContainer == null) {
                    kafkaTestContainer = new KafkaTestContainer();
                    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                        log.info("Shutting down KafkaTestContainer...");
                        kafkaTestContainer.stop();
                    }));
                }
            }
        }
        return kafkaTestContainer;
    }

    public static KafkaConsumer getConsumer(final String consumerGroupId, final Object valueDeserializer) {
        return new KafkaConsumer<>(consumerProperties(consumerGroupId, valueDeserializer));
    }

    @Override
    public void start() {
        if (!isRunning()) {
            super.start();
            System.setProperty("spring.kafka.properties.bootstrap.servers", getBootstrapServers());
            log.info("KafkaTestContainer started at {}", getBootstrapServers());
        }
    }

    @Override
    public void stop() {
        // Method intentionally left blank
    }

    private static Map<String, Object> consumerProperties(final String consumerGroupId,
                                                          final Object valueDeserializer) {
        final Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaTestContainer.getBootstrapServers());
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, Integer.MAX_VALUE);
        return configProps;
    }

}
