/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
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
 * Kafka Configuration for Exactly Once Semantics using cloud events.
 * <p>
 * Note: When concurrency > 1, message ordering within a partition is NOT guaranteed.
 * Use concurrency = 1 if strict ordering is required.
 */
@Configuration
@EnableKafka
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ncmp.kafka.eos.enabled", havingValue = "true")
public class EosKafkaConfig {

    private final KafkaProperties kafkaProperties;

    @Value("${cps.tracing.enabled:false}")
    private boolean tracingEnabled;

    @Value("${ncmp.notifications.avc-event-producer.transaction-id-prefix:tx-}")
    private String transactionIdPrefix;

    @Value("${ncmp.notifications.avc-event-consumer.concurrency:1}")
    private int concurrency;

    @Value("${ncmp.notifications.avc-event-consumer.max-poll-records:500}")
    private String maxPollRecords;

    private static final UUID CPS_NCMP_INSTANCE_UUID = UUID.randomUUID();
    private static final SslBundles NO_SSL = null;

    /**
     * Producer factory configured for exactly-once semantics.
     *
     * @return producer factory instance
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
        defaultKafkaProducerFactory.setTransactionIdPrefix("cps-" + transactionIdPrefix + CPS_NCMP_INSTANCE_UUID + "-");
        defaultKafkaProducerFactory.setProducerPerThread(true);
        return defaultKafkaProducerFactory;
    }

    /**
     * Consumer factory with read_committed isolation level for exactly once semantics.
     *
     * @return consumer factory instance
     */
    @Bean
    public ConsumerFactory<String, CloudEvent> cloudEventConsumerFactoryForEos() {
        final Map<String, Object> consumerConfigProperties = kafkaProperties.buildConsumerProperties(NO_SSL);
        consumerConfigProperties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        consumerConfigProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        consumerConfigProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerConfigProperties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);
        if (tracingEnabled) {
            consumerConfigProperties.put(ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG,
                    TracingConsumerInterceptor.class.getName());
        }
        return new DefaultKafkaConsumerFactory<>(consumerConfigProperties);
    }

    /**
     * Kafka template for exactly-once semantics.
     *
     * @return kafka template instance
     */
    @Bean(name = "cloudEventKafkaTemplateForEos")
    public KafkaTemplate<String, CloudEvent> cloudEventKafkaTemplateForEos(
            @Qualifier("cloudEventProducerFactoryForEos") ProducerFactory<String, CloudEvent> producerFactory,
            @Qualifier("cloudEventConsumerFactoryForEos") ConsumerFactory<String, CloudEvent> consumerFactory) {
        final KafkaTemplate<String, CloudEvent> kafkaTemplate = new KafkaTemplate<>(producerFactory);
        kafkaTemplate.setConsumerFactory(consumerFactory);
        if (tracingEnabled) {
            kafkaTemplate.setObservationEnabled(true);
        }
        return kafkaTemplate;
    }

    /**
     * Listener container factory with BATCH acknowledgment mode and infinite retries.
     *
     * @return listener container factory instance
     */
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, CloudEvent>
            cloudEventConcurrentKafkaListenerContainerFactoryForEos(
                    @Qualifier("cloudEventConsumerFactoryForEos") ConsumerFactory<String, CloudEvent> consumerFactory,
                    @Qualifier("kafkaEosTransactionManager") KafkaTransactionManager<String, CloudEvent> transactionManager) {
        final ConcurrentKafkaListenerContainerFactory<String, CloudEvent> containerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();
        containerFactory.setConsumerFactory(consumerFactory);
        containerFactory.setConcurrency(concurrency);
        containerFactory.setBatchListener(true);
        containerFactory.getContainerProperties().setAuthExceptionRetryInterval(Duration.ofSeconds(10));
        containerFactory.getContainerProperties().setAckMode(ContainerProperties.AckMode.BATCH);
        containerFactory.getContainerProperties().setKafkaAwareTransactionManager(transactionManager);
        containerFactory.setCommonErrorHandler(kafkaErrorHandlerForEos());
        if (tracingEnabled) {
            containerFactory.getContainerProperties().setObservationEnabled(true);
        }
        return containerFactory;
    }

    @Bean(name = "kafkaEosTransactionManager")
    public KafkaTransactionManager<String, CloudEvent> kafkaTransactionManagerForEos(
            @Qualifier("cloudEventProducerFactoryForEos") ProducerFactory<String, CloudEvent> producerFactory) {
        return new KafkaTransactionManager<>(producerFactory);
    }

    private DefaultErrorHandler kafkaErrorHandlerForEos() {
        final ExponentialBackOffWithMaxRetries exponentialBackOffWithMaxRetries =
                new ExponentialBackOffWithMaxRetries(Integer.MAX_VALUE);
        exponentialBackOffWithMaxRetries.setInitialInterval(1000L);
        exponentialBackOffWithMaxRetries.setMultiplier(2.0);
        exponentialBackOffWithMaxRetries.setMaxInterval(30_000L);
        final DefaultErrorHandler defaultErrorHandler = new DefaultErrorHandler(exponentialBackOffWithMaxRetries);
        defaultErrorHandler.addRetryableExceptions(KafkaException.class);
        return defaultErrorHandler;
    }
}
