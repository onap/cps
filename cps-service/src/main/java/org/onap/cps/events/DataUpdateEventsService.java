/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024 Tech Mahindra Ltd.
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
import io.micrometer.core.annotation.Timed;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.events.model.CpsDataUpdatedEvent;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataUpdateEventsService {

    private final EventsPublisher<CpsDataUpdatedEvent> eventsPublisher;

    private final JsonObjectMapper jsonObjectMapper;

    @Value("${app.core.events.topic:core-events}")
    private String topicName;

    @Value("${notification.enabled:true}")
    private boolean notificationsEnabled;

    /**
     * Publish the cps data update event with header to the public topic.
     *
     * @param eventKey     update event Id
     * @param cpsDataUpdatedEvent   cps data update event
     */
    @Timed(value = "cps.dataupdate.events.publish", description = "Time taken to publish Data Update event")
    public void publishDataUpdateEvent(final String eventKey, final CpsDataUpdatedEvent cpsDataUpdatedEvent) {
        if (notificationsEnabled) {
            try {
                final Map<String, String> extensions = createUpdateEventExtensions(eventKey);
                final CloudEvent dataUpdatedCloudEvent =
                        CloudEventBuilder.builder().type(CpsDataUpdatedEvent.class.getTypeName())
                                .event(cpsDataUpdatedEvent).extensions(extensions).setCloudEvent().build();
                eventsPublisher.publishCloudEvent(topicName, eventKey, dataUpdatedCloudEvent);
            } catch (final KafkaException  e) {
                log.error("Unable to publish message to topic : {} and cause : {}", topicName, e.getMessage());
            }
        } else {
            log.debug("Notifications disabled.");
        }
    }

    private Map<String, String> createUpdateEventExtensions(final String eventKey) {
        final Map<String, String> extensions = new HashMap<>();
        extensions.put("correlationid", eventKey);
        return extensions;
    }

}
