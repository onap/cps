/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2021-2022 Bell Canada.
 * Modifications Copyright (c) 2022-2023 Nordix Foundation
 * Modifications Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.notification;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.event.model.Content;
import org.onap.cps.event.model.CpsDataUpdatedEvent;
import org.onap.cps.event.model.Data;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.DataMapUtils;
import org.onap.cps.utils.PrefixResolver;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor(onConstructor = @__(@Lazy))
public class CpsDataUpdatedEventFactory {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    @Lazy
    private final CpsDataService cpsDataService;

    @Lazy
    private final PrefixResolver prefixResolver;


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
            cpsDataService.getDataNodes(anchor.getDataspaceName(), anchor.getName(),
                "/", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS).iterator().next();
        return toCpsDataUpdatedEvent(anchor, dataNode, observedTimestamp, operation);
    }

    @SneakyThrows(URISyntaxException.class)
    private CpsDataUpdatedEvent toCpsDataUpdatedEvent(final Anchor anchor,
                                                      final DataNode dataNode,
                                                      final OffsetDateTime observedTimestamp,
                                                      final Operation operation) {
        final CpsDataUpdatedEvent cpsDataUpdatedEvent = new CpsDataUpdatedEvent();
        cpsDataUpdatedEvent.withContent(createContent(anchor, dataNode, observedTimestamp, operation));
        cpsDataUpdatedEvent.withId(UUID.randomUUID().toString());
        cpsDataUpdatedEvent.withSchema(new URI("urn:cps:org.onap.cps:data-updated-event-schema:v1"));
        cpsDataUpdatedEvent.withSource(new URI("urn:cps:org.onap.cps"));
        cpsDataUpdatedEvent.withType("org.onap.cps.data-updated-event");
        return cpsDataUpdatedEvent;
    }

    /**
     * Generates CPS Delta Updated event. If observedTimestamp is not provided, then current timestamp is used.
     *
     * @param anchor            anchor
     * @param observedTimestamp observedTimestamp
     * @param operation         operation
     * @return CpsDeltaUpdatedEvent
     */

    public CpsDataUpdatedEvent createDeltaCpsDataUpdatedEvent(final Anchor anchor,
                                                              final OffsetDateTime observedTimestamp,
                                                              final Operation operation) {
        final DataNode dataNode = (operation == Operation.UPDATE) ? null :
                (DataNode) cpsDataService.getDeltaByDataspaceAndAnchors(anchor.getDataspaceName(), anchor.getName(),
                                                                        anchor.getName(), "/",
                                                                        FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
                                         .stream().iterator().next();
        return toCpsDeltaUpdatedEvent(anchor, dataNode, observedTimestamp, operation);
    }

    @SneakyThrows(URISyntaxException.class)
    private CpsDataUpdatedEvent toCpsDeltaUpdatedEvent(final Anchor anchor,
                                                       final DataNode dataNode,
                                                       final OffsetDateTime observedTimestamp,
                                                       final Operation operation) {
        final CpsDataUpdatedEvent cpsDeltaUpdatedEvent = new CpsDataUpdatedEvent();
        cpsDeltaUpdatedEvent.withContent(createContent(anchor, dataNode, observedTimestamp, operation));
        cpsDeltaUpdatedEvent.withId(UUID.randomUUID().toString());
        cpsDeltaUpdatedEvent.withSchema(new URI("urn:cps:org.onap.cps:data-updated-event-schema:v2"));
        cpsDeltaUpdatedEvent.withSource(new URI("urn:cps:org.onap.cps"));
        cpsDeltaUpdatedEvent.withType("org.onap.cps.delta-updated-event");
        return cpsDeltaUpdatedEvent;
    }

    private Data createData(final DataNode dataNode, final String prefix) {
        final Data data = new Data();
        DataMapUtils.toDataMapWithIdentifier(dataNode, prefix).forEach(data::setAdditionalProperty);
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
            final String prefix = prefixResolver.getPrefix(anchor, dataNode.getXpath());
            content.withData(createData(dataNode, prefix));
        }
        return content;
    }
}
