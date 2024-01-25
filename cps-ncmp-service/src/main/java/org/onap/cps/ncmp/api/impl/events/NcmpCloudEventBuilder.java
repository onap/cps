/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.events;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.ncmp.api.impl.utils.EventDateTimeFormatter;
import org.onap.cps.ncmp.api.impl.utils.context.CpsApplicationContext;
import org.onap.cps.utils.JsonObjectMapper;

@Builder(buildMethodName = "setCloudEvent")
public class NcmpCloudEventBuilder {

    private Object event;
    private Map<String, String> extensions;
    private String type;
    @Builder.Default
    private static final String EVENT_SPEC_VERSION_V1 = "1.0.0";

    /**
     * Creates ncmp cloud event with provided attributes.
     *
     * @return Cloud Event
     */
    public CloudEvent build() {
        final JsonObjectMapper jsonObjectMapper = CpsApplicationContext.getCpsBean(JsonObjectMapper.class);
        final CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create("NCMP"))
                .withType(type)
                .withDataSchema(URI.create("urn:cps:" + type + ":" + EVENT_SPEC_VERSION_V1))
                .withTime(EventDateTimeFormatter.toIsoOffsetDateTime(
                        EventDateTimeFormatter.getCurrentIsoFormattedDateTime()))
                .withData(jsonObjectMapper.asJsonBytes(event));
        extensions.entrySet().stream()
                .filter(extensionEntry -> StringUtils.isNotBlank(extensionEntry.getValue()))
                .forEach(extensionEntry ->
                        cloudEventBuilder.withExtension(extensionEntry.getKey(), extensionEntry.getValue()));
        return cloudEventBuilder.build();
    }

    /**
     * Creates an extension map for a cloud event with a single entry.
     *
     * @param key   the name of the extension
     * @param value the value of the extension
     * @return a map of extensions
     */
    public static Map<String, String> createCloudEventExtension(final String key, final String value) {
        final Map<String, String> extensions = new HashMap<>();
        extensions.put(key, value);
        return extensions;
    }
}
