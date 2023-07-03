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
import org.onap.cps.ncmp.api.impl.utils.CloudEventHelper;
import org.onap.cps.ncmp.api.impl.utils.DmiServiceNameOrganizer;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.events.avcsubscription1_0_0.client_to_ncmp.SubscriptionEvent;
import org.onap.cps.ncmp.events.avcsubscription1_0_0.ncmp_to_dmi.CmHandle;
import org.onap.cps.spi.exceptions.OperationNotYetSupportedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
@Slf4j
@RequiredArgsConstructor
public class SubscriptionEventForwarder {

    private final InventoryPersistence inventoryPersistence;
    private final EventsPublisher<CloudEvent> eventsPublisher;
    private final IMap<String, Set<String>> forwardedSubscriptionEventCache;
    private final SubscriptionEventResponseOutcome subscriptionEventResponseOutcome;
    private final SubscriptionEventMapper subscriptionEventMapper;
    private final ClientSubscriptionEventToNcmpSubscriptionEventMapper
            clientSubscriptionEventToNcmpSubscriptionEventMapper;
    private final SubscriptionPersistence subscriptionPersistence;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    @Value("${app.ncmp.avc.subscription-forward-topic-prefix}")
    private String dmiAvcSubscriptionTopicPrefix;

    @Value("${ncmp.timers.subscription-forwarding.dmi-response-timeout-ms:30000}")
    private int dmiResponseTimeoutInMs;

    /**
     * Forward subscription event.
     *
     * @param subscriptionEvent the event to be forwarded
     */
    public void forwardCreateSubscriptionEvent(final SubscriptionEvent subscriptionEvent) {
        final List<String> cmHandleTargets = subscriptionEvent.getData().getPredicates().getTargets();
        if (cmHandleTargets == null || cmHandleTargets.isEmpty()
                || cmHandleTargets.stream().anyMatch(id -> (id).contains("*"))) {
            throw new OperationNotYetSupportedException(
                    "CMHandle targets are required. \"Wildcard\" operations are not yet supported");
        }
        final Collection<YangModelCmHandle> yangModelCmHandles =
                inventoryPersistence.getYangModelCmHandles(cmHandleTargets);
        final Map<String, Map<String, Map<String, String>>> dmiPropertiesPerCmHandleIdPerServiceName
                = DmiServiceNameOrganizer.getDmiPropertiesPerCmHandleIdPerServiceName(yangModelCmHandles);
        findDmisAndRespond(subscriptionEvent, cmHandleTargets, dmiPropertiesPerCmHandleIdPerServiceName);
    }

    private void findDmisAndRespond(final SubscriptionEvent subscriptionEvent,
                                    final List<String> cmHandleTargetsAsStrings,
                           final Map<String, Map<String, Map<String, String>>>
                                            dmiPropertiesPerCmHandleIdPerServiceName) {
        final List<String> cmHandlesThatExistsInDb = dmiPropertiesPerCmHandleIdPerServiceName.entrySet().stream()
                .map(Map.Entry::getValue).map(Map::keySet).flatMap(Set::stream).collect(Collectors.toList());

        final List<String> targetCmHandlesDoesNotExistInDb = new ArrayList<>(cmHandleTargetsAsStrings);
        targetCmHandlesDoesNotExistInDb.removeAll(cmHandlesThatExistsInDb);

        final Set<String> dmisToRespond = new HashSet<>(dmiPropertiesPerCmHandleIdPerServiceName.keySet());

        if (dmisToRespond.isEmpty() || !targetCmHandlesDoesNotExistInDb.isEmpty()) {
            updatesCmHandlesToRejectedAndPersistSubscriptionEvent(subscriptionEvent, targetCmHandlesDoesNotExistInDb);
        }
        if (dmisToRespond.isEmpty()) {
            final String clientID = subscriptionEvent.getData().getSubscription().getClientID();
            final String subscriptionName = subscriptionEvent.getData().getSubscription().getName();
            subscriptionEventResponseOutcome.sendResponse(clientID, subscriptionName);
        } else {
            startResponseTimeout(subscriptionEvent, dmisToRespond);
            final org.onap.cps.ncmp.events.avcsubscription1_0_0.ncmp_to_dmi.SubscriptionEvent ncmpSubscriptionEvent =
                    clientSubscriptionEventToNcmpSubscriptionEventMapper.toNcmpSubscriptionEvent(subscriptionEvent);
            forwardEventToDmis(dmiPropertiesPerCmHandleIdPerServiceName, ncmpSubscriptionEvent);
        }
    }

    private void startResponseTimeout(final SubscriptionEvent subscriptionEvent, final Set<String> dmisToRespond) {
        final String subscriptionClientId = subscriptionEvent.getData().getSubscription().getClientID();
        final String subscriptionName = subscriptionEvent.getData().getSubscription().getName();
        final String subscriptionEventId = subscriptionClientId + subscriptionName;

        forwardedSubscriptionEventCache.put(subscriptionEventId, dmisToRespond,
                ForwardedSubscriptionEventCacheConfig.SUBSCRIPTION_FORWARD_STARTED_TTL_SECS, TimeUnit.SECONDS);
        final ResponseTimeoutTask responseTimeoutTask =
            new ResponseTimeoutTask(forwardedSubscriptionEventCache, subscriptionEventResponseOutcome,
                    subscriptionClientId, subscriptionName);
        try {
            executorService.schedule(responseTimeoutTask, dmiResponseTimeoutInMs, TimeUnit.MILLISECONDS);
        } catch (final RuntimeException ex) {
            log.info("Caught exception in ScheduledExecutorService for ResponseTimeoutTask. StackTrace: {}",
                    ex.toString());
        }
    }

    private void forwardEventToDmis(final Map<String, Map<String, Map<String, String>>> dmiNameCmHandleMap,
                                    final org.onap.cps.ncmp.events.avcsubscription1_0_0.ncmp_to_dmi.SubscriptionEvent
                                            ncmpSubscriptionEvent) {
        dmiNameCmHandleMap.forEach((dmiName, cmHandlePropertiesMap) -> {
            final List<CmHandle> cmHandleList = cmHandlePropertiesMap.entrySet().stream().map(etnry -> {
                final CmHandle cmHandle = new CmHandle();
                cmHandle.setId(etnry.getKey());
                cmHandle.setAdditionalProperties(etnry.getValue());
                return cmHandle;
            }).collect(Collectors.toList());

            ncmpSubscriptionEvent.getData().getPredicates().setTargets(cmHandleList);
            final String eventKey = createEventKey(ncmpSubscriptionEvent, dmiName);
            final String dmiAvcSubscriptionTopic = dmiAvcSubscriptionTopicPrefix + dmiName;

            final CloudEvent ncmpSubscriptionCloudEvent = CloudEventHelper.toCloudEvent(ncmpSubscriptionEvent,
                    eventKey);
            eventsPublisher.publishCloudEvent(dmiAvcSubscriptionTopic, eventKey, ncmpSubscriptionCloudEvent);
        });
    }

    private String createEventKey(
            final org.onap.cps.ncmp.events.avcsubscription1_0_0.ncmp_to_dmi.SubscriptionEvent subscriptionEvent,
            final String dmiName) {
        return subscriptionEvent.getData().getSubscription().getClientID()
            + "-"
            + subscriptionEvent.getData().getSubscription().getName()
            + "-"
            + dmiName;
    }

    private void updatesCmHandlesToRejectedAndPersistSubscriptionEvent(
            final SubscriptionEvent subscriptionEvent,
            final List<String> targetCmHandlesDoesNotExistInDb) {
        final YangModelSubscriptionEvent yangModelSubscriptionEvent =
                subscriptionEventMapper.toYangModelSubscriptionEvent(subscriptionEvent);
        yangModelSubscriptionEvent.getPredicates()
                .setTargetCmHandles(findRejectedCmHandles(targetCmHandlesDoesNotExistInDb,
                        yangModelSubscriptionEvent));
        subscriptionPersistence.saveSubscriptionEvent(yangModelSubscriptionEvent);
    }

    private static List<YangModelSubscriptionEvent.TargetCmHandle> findRejectedCmHandles(
            final List<String> targetCmHandlesDoesNotExistInDb,
            final YangModelSubscriptionEvent yangModelSubscriptionEvent) {
        return yangModelSubscriptionEvent.getPredicates().getTargetCmHandles().stream()
                    .filter(targetCmHandle -> targetCmHandlesDoesNotExistInDb.contains(targetCmHandle.getCmHandleId()))
                    .map(target -> new YangModelSubscriptionEvent.TargetCmHandle(target.getCmHandleId(),
                                    SubscriptionStatus.REJECTED)).collect(Collectors.toList());
    }
}
