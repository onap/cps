/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2024 Nordix Foundation
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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;

/**
 * EventsPublisher to publish events.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class EventsPublisher<T> implements DisposableBean {

    /**
     * KafkaTemplate for legacy (non-cloud) events.
     * Note: Cloud events should be used. This will be addressed as part of  https://jira.onap.org/browse/CPS-1717.
     */
    private final KafkaTemplate<String, T> legacyKafkaEventTemplate;

    private final KafkaTemplate<String, CloudEvent> cloudEventKafkaTemplate;

    @Override
    public void destroy() {
        log.info("{}::destroy", this.getClass().getCanonicalName());
        if (this.legacyKafkaEventTemplate != null) {
            this.legacyKafkaEventTemplate.destroy();
        }
        if (this.cloudEventKafkaTemplate != null) {
            this.cloudEventKafkaTemplate.destroy();
        }
    }

    /**
     * Generic CloudEvent publisher.
     *
     * @param topicName valid topic name
     * @param eventKey  message key
     * @param event     message payload
     */
    public void publishCloudEvent(final String topicName, final String eventKey, final CloudEvent event) {
        final CompletableFuture<SendResult<String, CloudEvent>> eventFuture =
                cloudEventKafkaTemplate.send(topicName, eventKey, event);
        eventFuture.whenComplete((result, e) -> {
            if (e == null) {
                log.debug("Successfully published event to topic : {} , Event : {}", result.getRecordMetadata().topic(),
                        result.getProducerRecord().value());

            } else {
                log.error("Unable to publish event to topic : {} due to {}", topicName, e.getMessage());
            }
        });
    }

    /**
     * Generic Event publisher.
     * Note: Cloud events should be used. This will be addressed as part of  https://jira.onap.org/browse/CPS-1717.
     *
     * @param topicName valid topic name
     * @param eventKey  message key
     * @param event     message payload
     */
    public void publishEvent(final String topicName, final String eventKey, final T event) {
        final CompletableFuture<SendResult<String, T>> eventFuture =
                legacyKafkaEventTemplate.send(topicName, eventKey, event);
        handleLegacyEventCallback(topicName, eventFuture);
    }

    /**
     * Generic Event Publisher with headers.
     *
     * @param topicName    valid topic name
     * @param eventKey     message key
     * @param eventHeaders event headers
     * @param event        message payload
     */
    public void publishEvent(final String topicName, final String eventKey, final Headers eventHeaders, final T event) {

        final ProducerRecord<String, T> producerRecord =
                new ProducerRecord<>(topicName, null, eventKey, event, eventHeaders);
        final CompletableFuture<SendResult<String, T>> eventFuture = legacyKafkaEventTemplate.send(producerRecord);
        handleLegacyEventCallback(topicName, eventFuture);
    }

    /**
     * Generic Event Publisher with headers.
     *
     * @param topicName    valid topic name
     * @param eventKey     message key
     * @param eventHeaders map of event headers
     * @param event        message payload
     */
    public void publishEvent(final String topicName, final String eventKey, final Map<String, Object> eventHeaders,
            final T event) {

        publishEvent(topicName, eventKey, convertToKafkaHeaders(eventHeaders), event);
    }

    private void handleLegacyEventCallback(final String topicName,
            final CompletableFuture<SendResult<String, T>> eventFuture) {
        eventFuture.whenComplete((result, e) -> {
            if (e == null) {
                log.debug("Successfully published event to topic : {} , Event : {}", result.getRecordMetadata().topic(),
                        result.getProducerRecord().value());
            } else {
                log.error("Unable to publish event to topic : {} due to {}", topicName, e.getMessage());
            }
        });
    }

    private Headers convertToKafkaHeaders(final Map<String, Object> eventMessageHeaders) {
        final Headers eventHeaders = new RecordHeaders();
        eventMessageHeaders.forEach((key, value) -> eventHeaders.add(key, SerializationUtils.serialize(value)));
        return eventHeaders;
    }

}
