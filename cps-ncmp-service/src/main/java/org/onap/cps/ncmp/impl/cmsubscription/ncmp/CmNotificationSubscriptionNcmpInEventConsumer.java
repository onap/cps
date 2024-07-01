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

package org.onap.cps.ncmp.impl.cmsubscription.ncmp;

import static org.onap.cps.ncmp.utils.events.CloudEventMapper.toTargetEvent;

import io.cloudevents.CloudEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.client_to_ncmp.CmNotificationSubscriptionNcmpInEvent;
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.client_to_ncmp.Predicate;
import org.onap.cps.ncmp.impl.cmsubscription.service.CmNotificationSubscriptionHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class CmNotificationSubscriptionNcmpInEventConsumer {

    private final CmNotificationSubscriptionHandler cmNotificationSubscriptionHandler;

    /**
     * Consume the specified event.
     *
     * @param subscriptionEventConsumerRecord the event to be consumed
     */
    @KafkaListener(topics = "${app.ncmp.avc.cm-subscription-ncmp-in}",
            containerFactory = "cloudEventConcurrentKafkaListenerContainerFactory")
    public void consumeSubscriptionEvent(final ConsumerRecord<String, CloudEvent> subscriptionEventConsumerRecord) {
        final CloudEvent cloudEvent = subscriptionEventConsumerRecord.value();
        final CmNotificationSubscriptionNcmpInEvent cmNotificationSubscriptionNcmpInEvent =
                toTargetEvent(cloudEvent, CmNotificationSubscriptionNcmpInEvent.class);
        log.info("Subscription with name {} to be mapped to hazelcast object...",
                cmNotificationSubscriptionNcmpInEvent.getData().getSubscriptionId());

        final String subscriptionId = cmNotificationSubscriptionNcmpInEvent.getData().getSubscriptionId();
        final List<Predicate> predicates = cmNotificationSubscriptionNcmpInEvent.getData().getPredicates();
        if ("subscriptionCreateRequest".equals(cloudEvent.getType())) {
            log.info("Subscription create request for source {} with subscription id {} ...",
                    cloudEvent.getSource(), subscriptionId);
            cmNotificationSubscriptionHandler.processSubscriptionCreateRequest(subscriptionId, predicates);
        }
        if ("subscriptionDeleteRequest".equals(cloudEvent.getType())) {
            log.info("Subscription delete request for source {} with subscription id {} ...",
                    cloudEvent.getSource(), subscriptionId);
            cmNotificationSubscriptionHandler.processSubscriptionDeleteRequest(subscriptionId, predicates);
        }
    }
}
