/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.event.model.SubscriptionEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionEventConsumer {

    private final SubscriptionEventForwardProducer subscriptionEventForwardProducer;

    /**
     * Consume the specified event.
     *
     * @param subscriptionEvent the event to be consumed
     */
    @KafkaListener(topics = "${app.ncmp.avc.subscription-topic}",
            properties = {"spring.json.value.default.type=org.onap.cps.ncmp.event.model.SubscriptionEvent"})
    public void consumeSubscriptionEvent(final SubscriptionEvent subscriptionEvent) {
        if ("CM".equals(subscriptionEvent.getEvent().getDataType().getDataCategory())) {
            log.debug("Consuming event {} ...", subscriptionEvent);
            if ("CREATE".equals(subscriptionEvent.getEventType().value())) {
                log.info("Subscription for ClientID {} with name{} ...",
                        subscriptionEvent.getEvent().getSubscription().getClientID(),
                        subscriptionEvent.getEvent().getSubscription().getName());
                // TODO : Delete subscriptionEventForwardProducer along with line below
                //  As for testing purpose only we are forwarding subscription to dmi-plugin
                // Need to be removed once @Luke would deliver CPS-1431
                // (Forward Subscription Information to DMI Plugin(s))
                subscriptionEventForwardProducer.forwardSubscriptionEventMessage(subscriptionEvent);
            }
        } else {
            log.trace("Non-CM subscription event ignored");
        }
    }
}
