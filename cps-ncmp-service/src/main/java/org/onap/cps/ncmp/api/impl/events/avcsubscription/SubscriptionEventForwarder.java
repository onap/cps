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

package org.onap.cps.ncmp.api.impl.events.avcsubscription;

import com.hazelcast.map.IMap;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.event.avc.ResponseTimeoutTask;
import org.onap.cps.ncmp.api.impl.events.EventsPublisher;
import org.onap.cps.ncmp.api.impl.operations.RequiredDmiService;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.event.model.SubscriptionEvent;
import org.onap.cps.spi.exceptions.OperationNotYetSupportedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionEventForwarder {

    private final InventoryPersistence inventoryPersistence;
    private final EventsPublisher<SubscriptionEvent> eventsPublisher;
    private final IMap<String, Set<String>> forwardedSubscriptionEventCache;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    private static final String DMI_AVC_SUBSCRIPTION_TOPIC_PREFIX = "ncmp-dmi-cm-avc-subscription-";

    @Value("${ncmp.timers.subscription-forwarding.dmi-response-timeout-ms:30000}")
    private int dmiResponseTimeoutInMs;

    /**
     * Forward subscription event.
     *
     * @param subscriptionEvent the event to be forwarded
     */
    public void forwardCreateSubscriptionEvent(final SubscriptionEvent subscriptionEvent) {
        final List<Object> cmHandleTargets = subscriptionEvent.getEvent().getPredicates().getTargets();

        if (cmHandleTargets == null || cmHandleTargets.isEmpty()
            || cmHandleTargets.stream().anyMatch(id -> ((String) id).contains("*"))) {
            throw new OperationNotYetSupportedException(
                "CMHandle targets are required. \"Wildcard\" operations are not yet supported");
        }

        final Map<String, Map<String, Map<String, String>>> dmiNameCmHandleMap =
            organizeByDmiName(cmHandleTargets);
        final Set<String> dmisToRespond = new HashSet<>(dmiNameCmHandleMap.keySet());
        startResponseTimeout(subscriptionEvent, dmisToRespond);
        forwardEventToDmis(dmiNameCmHandleMap, subscriptionEvent);
    }

    private void forwardEventToDmis(final Map<String, Map<String, Map<String, String>>> dmiNameCmHandleMap,
                                    final SubscriptionEvent subscriptionEvent) {
        dmiNameCmHandleMap.forEach((dmiName, cmHandlePropertiesMap) -> {
            subscriptionEvent.getEvent().getPredicates().setTargets(Collections.singletonList(cmHandlePropertiesMap));
            final String eventKey = createEventKey(subscriptionEvent, dmiName);
            eventsPublisher.publishEvent(
                DMI_AVC_SUBSCRIPTION_TOPIC_PREFIX + dmiName, eventKey, subscriptionEvent);
        });
    }

    private void startResponseTimeout(final SubscriptionEvent subscriptionEvent, final Set<String> dmisToRespond) {
        final String subscriptionEventId = subscriptionEvent.getEvent().getSubscription().getClientID()
            + subscriptionEvent.getEvent().getSubscription().getName();

        forwardedSubscriptionEventCache.put(subscriptionEventId, dmisToRespond);
        final ResponseTimeoutTask responseTimeoutTask =
            new ResponseTimeoutTask(forwardedSubscriptionEventCache, subscriptionEventId);
        executorService.schedule(responseTimeoutTask, dmiResponseTimeoutInMs, TimeUnit.MILLISECONDS);
    }

    private Map<String, Map<String, Map<String, String>>> organizeByDmiName(final List<Object> cmHandleTargets) {

        final List<String> cmHandleTargetsAsStrings = cmHandleTargets.stream().map(
            Objects::toString).collect(Collectors.toList());
        final Collection<YangModelCmHandle> yangModelCmHandles =
            inventoryPersistence.getYangModelCmHandles(cmHandleTargetsAsStrings);

        final Map<String, Map<String, Map<String, String>>> dmiNameCmHandlePropertiesMap = new HashMap<>();
        yangModelCmHandles.forEach(cmHandle -> {
            final String dmiName = cmHandle.resolveDmiServiceName(RequiredDmiService.DATA);
            if (!dmiNameCmHandlePropertiesMap.containsKey(dmiName)) {
                final Map<String, Map<String, String>> cmHandleDmiPropertiesMap = new HashMap<>();
                cmHandleDmiPropertiesMap.put(cmHandle.getId(), dmiPropertiesAsMap(cmHandle));
                dmiNameCmHandlePropertiesMap.put(cmHandle.getDmiDataServiceName(), cmHandleDmiPropertiesMap);
            } else {
                dmiNameCmHandlePropertiesMap.get(cmHandle.getDmiDataServiceName())
                    .put(cmHandle.getId(), dmiPropertiesAsMap(cmHandle));
            }
        });
        return dmiNameCmHandlePropertiesMap;
    }

    private String createEventKey(final SubscriptionEvent subscriptionEvent, final String dmiName) {
        return subscriptionEvent.getEvent().getSubscription().getClientID()
            + "-"
            + subscriptionEvent.getEvent().getSubscription().getName()
            + "-"
            + dmiName;
    }

    private Map<String, String> dmiPropertiesAsMap(final YangModelCmHandle yangModelCmHandle) {
        return yangModelCmHandle.getDmiProperties().stream().collect(
            Collectors.toMap(YangModelCmHandle.Property::getName, YangModelCmHandle.Property::getValue));
    }

}