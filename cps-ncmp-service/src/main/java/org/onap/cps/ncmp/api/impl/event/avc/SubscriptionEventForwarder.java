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

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.operations.RequiredDmiService;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.event.model.SubscriptionEvent;
import org.onap.cps.spi.exceptions.OperationNotYetSupportedException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;


@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionEventForwarder {

    private final InventoryPersistence inventoryPersistence;
    private final KafkaTemplate<String, SubscriptionEvent> subscriptionEventKafkaTemplate;

    private static final String DMI_AVC_SUBSCRIPTION_TOPIC = "ncmp-dmi-cm-avc-subscription-";

    /**
     * Forward subscription event.
     *
     * @param subscriptionEvent the event to be forwarded
     */
    public void forwardCreateSubscriptionEvent(final SubscriptionEvent subscriptionEvent) {
        if (subscriptionEvent.getEvent().getPredicates().getTargets() == null
            || subscriptionEvent.getEvent().getPredicates().getTargets().isEmpty()) {
            throw new OperationNotYetSupportedException(
                "CMHandle targets are required. \"Wildcard\" operations are not yet supported");
        }
        final List<String> cmHandleTargets = subscriptionEvent.getEvent().getPredicates().getTargets().stream().map(
            Objects::toString).collect(Collectors.toList());
        final Collection<YangModelCmHandle> cmHandles = inventoryPersistence.getYangModelCmHandles(cmHandleTargets);
        final Map<String, Map<String, Map<String, Serializable>>> dmiNameCmHandleMap = organizeByDmiName(cmHandles);
        dmiNameCmHandleMap.forEach((dmiName, cmHandlePropertiesMap) -> {
            subscriptionEvent.getEvent().getPredicates().setTargets(Collections.singletonList(cmHandlePropertiesMap));
            final String eventKey = subscriptionEvent.getEvent().getSubscription().getClientID() + "-"
                + subscriptionEvent.getEvent().getSubscription().getName() + "-" + dmiName;
            publishEvent(DMI_AVC_SUBSCRIPTION_TOPIC + dmiName, eventKey, subscriptionEvent);
        });
    }

    private void publishEvent(final String topicName, final String eventKey,
                              final SubscriptionEvent subscriptionEvent) {
        final ListenableFuture<SendResult<String, SubscriptionEvent>> subscriptionEventFuture =
            subscriptionEventKafkaTemplate.send(topicName, eventKey, subscriptionEvent);

        subscriptionEventFuture.addCallback(new ListenableFutureCallback<>() {
            @Override
            public void onFailure(final Throwable throwable) {
                log.error("Unable to publish event to topic : {} due to {}", topicName, throwable.getMessage());
            }

            @Override
            public void onSuccess(final SendResult<String, SubscriptionEvent> sendResult) {
                log.debug("Successfully published event to topic : {} , SubscriptionEvent : {}",
                    sendResult.getRecordMetadata().topic(), sendResult.getProducerRecord().value());
            }
        });
    }

    private Map<String, Map<String, Map<String, Serializable>>> organizeByDmiName(
        final Collection<YangModelCmHandle> cmHandles) {
        final Map<String, Map<String, Map<String, Serializable>>> dmiNameCmHandlePropertiesMap = new HashMap<>();
        cmHandles.forEach(cmHandle -> {
            final String dmiName = cmHandle.resolveDmiServiceName(RequiredDmiService.DATA);
            if (!dmiNameCmHandlePropertiesMap.containsKey(dmiName)) {
                dmiNameCmHandlePropertiesMap.put(cmHandle.getDmiDataServiceName(), new HashMap<>() {
                    {
                        put(cmHandle.getId(), cmHandle.dmiPropertiesAsMap());
                    }
                });
            } else {
                dmiNameCmHandlePropertiesMap.get(cmHandle.getDmiDataServiceName())
                    .put(cmHandle.getId(), cmHandle.dmiPropertiesAsMap());
            }
        });
        return dmiNameCmHandlePropertiesMap;
    }

}
