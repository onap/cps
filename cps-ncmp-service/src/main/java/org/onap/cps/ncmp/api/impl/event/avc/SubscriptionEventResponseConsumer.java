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

package org.onap.cps.ncmp.api.impl.event.avc;

import com.hazelcast.map.IMap;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.onap.cps.ncmp.api.impl.events.avcsubscription.SubscriptionEventResponseMapper;
import org.onap.cps.ncmp.api.impl.events.avcsubscription.SubscriptionEventResponseOutcome;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionPersistence;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent;
import org.onap.cps.ncmp.api.models.SubscriptionEventResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionEventResponseConsumer {

    private final IMap<String, Set<String>> forwardedSubscriptionEventCache;
    private final SubscriptionPersistence subscriptionPersistence;
    private final SubscriptionEventResponseMapper subscriptionEventResponseMapper;
    private final SubscriptionEventResponseOutcome subscriptionEventResponseOutcome;

    @Value("${notification.enabled:true}")
    private boolean notificationFeatureEnabled;

    @Value("${ncmp.model-loader.subscription:false}")
    private boolean subscriptionModelLoaderEnabled;

    /**
     * Consume subscription response event.
     *
     * @param subscriptionEventResponseConsumerRecord the event to be consumed
     */
    @KafkaListener(topics = "${app.ncmp.avc.subscription-response-topic}",
        properties = {"spring.json.value.default.type=org.onap.cps.ncmp.api.models.SubscriptionEventResponse"})
    public void consumeSubscriptionEventResponse(
            final ConsumerRecord<String, SubscriptionEventResponse> subscriptionEventResponseConsumerRecord) {
        final SubscriptionEventResponse subscriptionEventResponse = subscriptionEventResponseConsumerRecord.value();
        log.info("subscription event response of clientId: {} is received.", subscriptionEventResponse.getClientId());
        final String clientId = subscriptionEventResponse.getClientId();
        final String subscriptionName = subscriptionEventResponse.getSubscriptionName();
        final String subscriptionEventId = subscriptionEventResponse.getClientId()
            + subscriptionEventResponse.getSubscriptionName();
        boolean isFullOutcomeResponse = false;
        if (forwardedSubscriptionEventCache.containsKey(subscriptionEventId)) {
            final Set<String> dmiNames = forwardedSubscriptionEventCache.get(subscriptionEventId);

            dmiNames.remove(subscriptionEventResponse.getDmiName());
            forwardedSubscriptionEventCache.put(subscriptionEventId, dmiNames);
            isFullOutcomeResponse = forwardedSubscriptionEventCache.get(subscriptionEventId).isEmpty();

            if (isFullOutcomeResponse) {
                forwardedSubscriptionEventCache.remove(subscriptionEventId);
            }
        }
        if (subscriptionModelLoaderEnabled) {
            updateSubscriptionEvent(subscriptionEventResponse);
        }
        if (notificationFeatureEnabled) {
            subscriptionEventResponseOutcome.generateAndSendResponse(clientId, subscriptionName,
                    isFullOutcomeResponse);
        }
    }

    private void updateSubscriptionEvent(final SubscriptionEventResponse subscriptionEventResponse) {
        final YangModelSubscriptionEvent yangModelSubscriptionEvent =
                subscriptionEventResponseMapper
                        .toYangModelSubscriptionEvent(subscriptionEventResponse);
        subscriptionPersistence.saveSubscriptionEvent(yangModelSubscriptionEvent);
    }
}