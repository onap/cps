/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.datajobs.subscription.ncmp;

import static org.onap.cps.ncmp.impl.datajobs.subscription.models.CmSubscriptionStatus.REJECTED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.impl.datajobs.subscription.client_to_ncmp.DataSelector;
import org.onap.cps.ncmp.impl.datajobs.subscription.dmi.DmiInEventMapper;
import org.onap.cps.ncmp.impl.datajobs.subscription.dmi.EventProducer;
import org.onap.cps.ncmp.impl.datajobs.subscription.models.CmSubscriptionStatus;
import org.onap.cps.ncmp.impl.datajobs.subscription.ncmp_to_dmi.DataJobSubscriptionDmiInEvent;
import org.onap.cps.ncmp.impl.datajobs.subscription.utils.CmDataJobSubscriptionPersistenceService;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher;
import org.onap.cps.ncmp.impl.utils.JexParser;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true", matchIfMissing = true)
public class CmSubscriptionHandlerImpl implements CmSubscriptionHandler {

    private final CmDataJobSubscriptionPersistenceService cmDataJobSubscriptionPersistenceService;
    private final DmiInEventMapper dmiInEventMapper;
    private final EventProducer eventProducer;
    private final InventoryPersistence inventoryPersistence;
    private final AlternateIdMatcher alternateIdMatcher;

    @Override
    public void createSubscription(final DataSelector dataSelector,
                                   final String subscriptionId, final List<String> dataNodeSelectors) {
        if (cmDataJobSubscriptionPersistenceService.isNewSubscriptionId(subscriptionId)) {
            for (final String dataNodeSelector : dataNodeSelectors) {
                cmDataJobSubscriptionPersistenceService.add(subscriptionId, dataNodeSelector);
            }
            sendEventToDmis(subscriptionId,
                    cmDataJobSubscriptionPersistenceService.getInactiveDataNodeSelectors(subscriptionId),
                    dataSelector, "subscriptionCreateRequest");
        }
    }

    @Override
    public void deleteSubscription(final String subscriptionId) {
        final Collection<String> dataNodeSelectors =
                cmDataJobSubscriptionPersistenceService.getDataNodeSelectors(subscriptionId);
        final List<String> dataNodeSelectorsWithoutAnySubscriber = new ArrayList<>();
        for (final String dataNodeSelector : dataNodeSelectors) {
            cmDataJobSubscriptionPersistenceService.delete(subscriptionId, dataNodeSelector);
            if (cmDataJobSubscriptionPersistenceService.getSubscriptionIds(dataNodeSelector).isEmpty()) {
                dataNodeSelectorsWithoutAnySubscriber.add(dataNodeSelector);
            }
        }
        sendEventToDmis(subscriptionId, dataNodeSelectorsWithoutAnySubscriber, null, "subscriptionDeleteRequest");
    }

    @Override
    public void updateCmSubscriptionStatus(final String subscriptionId,
                                           final String dmiServiceName,
                                           final CmSubscriptionStatus cmSubscriptionStatus) {
        final List<String> dataNodeSelectors =
                cmDataJobSubscriptionPersistenceService.getInactiveDataNodeSelectors(subscriptionId);
        final List<String> rejectedDataNodeSelectors = new ArrayList<>();
        for (final String dataNodeSelector : dataNodeSelectors) {
            final String cmHandleId = getCmHandleId(dataNodeSelector);
            if (cmHandleId == null) {
                log.info("Failed to resolve cm handle ID for dataNodeSelector={}", dataNodeSelector);
                continue;
            }
            final String resolvedDmiServiceName = getDmiServiceName(cmHandleId);
            if (resolvedDmiServiceName.equals(dmiServiceName)) {
                cmDataJobSubscriptionPersistenceService.updateCmSubscriptionStatus(dataNodeSelector,
                        cmSubscriptionStatus);
                if (cmSubscriptionStatus.equals(REJECTED)) {
                    rejectedDataNodeSelectors.add(dataNodeSelector);
                }
            }

        }
        if (!rejectedDataNodeSelectors.isEmpty()) {
            logRejectedDataNodeSelectors(subscriptionId, dmiServiceName, rejectedDataNodeSelectors);
        }
    }

    private static void logRejectedDataNodeSelectors(final String subscriptionId, final String dmiServiceName,
                                                     final List<String> rejectedDataNodeSelectors) {
        final String dataNodeSelectorAsString =
                JexParser.toJsonExpressionsAsString(rejectedDataNodeSelectors);
        log.info("DataJob CREATE request with the following details was rejected by DMI plugin {}: "
                        + "dataJobId={} | dataNodeSelector={}", dmiServiceName, subscriptionId,
                dataNodeSelectorAsString);
    }

    private void sendEventToDmis(final String subscriptionId,
                                 final List<String> dataNodeSelectors,
                                 final DataSelector dataSelector,
                                 final String eventType) {
        final Map<String, CmHandleIdsAndDataNodeSelectors> cmHandleIdsAndDataNodeSelectorsPerDmi =
                createDmiInEventTargetsPerDmi(dataNodeSelectors);
        for (final Map.Entry<String, CmHandleIdsAndDataNodeSelectors> cmHandleIdsAndDataNodeSelectorsEntry :
                cmHandleIdsAndDataNodeSelectorsPerDmi.entrySet()) {
            final String dmiServiceName = cmHandleIdsAndDataNodeSelectorsEntry.getKey();
            final CmHandleIdsAndDataNodeSelectors cmHandleIdsAndDataNodeSelectors =
                    cmHandleIdsAndDataNodeSelectorsEntry.getValue();

            final DataJobSubscriptionDmiInEvent dmiInEvent;
            dmiInEvent = buildDmiInEvent(cmHandleIdsAndDataNodeSelectors, dataSelector);
            eventProducer.send(subscriptionId, dmiServiceName, eventType, dmiInEvent);
        }
    }

    private DataJobSubscriptionDmiInEvent buildDmiInEvent(
            final CmHandleIdsAndDataNodeSelectors cmHandleIdsAndDataNodeSelectors,
            final DataSelector dataSelector) {
        final List<String> cmHandleIds = new ArrayList<>(cmHandleIdsAndDataNodeSelectors.cmHandleIds);
        final List<String> dataNodeSelectors = new ArrayList<>(cmHandleIdsAndDataNodeSelectors.dataNodeSelectors);
        final List<String> notificationTypes;
        final String notificationFilter;
        if (dataSelector != null) {
            notificationTypes = dataSelector.getNotificationTypes();
            notificationFilter = dataSelector.getNotificationFilter();
        } else {
            notificationTypes = null;
            notificationFilter = null;
        }

        return dmiInEventMapper.toDmiInEvent(cmHandleIds, dataNodeSelectors, notificationTypes, notificationFilter);
    }

    private Map<String, CmHandleIdsAndDataNodeSelectors> createDmiInEventTargetsPerDmi(
            final List<String> dataNodeSelectors) {
        final Map<String, CmHandleIdsAndDataNodeSelectors> dmiInEventTargetsPerDmi = new HashMap<>();
        for (final String dataNodeSelector : dataNodeSelectors) {
            final String cmHandleId = getCmHandleId(dataNodeSelector);
            if (cmHandleId == null) {
                log.info("Failed to resolve cm handle ID for dataNodeSelector {}", dataNodeSelector);
            } else {
                final String dmiServiceName = getDmiServiceName(cmHandleId);
                final CmHandleIdsAndDataNodeSelectors cmHandleIdsAndDataNodeSelectors;
                if (dmiInEventTargetsPerDmi.get(dmiServiceName) == null) {
                    cmHandleIdsAndDataNodeSelectors =
                            new CmHandleIdsAndDataNodeSelectors(new HashSet<>(), new HashSet<>());
                    dmiInEventTargetsPerDmi.put(dmiServiceName, cmHandleIdsAndDataNodeSelectors);
                } else {
                    cmHandleIdsAndDataNodeSelectors = dmiInEventTargetsPerDmi.get(dmiServiceName);
                }
                cmHandleIdsAndDataNodeSelectors.cmHandleIds.add(cmHandleId);
                cmHandleIdsAndDataNodeSelectors.dataNodeSelectors.add(dataNodeSelector);
            }
        }
        return dmiInEventTargetsPerDmi;
    }

    private String getCmHandleId(final String dataNodeSelector) {
        final String alternateId = JexParser.extractFdnPrefix(dataNodeSelector).orElse("");
        if (alternateId.isEmpty()) {
            return null;
        }
        return alternateIdMatcher.getCmHandleId(alternateId);
    }

    private String getDmiServiceName(final String cmHandleId) {
        final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
        return yangModelCmHandle.getDmiServiceName();
    }

    private record CmHandleIdsAndDataNodeSelectors(Set<String> cmHandleIds, Set<String> dataNodeSelectors) {
    }
}
