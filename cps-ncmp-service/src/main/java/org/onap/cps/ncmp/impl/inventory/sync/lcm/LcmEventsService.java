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

package org.onap.cps.ncmp.impl.inventory.sync.lcm;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.events.EventsPublisher;
import org.onap.cps.ncmp.events.lcm.v1.LcmEvent;
import org.onap.cps.ncmp.events.lcm.v1.LcmEventHeader;
import org.onap.cps.ncmp.events.lcm.v1.Values;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.stereotype.Service;

/**
 * LcmEventsService to call the publisher and publish on the dedicated topic.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class LcmEventsService {

    private final EventsPublisher<LcmEvent> eventsPublisher;
    private final JsonObjectMapper jsonObjectMapper;
    private final MeterRegistry meterRegistry;

    @Value("${app.lcm.events.topic:ncmp-events}")
    private String topicName;

    @Value("${notification.enabled:true}")
    private boolean notificationsEnabled;

    /**
     * Publishes an LCM event to the dedicated topic with optional notification headers.
     * Capture and log KafkaException If an error occurs while publishing the event to Kafka
     *
     * @param cmHandleId     Cm Handle Id associated with the LCM event
     * @param lcmEvent       The LCM event object to be published
     * @param lcmEventHeader Optional headers associated with the LCM event
     */
    public void publishLcmEvent(final String cmHandleId, final LcmEvent lcmEvent, final LcmEventHeader lcmEventHeader) {

        final Timer.Sample timerSample = Timer.start(meterRegistry);

        try {
            if (notificationsEnabled) {
                final Map<String, Object> lcmEventHeadersMap =
                        jsonObjectMapper.convertToValueType(lcmEventHeader, Map.class);
                eventsPublisher.publishEvent(topicName, cmHandleId, lcmEventHeadersMap, lcmEvent);
            } else {
                log.debug("Notifications disabled.");
            }
        } catch (final KafkaException e) {
            log.error("Unable to publish message to topic : {} and cause : {}", topicName, e.getMessage());
        } finally {
            final String oldCmHandleState = extractCmHandleStateValue(lcmEvent.getEvent().getOldValues());
            final String newCmHandleState = extractCmHandleStateValue(lcmEvent.getEvent().getNewValues());
            timerSample.stop(Timer.builder("cps.ncmp.lcm.events.publish")
                    .description("Time taken to publish a LCM event")
                    .tag("class", "org.onap.cps.ncmp.impl.inventory.sync.lcm.LcmEventsService")
                    .tag("method", "publishLcmEvent")
                    .tag("oldCmHandleState", oldCmHandleState)
                    .tag("newCmHandleState ", newCmHandleState)
                    .register(meterRegistry));
        }
    }

    /**
     * Helper method to extract CmHandleState value from Values object.
     * Returns "Unknown" if Values or CmHandleState is null.
     */
    private String extractCmHandleStateValue(final Values values) {
        if (values != null && values.getCmHandleState() != null) {
            return values.getCmHandleState().value();
        }
        return "Unknown";
    }
}
