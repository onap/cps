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

package org.onap.cps.ncmp.api.impl.notifications.avc;

import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.onap.cps.ncmp.event.model.AvcEvent;


/**
 * Mapper for converting incoming {@link AvcEvent} to outgoing {@link AvcEvent}.
 */
@Mapper(componentModel = "spring")
public interface AvcEventMapper {

    @Mapping(source = "eventTime", target = "eventTime")
    @Mapping(source = "eventId", target = "eventId", qualifiedByName = "avcEventId")
    @Mapping(source = "eventCorrelationId", target = "eventCorrelationId")
    @Mapping(source = "eventSchema", target = "eventSchema")
    @Mapping(source = "eventSchemaVersion", target = "eventSchemaVersion")
    @Mapping(source = "eventTarget", target = "eventTarget")
    @Mapping(source = "eventType", target = "eventType")
    AvcEvent toOutgoingAvcEvent(AvcEvent incomingAvcEvent);

    @Named("avcEventId")
    static String getAvcEventId(String eventId) {
        return UUID.randomUUID().toString();
    }

}
