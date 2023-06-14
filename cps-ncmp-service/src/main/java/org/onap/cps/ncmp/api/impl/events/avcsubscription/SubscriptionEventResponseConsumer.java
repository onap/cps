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

package org.onap.cps.ncmp.api.impl.events.avcsubscription;

import com.hazelcast.map.IMap;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.onap.cps.ncmp.api.impl.config.embeddedcache.ForwardedSubscriptionEventCacheConfig;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionPersistence;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionStatus;
import org.onap.cps.ncmp.api.impl.utils.DataNodeHelper;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent;
import org.onap.cps.ncmp.api.models.SubscriptionEventResponse;
import org.onap.cps.spi.model.DataNode;
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
        final String clientId = subscriptionEventResponse.getClientId();
        log.info("subscription event response of clientId: {} is received.", clientId);
        final String subscriptionName = subscriptionEventResponse.getSubscriptionName();
        final String subscriptionEventId = clientId + subscriptionName;
        boolean createOutcomeResponse = false;
        if (forwardedSubscriptionEventCache.containsKey(subscriptionEventId)) {
            final Set<String> dmiNames = forwardedSubscriptionEventCache.get(subscriptionEventId);

            dmiNames.remove(subscriptionEventResponse.getDmiName());
            forwardedSubscriptionEventCache.put(subscriptionEventId, dmiNames,
                    ForwardedSubscriptionEventCacheConfig.SUBSCRIPTION_FORWARD_STARTED_TTL_SECS, TimeUnit.SECONDS);
            createOutcomeResponse = forwardedSubscriptionEventCache.get(subscriptionEventId).isEmpty();
        }
        if (subscriptionModelLoaderEnabled) {
            updateSubscriptionEvent(subscriptionEventResponse);
        }
        if (createOutcomeResponse
                && notificationFeatureEnabled
                && hasNoPendingCmHandles(clientId, subscriptionName)) {
            subscriptionEventResponseOutcome.sendResponse(clientId, subscriptionName);
            forwardedSubscriptionEventCache.remove(subscriptionEventId);
        }
    }

    private boolean hasNoPendingCmHandles(final String clientId, final String subscriptionName) {
        final Collection<DataNode> dataNodeSubscription = subscriptionPersistence.getCmHandlesForSubscriptionEvent(
                clientId, subscriptionName);
        final Map<String, SubscriptionStatus> cmHandleIdToStatusMap =
                DataNodeHelper.getCmHandleIdToStatusMapFromDataNodes(
                dataNodeSubscription);
        return !cmHandleIdToStatusMap.values().contains(SubscriptionStatus.PENDING);
    }

    private void updateSubscriptionEvent(final SubscriptionEventResponse subscriptionEventResponse) {
        final YangModelSubscriptionEvent yangModelSubscriptionEvent =
                subscriptionEventResponseMapper
                        .toYangModelSubscriptionEvent(subscriptionEventResponse);
        subscriptionPersistence.saveSubscriptionEvent(yangModelSubscriptionEvent);
    }
}