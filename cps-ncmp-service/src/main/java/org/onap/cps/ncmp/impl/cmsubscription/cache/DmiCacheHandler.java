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

package org.onap.cps.ncmp.impl.cmsubscription.cache;

import static org.onap.cps.ncmp.impl.cmsubscription.models.CmNotificationSubscriptionStatus.PENDING;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.data.models.DatastoreType;
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.client_to_ncmp.Predicate;
import org.onap.cps.ncmp.impl.cmsubscription.models.CmNotificationSubscriptionStatus;
import org.onap.cps.ncmp.impl.cmsubscription.models.DmiCmNotificationSubscriptionDetails;
import org.onap.cps.ncmp.impl.cmsubscription.models.DmiCmNotificationSubscriptionPredicate;
import org.onap.cps.ncmp.impl.cmsubscription.utils.CmNotificationSubscriptionPersistenceService;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DmiCacheHandler {

    private final CmNotificationSubscriptionPersistenceService cmNotificationSubscriptionPersistenceService;
    private final Map<String, Map<String, DmiCmNotificationSubscriptionDetails>> cmNotificationSubscriptionCache;
    private final InventoryPersistence inventoryPersistence;

    /**
     * Adds new subscription to the subscription cache.
     *
     * @param subscriptionId    subscription id
     * @param predicates        subscription request predicates
     */
    public void add(final String subscriptionId, final List<Predicate> predicates) {
        cmNotificationSubscriptionCache.put(subscriptionId, createDmiCmNotificationSubscriptionsPerDmi(predicates));
    }

    /**
     * Get cm notification subscription cache entry via subscription id.
     *
     * @param subscriptionId    subscription id
     * @return map of dmi cm notification subscriptions per dmi
     */
    public Map<String, DmiCmNotificationSubscriptionDetails> get(final String subscriptionId) {
        return cmNotificationSubscriptionCache.get(subscriptionId);
    }


    /**
     * Remove cache entries with CmNotificationSubscriptionStatus ACCEPTED/REJECTED via subscription id.
     *
     * @param subscriptionId subscription id as key in CM notification Subscription cache.
     */
    public void removeAcceptedAndRejectedDmiCmNotificationSubscriptionEntries(final String subscriptionId) {
        final Map<String, DmiCmNotificationSubscriptionDetails> dmiCmNotificationSubscriptionsPerDmi =
                cmNotificationSubscriptionCache.get(subscriptionId);
        final Map<String, DmiCmNotificationSubscriptionDetails> updatedDmiCmNotificationSubscriptionsPerDmi =
                dmiCmNotificationSubscriptionsPerDmi.entrySet().stream().filter(
                                dmiCmNotificationSubscription ->
                                        !isAcceptedOrRejected(dmiCmNotificationSubscription.getValue()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        cmNotificationSubscriptionCache.put(subscriptionId, updatedDmiCmNotificationSubscriptionsPerDmi);
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
            final Set<String> xpaths = new HashSet<>(requestPredicate.getScopeFilter().getXpathFilter());
            final Map<String, Set<String>> targetCmHandlesByDmiMap = groupTargetCmHandleIdsByDmi(targetFilter);
            for (final Map.Entry<String, Set<String>> targetCmHandlesByDmi: targetCmHandlesByDmiMap.entrySet()) {
                final DmiCmNotificationSubscriptionPredicate dmiCmNotificationSubscriptionPredicate =
                        new DmiCmNotificationSubscriptionPredicate(targetCmHandlesByDmi.getValue(),
                                datastoreType, xpaths);
                updateDmiCmNotificationSubscriptionDetailsPerDmi(targetCmHandlesByDmi.getKey(),
                        dmiCmNotificationSubscriptionPredicate,
                        dmiCmNotificationSubscriptionDetailsPerDmi);
            }
        }
        return dmiCmNotificationSubscriptionDetailsPerDmi;
    }

    /**
     *  Update status in map of subscription details per DMI.
     *
     * @param subscriptionId    String of subscription Id
     * @param dmiServiceName    String of dmiServiceName
     * @param status            String of status
     *
     */
    public void updateDmiCmNotificationSubscriptionStatusPerDmi(final String subscriptionId,
                                                                final String dmiServiceName,
                                                                final CmNotificationSubscriptionStatus status) {
        final Map<String, DmiCmNotificationSubscriptionDetails> dmiCmNotificationSubscriptionDetailsPerDmi =
                cmNotificationSubscriptionCache.get(subscriptionId);
        dmiCmNotificationSubscriptionDetailsPerDmi.get(dmiServiceName).setCmNotificationSubscriptionStatus(status);
        cmNotificationSubscriptionCache.put(subscriptionId, dmiCmNotificationSubscriptionDetailsPerDmi);
    }

    /**
     *  Persist map of subscription details per DMI.
     *
     * @param subscriptionId    String of subscription Id
     * @param dmiServiceName    String of dmiServiceName
     *
     */
    public void persistIntoDatabasePerDmi(final String subscriptionId, final String dmiServiceName) {
        final List<DmiCmNotificationSubscriptionPredicate> dmiCmNotificationSubscriptionPredicateList =
                cmNotificationSubscriptionCache.get(subscriptionId).get(dmiServiceName)
                        .getDmiCmNotificationSubscriptionPredicates();
        for (final DmiCmNotificationSubscriptionPredicate dmiCmNotificationSubscriptionPredicate:
                dmiCmNotificationSubscriptionPredicateList) {
            final DatastoreType datastoreType = dmiCmNotificationSubscriptionPredicate.getDatastoreType();
            final Set<String> cmHandles = dmiCmNotificationSubscriptionPredicate.getTargetCmHandleIds();
            final Set<String> xpaths = dmiCmNotificationSubscriptionPredicate.getXpaths();

            for (final String cmHandle: cmHandles) {
                for (final String xpath: xpaths) {
                    cmNotificationSubscriptionPersistenceService.addCmNotificationSubscription(datastoreType, cmHandle,
                            xpath, subscriptionId);
                }
            }
        }
    }

    /**
     *  Remove subscription from database per DMI service name.
     *
     * @param subscriptionId    String of subscription id
     * @param dmiServiceName    String of dmiServiceName
     *
     */
    public void removeFromDatabasePerDmi(final String subscriptionId, final String dmiServiceName) {
        final List<DmiCmNotificationSubscriptionPredicate> dmiCmNotificationSubscriptionPredicateList =
                cmNotificationSubscriptionCache.get(subscriptionId).get(dmiServiceName)
                        .getDmiCmNotificationSubscriptionPredicates();
        for (final DmiCmNotificationSubscriptionPredicate dmiCmNotificationSubscriptionPredicate:
                dmiCmNotificationSubscriptionPredicateList) {
            final DatastoreType datastoreType = dmiCmNotificationSubscriptionPredicate.getDatastoreType();
            final Set<String> cmHandles = dmiCmNotificationSubscriptionPredicate.getTargetCmHandleIds();
            final Set<String> xpaths = dmiCmNotificationSubscriptionPredicate.getXpaths();

            for (final String cmHandle: cmHandles) {
                for (final String xpath: xpaths) {
                    cmNotificationSubscriptionPersistenceService.removeCmNotificationSubscription(datastoreType,
                            cmHandle, xpath, subscriptionId);
                }
            }
        }
    }

    private void updateDmiCmNotificationSubscriptionDetailsPerDmi(
            final String dmiServiceName,
            final DmiCmNotificationSubscriptionPredicate dmiCmNotificationSubscriptionPredicate,
            final Map<String, DmiCmNotificationSubscriptionDetails> dmiCmNotificationSubscriptionDetailsPerDmi) {
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

    private Map<String, Set<String>> groupTargetCmHandleIdsByDmi(final List<String> targetCmHandleIds) {
        final Map<String, Set<String>> targetCmHandlesByDmiServiceNames = new HashMap<>();
        final Collection<YangModelCmHandle> yangModelCmHandles =
                inventoryPersistence.getYangModelCmHandles(targetCmHandleIds);

        for (final YangModelCmHandle yangModelCmHandle : yangModelCmHandles) {
            final String dmiServiceName = yangModelCmHandle.getDmiServiceName();
            targetCmHandlesByDmiServiceNames.putIfAbsent(dmiServiceName, new HashSet<>());
            targetCmHandlesByDmiServiceNames.get(dmiServiceName).add(yangModelCmHandle.getId());
        }
        return targetCmHandlesByDmiServiceNames;
    }

    private boolean isAcceptedOrRejected(
            final DmiCmNotificationSubscriptionDetails dmiCmNotificationSubscription) {
        return dmiCmNotificationSubscription.getCmNotificationSubscriptionStatus().toString().equals("ACCEPTED")
                || dmiCmNotificationSubscription.getCmNotificationSubscriptionStatus().toString().equals("REJECTED");
    }
}
