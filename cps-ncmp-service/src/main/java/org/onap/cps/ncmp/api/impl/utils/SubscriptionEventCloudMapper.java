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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.cloudevents.CloudEvent;
import io.cloudevents.core.CloudEventUtils;
import io.cloudevents.core.builder.CloudEventBuilder;
import io.cloudevents.core.data.PojoCloudEventData;
import io.cloudevents.jackson.PojoCloudEventDataMapper;
import java.net.URI;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.events.avcsubscription1_0_0.client_to_ncmp.SubscriptionEvent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class SubscriptionEventCloudMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Maps CloudEvent object to SubscriptionEvent.
     *
     * @param cloudEvent object.
     * @return SubscriptionEvent deserialized.
     */
    public static SubscriptionEvent toSubscriptionEvent(final CloudEvent cloudEvent) {
        final PojoCloudEventData<SubscriptionEvent> deserializedCloudEvent = CloudEventUtils
                .mapData(cloudEvent, PojoCloudEventDataMapper.from(objectMapper, SubscriptionEvent.class));
        if (deserializedCloudEvent == null) {
            log.debug("No data found in the consumed event");
            return null;
        } else {
            final SubscriptionEvent subscriptionEvent = deserializedCloudEvent.getValue();
            log.debug("Consuming event {}", subscriptionEvent);
            return subscriptionEvent;
        }
    }

    /**
     * Maps SubscriptionEvent to a CloudEvent.
     *
     * @param ncmpSubscriptionEvent object.
     * @param eventKey as String.
     * @param eventType as String.
     * @return CloudEvent builded.
     */
    public static CloudEvent toCloudEvent(
            final org.onap.cps.ncmp.events.avcsubscription1_0_0.ncmp_to_dmi.SubscriptionEvent ncmpSubscriptionEvent,
            final String eventKey, final String eventType) {
        try {
            return CloudEventBuilder.v1()
                    .withData(objectMapper.writeValueAsBytes(ncmpSubscriptionEvent))
                    .withId(eventKey).withType(eventType).withSource(
                            URI.create(ncmpSubscriptionEvent.getData().getSubscription().getClientID())).build();
        } catch (final Exception ex) {
            throw new RuntimeException("The Cloud Event could not be constructed.", ex);
        }
    }
}
