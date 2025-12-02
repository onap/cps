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

package org.onap.cps.ncmp.impl.inventory.sync.lcm;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.events.EventsProducer;
import org.onap.cps.ncmp.events.lcm.v1.LcmEvent;
import org.onap.cps.ncmp.events.lcm.v1.LcmEventHeader;
import org.onap.cps.ncmp.events.lcm.v1.Values;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.stereotype.Service;

/**
 * LcmEventsProducer to call the producer and send on the dedicated topic.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class LcmEventsProducer {

    private static final Tag TAG_METHOD = Tag.of("method", "sendLcmEvent");
    private static final Tag TAG_CLASS = Tag.of("class", LcmEventsProducer.class.getName());
    private static final String UNAVAILABLE_CM_HANDLE_STATE = "N/A";
    private final EventsProducer eventsProducer;
    private final JsonObjectMapper jsonObjectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${app.lcm.events.topic:ncmp-events}")
    private String topicName;

    @Value("${notification.enabled:true}")
    private boolean notificationsEnabled;

    /**
     * Sends an LCM event to the dedicated topic with optional notification headers.
     * Capture and log KafkaException If an error occurs while sending the event to Kafka
     *
     * @param cmHandleId     CM handle id associated with the LCM event
     * @param lcmEvent       The LCM event object to be sent
     * @param lcmEventHeader Optional headers associated with the LCM event
     */
    public void sendLcmEvent(final String cmHandleId, final LcmEvent lcmEvent, final LcmEventHeader lcmEventHeader) {
        if (notificationsEnabled) {
            lcmEventHeader.setEventId(lcmEvent.getEventId());
            lcmEventHeader.setEventTime(lcmEvent.getEventTime());
            final Timer.Sample timerSample = Timer.start(meterRegistry);
            try {
                @SuppressWarnings("unchecked")
                final Map<String, Object> lcmEventHeadersMap =
                        jsonObjectMapper.convertToValueType(lcmEventHeader, Map.class);
                eventsProducer.sendLegacyEvent(topicName, cmHandleId, lcmEventHeadersMap, lcmEvent);
            } catch (final KafkaException e) {
                log.error("Unable to send message to topic : {} and cause : {}", topicName, e.getMessage());
            } finally {
                recordMetrics(lcmEvent, timerSample);
            }
        } else {
            log.debug("Notifications disabled.");
        }
    }

    private void recordMetrics(final LcmEvent lcmEvent, final Timer.Sample timerSample) {
        final List<Tag> tags = new ArrayList<>(4);
        tags.add(TAG_CLASS);
        tags.add(TAG_METHOD);

        final String oldCmHandleState = extractCmHandleStateValue(lcmEvent.getEvent().getOldValues());
        tags.add(Tag.of("oldCmHandleState", oldCmHandleState));

        final String newCmHandleState = extractCmHandleStateValue(lcmEvent.getEvent().getNewValues());
        tags.add(Tag.of("newCmHandleState", newCmHandleState));

        timerSample.stop(Timer.builder("cps.ncmp.lcm.events.send")
                .description("Time taken to send a LCM event")
                .tags(tags)
                .register(meterRegistry));
    }

    /**
     * Extracts the CM handle state value from the given Values object.
     * If the provided Values object or its CM handle state is null, returns a default value.
     *
     * @param values The Values object containing CM handle state information.
     * @return The CM handle state value as a string, or a default value if null.
     */
    private String extractCmHandleStateValue(final Values values) {
        return (values != null && values.getCmHandleState() != null)
                ? values.getCmHandleState().value()
                : UNAVAILABLE_CM_HANDLE_STATE;
    }
}
