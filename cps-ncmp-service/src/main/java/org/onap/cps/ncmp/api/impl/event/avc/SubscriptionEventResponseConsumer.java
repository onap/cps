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

import java.util.concurrent.LinkedBlockingDeque;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.models.SubscriptionEventOutcome;
import org.onap.cps.ncmp.api.models.SubscriptionEventOutcomeStatus;
import org.onap.cps.ncmp.api.models.SubscriptionEventResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@EnableScheduling
public class SubscriptionEventResponseConsumer {

    @Value("${app.ncmp.avc.subscription-outcome-topic}")
    private String subscriptionOutcomeEventTopic;
    private final KafkaTemplate<String, SubscriptionEventOutcome> kafkaTemplate;
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
        consumedSubscriptionEventResponses.push(subscriptionEventResponse);
    }

    /**
     * Sends subscription event outcome message to the configured topic,
     * with a 30 seconds time interval.
     */
    @Scheduled(fixedDelay = 30000)
    public void publishSubscriptionEventResponse() {
        final SubscriptionEventOutcome subscriptionEventOutcome = new SubscriptionEventOutcome();
        subscriptionEventOutcome.setStatus(SubscriptionEventOutcomeStatus.COMPLETED);
        while (consumedSubscriptionEventResponses.size() > 0) {
            final SubscriptionEventResponse subscriptionEventResponse = consumedSubscriptionEventResponses.pop();
            final String status = subscriptionEventResponse.getStatus();
            if (status.equals("accepted")) {
                if (subscriptionEventOutcome.getAcceptedCmHandleIds() == null) {
                    subscriptionEventOutcome.setAcceptedCmHandleIds(subscriptionEventResponse.getCmHandleIds());
                } else {
                    subscriptionEventOutcome.getAcceptedCmHandleIds()
                            .addAll(subscriptionEventResponse.getCmHandleIds());
                }
            } else {
                if (subscriptionEventOutcome.getDeclinedCmHandleIds() == null) {
                    subscriptionEventOutcome.setDeclinedCmHandleIds(subscriptionEventResponse.getCmHandleIds());
                } else {
                    subscriptionEventOutcome.getDeclinedCmHandleIds()
                            .addAll(subscriptionEventResponse.getCmHandleIds());
                }
                subscriptionEventOutcome.setStatus(SubscriptionEventOutcomeStatus.PARTIALLY_COMPLETED);
            }
        }
        log.info("publishing subscription outcome event to topic {} ", subscriptionOutcomeEventTopic);
        kafkaTemplate.send(subscriptionOutcomeEventTopic, subscriptionEventOutcome);
    }
}
