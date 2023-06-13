/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2023 Nordix Foundation.
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

package org.onap.cps.ncmp.api.impl.config.kafka;

import io.cloudevents.kafka.CloudEventDeserializer;
import io.cloudevents.kafka.CloudEventSerializer;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

@EnableKafka
@Configuration
public class CloudEventConfig<T> {

    @Value(value = "${spring.kafka.bootstrap-server-address}")
    private String bootstrapServerAddress;

    @Value("${spring.kafka.consumer.group-id}")
    private String consumerGroupId;

    /**
     * This sets the strategy for creating Kafka Producer instances.
     *
     * @return Cloud event producer instance and provides convenience methods for sending messages to Kafka topics.
     */
    @Bean
    public ProducerFactory<String, T> cloudEventProducerFactory() {
        final Map<String, Object> producerConfigProperties = new HashMap<>();
        producerConfigProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerAddress);
        producerConfigProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerConfigProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, CloudEventSerializer.class);
        return new DefaultKafkaProducerFactory<>(producerConfigProperties);
    }

    /**
     * The ConsumerFactory implementation to produce new Consumer instances for provided Map configs and
     * optional Deserializers on each ConsumerFactory.
     *
     * @return an instance of consumer factory.
     */
    @Bean
    public ConsumerFactory<String, T> cloudEventConsumerFactory() {
        final Map<String, Object> consumerConfigProperties = new HashMap<>();
        consumerConfigProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServerAddress);
        consumerConfigProperties.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
        consumerConfigProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerConfigProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, CloudEventDeserializer.class);
        consumerConfigProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        return new DefaultKafkaConsumerFactory<>(consumerConfigProperties);
    }

    /**
     * A template for executing high-level operations. When used with a DefaultKafkaProducerFactory. The producer
     * factory and KafkaProducer ensure this.
     *
     * @return an instance of the Kafka template.
     */
    @Bean
    public KafkaTemplate<String, T> kafkaCloudEventTemplate() {
        final KafkaTemplate<String, T> kafkaTemplate = new KafkaTemplate<>(cloudEventProducerFactory());
        kafkaTemplate.setConsumerFactory(cloudEventConsumerFactory());
        return kafkaTemplate;
    }

    /**
     * The KafkaMessageListenerContainer receives all message from all topics or partitions on a single thread.
     *
     * @return ConcurrentKafkaListenerContainerFactory that delegates to one or more KafkaMessageListenerContainer
     *     instances to provide multithreaded consumption.
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, T> kafkaListenerContainerFactory() {
        final ConcurrentKafkaListenerContainerFactory<String, T> kafkaListenerContainerFactory = new
                ConcurrentKafkaListenerContainerFactory<>();
        kafkaListenerContainerFactory.setConsumerFactory(cloudEventConsumerFactory());
        kafkaListenerContainerFactory.setConcurrency(3);
        kafkaListenerContainerFactory.getContainerProperties().setPollTimeout(3000);
        return kafkaListenerContainerFactory;
    }

}
