/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import org.springframework.kafka.transaction.KafkaTransactionManager;

/**
 * kafka Configuration for implementing Exactly Once Semantics using cloud events.
 */
@Configuration
@EnableKafka
@RequiredArgsConstructor
public class ExactlyOnceSemanticsKafkaConfig {

    private final KafkaProperties kafkaProperties;

    @Value("${cps.tracing.enabled:false}")
    private boolean tracingEnabled;

    @Value("${ncmp.notifications.avc-event-producer.transaction-id-prefix:tx-}")
    private String transactionIdPrefixForExactlyOnceSemantics;

    private static final SslBundles NO_SSL = null;


    /**
     * This sets the strategy for creating cloud Kafka producer instance from kafka properties defined into
     * application.yml with CloudEventSerializer.This factory is configured to support
     * exactly-once semantics by enabling idempotence and setting a transaction ID prefix.
     *
     * @return cloud event producer instance.
     */
    @Bean
    public ProducerFactory<String, CloudEvent> cloudEventProducerFactoryForEos() {
        final Map<String, Object> producerConfigProperties = kafkaProperties.buildProducerProperties(NO_SSL);
        producerConfigProperties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        producerConfigProperties.put(ProducerConfig.ACKS_CONFIG, "all");
        if (tracingEnabled) {
            producerConfigProperties.put(ProducerConfig.INTERCEPTOR_CLASSES_CONFIG,
                    TracingProducerInterceptor.class.getName());
        }
        final DefaultKafkaProducerFactory<String, CloudEvent> defaultKafkaProducerFactory =
                new DefaultKafkaProducerFactory<>(producerConfigProperties);
        defaultKafkaProducerFactory.setTransactionIdPrefix(transactionIdPrefixForExactlyOnceSemantics);
        return defaultKafkaProducerFactory;
    }

    /**
     * The ConsumerFactory implementation to produce new legacy instance for provided kafka properties defined
     * into application.yml having CloudEventDeserializer as deserializer-value.This factory is configured with
     * read_committed isolation level to support exactly-once semantics.
     *
     * @return an instance of cloud consumer factory.
     */
    @Bean
    public ConsumerFactory<String, CloudEvent> cloudEventConsumerFactoryForEos() {
        final Map<String, Object> consumerConfigProperties = kafkaProperties.buildConsumerProperties(NO_SSL);
        consumerConfigProperties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        if (tracingEnabled) {
            consumerConfigProperties.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG,
                    TracingConsumerInterceptor.class.getName());
        }
        return new DefaultKafkaConsumerFactory<>(consumerConfigProperties);
    }


    /**
     * A cloud Kafka event template for executing high-level operations. The template is configured using the cloud
     * event producer and consumer factories to support
     * exactly-once semantics.
     *
     * @return an instance of cloud Kafka template.
     */
    @Bean(name = "cloudEventKafkaTemplateForEos")
    public KafkaTemplate<String, CloudEvent> cloudEventKafkaTemplateForEos() {
        final KafkaTemplate<String, CloudEvent> kafkaTemplate = new KafkaTemplate<>(cloudEventProducerFactoryForEos());
        kafkaTemplate.setConsumerFactory(cloudEventConsumerFactoryForEos());
        if (tracingEnabled) {
            kafkaTemplate.setObservationEnabled(true);
        }
        return kafkaTemplate;
    }

    /**
     * A Concurrent CloudEvent kafka listener container factory.
     * This factory supports exactly-once semantics, retry handling, and optional tracing.
     *
     * @return instance of Concurrent kafka listener factory
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CloudEvent>
                            cloudEventConcurrentKafkaListenerContainerFactoryForEos() {
        final ConcurrentKafkaListenerContainerFactory<String, CloudEvent> containerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();
        containerFactory.setConsumerFactory(cloudEventConsumerFactoryForEos());
        containerFactory.getContainerProperties().setAuthExceptionRetryInterval(Duration.ofSeconds(10));
        containerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.RECORD);
        containerFactory.getContainerProperties().setKafkaAwareTransactionManager(kafkaTransactionManagerForEos());
        containerFactory.setCommonErrorHandler(kafkaErrorHandlerWithMaxRetriesForEos());
        if (tracingEnabled) {
            containerFactory.getContainerProperties().setObservationEnabled(true);
        }

        return containerFactory;
    }

    private KafkaTransactionManager<String, CloudEvent> kafkaTransactionManagerForEos() {
        return new KafkaTransactionManager<>(cloudEventProducerFactoryForEos());
    }

    private DefaultErrorHandler kafkaErrorHandlerWithMaxRetriesForEos() {

        final ExponentialBackOffWithMaxRetries exponentialBackOffWithMaxRetries =
                new ExponentialBackOffWithMaxRetries(Integer.MAX_VALUE);
        exponentialBackOffWithMaxRetries.setInitialInterval(1000L); // 1 sec
        exponentialBackOffWithMaxRetries.setMultiplier(2.0);
        exponentialBackOffWithMaxRetries.setMaxInterval(30_000L); // 30 sec
        final DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler(exponentialBackOffWithMaxRetries);
        defaultErrorHandler.addRetryableExceptions(KafkaException.class);

        return defaultErrorHandler;
    }
}
