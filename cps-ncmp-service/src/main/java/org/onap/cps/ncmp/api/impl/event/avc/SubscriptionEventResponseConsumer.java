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
import java.util.concurrent.LinkedBlockingDeque;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.models.SubscriptionEventResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@EnableScheduling
public class SubscriptionEventResponseConsumer {

    private final IMap<String, Set<String>> forwardedSubscriptionEventCache;


    @Value("${app.ncmp.avc.subscription-outcome-topic}")
    private String subscriptionOutcomeEventTopic;
    private final LinkedBlockingDeque<SubscriptionEventResponse> consumedSubscriptionEventResponses =
        new LinkedBlockingDeque<>();

    /**
     * Consume subscription response event.
     *
     * @param subscriptionEventResponse the event to be consumed
     */
    @KafkaListener(topics = "${app.ncmp.avc.subscription-response-topic}",
        properties = {"spring.json.value.default.type=org.onap.cps.ncmp.api.models.SubscriptionEventResponse"})
    public void consumeSubscriptionEventResponse(final SubscriptionEventResponse subscriptionEventResponse) {
        log.info("subscription event response of clientId: {} is received.", subscriptionEventResponse.getClientId());
        final String subscriptionEventId = subscriptionEventResponse.getClientId()
            + subscriptionEventResponse.getSubscriptionName();
        final boolean createOutcomeResponse;
        if (forwardedSubscriptionEventCache.containsKey(subscriptionEventId)) {
            forwardedSubscriptionEventCache.get(subscriptionEventId).remove(subscriptionEventResponse.getDmiName());
            createOutcomeResponse = forwardedSubscriptionEventCache.get(subscriptionEventId).isEmpty();
            if (createOutcomeResponse) {
                forwardedSubscriptionEventCache.remove(subscriptionEventId);
            }
        } else {
            createOutcomeResponse = true;
        }
        updateSubscriptionEvent(subscriptionEventResponse);
        if (createOutcomeResponse) {
            log.info("placeholder to create full outcome response for subscriptionEventId: {}.", subscriptionEventId);
            //TODO Create outcome response
        }

        consumedSubscriptionEventResponses.push(subscriptionEventResponse);
    }

    private void updateSubscriptionEvent(final SubscriptionEventResponse subscriptionEventResponse) {
        log.info("placeholder to update persisted subscription for subscriptionEventId: {}.",
            subscriptionEventResponse.getClientId() + subscriptionEventResponse.getSubscriptionName());
    }
}