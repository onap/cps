/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.impl.data.async;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.onap.cps.ncmp.event.model.ncmp.async_m2m.DmiAsyncRequestResponseEvent;
import org.onap.cps.ncmp.event.model.ncmp.async_m2m.NcmpAsyncRequestResponseEvent;

/**
 * Mapper for converting DmiAsyncRequestResponseEvent to NcmpAsyncRequestResponseEvent.
 */
@Mapper(componentModel = "spring")
public interface NcmpAsyncRequestResponseEventMapper {

    @Mapping(source = "eventId", target = "eventId", qualifiedByName = "ncmpAsyncEventId")
    @Mapping(source = "eventTime", target = "eventTime", qualifiedByName = "currentTime")
    @Mapping(source = "eventId", target = "forwardedEvent.eventId")
    @Mapping(source = "eventCorrelationId", target = "forwardedEvent.eventCorrelationId")
    @Mapping(source = "eventSchema", target = "forwardedEvent.eventSchema")
    @Mapping(source = "eventSchemaVersion", target = "forwardedEvent.eventSchemaVersion")
    @Mapping(source = "eventSource", target = "forwardedEvent.eventSource")
    @Mapping(source = "eventTarget", target = "forwardedEvent.eventTarget")
    @Mapping(source = "eventTime", target = "forwardedEvent.eventTime")
    @Mapping(source = "eventType", target = "forwardedEvent.eventType")
    @Mapping(source = "eventContent.responseStatus", target = "forwardedEvent.responseStatus")
    @Mapping(source = "eventContent.responseCode", target = "forwardedEvent.responseCode")
    @Mapping(source = "eventContent.responseDataSchema", target = "forwardedEvent.responseDataSchema")
    NcmpAsyncRequestResponseEvent toNcmpAsyncEvent(DmiAsyncRequestResponseEvent dmiAsyncRequestResponseEvent);

    @Named("ncmpAsyncEventId")
    static String getNcmpAsyncEventId(String eventId) {
        return UUID.randomUUID().toString();
    }

    @Named("currentTime")
    static String getFormattedCurrentTime(String eventTime) {
        return ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
    }

    @AfterMapping
    default void mapAdditionalProperties(DmiAsyncRequestResponseEvent dmiAsyncRequestResponseEvent,
                                         @MappingTarget NcmpAsyncRequestResponseEvent ncmpAsyncRequestResponseEvent) {
        ncmpAsyncRequestResponseEvent.getForwardedEvent().setAdditionalProperty("response-data",
                dmiAsyncRequestResponseEvent.getEventContent().getResponseData().getAdditionalProperties());
    }

}
