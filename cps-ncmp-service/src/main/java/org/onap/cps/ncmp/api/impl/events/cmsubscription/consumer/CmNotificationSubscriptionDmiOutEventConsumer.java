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

package org.onap.cps.ncmp.api.impl.events.cmsubscription.consumer;

import static org.onap.cps.ncmp.api.impl.events.mapper.CloudEventMapper.toTargetEvent;

import io.cloudevents.CloudEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.CmNotificationSubscriptionEventsHandler;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.CmNotificationSubscriptionMappersHandler;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.DmiCmNotificationSubscriptionCacheHandler;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.CmNotificationSubscriptionStatus;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.DmiCmNotificationSubscriptionDetails;
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.dmi_to_ncmp.CmNotificationSubscriptionDmiOutEvent;
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.ncmp_to_client.CmNotificationSubscriptionNcmpOutEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CmNotificationSubscriptionDmiOutEventConsumer {

    private final DmiCmNotificationSubscriptionCacheHandler dmiCmNotificationSubscriptionCacheHandler;
    private final CmNotificationSubscriptionEventsHandler cmNotificationSubscriptionEventsHandler;
    private final CmNotificationSubscriptionMappersHandler cmNotificationSubscriptionMappersHandler;

    /**
     * Consume the Cm Notification Subscription event from the dmi-plugin.
     *
     * @param cmNotificationSubscriptionDmiOutEventConsumerRecord the event to be consumed
     */
    @KafkaListener(topics = "${app.ncmp.avc.cm-subscription-dmi-out}",
            containerFactory = "cloudEventConcurrentKafkaListenerContainerFactory")
    public void consumeCmNotificationSubscriptionDmiOutEvent(
            final ConsumerRecord<String, CloudEvent> cmNotificationSubscriptionDmiOutEventConsumerRecord) {
        final CloudEvent cloudEvent = cmNotificationSubscriptionDmiOutEventConsumerRecord.value();
        final CmNotificationSubscriptionDmiOutEvent cmNotificationSubscriptionDmiOutEvent =
                toTargetEvent(cloudEvent, CmNotificationSubscriptionDmiOutEvent.class);
        final String correlationId = String.valueOf(cloudEvent.getExtension("correlationid"));
        if ("subscriptionCreateResponse".equals(cloudEvent.getType()) && cmNotificationSubscriptionDmiOutEvent != null
                    && correlationId != null) {
            handleCmSubscriptionCreate(correlationId, cmNotificationSubscriptionDmiOutEvent);
        }
    }

    private void handleCmSubscriptionCreate(final String correlationId,
            final CmNotificationSubscriptionDmiOutEvent cmNotificationSubscriptionDmiOutEvent) {
        final String subscriptionId = correlationId.split("#")[0];
        final String dmiPluginName = correlationId.split("#")[1];

        if ("ACCEPTED".equals(cmNotificationSubscriptionDmiOutEvent.getData().getStatusMessage())) {
            handleCacheStatusPerDmi(subscriptionId, dmiPluginName, CmNotificationSubscriptionStatus.ACCEPTED);
            dmiCmNotificationSubscriptionCacheHandler.persistIntoDatabasePerDmi(subscriptionId, dmiPluginName);
            handleEventsStatusPerDmi(subscriptionId);
        }

        if ("REJECTED".equals(cmNotificationSubscriptionDmiOutEvent.getData().getStatusMessage())) {
            handleCacheStatusPerDmi(subscriptionId, dmiPluginName, CmNotificationSubscriptionStatus.REJECTED);
            handleEventsStatusPerDmi(subscriptionId);
        }

        log.info("Cm Subscription with id : {} handled by the dmi-plugin : {} has the status : {}", subscriptionId,
                dmiPluginName, cmNotificationSubscriptionDmiOutEvent.getData().getStatusMessage());
    }

    private void handleCacheStatusPerDmi(final String subscriptionId, final String dmiPluginName,
            final CmNotificationSubscriptionStatus cmNotificationSubscriptionStatus) {
        dmiCmNotificationSubscriptionCacheHandler.updateDmiCmNotificationSubscriptionStatusPerDmi(subscriptionId,
                dmiPluginName, cmNotificationSubscriptionStatus);
    }

    private void handleEventsStatusPerDmi(final String subscriptionId) {
        final Map<String, DmiCmNotificationSubscriptionDetails> dmiCmNotificationSubscriptionDetailsPerDmi =
                dmiCmNotificationSubscriptionCacheHandler.get(subscriptionId);
        final CmNotificationSubscriptionNcmpOutEvent cmNotificationSubscriptionNcmpOutEvent =
                cmNotificationSubscriptionMappersHandler.toCmNotificationSubscriptionNcmpOutEvent(subscriptionId,
                        dmiCmNotificationSubscriptionDetailsPerDmi);
        cmNotificationSubscriptionEventsHandler.publishCmNotificationSubscriptionNcmpOutEvent(subscriptionId,
                "subscriptionCreateResponse", cmNotificationSubscriptionNcmpOutEvent, false);
    }
}
