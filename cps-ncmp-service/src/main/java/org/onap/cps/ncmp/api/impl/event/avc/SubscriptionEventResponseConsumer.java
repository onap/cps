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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.models.SubscriptionEventOutcome;
import org.onap.cps.ncmp.api.models.SubscriptionEventOutcomeStatus;
import org.onap.cps.ncmp.api.models.SubscriptionEventResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionEventResponseConsumer {

    @Value("${app.ncmp.avc.subscription-outcome-topic}")
    private String subscriptionOutcomeEventTopic;
    private final KafkaTemplate<String, SubscriptionEventOutcome> kafkaTemplate;

    /**
     * Consume subscription response event.
     *
     * @param subscriptionEventResponse the event to be consumed
     */
    @KafkaListener(topics = "${app.ncmp.avc.subscription-response-topic}",
            properties = {"spring.json.value.default.type=org.onap.cps.ncmp.api.models.SubscriptionEventResponse"})
    public void consumeSubscriptionEventResponse(final SubscriptionEventResponse subscriptionEventResponse) {
        log.info("subscription event response of clientId: {} is received.", subscriptionEventResponse.getClientId());
        //TODO Need to implement logic to wait until timeframe (default 30 sec) and then send response
        final ScheduledExecutorService executorService =
                Executors.newSingleThreadScheduledExecutor();
        executorService.schedule(this::publishSubscriptionEventResponse, 30, TimeUnit.SECONDS);
    }

    /**
     * Sends subscription event outcome message to the configured topic.
     */
    public void publishSubscriptionEventResponse() {
        final SubscriptionEventOutcome subscriptionEventOutcome = new SubscriptionEventOutcome();
        //TODO calculation need to done here and populate below fields :
        // status, pendingCmHandleIds, declinedCmHandleIds or unRegisteredCmHandleIds
        subscriptionEventOutcome.setStatus(SubscriptionEventOutcomeStatus.COMPLETED);
        log.info("publishing subscription outcome event to topic {} ", subscriptionOutcomeEventTopic);
        kafkaTemplate.send(subscriptionOutcomeEventTopic, subscriptionEventOutcome);
    }
}
