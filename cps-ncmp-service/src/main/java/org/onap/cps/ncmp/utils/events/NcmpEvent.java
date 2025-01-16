/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 Nordix Foundation
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

package org.onap.cps.ncmp.utils.events;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.ncmp.config.CpsApplicationContext;
import org.onap.cps.ncmp.impl.utils.EventDateTimeFormatter;
import org.onap.cps.utils.JsonObjectMapper;

@Builder
public class NcmpEvent {

    private Object data;
    private Map<String, String> extensions;
    private String type;
    private String dataSchema;
    @Builder.Default
    private static final String CLOUD_EVENT_SPEC_VERSION_V1 = "1.0.0";
    @Builder.Default
    private static final String CLOUD_EVENT_SOURCE = "NCMP";

    /**
     * Creates ncmp cloud event with provided attributes.
     *
     * @return Cloud Event
     */
    public CloudEvent asCloudEvent() {
        final JsonObjectMapper jsonObjectMapper = CpsApplicationContext.getCpsBean(JsonObjectMapper.class);
        final CloudEventBuilder cloudEventBuilder = CloudEventBuilder.v1()
                .withId(UUID.randomUUID().toString())
                .withSource(URI.create(CLOUD_EVENT_SOURCE))
                .withType(type)
                .withDataSchema(URI.create(dataSchema))
                .withTime(EventDateTimeFormatter.toIsoOffsetDateTime(
                        EventDateTimeFormatter.getCurrentIsoFormattedDateTime()))
                .withData(jsonObjectMapper.asJsonBytes(data));
        extensions.entrySet().stream()
                .filter(extensionEntry -> StringUtils.isNotBlank(extensionEntry.getValue()))
                .forEach(extensionEntry ->
                        cloudEventBuilder.withExtension(extensionEntry.getKey(), extensionEntry.getValue()));
        return cloudEventBuilder.build();
    }
}
