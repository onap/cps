/*
 *  ============LICENSE_START=======================================================
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
import io.cloudevents.core.CloudEventUtils;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;
import io.cloudevents.jackson.PojoCloudEventDataMapper;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.events.cmsubscription1_0_0.client_to_ncmp.CmSubscriptionNcmpInEvent;
import org.onap.cps.ncmp.events.cmsubscription1_0_0.ncmp_to_dmi.CmSubscriptionDmiInEvent;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CmSubscriptionEventCloudMapper {

    private final ObjectMapper objectMapper;

    private static String randomId = UUID.randomUUID().toString();

    /**
     * Maps CloudEvent object to CmSubscriptionNcmpInEvent.
     *
     * @param cloudEvent object.
     * @return CmSubscriptionNcmpInEvent deserialized.
     */
    public CmSubscriptionNcmpInEvent toCmSubscriptionNcmpInEvent(final CloudEvent cloudEvent) {
        final PojoCloudEventData<CmSubscriptionNcmpInEvent> deserializedCloudEvent = CloudEventUtils
                .mapData(cloudEvent, PojoCloudEventDataMapper.from(objectMapper, CmSubscriptionNcmpInEvent.class));
        if (deserializedCloudEvent == null) {
            log.debug("No data found in the consumed event");
            return null;
        } else {
            final CmSubscriptionNcmpInEvent cmSubscriptionNcmpInEvent = deserializedCloudEvent.getValue();
            log.debug("Consuming event {}", cmSubscriptionNcmpInEvent);
            return cmSubscriptionNcmpInEvent;
        }
    }

    /**
     * Maps CmSubscriptionDmiInEvent to a CloudEvent.
     *
     * @param cmSubscriptionDmiInEvent object.
     * @param eventKey as String.
     * @return CloudEvent built.
     */
    public CloudEvent toCloudEvent(final CmSubscriptionDmiInEvent cmSubscriptionDmiInEvent, final String eventKey,
            final String eventType) {
        try {
            return CloudEventBuilder.v1().withId(randomId)
                    .withSource(URI.create(cmSubscriptionDmiInEvent.getData().getSubscription().getClientID()))
                    .withType(eventType).withExtension("correlationid", eventKey)
                    .withDataSchema(URI.create("urn:cps:" + CmSubscriptionDmiInEvent.class.getName() + ":1.0.0"))
                    .withData(objectMapper.writeValueAsBytes(cmSubscriptionDmiInEvent)).build();
        } catch (final JsonProcessingException jsonProcessingException) {
            log.error("The Cloud Event could not be constructed", jsonProcessingException);
        }
        return null;
    }
}
