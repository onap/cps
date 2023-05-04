/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2023 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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

package org.onap.cps.ncmp.api.impl.events.avc;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.events.EventsPublisher;
import org.onap.cps.ncmp.events.avc.v1.AvcEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Listener for AVC events.
 */
@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true", matchIfMissing = true)
public class AvcEventConsumer {


    @Value("${app.ncmp.avc.cm-events-topic}")
    private String cmEventsTopicName;

    private final EventsPublisher<AvcEvent> eventsPublisher;
    private final AvcEventMapper avcEventMapper;


    /**
     * Consume the specified event.
     *
     * @param avcEvent the event to be consumed and produced.
     */
    @KafkaListener(topics = "${app.dmi.cm-events.topic}",
            properties = {"spring.json.value.default.type=org.onap.cps.ncmp.events.avc.v1.AvcEvent"})
    public void consumeAndForward(@Payload final AvcEvent avcEvent, @Headers final MessageHeaders messageHeaders) {
        log.debug("Consuming AVC event {} ...", avcEvent);
        final Map<String, Object> mutatedEventHeaders = mutateEventHeaderWithEventId(messageHeaders);
        final AvcEvent outgoingAvcEvent = avcEventMapper.toOutgoingAvcEvent(avcEvent);
        eventsPublisher.publishEvent(cmEventsTopicName, mutatedEventHeaders.get("eventId").toString(),
                mutatedEventHeaders, outgoingAvcEvent);
    }

    private Map<String, Object> mutateEventHeaderWithEventId(final MessageHeaders messageHeaders) {
        final Map<String, Object> eventHeaders = new LinkedHashMap<>(messageHeaders);
        eventHeaders.put("eventId", UUID.randomUUID().toString());
        return eventHeaders;
    }
}
