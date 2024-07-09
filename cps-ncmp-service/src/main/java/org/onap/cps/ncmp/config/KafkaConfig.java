/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023-2024 Nordix Foundation
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

package org.onap.cps.ncmp.config;

import io.cloudevents.CloudEvent;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.TracingConsumerInterceptor;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.TracingProducerInterceptor;
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

/**
 * kafka Configuration for legacy and cloud events.
 *
 * @param <T> valid legacy event to be published over the wire.
 */
@Configuration
@EnableKafka
@RequiredArgsConstructor
public class KafkaConfig<T> {

    private final KafkaProperties kafkaProperties;

    @Value("${cps.tracing.enabled:false}")
    private boolean tracingEnabled;

    private static final SslBundles NO_SSL = null;

    /**
     * This sets the strategy for creating legacy Kafka producer instance from kafka properties defined into
     * application.yml and replaces value-serializer by JsonSerializer.
     *
     * @return legacy event producer instance.
     */
    @Bean
    public ProducerFactory<String, T> legacyEventProducerFactory() {
        final Map<String, Object> producerConfigProperties = kafkaProperties.buildProducerProperties(NO_SSL);
        producerConfigProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        if (tracingEnabled) {
            producerConfigProperties.put(
                ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingProducerInterceptor.class.getName());
        }
        return new DefaultKafkaProducerFactory<>(producerConfigProperties);
    }

    /**
     * The ConsumerFactory implementation is to produce new legacy instance for provided kafka properties defined
     * into application.yml and replaces deserializer-value by JsonDeserializer.
     *
     * @return an instance of legacy consumer factory.
     */
    @Bean
    public ConsumerFactory<String, T> legacyEventConsumerFactory() {
        final Map<String, Object> consumerConfigProperties = kafkaProperties.buildConsumerProperties(NO_SSL);
        consumerConfigProperties.put("spring.deserializer.value.delegate.class", JsonDeserializer.class);
        if (tracingEnabled) {
            consumerConfigProperties.put(
                ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingConsumerInterceptor.class.getName());
        }
        return new DefaultKafkaConsumerFactory<>(consumerConfigProperties);
    }

    /**
     * A legacy Kafka event template for executing high-level operations. The legacy producer factory ensure this.
     *
     * @return an instance of legacy Kafka template.
     */
    @Bean
    @Primary
    public KafkaTemplate<String, T> legacyEventKafkaTemplate() {
        final KafkaTemplate<String, T> kafkaTemplate = new KafkaTemplate<>(legacyEventProducerFactory());
        kafkaTemplate.setConsumerFactory(legacyEventConsumerFactory());
        if (tracingEnabled) {
            kafkaTemplate.setObservationEnabled(true);
        }
        return kafkaTemplate;
    }

    /**
     * A legacy concurrent kafka listener container factory.
     *
     * @return instance of Concurrent kafka listener factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, T> legacyEventConcurrentKafkaListenerContainerFactory() {
        final ConcurrentKafkaListenerContainerFactory<String, T> containerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();
        containerFactory.setConsumerFactory(legacyEventConsumerFactory());
        containerFactory.getContainerProperties().setAuthExceptionRetryInterval(Duration.ofSeconds(10));
        if (tracingEnabled) {
            containerFactory.getContainerProperties().setObservationEnabled(true);
        }
        return containerFactory;
    }

    /**
     * This sets the strategy for creating cloud Kafka producer instance from kafka properties defined into
     * application.yml with CloudEventSerializer.
     *
     * @return cloud event producer instance.
     */
    @Bean
    public ProducerFactory<String, CloudEvent> cloudEventProducerFactory() {
        final Map<String, Object> producerConfigProperties = kafkaProperties.buildProducerProperties(NO_SSL);
        if (tracingEnabled) {
            producerConfigProperties.put(
                ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingProducerInterceptor.class.getName());
        }
        return new DefaultKafkaProducerFactory<>(producerConfigProperties);
    }

    /**
     * The ConsumerFactory implementation to produce new legacy instance for provided kafka properties defined
     * into application.yml having CloudEventDeserializer as deserializer-value.
     *
     * @return an instance of cloud consumer factory.
     */
    @Bean
    public ConsumerFactory<String, CloudEvent> cloudEventConsumerFactory() {
        final Map<String, Object> consumerConfigProperties = kafkaProperties.buildConsumerProperties(NO_SSL);
        if (tracingEnabled) {
            consumerConfigProperties.put(
                ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingConsumerInterceptor.class.getName());
        }
        return new DefaultKafkaConsumerFactory<>(consumerConfigProperties);
    }


    /**
     * A cloud Kafka event template for executing high-level operations. The cloud producer factory ensure this.
     *
     * @return an instance of cloud Kafka template.
     */
    @Bean
    public KafkaTemplate<String, CloudEvent> cloudEventKafkaTemplate() {
        final KafkaTemplate<String, CloudEvent> kafkaTemplate =
            new KafkaTemplate<>(cloudEventProducerFactory());
        kafkaTemplate.setConsumerFactory(cloudEventConsumerFactory());
        if (tracingEnabled) {
            kafkaTemplate.setObservationEnabled(true);
        }
        return kafkaTemplate;
    }

    /**
     * A Concurrent CloudEvent kafka listener container factory.
     *
     * @return instance of Concurrent kafka listener factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CloudEvent>
                                        cloudEventConcurrentKafkaListenerContainerFactory() {
        final ConcurrentKafkaListenerContainerFactory<String, CloudEvent> containerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();
        containerFactory.setConsumerFactory(cloudEventConsumerFactory());
        containerFactory.getContainerProperties().setAuthExceptionRetryInterval(Duration.ofSeconds(10));
        if (tracingEnabled) {
            containerFactory.getContainerProperties().setObservationEnabled(true);
        }
        return containerFactory;
    }

}
