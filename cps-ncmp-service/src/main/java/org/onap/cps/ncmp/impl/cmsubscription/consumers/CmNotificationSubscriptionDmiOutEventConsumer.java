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

package org.onap.cps.ncmp.impl.cmsubscription.consumers;

import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_DATA_SUBSCRIPTION_ACCEPTED;
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_DATA_SUBSCRIPTION_REJECTED;
import static org.onap.cps.ncmp.utils.events.CloudEventMapper.toTargetEvent;

import io.cloudevents.CloudEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.onap.cps.ncmp.api.NcmpResponseStatus;
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.dmi_to_ncmp.CmNotificationSubscriptionDmiOutEvent;
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.dmi_to_ncmp.Data;
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.ncmp_to_client.CmNotificationSubscriptionNcmpOutEvent;
import org.onap.cps.ncmp.impl.cmsubscription.CmNotificationSubscriptionEventsFacade;
import org.onap.cps.ncmp.impl.cmsubscription.CmNotificationSubscriptionMappersFacade;
import org.onap.cps.ncmp.impl.cmsubscription.cache.DmiCacheHandler;
import org.onap.cps.ncmp.impl.cmsubscription.models.CmNotificationSubscriptionStatus;
import org.onap.cps.ncmp.impl.cmsubscription.models.DmiCmNotificationSubscriptionDetails;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CmNotificationSubscriptionDmiOutEventConsumer {

    private final DmiCacheHandler dmiCacheHandler;
    private final CmNotificationSubscriptionEventsFacade cmNotificationSubscriptionEventsFacade;
    private final CmNotificationSubscriptionMappersFacade cmNotificationSubscriptionMappersFacade;

    private static final String CM_DATA_SUBSCRIPTION_CORRELATION_ID_SEPARATOR = "#";

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
        if (cmNotificationSubscriptionDmiOutEvent != null && correlationId != null) {
            final String eventType = cloudEvent.getType();
            handleCmSubscriptionDmiOutEvent(correlationId, eventType, cmNotificationSubscriptionDmiOutEvent);
        }
    }

    private void handleCmSubscriptionDmiOutEvent(final String correlationId,
                                                 final String eventType,
                                                 final CmNotificationSubscriptionDmiOutEvent
                                                         cmNotificationSubscriptionDmiOutEvent) {
        final String subscriptionId = correlationId.split(CM_DATA_SUBSCRIPTION_CORRELATION_ID_SEPARATOR)[0];
        final String dmiPluginName = correlationId.split(CM_DATA_SUBSCRIPTION_CORRELATION_ID_SEPARATOR)[1];

        if (checkStatusCodeAndMessage(CM_DATA_SUBSCRIPTION_ACCEPTED, cmNotificationSubscriptionDmiOutEvent.getData())) {
            handleCacheStatusPerDmi(subscriptionId, dmiPluginName, CmNotificationSubscriptionStatus.ACCEPTED);
            if (eventType.equals("subscriptionCreateResponse")) {
                dmiCacheHandler.persistIntoDatabasePerDmi(subscriptionId, dmiPluginName);
            }
            if (eventType.equals("subscriptionDeleteResponse")) {
                dmiCacheHandler.removeFromDatabasePerDmi(subscriptionId, dmiPluginName);
            }
            handleEventsStatusPerDmi(subscriptionId, eventType);
        }

        if (checkStatusCodeAndMessage(CM_DATA_SUBSCRIPTION_REJECTED, cmNotificationSubscriptionDmiOutEvent.getData())) {
            handleCacheStatusPerDmi(subscriptionId, dmiPluginName, CmNotificationSubscriptionStatus.REJECTED);
            handleEventsStatusPerDmi(subscriptionId, eventType);
        }

        log.info("Cm Subscription with id : {} handled by the dmi-plugin : {} has the status : {}", subscriptionId,
                dmiPluginName, cmNotificationSubscriptionDmiOutEvent.getData().getStatusMessage());
    }

    private void handleCacheStatusPerDmi(final String subscriptionId, final String dmiPluginName,
                                         final CmNotificationSubscriptionStatus cmNotificationSubscriptionStatus) {
        dmiCacheHandler.updateDmiCmNotificationSubscriptionStatusPerDmi(subscriptionId,
                dmiPluginName, cmNotificationSubscriptionStatus);
    }

    private void handleEventsStatusPerDmi(final String subscriptionId, final String eventType) {
        final Map<String, DmiCmNotificationSubscriptionDetails> dmiCmNotificationSubscriptionDetailsPerDmi =
                dmiCacheHandler.get(subscriptionId);
        final CmNotificationSubscriptionNcmpOutEvent cmNotificationSubscriptionNcmpOutEvent =
                cmNotificationSubscriptionMappersFacade.toCmNotificationSubscriptionNcmpOutEvent(subscriptionId,
                        dmiCmNotificationSubscriptionDetailsPerDmi);
        cmNotificationSubscriptionEventsFacade.publishCmNotificationSubscriptionNcmpOutEvent(subscriptionId,
                eventType, cmNotificationSubscriptionNcmpOutEvent, false);
    }

    private boolean checkStatusCodeAndMessage(final NcmpResponseStatus ncmpResponseStatus,
                                              final Data cmNotificationSubscriptionDmiOutData) {
        return ncmpResponseStatus.getCode().equals(cmNotificationSubscriptionDmiOutData.getStatusCode())
                && ncmpResponseStatus.getMessage()
                .equals(cmNotificationSubscriptionDmiOutData.getStatusMessage());
    }
}
