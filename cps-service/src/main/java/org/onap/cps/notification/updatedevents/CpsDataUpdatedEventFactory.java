/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2021-2022 Bell Canada.
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

package org.onap.cps.notification.updatedevents;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.event.model.Content;
import org.onap.cps.event.model.CpsDataUpdatedEvent;
import org.onap.cps.event.model.Data;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.DataMapUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor(onConstructor = @__(@Lazy))
public class CpsDataUpdatedEventFactory {

    private static final URI EVENT_SCHEMA;
    private static final URI EVENT_SOURCE;
    private static final String EVENT_TYPE = "org.onap.cps.data-updated-event";
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    static {
        try {
            EVENT_SCHEMA = new URI("urn:cps:org.onap.cps:data-updated-event-schema:v1");
            EVENT_SOURCE = new URI("urn:cps:org.onap.cps");
        } catch (final URISyntaxException e) {
            // As it is fixed string, I don't expect to see this error
            throw new IllegalArgumentException(e);
        }
    }

    @Lazy
    private final CpsDataService cpsDataService;

    /**
     * Generates CPS Data Updated event. If observedTimestamp is not provided, then current timestamp is used.
     *
     * @param anchor            anchor
     * @param observedTimestamp observedTimestamp
     * @param operation         operation
     * @return CpsDataUpdatedEvent
     */
    public CpsDataUpdatedEvent createCpsDataUpdatedEvent(final Anchor anchor,
        final OffsetDateTime observedTimestamp, final Operation operation) {
        final var dataNode = (operation == Operation.DELETE) ? null :
            cpsDataService.getDataNode(anchor.getDataspaceName(), anchor.getName(),
                "/", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
        return toCpsDataUpdatedEvent(anchor, dataNode, observedTimestamp, operation);
    }

    private CpsDataUpdatedEvent toCpsDataUpdatedEvent(final Anchor anchor, final DataNode dataNode,
        final OffsetDateTime observedTimestamp, final Operation operation) {
        final var cpsDataUpdatedEvent = new CpsDataUpdatedEvent();
        cpsDataUpdatedEvent.withContent(createContent(anchor, dataNode, observedTimestamp, operation));
        cpsDataUpdatedEvent.withId(UUID.randomUUID().toString());
        cpsDataUpdatedEvent.withSchema(EVENT_SCHEMA);
        cpsDataUpdatedEvent.withSource(EVENT_SOURCE);
        cpsDataUpdatedEvent.withType(EVENT_TYPE);
        return cpsDataUpdatedEvent;
    }

    private Data createData(final DataNode dataNode) {
        final var data = new Data();
        DataMapUtils.toDataMapWithIdentifier(dataNode).forEach(data::setAdditionalProperty);
        return data;
    }

    private Content createContent(final Anchor anchor, final DataNode dataNode,
        final OffsetDateTime observedTimestamp, final Operation operation) {
        final var content = new Content();
        content.withAnchorName(anchor.getName());
        content.withDataspaceName(anchor.getDataspaceName());
        content.withSchemaSetName(anchor.getSchemaSetName());
        content.withOperation(Content.Operation.fromValue(operation.name()));
        content.withObservedTimestamp(
            DATE_TIME_FORMATTER.format(observedTimestamp == null ? OffsetDateTime.now() : observedTimestamp));
        if (dataNode != null) {
            content.withData(createData(dataNode));
        }
        return content;
    }
}
