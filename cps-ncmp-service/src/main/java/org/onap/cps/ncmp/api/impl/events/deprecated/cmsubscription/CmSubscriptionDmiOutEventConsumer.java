/*
 * ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.api.impl.events.deprecated.cmsubscription;

import static org.onap.cps.ncmp.api.impl.events.mapper.CloudEventMapper.toTargetEvent;

import com.hazelcast.map.IMap;
import io.cloudevents.CloudEvent;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.onap.cps.ncmp.api.impl.config.embeddedcache.ForwardedSubscriptionEventCacheConfig;
import org.onap.cps.ncmp.api.impl.deprecated.subscriptions.SubscriptionPersistence;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent;
import org.onap.cps.ncmp.api.models.CmSubscriptionEvent;
import org.onap.cps.ncmp.events.cmsubscription1_0_0.dmi_to_ncmp.CmSubscriptionDmiOutEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CmSubscriptionDmiOutEventConsumer {

    private final IMap<String, Set<String>> forwardedSubscriptionEventCache;
    private final SubscriptionPersistence subscriptionPersistence;
    private final CmSubscriptionDmiOutEventToYangModelSubscriptionEventMapper
            cmSubscriptionDmiOutEventToYangModelSubscriptionEventMapper;
    private final CmSubscriptionNcmpOutEventPublisher cmSubscriptionNcmpOutEventPublisher;

    @Value("${notification.enabled:true}")
    private boolean notificationFeatureEnabled;

    @Value("${ncmp.model-loader.subscription:false}")
    private boolean subscriptionModelLoaderEnabled;

    /**
     * Consume subscription response event.
     *
     * @param cmSubscriptionDmiOutConsumerRecord the event to be consumed
     */
    @KafkaListener(topics = "${app.ncmp.avc.subscription-response-topic}",
            containerFactory = "cloudEventConcurrentKafkaListenerContainerFactory")
    public void consumeDmiOutEvent(
            final ConsumerRecord<String, CloudEvent> cmSubscriptionDmiOutConsumerRecord) {
        final CloudEvent cloudEvent = cmSubscriptionDmiOutConsumerRecord.value();
        final String eventType = cmSubscriptionDmiOutConsumerRecord.value().getType();
        final CmSubscriptionDmiOutEvent cmSubscriptionDmiOutEvent =
                toTargetEvent(cloudEvent, CmSubscriptionDmiOutEvent.class);
        final String clientId = cmSubscriptionDmiOutEvent.getData().getClientId();
        log.info("subscription event response of clientId: {} is received.", clientId);
        final String subscriptionName = cmSubscriptionDmiOutEvent.getData().getSubscriptionName();
        final String subscriptionEventId = clientId + subscriptionName;
        boolean createOutcomeResponse = true;
        if (forwardedSubscriptionEventCache.containsKey(subscriptionEventId)) {
            final Set<String> dmiNames = forwardedSubscriptionEventCache.get(subscriptionEventId);
            dmiNames.remove(cmSubscriptionDmiOutEvent.getData().getDmiName());
            forwardedSubscriptionEventCache.put(subscriptionEventId, dmiNames,
                    ForwardedSubscriptionEventCacheConfig.SUBSCRIPTION_FORWARD_STARTED_TTL_SECS, TimeUnit.SECONDS);
            createOutcomeResponse = forwardedSubscriptionEventCache.get(subscriptionEventId).isEmpty();
        }
        if (subscriptionModelLoaderEnabled) {
            updateSubscriptionEvent(cmSubscriptionDmiOutEvent);
        }
        if (createOutcomeResponse
                && notificationFeatureEnabled) {

            final CmSubscriptionEvent cmSubscriptionEvent = new CmSubscriptionEvent();
            cmSubscriptionEvent.setClientId(cmSubscriptionDmiOutEvent.getData().getClientId());
            cmSubscriptionEvent.setSubscriptionName(cmSubscriptionDmiOutEvent.getData().getSubscriptionName());

            cmSubscriptionNcmpOutEventPublisher.sendResponse(cmSubscriptionEvent, eventType);
            forwardedSubscriptionEventCache.remove(subscriptionEventId);
        }
    }

    private void updateSubscriptionEvent(final CmSubscriptionDmiOutEvent cmSubscriptionDmiOutEvent) {
        final YangModelSubscriptionEvent yangModelSubscriptionEvent =
                cmSubscriptionDmiOutEventToYangModelSubscriptionEventMapper
                        .toYangModelSubscriptionEvent(cmSubscriptionDmiOutEvent);
        subscriptionPersistence.saveSubscriptionEvent(yangModelSubscriptionEvent);
    }
}