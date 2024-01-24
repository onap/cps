/*
 *  ============LICENSE_START=======================================================
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

import static org.onap.cps.ncmp.api.impl.events.cmsubscription.model.CmNotificationSubscriptionStatus.PENDING;

import com.hazelcast.map.IMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.DmiCmNotificationSubscriptionDetails;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.DmiCmNotificationSubscriptionPredicate;
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.impl.operations.DatastoreType;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.client_to_ncmp.Predicate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DmiCmNotificationSubscriptionCacheHandler {

    private final IMap<String, Map<String, DmiCmNotificationSubscriptionDetails>> cmNotificationSubscriptionCache;
    private final InventoryPersistence inventoryPersistence;

    /**
     * Adds new subscription to the subscription cache.
     *
     * @param subscriptionId    subscription Id
     * @param predicates        subscription request predicates
     */
    public void add(final String subscriptionId, final List<Predicate> predicates) {
        final Map<String, DmiCmNotificationSubscriptionDetails> dmiCmNotificationSubscriptionDetailsPerDmi =
                createDmiCmNotificationSubscriptionsPerDmi(predicates);
        cmNotificationSubscriptionCache.put(subscriptionId, dmiCmNotificationSubscriptionDetailsPerDmi);
    }

    /**
     *  Creates map of subscription details per DMI.
     *
     * @param predicates    CM Subscription Create Request Predicates
     * @return              Map of DmiCmNotificationSubscription per DMI plugin
     */
    public Map<String, DmiCmNotificationSubscriptionDetails> createDmiCmNotificationSubscriptionsPerDmi(
            final List<Predicate> predicates) {
        final Map<String, DmiCmNotificationSubscriptionDetails> dmiCmNotificationSubscriptionDetailsPerDmi =
                new HashMap<>();
        for (final Predicate requestPredicate : predicates) {
            final List<String> targetFilter = requestPredicate.getTargetFilter();
            final DatastoreType datastoreType = DatastoreType.fromDatastoreName(
                    requestPredicate.getScopeFilter().getDatastore().toString());
            final List<String> xpaths = requestPredicate.getScopeFilter().getXpathFilter();
            final Map<String, List<String>> targetCmHandlesByDmiMap = groupTargetCmHandleIdsByDmi(targetFilter);
            for (final Map.Entry<String, List<String>> targetCmHandlesByDmi: targetCmHandlesByDmiMap.entrySet()) {
                final String dmiServiceName = targetCmHandlesByDmi.getKey();
                final List<String> targetCmHandleIds = targetCmHandlesByDmi.getValue();
                final DmiCmNotificationSubscriptionPredicate dmiCmNotificationSubscriptionPredicate =
                        createDmiCmNotificationSubscriptionPredicate(targetCmHandleIds, datastoreType, xpaths);
                if (dmiCmNotificationSubscriptionDetailsPerDmi.containsKey(dmiServiceName)) {
                    dmiCmNotificationSubscriptionDetailsPerDmi.get(dmiServiceName)
                            .getDmiCmNotificationSubscriptionPredicates().add(dmiCmNotificationSubscriptionPredicate);
                } else {
                    dmiCmNotificationSubscriptionDetailsPerDmi.put(dmiServiceName,
                            createDmiCmNotificationSubscriptionDetailsWithPredicate(
                                    dmiCmNotificationSubscriptionPredicate));
                }
            }
        }
        return dmiCmNotificationSubscriptionDetailsPerDmi;
    }

    private DmiCmNotificationSubscriptionDetails createDmiCmNotificationSubscriptionDetailsWithPredicate(
            final DmiCmNotificationSubscriptionPredicate dmiCmNotificationSubscriptionPredicate) {
        final DmiCmNotificationSubscriptionDetails dmiCmNotificationSubscriptionDetails =
                new DmiCmNotificationSubscriptionDetails();
        dmiCmNotificationSubscriptionDetails.setDmiCmNotificationSubscriptionPredicates(new ArrayList<>());
        dmiCmNotificationSubscriptionDetails.getDmiCmNotificationSubscriptionPredicates()
                .add(dmiCmNotificationSubscriptionPredicate);
        dmiCmNotificationSubscriptionDetails.setCmNotificationSubscriptionStatus(PENDING);
        return  dmiCmNotificationSubscriptionDetails;
    }

    protected Map<String, List<String>> groupTargetCmHandleIdsByDmi(final List<String> targetCmHandles) {
        final Map<String, List<String>> targetCmHandlesByDmiServiceNames = new HashMap<>();
        final Collection<YangModelCmHandle> yangModelCmHandles =
                inventoryPersistence.getYangModelCmHandles(targetCmHandles);

        for (final YangModelCmHandle yangModelCmHandle : yangModelCmHandles) {
            final String dmiServiceName = yangModelCmHandle.getDmiServiceName();
            final String cmHandleId = yangModelCmHandle.getId();
            targetCmHandlesByDmiServiceNames.computeIfAbsent(dmiServiceName, k -> new ArrayList<>()).add(cmHandleId);
        }
        return targetCmHandlesByDmiServiceNames;
    }

    private DmiCmNotificationSubscriptionPredicate createDmiCmNotificationSubscriptionPredicate(
            final List<String> targetCmHandleIds, final DatastoreType datastoreType, final List<String> xpaths) {
        final DmiCmNotificationSubscriptionPredicate dmiCmNotificationSubscriptionPredicate =
                new DmiCmNotificationSubscriptionPredicate();
        dmiCmNotificationSubscriptionPredicate.setTargetCmHandleIds(targetCmHandleIds);
        dmiCmNotificationSubscriptionPredicate.setDatastoreType(datastoreType);
        dmiCmNotificationSubscriptionPredicate.setXpaths(xpaths);
        return dmiCmNotificationSubscriptionPredicate;
    }
}