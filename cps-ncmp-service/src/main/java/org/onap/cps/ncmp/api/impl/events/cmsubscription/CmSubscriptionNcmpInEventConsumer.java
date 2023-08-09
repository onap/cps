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

package org.onap.cps.ncmp.api.impl.events.cmsubscription;

import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_OPERATIONAL;
import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_RUNNING;

import io.cloudevents.CloudEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionPersistence;
import org.onap.cps.ncmp.api.impl.utils.CmSubscriptionEventCloudMapper;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent;
import org.onap.cps.ncmp.events.cmsubscription1_0_0.client_to_ncmp.CmSubscriptionNcmpInEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class CmSubscriptionNcmpInEventConsumer {

    private final CmSubscriptionNcmpInEventForwarder cmSubscriptionNcmpInEventForwarder;
    private final CmSubscriptionNcmpInEventMapper cmSubscriptionNcmpInEventMapper;
    private final SubscriptionPersistence subscriptionPersistence;
    private final CmSubscriptionEventCloudMapper cmSubscriptionEventCloudMapper;

    @Value("${notification.enabled:true}")
    private boolean notificationFeatureEnabled;

    @Value("${ncmp.model-loader.subscription:false}")
    private boolean subscriptionModelLoaderEnabled;

    /**
     * Consume the specified event.
     *
     * @param subscriptionEventConsumerRecord the event to be consumed
     */
    @KafkaListener(topics = "${app.ncmp.avc.subscription-topic}",
            containerFactory = "cloudEventConcurrentKafkaListenerContainerFactory")
    public void consumeSubscriptionEvent(final ConsumerRecord<String, CloudEvent> subscriptionEventConsumerRecord) {
        final CloudEvent cloudEvent = subscriptionEventConsumerRecord.value();
        final String eventType = subscriptionEventConsumerRecord.value().getType();
        final CmSubscriptionNcmpInEvent cmSubscriptionNcmpInEvent =
                cmSubscriptionEventCloudMapper.toCmSubscriptionNcmpInEvent(cloudEvent);
        final String eventDatastore = cmSubscriptionNcmpInEvent.getData().getPredicates().getDatastore();
        if (!eventDatastore.equals(PASSTHROUGH_RUNNING.getDatastoreName()) || eventDatastore.equals(
                PASSTHROUGH_OPERATIONAL.getDatastoreName())) {
            throw new UnsupportedOperationException(
                    "passthrough datastores are currently only supported for event subscriptions");
        }
        if ("CM".equals(cmSubscriptionNcmpInEvent.getData().getDataType().getDataCategory())) {
            if (subscriptionModelLoaderEnabled) {
                persistSubscriptionEvent(cmSubscriptionNcmpInEvent);
            }
            if ("subscriptionCreated".equals(cloudEvent.getType())) {
                log.info("Subscription for ClientID {} with name {} ...",
                        cmSubscriptionNcmpInEvent.getData().getSubscription().getClientID(),
                        cmSubscriptionNcmpInEvent.getData().getSubscription().getName());
                if (notificationFeatureEnabled) {
                    cmSubscriptionNcmpInEventForwarder.forwardCreateSubscriptionEvent(cmSubscriptionNcmpInEvent,
                            eventType);
                }
            }
        } else {
            log.trace("Non-CM subscription event ignored");
        }
    }

    private void persistSubscriptionEvent(final CmSubscriptionNcmpInEvent cmSubscriptionNcmpInEvent) {
        final YangModelSubscriptionEvent yangModelSubscriptionEvent =
                cmSubscriptionNcmpInEventMapper.toYangModelSubscriptionEvent(cmSubscriptionNcmpInEvent);
        subscriptionPersistence.saveSubscriptionEvent(yangModelSubscriptionEvent);
    }

}
