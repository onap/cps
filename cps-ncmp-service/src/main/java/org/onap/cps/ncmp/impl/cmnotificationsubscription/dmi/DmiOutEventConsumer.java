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

import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_DATA_SUBSCRIPTION_ACCEPTED;
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_DATA_SUBSCRIPTION_REJECTED;
import static org.onap.cps.ncmp.utils.events.CloudEventMapper.toTargetEvent;

import io.cloudevents.CloudEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.onap.cps.ncmp.api.NcmpResponseStatus;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.cache.DmiCacheHandler;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.CmSubscriptionStatus;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionDetails;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.ncmp.NcmpOutEventMapper;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.ncmp.NcmpOutEventProducer;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.dmi_to_ncmp.Data;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.dmi_to_ncmp.DmiOutEvent;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_client.NcmpOutEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true", matchIfMissing = true)
public class DmiOutEventConsumer {

    private final DmiCacheHandler dmiCacheHandler;
    private final NcmpOutEventProducer ncmpOutEventProducer;
    private final NcmpOutEventMapper ncmpOutEventMapper;

    private static final String CM_SUBSCRIPTION_CORRELATION_ID_SEPARATOR = "#";

    /**
     * Consume the Cm Notification Subscription event from the dmi-plugin.
     *
     * @param dmiOutEventAsConsumerRecord the event to be consumed
     */
    @KafkaListener(topics = "${app.ncmp.avc.cm-subscription-dmi-out}",
            containerFactory = "cloudEventConcurrentKafkaListenerContainerFactory")
    public void consumeDmiOutEvent(final ConsumerRecord<String, CloudEvent> dmiOutEventAsConsumerRecord) {
        final CloudEvent cloudEvent = dmiOutEventAsConsumerRecord.value();
        final DmiOutEvent dmiOutEvent = toTargetEvent(cloudEvent, DmiOutEvent.class);
        final String correlationId = String.valueOf(cloudEvent.getExtension("correlationid"));
        if (dmiOutEvent != null && correlationId != null) {
            final String eventType = cloudEvent.getType();
            handleDmiOutEvent(correlationId, eventType, dmiOutEvent);
        }
    }

    private void handleDmiOutEvent(final String correlationId, final String eventType,
            final DmiOutEvent dmiOutEvent) {
        final String subscriptionId = correlationId.split(CM_SUBSCRIPTION_CORRELATION_ID_SEPARATOR)[0];
        final String dmiPluginName = correlationId.split(CM_SUBSCRIPTION_CORRELATION_ID_SEPARATOR)[1];

        if (checkStatusCodeAndMessage(CM_DATA_SUBSCRIPTION_ACCEPTED, dmiOutEvent.getData())) {
            handleCacheStatusPerDmi(subscriptionId, dmiPluginName, CmSubscriptionStatus.ACCEPTED);
            if (eventType.equals("subscriptionCreateResponse")) {
                dmiCacheHandler.persistIntoDatabasePerDmi(subscriptionId, dmiPluginName);
            }
            if (eventType.equals("subscriptionDeleteResponse")) {
                dmiCacheHandler.removeFromDatabase(subscriptionId, dmiPluginName);
            }
            handleEventsStatusPerDmi(subscriptionId, eventType);
        }

        if (checkStatusCodeAndMessage(CM_DATA_SUBSCRIPTION_REJECTED, dmiOutEvent.getData())) {
            handleCacheStatusPerDmi(subscriptionId, dmiPluginName, CmSubscriptionStatus.REJECTED);
            handleEventsStatusPerDmi(subscriptionId, eventType);
        }

        log.info("Cm Subscription with id : {} handled by the dmi-plugin : {} has the status : {}", subscriptionId,
                dmiPluginName, dmiOutEvent.getData().getStatusMessage());
    }

    private void handleCacheStatusPerDmi(final String subscriptionId, final String dmiPluginName,
            final CmSubscriptionStatus cmSubscriptionStatus) {
        dmiCacheHandler.updateDmiSubscriptionStatus(subscriptionId, dmiPluginName,
                cmSubscriptionStatus);
    }

    private void handleEventsStatusPerDmi(final String subscriptionId, final String eventType) {
        final Map<String, DmiCmSubscriptionDetails> dmiSubscriptionsPerDmi = dmiCacheHandler.get(subscriptionId);
        final NcmpOutEvent ncmpOutEvent = ncmpOutEventMapper.toNcmpOutEvent(subscriptionId, dmiSubscriptionsPerDmi);
        ncmpOutEventProducer.publishNcmpOutEvent(subscriptionId, eventType, ncmpOutEvent, false);
    }

    private boolean checkStatusCodeAndMessage(final NcmpResponseStatus ncmpResponseStatus,
            final Data dmiOutData) {
        return ncmpResponseStatus.getCode().equals(dmiOutData.getStatusCode())
                       && ncmpResponseStatus.getMessage()
                                  .equals(dmiOutData.getStatusMessage());
    }
}
