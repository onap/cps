/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.events;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.SerializationUtils;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * EventsPublisher to publish events.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class EventsPublisher<T> {

    @Deprecated
    //TODO once all events will be cloud compliant this kafka template need to be removed and only
    // kafkaCloudEventTemplate will be used in future.
    private final KafkaTemplate<String, T> eventKafkaTemplate;
    private final KafkaTemplate<String, T> kafkaCloudEventTemplate;

    /**
     * Generic Event publisher.
     *
     * @param topicName valid topic name
     * @param eventKey  message key
     * @param event     message payload
     * @param isCloudEvent     cloud event flag
     * @deprecated This method is not needed anymore since the use of headers will be in place.
     */
    @Deprecated
    // TODO once all events will be cloud compliant will remove @Deprecated tag, and boolean flag isCloudEvent as well
    //  from this method and will use kafkaCloudEventTemplate only.
    public void publishEvent(final String topicName, final String eventKey, final T event, final boolean isCloudEvent) {
        final ListenableFuture<SendResult<String, T>> eventFuture = isCloudEvent
                ? kafkaCloudEventTemplate.send(topicName, eventKey, event) :
                eventKafkaTemplate.send(topicName, eventKey, event);
        eventFuture.addCallback(handleCallback(topicName));
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
        final ListenableFuture<SendResult<String, T>> eventFuture = eventKafkaTemplate.send(producerRecord);
        eventFuture.addCallback(handleCallback(topicName));
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

    private ListenableFutureCallback<SendResult<String, T>> handleCallback(final String topicName) {
        return new ListenableFutureCallback<>() {
            @Override
            public void onFailure(final Throwable throwable) {
                log.error("Unable to publish event to topic : {} due to {}", topicName, throwable.getMessage());
            }

            @Override
            public void onSuccess(final SendResult<String, T> sendResult) {
                log.debug("Successfully published event to topic : {} , Event : {}",
                        sendResult.getRecordMetadata().topic(), sendResult.getProducerRecord().value());
            }
        };
    }

    private Headers convertToKafkaHeaders(final Map<String, Object> eventMessageHeaders) {
        final Headers eventHeaders = new RecordHeaders();
        eventMessageHeaders.forEach((key, value) -> eventHeaders.add(key, SerializationUtils.serialize(value)));
        return eventHeaders;
    }

}
