/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2021 Bell Canada.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.notification;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.event.model.EventSchemaMapper;
import org.onap.cps.event.model.v1.Content;
import org.onap.cps.event.model.v1.CpsDataUpdatedEvent;
import org.onap.cps.event.model.v1.Data;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.DataMapUtils;
import org.springframework.stereotype.Component;

@Component
public class CpsDataUpdatedEventFactory {

    private static final URI EVENT_SCHEMA;
    private static final URI EVENT_SOURCE;
    private static final String EVENT_TYPE = "org.onap.cps.data-updated-event";
    private static final DateTimeFormatter dateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    static  {
        try {
            EVENT_SCHEMA = new URI("urn:cps:org.onap.cps:data-updated-event-schema:v1");
            EVENT_SOURCE = new URI("urn:cps:org.onap.cps");
        } catch (final URISyntaxException e) {
            // As it is fixed string, I don't expect to see this error
            throw new IllegalArgumentException(e);
        }
    }

    private CpsDataService cpsDataService;
    private CpsAdminService cpsAdminService;
    private EventSchemaMapper eventSchemaMapper;

    /**
     * Constructor.
     */
    public CpsDataUpdatedEventFactory(
            final CpsDataService cpsDataService,
            final CpsAdminService cpsAdminService,
            final EventSchemaMapper eventSchemaMapper) {
        this.cpsDataService = cpsDataService;
        this.cpsAdminService = cpsAdminService;
        this.eventSchemaMapper = eventSchemaMapper;
    }

    public org.onap.cps.event.model.v0.CpsDataUpdatedEvent createCpsDataUpdatedEventV0(
            final String dataspaceName, final String anchorName) {
        final var eventV1 = this.createCpsDataUpdatedEventV1(dataspaceName, anchorName);
        return this.eventSchemaMapper.v1ToV0(eventV1);
    }

    /**
     * Create the data updated event corresponding to the specified datatspace and anchor.
     */
    public CpsDataUpdatedEvent createCpsDataUpdatedEventV1(final String dataspaceName, final String anchorName) {
        final var dataNode = cpsDataService
            .getDataNode(dataspaceName, anchorName, "/", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
        final var anchor = cpsAdminService.getAnchor(dataspaceName, anchorName);
        return toCpsDataUpdatedEvent(anchor, dataNode);
    }

    private CpsDataUpdatedEvent toCpsDataUpdatedEvent(final Anchor anchor, final DataNode dataNode) {
        final var cpsDataUpdatedEvent = new CpsDataUpdatedEvent();
        cpsDataUpdatedEvent.withContent(createContent(anchor, dataNode));
        cpsDataUpdatedEvent.withId(UUID.randomUUID().toString());
        cpsDataUpdatedEvent.withSchema(EVENT_SCHEMA);
        cpsDataUpdatedEvent.withSource(EVENT_SOURCE);
        cpsDataUpdatedEvent.withType(EVENT_TYPE);
        return cpsDataUpdatedEvent;
    }

    private Data createData(final DataNode dataNode) {
        final var data = new Data();
        DataMapUtils.toDataMap(dataNode).forEach(data::setAdditionalProperty);
        return data;
    }

    private Content createContent(final Anchor anchor, final DataNode dataNode) {
        final var content = new Content();
        content.withOperation(Content.Operation.MODIFICATION);
        content.withAnchorName(anchor.getName());
        content.withDataspaceName(anchor.getDataspaceName());
        content.withSchemaSetName(anchor.getSchemaSetName());
        content.withData(createData(dataNode));
        content.withObservedTimestamp(dateTimeFormatter.format(OffsetDateTime.now()));
        return content;
    }
}
