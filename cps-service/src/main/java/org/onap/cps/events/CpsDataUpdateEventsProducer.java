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

import io.cloudevents.CloudEvent;
import io.micrometer.core.annotation.Timed;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDeltaService;
import org.onap.cps.api.CpsNotificationService;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.model.DeltaReport;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.events.model.CpsDataUpdatedEvent;
import org.onap.cps.events.model.Data;
import org.onap.cps.events.model.Data.Operation;
import org.onap.cps.utils.DateTimeUtility;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CpsDataUpdateEventsProducer {

    private final EventsProducer<CpsDataUpdatedEvent> eventsProducer;

    private final CpsNotificationService cpsNotificationService;

    private final CpsDeltaService cpsDeltaService;

    private final JsonObjectMapper jsonObjectMapper;

    @Value("${app.cps.data-updated.topic:cps-data-updated-events}")
    private String topicName;

    @Value("${app.cps.data-updated.change-event-notifications-enabled:false}")
    private boolean cpsChangeEventNotificationsEnabled;

    @Value("${notification.enabled:false}")
    private boolean notificationsEnabled;

    @Value("${app.cps.data-updated.delta-notification:false}")
    private boolean deltaNotificationEnabled;

    /**
     * Send the cps data update event with header to the public topic.
     *
     * @param anchor Anchor of the updated data
     * @param dataNodes data nodes
     * @param xpath  xpath of the updated data
     * @param operation operation performed on the data
     * @param observedTimestamp timestamp when data was updated.
     */
    @Timed(value = "cps.dataupdate.events.send", description = "Time taken to send Data Update event")
    public void sendCpsDataUpdateEvent(final Anchor anchor, final Collection<DataNode> dataNodes, final String xpath,
                                       final Operation operation, final OffsetDateTime observedTimestamp) {
        if (notificationsEnabled && cpsChangeEventNotificationsEnabled && isNotificationEnabledForAnchor(anchor)) {
            final CpsDataUpdatedEvent cpsDataUpdatedEvent = createCpsDataUpdatedEvent(anchor,
                    observedTimestamp, xpath, dataNodes, operation);
            final String updateEventId = anchor.getDataspaceName() + ":" + anchor.getName();
            final Map<String, String> extensions = createUpdateEventExtensions(updateEventId);
            final CloudEvent cpsDataUpdatedEventAsCloudEvent =
                    CpsEvent.builder().type(CpsDataUpdatedEvent.class.getTypeName()).data(cpsDataUpdatedEvent)
                            .extensions(extensions).build().asCloudEvent();
            eventsProducer.sendCloudEvent(topicName, updateEventId, cpsDataUpdatedEventAsCloudEvent);
        } else {
            log.debug("State of Overall Notifications : {} and Cps Change Event Notifications : {}",
                    notificationsEnabled, cpsChangeEventNotificationsEnabled);
        }
    }

    private boolean isNotificationEnabledForAnchor(final Anchor anchor) {
        return cpsNotificationService.isNotificationEnabled(anchor.getDataspaceName(), anchor.getName());
    }

    private CpsDataUpdatedEvent createCpsDataUpdatedEvent(final Anchor anchor, final OffsetDateTime observedTimestamp,
                                                          final String xpath, final Collection<DataNode> dataNodes,
                                                          final Operation operation) {
        final CpsDataUpdatedEvent cpsDataUpdatedEvent = new CpsDataUpdatedEvent();
        final Data updateEventData = new Data();
        updateEventData.setObservedTimestamp(DateTimeUtility.toString(observedTimestamp));
        updateEventData.setDataspaceName(anchor.getDataspaceName());
        updateEventData.setAnchorName(anchor.getName());
        updateEventData.setSchemaSetName(anchor.getSchemaSetName());
        updateEventData.setOperation(operation);
        updateEventData.setXpath(xpath);
        if (deltaNotificationEnabled) {
            final String generatedDeltaReport = generateUpdateDeltaReport(anchor.getDataspaceName(), anchor.getName(),
                    xpath, dataNodes, operation);
            updateEventData.setDelta(generatedDeltaReport);
        }
        cpsDataUpdatedEvent.setData(updateEventData);
        return cpsDataUpdatedEvent;
    }

    private Map<String, String> createUpdateEventExtensions(final String eventKey) {
        final Map<String, String> extensions = new HashMap<>();
        extensions.put("correlationid", eventKey);
        return extensions;
    }

    private String generateUpdateDeltaReport(final String dataspaceName, final String anchorName,
                                                        final String parentNodeXpath,
                                                        final Collection<DataNode> dataNodes,
                                                        final Operation operation) {
        if (!deltaNotificationEnabled) {
            return null;
        }

        if (!Operation.REPLACE.equals(operation)) {
            return jsonObjectMapper.asJsonString(dataNodes);
        }

        List<DeltaReport> deltaReport = Collections.emptyList();
        try {
            deltaReport = cpsDeltaService.getDeltaByDataspaceAnchorAndPayload(dataspaceName, anchorName,
                    parentNodeXpath, Collections.emptyMap(), jsonObjectMapper.asJsonString(dataNodes),
                    FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
        } catch (final Exception exception) {
            log.error("Failed to generate delta report", exception);
        }
        return jsonObjectMapper.asJsonString(deltaReport);
    }
}
