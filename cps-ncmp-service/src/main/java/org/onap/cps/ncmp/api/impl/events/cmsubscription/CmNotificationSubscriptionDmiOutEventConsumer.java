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

package org.onap.cps.ncmp.api.impl.events.cmsubscription;

import static org.onap.cps.ncmp.api.impl.events.mapper.CloudEventMapper.toTargetEvent;

import io.cloudevents.CloudEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.CmNotificationSubscriptionStatus;
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.dmi_to_ncmp.CmNotificationSubscriptionDmiOutEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CmNotificationSubscriptionDmiOutEventConsumer {

    private DmiCmNotificationSubscriptionCacheHandler dmiCmNotificationSubscriptionCacheHandler;

    /**
     * Consume the Cm Notification Subscription event from the dmi-plugin.
     *
     * @param cmNotificationSubscriptionDmiOutEventConsumerRecord the event to be consumed
     */
    @KafkaListener(topics = "${app.ncmp.avc.subscription-response-topic}",
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
            dmiCmNotificationSubscriptionCacheHandler.updateDmiCmNotificationSubscriptionCacheStatusPerDmi(
                subscriptionId, dmiPluginName, CmNotificationSubscriptionStatus.ACCEPTED);
            dmiCmNotificationSubscriptionCacheHandler.persistCacheIntoDatabasePerDmi(subscriptionId, dmiPluginName);
        }

        if ("REJECTED".equals(cmNotificationSubscriptionDmiOutEvent.getData().getStatusMessage())) {
            dmiCmNotificationSubscriptionCacheHandler.updateDmiCmNotificationSubscriptionCacheStatusPerDmi(
                subscriptionId, dmiPluginName, CmNotificationSubscriptionStatus.REJECTED);
        }

        log.info("Cm Subscription with id : {} handled by the dmi-plugin : {} has the status : {}", subscriptionId,
                dmiPluginName, cmNotificationSubscriptionDmiOutEvent.getData().getStatusMessage());
    }
}
