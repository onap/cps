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

    private final Map<String, Map<String, DmiCmNotificationSubscriptionDetails>> cmNotificationSubscriptionCache;
    private final InventoryPersistence inventoryPersistence;
    private final Map<String, DmiCmNotificationSubscriptionDetails> dmiCmNotificationSubscriptionDetailsPerDmi;

    /**
     * Adds new subscription to the subscription cache.
     *
     * @param subscriptionId    subscription Id
     * @param predicates        subscription request predicates
     */
    public void add(final String subscriptionId, final List<Predicate> predicates) {
        cmNotificationSubscriptionCache.put(subscriptionId, createDmiCmNotificationSubscriptionsPerDmi(predicates));
    }

    /**
     *  Creates map of subscription details per DMI.
     *
     * @param predicates    CM Subscription Create Request Predicates
     * @return              Map of DmiCmNotificationSubscription per DMI plugin
     */
    public Map<String, DmiCmNotificationSubscriptionDetails> createDmiCmNotificationSubscriptionsPerDmi(
            final List<Predicate> predicates) {
        for (final Predicate requestPredicate : predicates) {
            final List<String> targetFilter = requestPredicate.getTargetFilter();
            final DatastoreType datastoreType = DatastoreType.fromDatastoreName(
                    requestPredicate.getScopeFilter().getDatastore().toString());
            final List<String> xpaths = requestPredicate.getScopeFilter().getXpathFilter();
            final Map<String, List<String>> targetCmHandlesByDmiMap = groupTargetCmHandleIdsByDmi(targetFilter);
            for (final Map.Entry<String, List<String>> targetCmHandlesByDmi: targetCmHandlesByDmiMap.entrySet()) {
                final DmiCmNotificationSubscriptionPredicate dmiCmNotificationSubscriptionPredicate =
                        new DmiCmNotificationSubscriptionPredicate(targetCmHandlesByDmi.getValue(),
                                datastoreType, xpaths);
                updateDmiCmNotificationSubscriptionDetailsPerDmi(targetCmHandlesByDmi.getKey(),
                        dmiCmNotificationSubscriptionPredicate);
            }
        }
        return dmiCmNotificationSubscriptionDetailsPerDmi;
    }

    private void updateDmiCmNotificationSubscriptionDetailsPerDmi(
            final String dmiServiceName,
            final DmiCmNotificationSubscriptionPredicate dmiCmNotificationSubscriptionPredicate) {
        if (dmiCmNotificationSubscriptionDetailsPerDmi.containsKey(dmiServiceName)) {
            dmiCmNotificationSubscriptionDetailsPerDmi.get(dmiServiceName)
                    .getDmiCmNotificationSubscriptionPredicates().add(dmiCmNotificationSubscriptionPredicate);
        } else {
            dmiCmNotificationSubscriptionDetailsPerDmi.put(dmiServiceName,
                    new DmiCmNotificationSubscriptionDetails(
                            new ArrayList<>(List.of(dmiCmNotificationSubscriptionPredicate)),
                            PENDING));
        }
    }

    private Map<String, List<String>> groupTargetCmHandleIdsByDmi(final List<String> targetCmHandleIds) {
        final Map<String, List<String>> targetCmHandlesByDmiServiceNames = new HashMap<>();
        final Collection<YangModelCmHandle> yangModelCmHandles =
                inventoryPersistence.getYangModelCmHandles(targetCmHandleIds);

        for (final YangModelCmHandle yangModelCmHandle : yangModelCmHandles) {
            final String dmiServiceName = yangModelCmHandle.getDmiServiceName();
            targetCmHandlesByDmiServiceNames.putIfAbsent(dmiServiceName, new ArrayList<>());
            targetCmHandlesByDmiServiceNames.get(dmiServiceName).add(yangModelCmHandle.getId());
        }
        return targetCmHandlesByDmiServiceNames;
    }
}