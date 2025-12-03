/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2024-2025 TechMahindra Ltd.
 * Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

import static org.onap.cps.events.model.EventPayload.Action.fromValue;

import io.cloudevents.CloudEvent;
import io.micrometer.core.annotation.Timed;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsNotificationService;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.events.model.CpsDataUpdatedEvent;
import org.onap.cps.events.model.EventPayload;
import org.onap.cps.utils.DateTimeUtility;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CpsDataUpdateEventsProducer {

    private final EventProducer eventProducer;

    private final CpsNotificationService cpsNotificationService;

    @Value("${app.cps.data-updated.topic:cps-data-updated-events}")
    private String topicName;

    @Value("${app.cps.data-updated.change-event-notifications-enabled:false}")
    private boolean cpsChangeEventNotificationsEnabled;

    @Value("${notification.enabled:false}")
    private boolean notificationsEnabled;

    /**
     * Send the cps data update event with header to the public topic.
     *
     * @param anchor            Anchor of the updated data
     * @param xpath             xpath of the updated data
     * @param action            operation performed on the data
     * @param observedTimestamp timestamp when data was updated.
     */
    @Timed(value = "cps.data.update.events.send", description = "Time taken to send Data Update event")
    public void sendCpsDataUpdateEvent(final Anchor anchor, final String xpath,
                                       final String action, final OffsetDateTime observedTimestamp) {
        if (notificationsEnabled && cpsChangeEventNotificationsEnabled && isNotificationEnabledForAnchor(anchor)) {
            final CpsDataUpdatedEvent cpsDataUpdatedEvent =
                createCpsDataUpdatedEvent(anchor, observedTimestamp, xpath, action);
            final String updateEventId = anchor.getDataspaceName() + ":" + anchor.getName();
            final Map<String, String> extensions = createUpdateEventExtensions(updateEventId);
            final CloudEvent cpsDataUpdatedEventAsCloudEvent =
                    CpsEvent.builder().type(CpsDataUpdatedEvent.class.getTypeName()).data(cpsDataUpdatedEvent)
                            .extensions(extensions).build().asCloudEvent();
            eventProducer.sendCloudEvent(topicName, updateEventId, cpsDataUpdatedEventAsCloudEvent);
        } else {
            log.debug("State of Overall Notifications : {} and Cps Change Event Notifications : {}",
                    notificationsEnabled, cpsChangeEventNotificationsEnabled);
        }
    }

    private boolean isNotificationEnabledForAnchor(final Anchor anchor) {
        return cpsNotificationService.isNotificationEnabled(anchor.getDataspaceName(), anchor.getName());
    }

    private CpsDataUpdatedEvent createCpsDataUpdatedEvent(final Anchor anchor, final OffsetDateTime observedTimestamp,
                                                          final String xpath, final String action) {
        final CpsDataUpdatedEvent cpsDataUpdatedEvent = new CpsDataUpdatedEvent();
        final EventPayload updateEventData = new EventPayload();
        updateEventData.setObservedTimestamp(DateTimeUtility.toString(observedTimestamp));
        updateEventData.setDataspaceName(anchor.getDataspaceName());
        updateEventData.setAnchorName(anchor.getName());
        updateEventData.setSchemaSetName(anchor.getSchemaSetName());
        updateEventData.setAction(fromValue(action));
        updateEventData.setXpath(xpath);
        cpsDataUpdatedEvent.setEventPayload(updateEventData);
        return cpsDataUpdatedEvent;
    }

    private Map<String, String> createUpdateEventExtensions(final String eventKey) {
        final Map<String, String> extensions = new HashMap<>();
        extensions.put("correlationid", eventKey);
        return extensions;
    }
}
