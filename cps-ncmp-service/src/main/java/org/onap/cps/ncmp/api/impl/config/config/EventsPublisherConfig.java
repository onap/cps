/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.config.config;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * Generic Events Publisher Config.
 *
 * @param <T> Valid Event to be published over the wire.
 */
@Configuration
@RequiredArgsConstructor
public class EventsPublisherConfig<T> {

    @Value("${spring.kafka.bootstrap-address}")
    private String bootstrapAddress;

    /**
     * Kafka Producer config.
     *
     * @return kafka producer factory
     */
    @Bean
    public ProducerFactory<String, T> producerFactory() {
        final Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configProps);
    }


    /**
     * Consumer factory.
     *
     * @return Consumer factory.
     */
    @Bean
    public ConsumerFactory<String, T> consumerFactory() {
        final Map<String, Object> configProps = new HashMap<>();
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProps.put("spring.deserializer.key.delegate.class", StringDeserializer.class);
        configProps.put("spring.deserializer.value.delegate.class", JsonDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(configProps);
    }


    /**
     * KafkaTemplate to support non cloudevents.
     *
     * @return KafkaTemplate
     */
    @Bean(name = "eventKafkaTemplate")
    public KafkaTemplate<String, T> kafkaTemplate() {
        final KafkaTemplate<String, T> kafkaTemplate = new KafkaTemplate<>(producerFactory());
        kafkaTemplate.setConsumerFactory(consumerFactory());
        return kafkaTemplate;
    }
}
