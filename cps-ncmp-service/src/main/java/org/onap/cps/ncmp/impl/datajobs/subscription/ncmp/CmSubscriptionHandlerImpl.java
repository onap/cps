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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.impl.datajobs.subscription.client_to_ncmp.DataSelector;
import org.onap.cps.ncmp.impl.datajobs.subscription.dmi.DmiInEventMapper;
import org.onap.cps.ncmp.impl.datajobs.subscription.dmi.EventProducer;
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
    private final EventProducer eventProducer;
    private final InventoryPersistence inventoryPersistence;
    private final AlternateIdMatcher alternateIdMatcher;

    @Override
    public void processSubscriptionCreate(final DataSelector dataSelector,
                                          final String subscriptionId, final List<String> dataNodeSelectors) {
        for (final String dataNodeSelector : dataNodeSelectors) {
            cmDataJobSubscriptionPersistenceService.add(subscriptionId, dataNodeSelector);
        }
        sendCreateEventToDmis(subscriptionId, dataSelector);
    }

    private void sendCreateEventToDmis(final String subscriptionId, final DataSelector dataSelector) {
        final List<String> dataNodeSelectors =
                cmDataJobSubscriptionPersistenceService.getInactiveDataNodeSelectors(subscriptionId);
        final Map<String, CmHandleIdsAndDataNodeSelectors> cmHandleIdsAndDataNodeSelectorsPerDmi =
                createDmiInEventTargetsPerDmi(dataNodeSelectors);

        for (final Map.Entry<String, CmHandleIdsAndDataNodeSelectors> cmHandleIdsAndDataNodeSelectorsEntry :
                cmHandleIdsAndDataNodeSelectorsPerDmi.entrySet()) {
            final String dmiServiceName = cmHandleIdsAndDataNodeSelectorsEntry.getKey();
            final CmHandleIdsAndDataNodeSelectors cmHandleIdsAndDataNodeSelectors =
                    cmHandleIdsAndDataNodeSelectorsEntry.getValue();
            final DataJobSubscriptionDmiInEvent dmiInEvent =
                    buildDmiInEvent(cmHandleIdsAndDataNodeSelectors, dataSelector);
            eventProducer.send(subscriptionId, dmiServiceName, "subscriptionCreateRequest", dmiInEvent);
        }
    }


    private DataJobSubscriptionDmiInEvent buildDmiInEvent(
            final CmHandleIdsAndDataNodeSelectors cmHandleIdsAndDataNodeSelectors,
            final DataSelector dataSelector) {
        final List<String> cmHandleIds = new ArrayList<>(cmHandleIdsAndDataNodeSelectors.cmHandleIds);
        final List<String> dataNodeSelectors = new ArrayList<>(cmHandleIdsAndDataNodeSelectors.dataNodeSelectors);
        final List<String> notificationTypes = dataSelector.getNotificationTypes();
        final String notificationFilter = dataSelector.getNotificationFilter();
        return dmiInEventMapper.toDmiInEvent(cmHandleIds, dataNodeSelectors, notificationTypes, notificationFilter);
    }

    private Map<String, CmHandleIdsAndDataNodeSelectors> createDmiInEventTargetsPerDmi(
            final List<String> dataNodeSelectors) {
        final Map<String, CmHandleIdsAndDataNodeSelectors> dmiInEventTargetsPerDmi = new HashMap<>();
        for (final String dataNodeSelector : dataNodeSelectors) {
            final String cmHandleId = getCmHandleId(dataNodeSelector);
            if (cmHandleId != null) {
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

    private record CmHandleIdsAndDataNodeSelectors(Set<String> cmHandleIds, Set<String> dataNodeSelectors) {}

}
