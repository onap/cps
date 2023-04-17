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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * EventsPublisher to publish events.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class EventsPublisher<T> {

    private final KafkaTemplate<String, T> eventKafkaTemplate;

    /**
     * LCM Event publisher.
     *
     * @param topicName valid topic name
     * @param eventKey  message key
     * @param event message payload
     */
    public void publishEvent(final String topicName, final String eventKey, final T event) {
        final ListenableFuture<SendResult<String, T>> eventFuture =
                eventKafkaTemplate.send(topicName, eventKey, event);

        eventFuture.addCallback(new ListenableFutureCallback<>() {
            @Override
            public void onFailure(final Throwable throwable) {
                log.error("Unable to publish event to topic : {} due to {}", topicName, throwable.getMessage());
            }

            @Override
            public void onSuccess(final SendResult<String, T> sendResult) {
                log.debug("Successfully published event to topic : {} , Event : {}",
                        sendResult.getRecordMetadata().topic(), sendResult.getProducerRecord().value());
            }
        });
    }
}
