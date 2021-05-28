
/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Bell Canada. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.notification;


import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.event.model.Content;
import org.onap.cps.event.model.CpsDataUpdatedEvent;
import org.onap.cps.event.model.CpsDataUpdatedEvent.Schema;
import org.onap.cps.event.model.Data;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.DataMapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
// TODO Fix java docs
public class NotificationService {

    private boolean dataUpdatedEventNotificationEnabled;
    private NotificationPublisher publisher;
    private CpsDataService cpsDataService;
    private CpsAdminService cpsAdminService;
    private CpsDataUpdatedEventConverter cpsDataUpdatedEventConverter = new CpsDataUpdatedEventConverter();

    /**
     * Constructor.
     *
     * @param dataUpdatedEventNotificationEnabled abcd
     * @param publisher                           abcd
     * @param cpsDataService                      abcd
     */
    @Autowired
    public NotificationService(
        @Value("${notification.data-updated.enabled}") final boolean dataUpdatedEventNotificationEnabled,
        final NotificationPublisher publisher,
        final CpsDataService cpsDataService,
        final CpsAdminService cpsAdminService) {
        this.dataUpdatedEventNotificationEnabled = dataUpdatedEventNotificationEnabled;
        this.publisher = publisher;
        this.cpsDataService = cpsDataService;
        this.cpsAdminService = cpsAdminService;
    }

    /**
     * update data event.
     *
     * @param dataspaceName dataspaceName
     * @param anchorName    anchorName
     */
    public void processDataUpdatedEvent(final String dataspaceName, final String anchorName) {
        if (canSendNotification()) {
            final var dataNode = cpsDataService
                .getDataNode(dataspaceName, anchorName, "/", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
            final var anchor = cpsAdminService.getAnchor(dataspaceName, anchorName);

            final var cpsDataUpdatedEvent = cpsDataUpdatedEventConverter.toCpsDataUpdatedEvent(anchor, dataNode);
            publisher.sendNotification(cpsDataUpdatedEvent);
        }
    }

    /*
    Add more complex rules based on dataspace and anchor later
     */
    private boolean canSendNotification() {
        return dataUpdatedEventNotificationEnabled;
    }

    // TODO Find a better name
    static class CpsDataUpdatedEventConverter {

        private static final URI EVENT_SOURCE;
        private static final String EVENT_TYPE = "org.onap.cps.data-updated-event";
        private static final DateTimeFormatter dateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

        static  {
            try {
                EVENT_SOURCE = new URI("urn:cps:org.onap.cps");
            } catch (final URISyntaxException e) {
                // As it is fixed string, I don't expect to see this error
                throw new IllegalArgumentException(e);
            }
        }

        CpsDataUpdatedEvent toCpsDataUpdatedEvent(final Anchor anchor, final DataNode dataNode) {
            final var cpsDataUpdatedEvent = new CpsDataUpdatedEvent();
            cpsDataUpdatedEvent.withContent(createContent(anchor, dataNode));
            cpsDataUpdatedEvent.withId(UUID.randomUUID().toString());
            cpsDataUpdatedEvent.withSchema(Schema.URN_CPS_ORG_ONAP_CPS_DATA_UPDATED_EVENT_SCHEMA_1_1_0_SNAPSHOT);
            cpsDataUpdatedEvent.withSource(EVENT_SOURCE);
            cpsDataUpdatedEvent.withType(EVENT_TYPE);
            return cpsDataUpdatedEvent;
        }

        private Data createData(final DataNode dataNode) {
            final var data = new Data();
            DataMapUtils.toDataMap(dataNode).forEach((k, v) -> data.setAdditionalProperty(k, v));
            return data;
        }

        private Content createContent(final Anchor anchor, final DataNode dataNode) {
            final var content = new Content();
            content.withAnchorName(anchor.getName());
            content.withDataspaceName(anchor.getDataspaceName());
            content.withSchemaSetName(anchor.getSchemaSetName());
            content.withData(createData(dataNode));
            content.withObservedTimestamp(dateTimeFormatter.format(OffsetDateTime.now()));
            return content;
        }
    }
}
