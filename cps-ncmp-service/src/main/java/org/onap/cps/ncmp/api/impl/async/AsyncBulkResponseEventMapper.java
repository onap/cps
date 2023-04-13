/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.async;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.onap.cps.ncmp.event.model.AsyncBulkResponseEvent;
import org.onap.cps.ncmp.event.model.BulkResponseEvent;

/**
 * Mapper for converting BulkResponseEvent to AsyncBulkResponseEvent.
 */
@Mapper(componentModel = "spring")
public interface AsyncBulkResponseEventMapper {

    @Mapping(source = "eventId", target = "eventId", qualifiedByName = "ncmpAsyncBulkEventId")
    @Mapping(source = "eventTime", target = "eventTime", qualifiedByName = "currentTime")
    AsyncBulkResponseEvent toNcmpAsyncBulkEvent(BulkResponseEvent bulkResponseEvent);

    @Named("ncmpAsyncBulkEventId")
    static String getAsyncBulkEventId(String eventId) {
        return UUID.randomUUID().toString();
    }

    @Named("currentTime")
    static String getFormattedCurrentTime(String eventTime) {
        return ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ"));
    }
}
