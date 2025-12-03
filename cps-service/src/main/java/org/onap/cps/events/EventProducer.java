/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

/**
 * EventProducer to send events.
 */

@Slf4j
@Service
@RequiredArgsConstructor
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

    @Qualifier("cloudEventKafkaTemplateForEos")
    private final KafkaTemplate<String, CloudEvent> cloudEventKafkaTemplateForEos;

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
        eventFuture.whenComplete((result, e) -> logOutcome(topicName, result, e, false));
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

    /**
     * Generic CloudEvent sender ensuring Exactly Once Semantics behaviour.
     *
     * @param topicName valid topic name
     * @param eventKey  message key
     * @param event     message payload
     */
    public void sendCloudEventUsingEos(final String topicName, final String eventKey, final CloudEvent event) {
        final CompletableFuture<SendResult<String, CloudEvent>> eventFuture =
                cloudEventKafkaTemplateForEos.send(topicName, eventKey, event);
        eventFuture.whenComplete((result, e) -> logOutcome(topicName, result, e, true));
    }

    private void handleLegacyEventCallback(final String topicName,
            final CompletableFuture<SendResult<String, LegacyEvent>> eventFuture) {
        eventFuture.whenComplete((result, e) -> logOutcome(topicName, result, e, false));
    }

    private Headers convertToKafkaHeaders(final Map<String, Object> headersAsMap) {
        final Headers headers = new RecordHeaders();
        headersAsMap.forEach((key, value) -> headers.add(key, SerializationUtils.serialize(value)));
        return headers;
    }

    private static void logOutcome(final String topicName, final SendResult<String, ?> result, final Throwable e,
            final boolean throwKafkaException) {
        if (e == null) {
            final Object event = result.getProducerRecord().value();
            log.debug("Successfully sent event to topic : {} , Event : {}", topicName, event);
        } else {
            log.error("Unable to send event to topic : {} due to {}", topicName, e.getMessage());
            if (throwKafkaException && e instanceof KafkaException) {
                throw (KafkaException) e;
            }
        }
    }

}
