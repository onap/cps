/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.notification;

import lombok.extern.slf4j.Slf4j;
import org.onap.cps.event.model.CpsAsyncRequestResponseEventWithOrigin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

@Component
@Slf4j
public class CpsAsyncRequestResponseEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * KafkaTemplate and Topic name.
     *
     * @param kafkaTemplate kafka template
     */
    @Autowired
    public CpsAsyncRequestResponseEventProducer(final KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    /**
     * Sends message to the configured topic with a message key.
     *
     * @param messageKey message key
     * @param payload    message payload
     */
    public void sendMessage(final String messageKey, final CpsAsyncRequestResponseEventWithOrigin payload) {
        final ListenableFuture<SendResult<String, Object>> resultListenableFuture =
            kafkaTemplate.send(payload.getEventTarget(), messageKey, payload);

        resultListenableFuture.addCallback(new ListenableFutureCallback<>() {
            @Override
            public void onFailure(final Throwable ex) {
                log.warn("Failed to resultListenableFuture the messages {}", ex.getMessage()
                );
            }

            @Override
            public void onSuccess(final SendResult<String, Object> result) {
                log.debug("Sent message {}", result.getProducerRecord());
            }
        });
    }
}
