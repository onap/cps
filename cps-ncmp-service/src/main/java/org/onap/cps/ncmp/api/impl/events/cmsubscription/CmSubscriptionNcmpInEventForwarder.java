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

package org.onap.cps.ncmp.api.impl.events.cmsubscription;

import com.hazelcast.map.IMap;
import io.cloudevents.CloudEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.config.embeddedcache.ForwardedSubscriptionEventCacheConfig;
import org.onap.cps.ncmp.api.impl.events.EventsPublisher;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionPersistence;
import org.onap.cps.ncmp.api.impl.subscriptions.SubscriptionStatus;
import org.onap.cps.ncmp.api.impl.utils.CmSubscriptionEventCloudMapper;
import org.onap.cps.ncmp.api.impl.utils.DmiServiceNameOrganizer;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.models.CmSubscriptionEvent;
import org.onap.cps.ncmp.events.cmsubscription1_0_0.client_to_ncmp.CmSubscriptionNcmpInEvent;
import org.onap.cps.ncmp.events.cmsubscription1_0_0.ncmp_to_dmi.CmHandle;
import org.onap.cps.ncmp.events.cmsubscription1_0_0.ncmp_to_dmi.CmSubscriptionDmiInEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class CmSubscriptionNcmpInEventForwarder {

    private final InventoryPersistence inventoryPersistence;
    private final EventsPublisher<CloudEvent> eventsPublisher;
    private final IMap<String, Set<String>> forwardedSubscriptionEventCache;
    private final CmSubscriptionNcmpOutEventPublisher cmSubscriptionNcmpOutEventPublisher;
    private final CmSubscriptionNcmpInEventMapper cmSubscriptionNcmpInEventMapper;
    private final CmSubscriptionEventCloudMapper cmSubscriptionEventCloudMapper;
    private final CmSubscriptionNcmpInEventToCmSubscriptionDmiInEventMapper
            cmSubscriptionNcmpInEventToCmSubscriptionDmiInEventMapper;
    private final SubscriptionPersistence subscriptionPersistence;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    @Value("${app.ncmp.avc.subscription-forward-topic-prefix}")
    private String dmiAvcSubscriptionTopicPrefix;

    @Value("${ncmp.timers.subscription-forwarding.dmi-response-timeout-ms:30000}")
    private int dmiResponseTimeoutInMs;

    /**
     * Forward subscription event.
     *
     * @param cmSubscriptionNcmpInEvent the event to be forwarded
     */
    public void forwardCreateSubscriptionEvent(final CmSubscriptionNcmpInEvent cmSubscriptionNcmpInEvent,
            final String eventType) {
        final List<String> cmHandleTargets = cmSubscriptionNcmpInEvent.getData().getPredicates().getTargets();
        if (cmHandleTargets == null || cmHandleTargets.isEmpty() || cmHandleTargets.stream()
                .anyMatch(id -> (id).contains("*"))) {
            throw new UnsupportedOperationException(
                    "CMHandle targets are required. \"Wildcard\" operations are not yet supported");
        }
        final Collection<YangModelCmHandle> yangModelCmHandles =
                inventoryPersistence.getYangModelCmHandles(cmHandleTargets);
        final Map<String, Map<String, Map<String, String>>> dmiPropertiesPerCmHandleIdPerServiceName =
                DmiServiceNameOrganizer.getDmiPropertiesPerCmHandleIdPerServiceName(yangModelCmHandles);
        findDmisAndRespond(cmSubscriptionNcmpInEvent, eventType, cmHandleTargets,
                dmiPropertiesPerCmHandleIdPerServiceName);
    }

    private void findDmisAndRespond(final CmSubscriptionNcmpInEvent cmSubscriptionNcmpInEvent, final String eventType,
            final List<String> cmHandleTargetsAsStrings,
            final Map<String, Map<String, Map<String, String>>> dmiPropertiesPerCmHandleIdPerServiceName) {

        final CmSubscriptionEvent cmSubscriptionEvent = new CmSubscriptionEvent();
        cmSubscriptionEvent.setSubscriptionName(cmSubscriptionNcmpInEvent.getData().getSubscription().getName());
        cmSubscriptionEvent.setClientId(cmSubscriptionNcmpInEvent.getData().getSubscription().getClientID());

        final List<String> cmHandlesThatExistsInDb =
                dmiPropertiesPerCmHandleIdPerServiceName.entrySet().stream().map(Map.Entry::getValue).map(Map::keySet)
                        .flatMap(Set::stream).collect(Collectors.toList());

        final List<String> targetCmHandlesDoesNotExistInDb = new ArrayList<>(cmHandleTargetsAsStrings);
        targetCmHandlesDoesNotExistInDb.removeAll(cmHandlesThatExistsInDb);

        final Set<String> dmisToRespond = new HashSet<>(dmiPropertiesPerCmHandleIdPerServiceName.keySet());

        if (dmisToRespond.isEmpty() || !targetCmHandlesDoesNotExistInDb.isEmpty()) {
            updatesCmHandlesToRejectedAndPersistSubscriptionEvent(cmSubscriptionNcmpInEvent,
                    targetCmHandlesDoesNotExistInDb);
        }
        if (dmisToRespond.isEmpty()) {
            cmSubscriptionNcmpOutEventPublisher.sendResponse(cmSubscriptionEvent,
                    "subscriptionCreatedStatus");
        } else {
            startResponseTimeout(cmSubscriptionEvent, dmisToRespond);
            final CmSubscriptionDmiInEvent cmSubscriptionDmiInEvent =
                    cmSubscriptionNcmpInEventToCmSubscriptionDmiInEventMapper.toCmSubscriptionDmiInEvent(
                            cmSubscriptionNcmpInEvent);
            forwardEventToDmis(dmiPropertiesPerCmHandleIdPerServiceName, cmSubscriptionDmiInEvent, eventType);
        }
    }

    private void startResponseTimeout(final CmSubscriptionEvent cmSubscriptionEvent,
                                      final Set<String> dmisToRespond) {
        final String subscriptionClientId = cmSubscriptionEvent.getClientId();
        final String subscriptionName = cmSubscriptionEvent.getSubscriptionName();
        final String subscriptionEventId = subscriptionClientId + subscriptionName;

        forwardedSubscriptionEventCache.put(subscriptionEventId, dmisToRespond,
                ForwardedSubscriptionEventCacheConfig.SUBSCRIPTION_FORWARD_STARTED_TTL_SECS, TimeUnit.SECONDS);
        final ResponseTimeoutTask responseTimeoutTask =
            new ResponseTimeoutTask(forwardedSubscriptionEventCache, cmSubscriptionNcmpOutEventPublisher,
                    cmSubscriptionEvent);

        executorService.schedule(responseTimeoutTask, dmiResponseTimeoutInMs, TimeUnit.MILLISECONDS);
    }

    private void forwardEventToDmis(final Map<String, Map<String, Map<String, String>>> dmiNameCmHandleMap,
            final CmSubscriptionDmiInEvent cmSubscriptionDmiInEvent, final String eventType) {
        dmiNameCmHandleMap.forEach((dmiName, cmHandlePropertiesMap) -> {
            final List<CmHandle> cmHandleTargets = cmHandlePropertiesMap.entrySet().stream().map(
                    cmHandleAndProperties -> {
                        final CmHandle cmHandle = new CmHandle();
                        cmHandle.setId(cmHandleAndProperties.getKey());
                        cmHandle.setAdditionalProperties(cmHandleAndProperties.getValue());
                        return cmHandle;
                    }).collect(Collectors.toList());

            cmSubscriptionDmiInEvent.getData().getPredicates().setTargets(cmHandleTargets);
            final String eventKey = createEventKey(cmSubscriptionDmiInEvent, dmiName);
            final String dmiAvcSubscriptionTopic = dmiAvcSubscriptionTopicPrefix + dmiName;

            final CloudEvent cmSubscriptionDmiInCloudEvent =
                    cmSubscriptionEventCloudMapper.toCloudEvent(cmSubscriptionDmiInEvent, eventKey, eventType);
            eventsPublisher.publishCloudEvent(dmiAvcSubscriptionTopic, eventKey, cmSubscriptionDmiInCloudEvent);
        });
    }

    private String createEventKey(final CmSubscriptionDmiInEvent cmSubscriptionDmiInEvent, final String dmiName) {
        return cmSubscriptionDmiInEvent.getData().getSubscription().getClientID() + "-"
                       + cmSubscriptionDmiInEvent.getData().getSubscription().getName() + "-" + dmiName;
    }

    private void updatesCmHandlesToRejectedAndPersistSubscriptionEvent(
            final CmSubscriptionNcmpInEvent cmSubscriptionNcmpInEvent,
            final List<String> targetCmHandlesDoesNotExistInDb) {
        final YangModelSubscriptionEvent yangModelSubscriptionEvent =
                cmSubscriptionNcmpInEventMapper.toYangModelSubscriptionEvent(cmSubscriptionNcmpInEvent);
        yangModelSubscriptionEvent.getPredicates()
                .setTargetCmHandles(findRejectedCmHandles(targetCmHandlesDoesNotExistInDb, yangModelSubscriptionEvent));
        subscriptionPersistence.saveSubscriptionEvent(yangModelSubscriptionEvent);
    }

    private static List<YangModelSubscriptionEvent.TargetCmHandle> findRejectedCmHandles(
            final List<String> targetCmHandlesDoesNotExistInDb,
            final YangModelSubscriptionEvent yangModelSubscriptionEvent) {
        return yangModelSubscriptionEvent.getPredicates().getTargetCmHandles().stream()
                    .filter(targetCmHandle -> targetCmHandlesDoesNotExistInDb.contains(targetCmHandle.getCmHandleId()))
                    .map(target -> new YangModelSubscriptionEvent.TargetCmHandle(target.getCmHandleId(),
                                    SubscriptionStatus.REJECTED, "Targets not found"))
                .collect(Collectors.toList());
    }
}
