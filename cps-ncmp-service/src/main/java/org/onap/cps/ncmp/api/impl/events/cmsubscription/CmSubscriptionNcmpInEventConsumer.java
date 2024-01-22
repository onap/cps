/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

import static org.onap.cps.ncmp.api.impl.events.mapper.CloudEventMapper.toTargetEvent;

import io.cloudevents.CloudEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.onap.cps.ncmp.events.cmsubscription_merge1_0_0.client_to_ncmp.CmSubscriptionNcmpInEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CmSubscriptionNcmpInEventConsumer {

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
        final CmSubscriptionNcmpInEvent cmSubscriptionNcmpInEvent =
            toTargetEvent(cloudEvent, CmSubscriptionNcmpInEvent.class);
        if (subscriptionModelLoaderEnabled) {
            log.info("Subscription with name {} to be mapped to hazelcast object...",
                cmSubscriptionNcmpInEvent.getData().getSubscriptionId());
        }
        if ("subscriptionCreated".equals(cloudEvent.getType()) && cmSubscriptionNcmpInEvent != null) {
            log.info("Subscription for ClientID {} with name {} ...",
                cloudEvent.getSource(),
                cmSubscriptionNcmpInEvent.getData().getSubscriptionId());
        }
    }
}
