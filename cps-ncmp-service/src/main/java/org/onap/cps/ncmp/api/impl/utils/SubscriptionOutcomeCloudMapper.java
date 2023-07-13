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
import io.cloudevents.core.builder.CloudEventBuilder;
import java.net.URI;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.events.avcsubscription1_0_0.dmi_to_ncmp.SubscriptionEventResponse;
import org.onap.cps.ncmp.events.avcsubscription1_0_0.ncmp_to_client.SubscriptionEventOutcome;
import org.onap.cps.spi.exceptions.CloudEventConstructionException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class SubscriptionOutcomeCloudMapper {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static String randomId = UUID.randomUUID().toString();

    /**
     * Maps SubscriptionEventOutcome to a CloudEvent.
     *
     * @param subscriptionEventOutcome object
     * @return CloudEvent
     */
    public static CloudEvent toCloudEvent(final SubscriptionEventOutcome subscriptionEventOutcome,
                                          final String eventKey, final String eventType) {
        try {
            return CloudEventBuilder.v1()
                    .withId(randomId)
                    .withSource(URI.create("NCMP"))
                    .withType(eventType)
                    .withExtension("correlationid", eventKey)
                    .withDataSchema(URI.create("urn:cps:" + SubscriptionEventResponse.class.getName() + ":1.0.0"))
                    .withData(objectMapper.writeValueAsBytes(subscriptionEventOutcome)).build();
        } catch (final Exception ex) {
            throw new CloudEventConstructionException("The Cloud Event could not be constructed", "Invalid object to "
                    + "serialize or required headers is missing", ex);
        }
    }
}
