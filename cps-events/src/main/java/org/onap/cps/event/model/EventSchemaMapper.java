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

package org.onap.cps.event.model;

import java.net.URI;
import java.net.URISyntaxException;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for data updated event schema.
 */
@Mapper(componentModel = "spring")
public abstract class EventSchemaMapper {

    protected static final URI EVENT_SCHEMA_V0;
    protected static final URI EVENT_SCHEMA_V1;

    static  {
        try {
            EVENT_SCHEMA_V0 = new URI("urn:cps:org.onap.cps:data-updated-event-schema:v0");
            EVENT_SCHEMA_V1 = new URI("urn:cps:org.onap.cps:data-updated-event-schema:v1");
        } catch (final URISyntaxException e) {
            // As it is fixed string, this exception is not expected
            throw new IllegalArgumentException(e);
        }
    }

    @Mapping(expression = "java(EVENT_SCHEMA_V0)", target = "schema")
    @Mapping(target = "withSchema", ignore = true)
    @Mapping(target = "withType", ignore = true)
    @Mapping(target = "withSource", ignore = true)
    @Mapping(target = "withId", ignore = true)
    @Mapping(target = "withContent", ignore = true)
    public abstract org.onap.cps.event.model.v0.CpsDataUpdatedEvent v1ToV0(
        org.onap.cps.event.model.v1.CpsDataUpdatedEvent event);

    @Mapping(expression = "java(EVENT_SCHEMA_V1)", target = "schema")
    @Mapping(
        expression = "java(org.onap.cps.event.model.v1.Content.Operation.MODIFICATION)", target = "content.operation")
    @Mapping(target = "withSchema", ignore = true)
    @Mapping(target = "withType", ignore = true)
    @Mapping(target = "withSource", ignore = true)
    @Mapping(target = "withId", ignore = true)
    @Mapping(target = "withContent", ignore = true)
    public abstract org.onap.cps.event.model.v1.CpsDataUpdatedEvent v0ToV1(
        org.onap.cps.event.model.v0.CpsDataUpdatedEvent event);

}
