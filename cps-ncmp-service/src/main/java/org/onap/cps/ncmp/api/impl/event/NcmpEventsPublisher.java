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

package org.onap.cps.ncmp.api.impl.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.ncmp.cmhandle.lcm.event.NcmpEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

/**
 * NcmpEventsPublisher to publish the NcmpEvents on event of CREATE, UPDATE and DELETE.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class NcmpEventsPublisher {

    private final KafkaTemplate<String, NcmpEvent> ncmpEventKafkaTemplate;

    @Value("${app.ncmp.events.topic:ncmp-events}")
    private final String topicName;

    /**
     * Event publisher.
     *
     * @param eventKey  message key
     * @param eventData message payload
     */
    public void publishEvent(final String eventKey, final NcmpEvent eventData) {
        final ListenableFuture<SendResult<String, NcmpEvent>> send =
                ncmpEventKafkaTemplate.send(topicName, eventKey, eventData);

        send.addCallback(new ListenableFutureCallback<>() {
            @Override
            public void onFailure(final Throwable ex) {
                log.error("Unable to publish event to topic : {} due to {}", topicName, ex.getMessage());
            }

            @Override
            public void onSuccess(final SendResult<String, NcmpEvent> result) {
                log.info("Successfully published event to topic : {} , NcmpEvent : {}",
                        result.getRecordMetadata().topic(), result.getProducerRecord().value());
            }
        });
    }
}
