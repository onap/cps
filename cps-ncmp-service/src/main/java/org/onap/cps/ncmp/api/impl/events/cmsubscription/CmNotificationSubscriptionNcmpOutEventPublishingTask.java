/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.events.cmsubscription;

import io.cloudevents.CloudEvent;
import io.cloudevents.core.builder.CloudEventBuilder;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.events.EventsPublisher;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.mapper.CmNotificationSubscriptionNcmpOutEventMapper;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.DmiCmNotificationSubscriptionDetails;
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.ncmp_to_client.CmNotificationSubscriptionNcmpOutEvent;
import org.onap.cps.utils.JsonObjectMapper;

@Slf4j
@RequiredArgsConstructor
public class CmNotificationSubscriptionNcmpOutEventPublishingTask implements Runnable {


    private final String topicName;
    private final String subscriptionId;
    private final String eventType;
    private final EventsPublisher<CloudEvent> eventsPublisher;
    private final JsonObjectMapper jsonObjectMapper;
    private final Map<String, Map<String, DmiCmNotificationSubscriptionDetails>> cmNotificationSubscriptionCache;
    private final CmNotificationSubscriptionNcmpOutEventMapper cmNotificationSubscriptionNcmpOutEventMapper;

    /**
     * Delegating the responsibility of publishing CmNotificationSubscriptionNcmpOutEvent as a separate task which will
     * be called after a specified delay.
     */
    @Override
    public void run() {
        final Map<String, DmiCmNotificationSubscriptionDetails> dmiCmNotificationSubscriptionDetailsMap =
                cmNotificationSubscriptionCache.get(subscriptionId);
        final CmNotificationSubscriptionNcmpOutEvent cmNotificationSubscriptionNcmpOutEvent =
                cmNotificationSubscriptionNcmpOutEventMapper.toCmNotificationSubscriptionNcmpOutEvent(subscriptionId,
                        dmiCmNotificationSubscriptionDetailsMap);
        eventsPublisher.publishCloudEvent(topicName, subscriptionId,
                buildAndGetCmNotificationNcmpOutEventAsCloudEvent(subscriptionId, eventType,
                        cmNotificationSubscriptionNcmpOutEvent));
    }

    private CloudEvent buildAndGetCmNotificationNcmpOutEventAsCloudEvent(final String subscriptionId,
            final String eventType,
            final CmNotificationSubscriptionNcmpOutEvent cmNotificationSubscriptionNcmpOutEvent) {

        return CloudEventBuilder.v1().withId(UUID.randomUUID().toString()).withType(eventType)
                .withSource(URI.create("NCMP")).withDataSchema(URI.create("org.onap.ncmp.cm.subscription:1.0.0"))
                .withExtension("correlationid", subscriptionId)
                .withData(jsonObjectMapper.asJsonBytes(cmNotificationSubscriptionNcmpOutEvent)).build();
    }


}
