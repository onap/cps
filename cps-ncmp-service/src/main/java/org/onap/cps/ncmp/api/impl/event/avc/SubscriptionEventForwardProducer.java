/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2023 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.event.avc;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.event.model.SubscriptionEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

/**
 * Producer for subscription forward event.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionEventForwardProducer {

    @Value("${app.ncmp.avc.subscription-forward-topic}")
    private String subscriptionForwardingEventTopic;
    private final KafkaTemplate<String, SubscriptionEvent> kafkaTemplate;

    /**
     * Sends message to the configured topic with a message key.
     *
     * @param forwardSubscriptionEvent message payload
     */
    public void forwardSubscriptionEventMessage(final SubscriptionEvent forwardSubscriptionEvent) {
        log.info("Forwarding subscription event {} to topic {} ",
                forwardSubscriptionEvent.getEvent().getSubscription().getClientID(), subscriptionForwardingEventTopic);
        kafkaTemplate.send(subscriptionForwardingEventTopic, forwardSubscriptionEvent);
    }
}
