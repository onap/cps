/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.inventory.sync.lcm;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.events.EventProducer;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.events.lcm.v1.LcmEvent;
import org.onap.cps.ncmp.events.lcm.v1.Values;
import org.onap.cps.ncmp.impl.utils.YangDataConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Producer service for sending Lifecycle Management (LCM) events.
 * This service is responsible for creating and publishing LCM events to Kafka topics
 * when CM handle state transitions occur. It supports asynchronous batch processing
 * and includes metrics collection for monitoring event publishing performance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LcmEventProducer {

    private static final Tag METRIC_TAG_METHOD = Tag.of("method", "sendLcmEvent");
    private static final Tag METRIC_TAG_CLASS = Tag.of("class", LcmEventProducer.class.getName());
    private final EventProducer eventProducer;
    private final LcmEventObjectCreator lcmEventObjectCreator;
    private final MeterRegistry meterRegistry;

    @Value("${app.lcm.events.topic:ncmp-events}")
    private String topicName;

    @Value("${notification.enabled:true}")
    private boolean notificationsEnabled;

    /**
     * Sends LCM events in batches asynchronously for CM handle state transitions.
     * This method processes a collection of CM handle transition pairs and sends
     * corresponding LCM events to the configured Kafka topic. The processing is
     * performed asynchronously using the "notificationExecutor" thread pool.
     *
     * @param cmHandleTransitionPairs Collection of pairs containing current and target
     *                               CM handle states represented as YangModelCmHandle objects
     */
    @Async("notificationExecutor")
    public void sendLcmEventBatchAsynchronously(final Collection<CmHandleTransitionPair> cmHandleTransitionPairs) {
        cmHandleTransitionPairs.forEach(cmHandleTransitionPair -> sendLcmEvent(
            YangDataConverter.toNcmpServiceCmHandle(cmHandleTransitionPair.currentYangModelCmHandle()),
            YangDataConverter.toNcmpServiceCmHandle(cmHandleTransitionPair.targetYangModelCmHandle())
        ));
    }

    /**
     * Sends a single LCM event for a CM handle state transition.
     * Creates an LCM event using the provided current and target CM handle states,
     * publishes it to the configured Kafka topic, and records metrics for monitoring.
     * Event publishing is conditional based on the notifications enabled configuration.
     *
     * @param currentNcmpServiceCmHandle The current state of the CM handle
     * @param targetNcmpServiceCmHandle  The target state of the CM handle
     */
    private void sendLcmEvent(final NcmpServiceCmHandle currentNcmpServiceCmHandle,
                              final NcmpServiceCmHandle targetNcmpServiceCmHandle) {
        if (notificationsEnabled) {
            final LcmEvent lcmEvent = lcmEventObjectCreator.createLcmEvent(currentNcmpServiceCmHandle,
                                                                           targetNcmpServiceCmHandle);
            final Timer.Sample timerSample = Timer.start(meterRegistry);
            try {
                final Map<String, Object> headersAsMap = extractHeadersAsMap(lcmEvent);
                final String eventKey = currentNcmpServiceCmHandle.getCmHandleId();
                eventProducer.sendLegacyEvent(topicName, eventKey, headersAsMap, lcmEvent);
                recordMetrics(lcmEvent, timerSample);
            } catch (final KafkaException e) {
                log.error("Unable to send message to topic : {} and cause : {}", topicName, e.getMessage());
            }
        } else {
            log.debug("Notifications disabled.");
        }
    }

    private Map<String, Object> extractHeadersAsMap(final LcmEvent lcmEvent) {
        final Map<String, Object> headersAsMap = new HashMap<>(7);
        headersAsMap.put("eventId", lcmEvent.getEventId());
        headersAsMap.put("eventCorrelationId", lcmEvent.getEventCorrelationId());
        headersAsMap.put("eventTime", lcmEvent.getEventTime());
        headersAsMap.put("eventSource", lcmEvent.getEventSource());
        headersAsMap.put("eventType", lcmEvent.getEventType());
        headersAsMap.put("eventSchema", lcmEvent.getEventSchema());
        headersAsMap.put("eventSchemaVersion", lcmEvent.getEventSchemaVersion());
        return headersAsMap;
    }

    private void recordMetrics(final LcmEvent lcmEvent, final Timer.Sample timerSample) {
        final List<Tag> tags = new ArrayList<>(4);
        tags.add(METRIC_TAG_CLASS);
        tags.add(METRIC_TAG_METHOD);
        tags.add(createCmHandleStateTag("oldCmHandleState", lcmEvent.getEvent().getOldValues()));
        tags.add(createCmHandleStateTag("newCmHandleState", lcmEvent.getEvent().getNewValues()));
        timerSample.stop(Timer.builder("cps.ncmp.lcm.events.send")
            .description("Time taken to send a LCM event")
            .tags(tags)
            .register(meterRegistry));
    }

    private Tag createCmHandleStateTag(final String tageLabel, final Values values) {
        return Tag.of(tageLabel, values.getCmHandleState().value());
    }

}
