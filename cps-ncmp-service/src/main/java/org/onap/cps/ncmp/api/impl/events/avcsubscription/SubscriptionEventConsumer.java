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

package org.onap.cps.ncmp.api.impl.events.avcsubscription;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionPersistence;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent;
import org.onap.cps.ncmp.event.model.InnerSubscriptionEvent;
import org.onap.cps.ncmp.event.model.SubscriptionEvent;
import org.onap.cps.spi.exceptions.OperationNotYetSupportedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionEventConsumer {

    private final SubscriptionEventForwarder subscriptionEventForwarder;
    private final SubscriptionEventMapper subscriptionEventMapper;
    private final SubscriptionPersistence subscriptionPersistence;

    @Value("${notification.enabled:true}")
    private boolean notificationFeatureEnabled;

    @Value("${ncmp.model-loader.subscription:false}")
    private boolean subscriptionModelLoaderEnabled;

    /**
     * Consume the specified event.
     *
     * @param subscriptionEvent the event to be consumed
     */
    @KafkaListener(topics = "${app.ncmp.avc.subscription-topic}",
            properties = {"spring.json.value.default.type=org.onap.cps.ncmp.event.model.SubscriptionEvent"})
    public void consumeSubscriptionEvent(final SubscriptionEvent subscriptionEvent) {
        final InnerSubscriptionEvent event = subscriptionEvent.getEvent();
        final String eventDatastore = event.getPredicates().getDatastore();
        if (!(eventDatastore.equals("passthrough-running") || eventDatastore.equals("passthrough-operational"))) {
            throw new OperationNotYetSupportedException(
                "passthrough datastores are currently only supported for event subscriptions");
        }
        if ("CM".equals(event.getDataType().getDataCategory())) {
            log.debug("Consuming event {} ...", subscriptionEvent);
            if (subscriptionModelLoaderEnabled) {
                persistSubscriptionEvent(subscriptionEvent);
            }
            if ("CREATE".equals(subscriptionEvent.getEventType().value())) {
                log.info("Subscription for ClientID {} with name {} ...",
                        event.getSubscription().getClientID(),
                        event.getSubscription().getName());
                if (notificationFeatureEnabled) {
                    subscriptionEventForwarder.forwardCreateSubscriptionEvent(subscriptionEvent);
                }
            }
        } else {
            log.trace("Non-CM subscription event ignored");
        }
    }

    private void persistSubscriptionEvent(final SubscriptionEvent subscriptionEvent) {
        final YangModelSubscriptionEvent yangModelSubscriptionEvent =
            subscriptionEventMapper.toYangModelSubscriptionEvent(subscriptionEvent);
        subscriptionPersistence.saveSubscriptionEvent(yangModelSubscriptionEvent);
    }

}
