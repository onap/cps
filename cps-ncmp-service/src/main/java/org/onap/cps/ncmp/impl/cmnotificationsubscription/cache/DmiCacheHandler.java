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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.cache;

import static org.onap.cps.ncmp.impl.cmnotificationsubscription.models.CmSubscriptionStatus.PENDING;

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
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.CmSubscriptionStatus;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionDetails;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionPredicate;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.utils.CmSubscriptionPersistenceService;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.client_to_ncmp.Predicate;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DmiCacheHandler {

    private final CmSubscriptionPersistenceService cmSubscriptionPersistenceService;
    private final Map<String, Map<String, DmiCmSubscriptionDetails>> cmNotificationSubscriptionCache;
    private final InventoryPersistence inventoryPersistence;

    /**
     * Adds subscription to the subscription cache.
     *
     * @param subscriptionId    subscription id
     * @param predicates        subscription request predicates
     */
    public void add(final String subscriptionId, final List<Predicate> predicates) {
        cmNotificationSubscriptionCache.put(subscriptionId, createDmiSubscriptionsPerDmi(predicates));
    }

    /**
     * Adds subscription to the subscription cache.
     *
     * @param dmiSubscriptionsPerDmi Map of DmiCmSubscriptionDetails per DMI
     */
    public void add(final String subscriptionId,
                    final Map<String, DmiCmSubscriptionDetails>
                            dmiSubscriptionsPerDmi) {
        cmNotificationSubscriptionCache.put(subscriptionId, dmiSubscriptionsPerDmi);
    }

    /**
     * Get cm notification subscription cache entry via subscription id.
     *
     * @param subscriptionId    subscription id
     * @return map of dmi cm notification subscriptions per dmi
     */
    public Map<String, DmiCmSubscriptionDetails> get(final String subscriptionId) {
        return cmNotificationSubscriptionCache.get(subscriptionId);
    }


    /**
     * Remove cache entries with CmNotificationSubscriptionStatus ACCEPTED/REJECTED via subscription id.
     *
     * @param subscriptionId subscription id as key in CM notification Subscription cache.
     */
    public void removeAcceptedAndRejectedDmiSubscriptionEntries(final String subscriptionId) {
        final Map<String, DmiCmSubscriptionDetails> dmiSubscriptionsPerDmi =
                cmNotificationSubscriptionCache.get(subscriptionId);
        final Map<String, DmiCmSubscriptionDetails> updatedDmiSubscriptionsPerDmi =
                dmiSubscriptionsPerDmi.entrySet().stream()
                        .filter(dmiCmNotificationSubscription -> !isAcceptedOrRejected(
                                dmiCmNotificationSubscription.getValue()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        cmNotificationSubscriptionCache.put(subscriptionId, updatedDmiSubscriptionsPerDmi);
    }

    /**
     *  Creates map of subscription details per DMI.
     *
     * @param predicates    CM Subscription Create Request Predicates
     * @return              Map of DmiCmNotificationSubscription per DMI plugin
     */
    public Map<String, DmiCmSubscriptionDetails> createDmiSubscriptionsPerDmi(
            final List<Predicate> predicates) {
        final Map<String, DmiCmSubscriptionDetails> dmiSubscriptionsPerDmi =
                new HashMap<>();
        for (final Predicate requestPredicate : predicates) {
            final List<String> targetFilter = requestPredicate.getTargetFilter();
            final DatastoreType datastoreType = DatastoreType.fromDatastoreName(
                    requestPredicate.getScopeFilter().getDatastore().toString());
            final Set<String> xpaths = new HashSet<>(requestPredicate.getScopeFilter().getXpathFilter());
            final Map<String, Set<String>> targetCmHandlesByDmiMap = groupTargetCmHandleIdsByDmi(targetFilter);
            for (final Map.Entry<String, Set<String>> targetCmHandlesByDmi: targetCmHandlesByDmiMap.entrySet()) {
                final DmiCmSubscriptionPredicate dmiCmSubscriptionPredicate =
                        new DmiCmSubscriptionPredicate(targetCmHandlesByDmi.getValue(),
                                datastoreType, xpaths);
                updateDmiSubscriptionDetailsPerDmi(targetCmHandlesByDmi.getKey(),
                        dmiCmSubscriptionPredicate,
                        dmiSubscriptionsPerDmi);
            }
        }
        return dmiSubscriptionsPerDmi;
    }

    /**
     *  Update status in map of subscription details per DMI.
     *
     * @param subscriptionId    String of subscription Id
     * @param dmiServiceName    String of dmiServiceName
     * @param status            String of status
     *
     */
    public void updateDmiSubscriptionStatusPerDmi(final String subscriptionId, final String dmiServiceName,
                                                  final CmSubscriptionStatus status) {
        final Map<String, DmiCmSubscriptionDetails> dmiSubscriptionsPerDmi =
                cmNotificationSubscriptionCache.get(subscriptionId);
        dmiSubscriptionsPerDmi.get(dmiServiceName).setCmSubscriptionStatus(status);
        cmNotificationSubscriptionCache.put(subscriptionId, dmiSubscriptionsPerDmi);
    }

    /**
     *  Persist map of subscription details per DMI.
     *
     * @param subscriptionId    String of subscription Id
     * @param dmiServiceName    String of dmiServiceName
     *
     */
    public void persistIntoDatabasePerDmi(final String subscriptionId, final String dmiServiceName) {
        final List<DmiCmSubscriptionPredicate> dmiCmSubscriptionPredicates =
                cmNotificationSubscriptionCache.get(subscriptionId).get(dmiServiceName)
                        .getDmiCmSubscriptionPredicates();
        for (final DmiCmSubscriptionPredicate dmiCmSubscriptionPredicate : dmiCmSubscriptionPredicates) {
            final DatastoreType datastoreType = dmiCmSubscriptionPredicate.getDatastoreType();
            final Set<String> cmHandles = dmiCmSubscriptionPredicate.getTargetCmHandleIds();
            final Set<String> xpaths = dmiCmSubscriptionPredicate.getXpaths();

            for (final String cmHandle: cmHandles) {
                for (final String xpath: xpaths) {
                    cmSubscriptionPersistenceService.addCmSubscription(datastoreType, cmHandle,
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
        final List<DmiCmSubscriptionPredicate> dmiCmSubscriptionPredicates =
                cmNotificationSubscriptionCache.get(subscriptionId).get(dmiServiceName)
                        .getDmiCmSubscriptionPredicates();
        for (final DmiCmSubscriptionPredicate dmiCmSubscriptionPredicate : dmiCmSubscriptionPredicates) {
            final DatastoreType datastoreType = dmiCmSubscriptionPredicate.getDatastoreType();
            final Set<String> cmHandles = dmiCmSubscriptionPredicate.getTargetCmHandleIds();
            final Set<String> xpaths = dmiCmSubscriptionPredicate.getXpaths();

            for (final String cmHandle: cmHandles) {
                for (final String xpath: xpaths) {
                    cmSubscriptionPersistenceService.removeCmSubscription(datastoreType,
                            cmHandle, xpath, subscriptionId);
                }
            }
        }
    }

    private void updateDmiSubscriptionDetailsPerDmi(
            final String dmiServiceName,
            final DmiCmSubscriptionPredicate dmiCmSubscriptionPredicate,
            final Map<String, DmiCmSubscriptionDetails> dmiSubscriptionsPerDmi) {
        if (dmiSubscriptionsPerDmi.containsKey(dmiServiceName)) {
            dmiSubscriptionsPerDmi.get(dmiServiceName)
                    .getDmiCmSubscriptionPredicates().add(dmiCmSubscriptionPredicate);
        } else {
            dmiSubscriptionsPerDmi.put(dmiServiceName,
                    new DmiCmSubscriptionDetails(
                            new ArrayList<>(List.of(dmiCmSubscriptionPredicate)),
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

    private boolean isAcceptedOrRejected(final DmiCmSubscriptionDetails dmiCmSubscription) {
        return dmiCmSubscription.getCmSubscriptionStatus().toString().equals("ACCEPTED")
                || dmiCmSubscription.getCmSubscriptionStatus().toString().equals("REJECTED");
    }
}
