/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023-2026 OpenInfra Foundation Europe. All rights reserved.
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
import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.onap.cps.events.LegacyEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
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
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;

/**
 * kafka Configuration for legacy and cloud events.
 */
@Configuration
@EnableKafka
@RequiredArgsConstructor
@Slf4j
public class KafkaConfig {

    private final KafkaProperties kafkaProperties;

    @Value("${cps.tracing.enabled:false}")
    private boolean tracingEnabled;

    /**
     * This sets the strategy for creating legacy Kafka producer instance from kafka properties defined into
     * application.yml and replaces value-serializer by JsonSerializer.
     *
     * @return legacy event producer instance.
     */
    @Bean
    public ProducerFactory<String, LegacyEvent> legacyEventProducerFactory() {
        final Map<String, Object> producerConfigProperties = kafkaProperties.buildProducerProperties();
        producerConfigProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(producerConfigProperties);
    }

    /**
     * The ConsumerFactory implementation is to produce new legacy instance for provided kafka properties defined
     * into application.yml and replaces deserializer-value by JsonDeserializer.
     *
     * @return an instance of legacy consumer factory.
     */
    @Bean
    public ConsumerFactory<String, LegacyEvent> legacyEventConsumerFactory() {
        final Map<String, Object> consumerConfigProperties = kafkaProperties.buildConsumerProperties();
        consumerConfigProperties.put("spring.deserializer.value.delegate.class", JacksonJsonDeserializer.class);
        return new DefaultKafkaConsumerFactory<>(consumerConfigProperties);
    }

    /**
     * A legacy Kafka event template for executing high-level operations. The legacy producer factory ensure this.
     *
     * @return an instance of legacy Kafka template.
     */
    @Bean
    @Primary
    public KafkaTemplate<String, LegacyEvent> legacyEventKafkaTemplate() {
        final KafkaTemplate<String, LegacyEvent> kafkaTemplate = new KafkaTemplate<>(legacyEventProducerFactory());
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
    public ConcurrentKafkaListenerContainerFactory<String, LegacyEvent>
                            legacyEventConcurrentKafkaListenerContainerFactory() {
        final ConcurrentKafkaListenerContainerFactory<String, LegacyEvent> containerFactory =
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
        final Map<String, Object> producerConfigProperties = kafkaProperties.buildProducerProperties();
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
        final Map<String, Object> consumerConfigProperties = kafkaProperties.buildConsumerProperties();
        return new DefaultKafkaConsumerFactory<>(consumerConfigProperties);
    }


    /**
     * A cloud Kafka event template for executing high-level operations. The cloud producer factory ensure this.
     *
     * @return an instance of cloud Kafka template.
     */
    @Bean
    public KafkaTemplate<String, CloudEvent> cloudEventKafkaTemplate() {
        final KafkaTemplate<String, CloudEvent> kafkaTemplate = new KafkaTemplate<>(cloudEventProducerFactory());
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

    /**
     * Default AVC event listener container factory for single-record mode.
     * Configured as a batch listener with max-poll-records=1, no transactions.
     * Only created when ExactlyOnceSemanticsKafkaConfig is disabled ie. ncmp.kafka.eos.enabled = false.
     *
     * @return batch listener container factory for single-record processing
     */
    @Bean
    @ConditionalOnMissingBean(name = "cmAvcEventListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, CloudEvent>
            cmAvcEventListenerContainerFactory() {
        log.info("Configuring CM AVC event listener in non-transactional mode");
        final Map<String, Object> consumerConfigProperties = kafkaProperties.buildConsumerProperties();
        final ConcurrentKafkaListenerContainerFactory<String, CloudEvent> containerFactory =
                new ConcurrentKafkaListenerContainerFactory<>();
        containerFactory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(consumerConfigProperties));
        containerFactory.setBatchListener(true);
        containerFactory.getContainerProperties().setAuthExceptionRetryInterval(Duration.ofSeconds(10));
        if (tracingEnabled) {
            containerFactory.getContainerProperties().setObservationEnabled(true);
        }
        return containerFactory;
    }

}
