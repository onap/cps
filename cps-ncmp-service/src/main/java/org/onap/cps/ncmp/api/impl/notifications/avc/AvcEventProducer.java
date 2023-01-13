/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.notifications.avc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.event.model.AvcEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Producer for AVC events.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvcEventProducer {

    private final KafkaTemplate<String, AvcEvent> kafkaTemplate;

    private final AvcEventMapper avcEventMapper;

    private AvcEvent outgoingAvcEvent;

    /**
     * Sends message to the configured topic with a message key.
     *
     * @param incomingAvcEvent message payload
     */
    public void sendMessage(final AvcEvent incomingAvcEvent) {
        // generate new event id while keeping other data
        outgoingAvcEvent = avcEventMapper.toOutgoingAvcEvent(incomingAvcEvent);
        log.debug("Forwarding AVC event {} to topic {} ", outgoingAvcEvent.getEventId(), "cm-events");
        kafkaTemplate.send("cm-events", outgoingAvcEvent.getEventId(), outgoingAvcEvent);
    }
}
