/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.events;

import io.cloudevents.CloudEvent;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

/**
 * EventProducer to send events.
 */

@Slf4j
@Service
public class EventProducer {

    /**
     * KafkaTemplate for legacy (non-cloud) events.
     * Note: Cloud events should be used. This will be addressed as part of
     * <a href="https://lf-onap.atlassian.net/browse/CPS-1717">...</a>.
     */
    @Qualifier("legacyEventKafkaTemplate")
    private final KafkaTemplate<String, LegacyEvent> legacyEventKafkaTemplate;

    @Qualifier("cloudEventKafkaTemplate")
    private final KafkaTemplate<String, CloudEvent> cloudEventKafkaTemplate;

    private final AtomicLong batchEventLatencyInMs = new AtomicLong(0);

    private KafkaTemplate<String, CloudEvent> cloudEventKafkaTemplateForExactlyOnceSemantics;

    /**
     * Constructor for EventProducer.
     *
     * @param legacyEventKafkaTemplate     the Kafka template for legacy events
     * @param cloudEventKafkaTemplate      the Kafka template for cloud events
     * @param meterRegistry                the meter registry for metrics
     */
    public EventProducer(@Qualifier("legacyEventKafkaTemplate")
                         final KafkaTemplate<String, LegacyEvent> legacyEventKafkaTemplate,
                         @Qualifier("cloudEventKafkaTemplate")
                         final KafkaTemplate<String, CloudEvent> cloudEventKafkaTemplate,
                         final MeterRegistry meterRegistry) {
        this.legacyEventKafkaTemplate = legacyEventKafkaTemplate;
        this.cloudEventKafkaTemplate = cloudEventKafkaTemplate;
        registerGaugeForLatency(meterRegistry);
    }

    /**
     * Setter for optional ExactlyOnceSemantics Kafka template (used when ExactlyOnceSemantics is enabled).
     *
     * @param cloudEventKafkaTemplateForExactlyOnceSemantics the ExactlyOnceSemantics-enabled Kafka template
     */
    @Autowired(required = false)
    public void setCloudEventKafkaTemplateForExactlyOnceSemantics(
            @Qualifier("cloudEventKafkaTemplateForExactlyOnceSemantics")
            final KafkaTemplate<String, CloudEvent> cloudEventKafkaTemplateForExactlyOnceSemantics) {
        this.cloudEventKafkaTemplateForExactlyOnceSemantics = cloudEventKafkaTemplateForExactlyOnceSemantics;
    }

    /**
     * Generic CloudEvent sender.
     *
     * @param topicName valid topic name
     * @param eventKey  message key
     * @param event     message payload
     */
    public void sendCloudEvent(final String topicName, final String eventKey, final CloudEvent event) {
        final CompletableFuture<SendResult<String, CloudEvent>> eventFuture =
                cloudEventKafkaTemplate.send(topicName, eventKey, event);
        eventFuture.whenComplete((result, e) -> logOutcome(topicName, result, e));
    }

    /**
     * Legacy Event sender. Schemas that implement LegacyEvent are eligible to use this method.
     * Note: Cloud events should be used. This will be addressed as part of  <a
     * href="https://lf-onap.atlassian.net/browse/CPS-1717">...</a>.
     *
     * @param topicName valid topic name
     * @param eventKey  message key
     * @param event     message payload
     */
    public void sendLegacyEvent(final String topicName, final String eventKey, final LegacyEvent event) {
        final CompletableFuture<SendResult<String, LegacyEvent>> eventFuture =
                legacyEventKafkaTemplate.send(topicName, eventKey, event);
        handleLegacyEventCallback(topicName, eventFuture);
    }

    /**
     * Legacy Event sender with headers in a Map. Schemas that implement LegacyEvent are eligible to use this method.
     *
     * @param topicName    valid topic name
     * @param eventKey     message key
     * @param headersAsMap map of legacyEvent headers
     * @param legacyEvent  message payload
     */
    public void sendLegacyEvent(final String topicName,
                                final String eventKey,
                                final Map<String, Object> headersAsMap,
                                final LegacyEvent legacyEvent) {
        final Headers headers = convertToKafkaHeaders(headersAsMap);
        final ProducerRecord<String, LegacyEvent> producerRecord =
            new ProducerRecord<>(topicName, null, eventKey, legacyEvent, headers);
        final CompletableFuture<SendResult<String, LegacyEvent>> eventFuture =
            legacyEventKafkaTemplate.send(producerRecord);
        handleLegacyEventCallback(topicName, eventFuture);
    }

    private void handleLegacyEventCallback(final String topicName,
            final CompletableFuture<SendResult<String, LegacyEvent>> eventFuture) {
        eventFuture.whenComplete((result, e) -> logOutcome(topicName, result, e));
    }

    private Headers convertToKafkaHeaders(final Map<String, Object> headersAsMap) {
        final Headers headers = new RecordHeaders();
        headersAsMap.forEach((key, value) -> headers.add(key, SerializationUtils.serialize(value)));
        return headers;
    }

    private static void logOutcome(final String topicName, final SendResult<String, ?> result, final Throwable e) {
        if (e == null) {
            final Object event = result.getProducerRecord().value();
            log.debug("Successfully sent event to topic : {} , Event : {}", topicName, event);
        } else {
            log.error("Unable to send event to topic : {} due to {}", topicName, e.getMessage());
        }
    }

    /**
     * Send a batch of CloudEvents in parallel using the transactional Kafka template.
     * If any event fails to send, an exception is thrown to trigger batch retry.
     *
     * @param topicName valid topic name
     * @param events    list of event key-value pairs to send
     * @throws EventBatchSendException if any event fails to send
     * @throws IllegalStateException if ExactlyOnceSemantics Kafka template is not configured
     */
    public void sendCloudEventBatch(final String topicName, final List<Map.Entry<String, CloudEvent>> events) {
        if (cloudEventKafkaTemplateForExactlyOnceSemantics == null) {
            throw new IllegalStateException("ExactlyOnceSemantics Kafka template is not configured. "
                    + "Enable it by setting ncmp.kafka.eos.enabled=true");
        }

        if (events == null || events.isEmpty()) {
            log.debug("No events to send in batch");
            return;
        }

        log.debug("Sending batch of {} events to topic: {}", events.size(), topicName);

        final List<CompletableFuture<SendResult<String, CloudEvent>>> futures = events.stream()
                .map(entry -> {
                    recordEventLatency(entry.getValue());
                    return cloudEventKafkaTemplateForExactlyOnceSemantics.send(
                        topicName,
                        entry.getKey(),
                        entry.getValue());
                }).toList();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            log.debug("Successfully sent batch of {} events to topic: {}", events.size(), topicName);
        } catch (final Exception exception) {
            log.error("Batch send failed for topic: {} with error: {}", topicName, exception.getMessage());
            throw new EventBatchSendException(
                    "Failed to send batch of events",
                    String.format("Topic: %s, Batch size: %d", topicName, events.size()),
                    exception);
        }
    }

    private void registerGaugeForLatency(final MeterRegistry meterRegistry) {
        Gauge.builder("cps.cloud.event.batch.send.latency", batchEventLatencyInMs, AtomicLong::get)
                .description("Latest maximum end-to-end latency in ms between CloudEvent time and send time")
                .register(meterRegistry);
    }

    private void recordEventLatency(final CloudEvent cloudEvent) {
        final OffsetDateTime offsetDateTime = cloudEvent.getTime();
        if (offsetDateTime != null) {
            final long latencyInMs = Duration.between(offsetDateTime, OffsetDateTime.now()).toMillis();
            batchEventLatencyInMs.set(latencyInMs);
        }
    }

}
