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

import static org.onap.cps.ncmp.impl.datajobs.subscription.models.CmSubscriptionStatus.UNKNOWN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.ncmp.impl.datajobs.subscription.client_to_ncmp.DataSelector;
import org.onap.cps.ncmp.impl.datajobs.subscription.dmi.DmiInEventMapper;
import org.onap.cps.ncmp.impl.datajobs.subscription.dmi.DmiInEventProducer;
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
@ConditionalOnProperty(name = "notification.enabled", havingValue = "true", matchIfMissing = true)
public class CmSubscriptionHandlerImpl implements CmSubscriptionHandler {

    private final CmDataJobSubscriptionPersistenceService cmDataJobSubscriptionPersistenceService;
    private final DmiInEventMapper dmiInEventMapper;
    private final DmiInEventProducer dmiInEventProducer;
    private final InventoryPersistence inventoryPersistence;
    private final AlternateIdMatcher alternateIdMatcher;

    @Override
    public void processSubscriptionCreateRequest(final String subscriptionId, final List<String> dataNodeSelectors,
                                                 final DataSelector dataJobExtraAttr) {
        for (final String dataNodeSelector : dataNodeSelectors) {
            cmDataJobSubscriptionPersistenceService.addSubscription(dataNodeSelector, subscriptionId);
        }
        sendDmiInEventPerDmi(subscriptionId, dataJobExtraAttr);
    }

    private void sendDmiInEventPerDmi(final String subscriptionId, final DataSelector dataJobExtraAttr) {
        final List<String> dmiInEventDataNodeSelectors = getDataNodeSelectorsForDmiInEvent(subscriptionId);
        final Map<String, DmiInEventTargets> dmiInEventTargetsPerDmi = new HashMap<>();
        populateDmiInEventTargetsPerDmi(dmiInEventDataNodeSelectors, dmiInEventTargetsPerDmi);

        for (final String dmiServiceName : dmiInEventTargetsPerDmi.keySet()) {
            final DataJobSubscriptionDmiInEvent dmiInEvent =
                    buildDmiInEvent(dmiServiceName, dmiInEventTargetsPerDmi, dataJobExtraAttr);
            dmiInEventProducer.sendDmiInEvent(subscriptionId, dmiServiceName,
                    "subscriptionCreateRequest", dmiInEvent);
        }
    }

    private List<String> getDataNodeSelectorsForDmiInEvent(final String subscriptionId) {
        final List<String> dataNodeSelectorsForDmiInEvent =
                cmDataJobSubscriptionPersistenceService.getRejectedDataNodeSelectors(subscriptionId);
        final Collection<DataNode> dataNodesForSubscriptionWithUnknownStatus =
                cmDataJobSubscriptionPersistenceService.getDataNodesForSubscription(subscriptionId, UNKNOWN);
        for (final DataNode dataNode : dataNodesForSubscriptionWithUnknownStatus) {
            final Collection<String> dataJobIds = (Collection<String>) dataNode.getLeaves().get("dataJobId");
            if (isOnlySubscriber(dataJobIds, subscriptionId)) {
                final String dataNodeSelector = dataNode.getLeaves().get("dataNodeSelector").toString();
                dataNodeSelectorsForDmiInEvent.add(dataNodeSelector);
            }
        }

        return dataNodeSelectorsForDmiInEvent;
    }
    
    private boolean isOnlySubscriber(final Collection<String> dataJobIds, final String subscriptionId) {
        return (dataJobIds.size()==1) && (dataJobIds.contains(subscriptionId));
    }

    private DataJobSubscriptionDmiInEvent buildDmiInEvent(final String dmiServiceName,
                                       final Map<String, DmiInEventTargets> dmiInEventTargetsPerDmi,
                                       final DataSelector dataJobExtraAttr) {
        final DmiInEventTargets dmiInEventTargets = dmiInEventTargetsPerDmi.get(dmiServiceName);
        final List<String> cmHandleIds = new ArrayList<>(dmiInEventTargets.cmHandleIds);
        final List<String> dataNodeSelectors = new ArrayList<>(dmiInEventTargets.dataNodeSelectors);
        final List<String> notificationTypes = dataJobExtraAttr.getNotificationTypes();
        final String notificationFilter = dataJobExtraAttr.getNotificationFilter();
        return dmiInEventMapper.toDmiInEvent(cmHandleIds, dataNodeSelectors, notificationTypes, notificationFilter);
    }

    private void populateDmiInEventTargetsPerDmi(final List<String> dataNodeSelectors,
                                                 final Map<String, DmiInEventTargets> dmiInEventTargetsPerDmi) {
        for (final String dataNodeSelector : dataNodeSelectors) {
            final String cmHandleId = getCmHandleId(dataNodeSelector);
            final String dmiServiceName = getDmiServiceName(cmHandleId);
            if (dmiInEventTargetsPerDmi.get(dmiServiceName) == null) {
                final DmiInEventTargets dmiInEventTargets = getDmiInEventTargets(dataNodeSelector, cmHandleId);
                dmiInEventTargetsPerDmi.put(dmiServiceName, dmiInEventTargets);
            } else {
                dmiInEventTargetsPerDmi.get(dmiServiceName).cmHandleIds.add(cmHandleId);
                dmiInEventTargetsPerDmi.get(dmiServiceName).dataNodeSelectors.add(dataNodeSelector);
            }
        }
    }

    private static DmiInEventTargets getDmiInEventTargets(String dataNodeSelector, String cmHandleId) {
        final DmiInEventTargets dmiInEventTargets = new DmiInEventTargets(
                new HashSet<>(1), new HashSet<>(1));
        dmiInEventTargets.cmHandleIds.add(cmHandleId);
        dmiInEventTargets.dataNodeSelectors.add(dataNodeSelector);
        return dmiInEventTargets;
    }

    private String getCmHandleId(String dataNodeSelector) {
        final String alternateId = JexParser.extractFdnPrefix(dataNodeSelector).orElse("");
        return alternateIdMatcher.getCmHandleId(alternateId);
    }

    private String getDmiServiceName(String cmHandleId) {
        final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
        return yangModelCmHandle.getDmiServiceName();
    }

    private record DmiInEventTargets(Set<String> dataNodeSelectors, Set<String> cmHandleIds) {}

}
