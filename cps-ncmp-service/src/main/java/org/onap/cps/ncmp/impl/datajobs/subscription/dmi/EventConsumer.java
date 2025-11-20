
/*
 *  ===========LICENSE_START========================================================
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

import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_DATA_SUBSCRIPTION_ACCEPTED;
import static org.onap.cps.ncmp.utils.events.CloudEventMapper.toTargetEvent;

import io.cloudevents.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.onap.cps.ncmp.impl.datajobs.subscription.dmi_to_ncmp.DataJobSubscriptionDmiOutEvent;
import org.onap.cps.ncmp.impl.datajobs.subscription.models.CmSubscriptionStatus;
import org.onap.cps.ncmp.impl.datajobs.subscription.ncmp.CmSubscriptionHandler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true", matchIfMissing = true)
public class EventConsumer {

    private static final String CORRELATION_ID_SEPARATOR = "#";

    private final CmSubscriptionHandler cmSubscriptionHandler;

    /**
     * Consume the Cm Notification Subscription response event from the dmi-plugin.
     *
     * @param dmiOutEventAsConsumerRecord the event to be consumed
     */
    @KafkaListener(topics = "${app.ncmp.avc.cm-subscription-dmi-out}",
            containerFactory = "cloudEventConcurrentKafkaListenerContainerFactory")
    public void consumeDmiOutEvent(final ConsumerRecord<String, CloudEvent> dmiOutEventAsConsumerRecord) {
        final CloudEvent cloudEvent = dmiOutEventAsConsumerRecord.value();
        final DataJobSubscriptionDmiOutEvent dmiOutEvent =
                toTargetEvent(cloudEvent, DataJobSubscriptionDmiOutEvent.class);
        final String correlationId = String.valueOf(cloudEvent.getExtension("correlationid"));
        final String eventType = cloudEvent.getType();

        log.info("Consumed DMI subscription response event with details: | correlationId={} | eventType={}",
                correlationId, eventType);

        if (dmiOutEvent != null && correlationId != null) {
            final String[] parts = correlationId.split(CORRELATION_ID_SEPARATOR);
            final String subscriptionId = parts[0];
            final String dmiPluginName = parts[1];

            if ("subscriptionCreateResponse".equals(eventType)) {
                final CmSubscriptionStatus cmSubscriptionStatus = getCmSubscriptionStatus(dmiOutEvent);
                cmSubscriptionHandler.updateCmSubscriptionStatus(subscriptionId, dmiPluginName, cmSubscriptionStatus);
            }
        }
        log.info("Finished processing DMI subscription response event with details: | correlationId={} | eventType={}",
                correlationId, eventType);
    }

    private CmSubscriptionStatus getCmSubscriptionStatus(final DataJobSubscriptionDmiOutEvent dmiOutEvent) {
        final String statusMessage = dmiOutEvent.getData().getStatusMessage();
        final String statusCode = dmiOutEvent.getData().getStatusCode();
        if (statusCode.equals(CM_DATA_SUBSCRIPTION_ACCEPTED.getCode())
                && statusMessage.equals(CM_DATA_SUBSCRIPTION_ACCEPTED.getMessage())) {
            return CmSubscriptionStatus.ACCEPTED;
        }
        return CmSubscriptionStatus.REJECTED;
    }
}
