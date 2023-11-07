/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.events.avc.ncmp_to_client.AttributeValueChangeEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AttributeValueChangeEventCloudMapper {

    private static String randomId = UUID.randomUUID().toString();
    private final ObjectMapper objectMapper;
    private static final String NCMP_SOURCE_PREFIX = "ncmp.";

    /**
     * Maps AttributeValueChangeEvent to a CloudEvent.
     *
     * @param avcEvent object
     * @param eventKey as String
     * @return CloudEvent built.
     */
    public CloudEvent toCloudEvent(final AttributeValueChangeEvent avcEvent, final String eventKey,
                                   final String eventType) {
        try {
            return CloudEventBuilder.v1().withId(randomId)
                .withSource(URI.create(NCMP_SOURCE_PREFIX + eventKey))
                .withType(eventType)
                .withExtension("correlationid", eventKey)
                .withDataSchema(URI.create("urn:cps:" + AttributeValueChangeEvent.class.getName() + ":1.0.0"))
                .withData(objectMapper.writeValueAsBytes(avcEvent)).build();
        } catch (final JsonProcessingException jsonProcessingException) {
            log.error("The Cloud Event could not be constructed", jsonProcessingException);
        }
        return null;
    }
}
