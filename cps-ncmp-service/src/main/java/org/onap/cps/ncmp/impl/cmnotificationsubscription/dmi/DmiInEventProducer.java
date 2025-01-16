/*
 * ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.dmi;

import static org.onap.cps.ncmp.events.NcmpEventDataSchema.SUBSCRIPTIONS_EVENT_V1;

import io.cloudevents.CloudEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.events.EventsPublisher;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_dmi.DmiInEvent;
import org.onap.cps.ncmp.utils.events.NcmpEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true", matchIfMissing = true)
public class DmiInEventProducer {

    private final EventsPublisher<CloudEvent> eventsPublisher;

    @Value("${app.ncmp.avc.cm-subscription-dmi-in}")
    private String dmiInEventTopic;

    /**
     * Publish the event to the provided dmi plugin with key as subscription id and the event is in Cloud Event format.
     *
     * @param subscriptionId Cm Subscription Id
     * @param dmiPluginName  Dmi Plugin Name
     * @param eventType      Type of event
     * @param dmiInEvent     Cm Notification Subscription event for Dmi
     */
    public void publishDmiInEvent(final String subscriptionId, final String dmiPluginName,
            final String eventType, final DmiInEvent dmiInEvent) {
        eventsPublisher.publishCloudEvent(dmiInEventTopic, subscriptionId,
                buildAndGetDmiInEventAsCloudEvent(subscriptionId, dmiPluginName, eventType, dmiInEvent));

    }

    private CloudEvent buildAndGetDmiInEventAsCloudEvent(final String subscriptionId, final String dmiPluginName,
            final String eventType, final DmiInEvent dmiInEvent) {
        return NcmpEvent.builder().type(eventType).dataSchema(SUBSCRIPTIONS_EVENT_V1.getDataSchema())
                       .extensions(Map.of("correlationid", subscriptionId.concat("#").concat(dmiPluginName)))
                       .data(dmiInEvent).build().asCloudEvent();
    }


}
