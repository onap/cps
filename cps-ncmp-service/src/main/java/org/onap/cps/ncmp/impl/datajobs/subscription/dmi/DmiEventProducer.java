/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.datajobs.subscription.dmi;

import static org.onap.cps.ncmp.events.NcmpEventDataSchema.SUBSCRIPTIONS_V1;

import io.cloudevents.CloudEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.events.EventProducer;
import org.onap.cps.ncmp.impl.datajobs.subscription.ncmp_to_dmi.DataJobSubscriptionDmiInEvent;
import org.onap.cps.ncmp.utils.events.NcmpEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true", matchIfMissing = true)
public class DmiEventProducer {

    private final EventProducer eventProducer;

    @Value("${app.ncmp.avc.cm-subscription-dmi-in}")
    private String dmiInEventTopic;

    /**
     * Send the event to the provided dmi plugin with key as subscription id and the event is in Cloud Event format.
     *
     * @param subscriptionId CM subscription id
     * @param dmiPluginName  Dmi plugin Name
     * @param eventType      Type of event
     * @param event          Cm Notification Subscription event for Dmi
     */
    public void send(final String subscriptionId, final String dmiPluginName,
                     final String eventType, final DataJobSubscriptionDmiInEvent event) {
        eventProducer.sendCloudEvent(dmiInEventTopic, subscriptionId,
            toCloudEvent(eventType, event, subscriptionId, dmiPluginName));

    }

    private CloudEvent toCloudEvent(final String eventType, final DataJobSubscriptionDmiInEvent event,
                                    final String subscriptionId, final String dmiPluginName) {
        return NcmpEvent.builder()
            .type(eventType)
            .dataSchema(SUBSCRIPTIONS_V1.getDataSchema())
            .data(event)
            .extensions(Map.of("correlationid", String.join("#", subscriptionId, dmiPluginName)))
            .build()
            .asCloudEvent();
    }

}
