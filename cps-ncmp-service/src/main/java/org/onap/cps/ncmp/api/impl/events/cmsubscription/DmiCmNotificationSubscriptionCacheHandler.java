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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.CmNotificationSubscriptionStatus;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.DmiCmNotificationSubscriptionDetails;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.model.DmiCmNotificationSubscriptionPredicate;
import org.onap.cps.ncmp.api.impl.events.cmsubscription.service.CmNotificationSubscriptionPersistenceService;
import org.onap.cps.ncmp.api.impl.operations.DatastoreType;
import org.onap.cps.ncmp.events.cmnotificationsubscription_merge1_0_0.client_to_ncmp.Predicate;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DmiCmNotificationSubscriptionCacheHandler {

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

<<<<<<< Updated upstream
=======

    /**
     * Adds existing subscription to the subscription cache.
     *
     * @param subscriptionId    subscription id
     */
    public void add(final String subscriptionId) {
        final Map<String, DmiCmNotificationSubscriptionDetails> dmiCmNotificationSubscriptionPerDmi =
                getAllDmiCmNotificationSubscriptionsPerDmiForSubscription(subscriptionId);
        cmNotificationSubscriptionCache.put(subscriptionId, dmiCmNotificationSubscriptionPerDmi);
        final DmiCmSubscriptionTuple dmiCmNotificationTuplePerDmi = getDmiCmNotificationTuplePerDmi(subscriptionId);

    }

>>>>>>> Stashed changes
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

<<<<<<< Updated upstream
=======
    private Map<String, DmiCmNotificationSubscriptionDetails> getAllDmiCmNotificationSubscriptionsPerDmiForSubscription(
            final String subscriptionId) {
        final Map<String, DmiCmNotificationSubscriptionDetails> dmiCmNotificationSubscriptionDetailsPerDmi =
                new HashMap<>();
        final Collection<DataNode> subscriptionNodes =
                cmNotificationSubscriptionPersistenceService.getAllNodesForSubscriptionId(subscriptionId);
        for (final DataNode existingDataNode: subscriptionNodes) {
            final DatastoreType datastoreType = extractCmSubscriptionDatastoreFromXpath(existingDataNode.getXpath());
            final String cmHandleId = extractCmSubscriptionCmHandleIdFromXpath(existingDataNode.getXpath());
            final String xpath = existingDataNode.getLeaves().get("xpath").toString();
            final String dmiServiceName = inventoryPersistence.getYangModelCmHandle(cmHandleId).getDmiServiceName();
            final DmiCmNotificationSubscriptionPredicate dmiCmNotificationSubscriptionPredicate =
                    new DmiCmNotificationSubscriptionPredicate(Set.of(cmHandleId), datastoreType, Set.of(xpath));
            if (dmiCmNotificationSubscriptionDetailsPerDmi.containsKey(dmiServiceName)) {
                dmiCmNotificationSubscriptionDetailsPerDmi.get(dmiServiceName)
                        .getDmiCmNotificationSubscriptionPredicates().add(dmiCmNotificationSubscriptionPredicate);
            } else {
                final List<DmiCmNotificationSubscriptionPredicate> dmiCmNotificationSubscriptionPredicateList =
                        new ArrayList<>();
                dmiCmNotificationSubscriptionPredicateList.add(dmiCmNotificationSubscriptionPredicate);
                final DmiCmNotificationSubscriptionDetails dmiCmNotificationSubscriptionDetails =
                        new DmiCmNotificationSubscriptionDetails(dmiCmNotificationSubscriptionPredicateList,
                                PENDING);
                dmiCmNotificationSubscriptionDetailsPerDmi.put(dmiServiceName, dmiCmNotificationSubscriptionDetails);
            }
        }
        return dmiCmNotificationSubscriptionDetailsPerDmi;
    }

    private DmiCmSubscriptionTuple getDmiCmNotificationTuplePerDmi(final String clientSubscriptionId) {
        final Map<String, Collection<DmiCmSubscriptionKey>> lastSubscibersPerDmi = new HashMap<>();
        final Map<String, Collection<DmiCmSubscriptionKey>> otherSubscibersPerDmi = new HashMap<>();
        final Collection<DataNode> subscriptionNodes =
                cmNotificationSubscriptionPersistenceService.getAllNodesForSubscriptionId(clientSubscriptionId);

        for (final DataNode subscriptionNode : subscriptionNodes) {
            final DatastoreType datastoreType = extractCmSubscriptionDatastoreFromXpath(subscriptionNode.getXpath());
            final String cmHandleId = extractCmSubscriptionCmHandleIdFromXpath(subscriptionNode.getXpath());
            final String xpath = subscriptionNode.getLeaves().get("xpath").toString();
            final String dmiServiceName = inventoryPersistence.getYangModelCmHandle(cmHandleId).getDmiServiceName();
            final List<String> subscribers = (List) subscriptionNode.getLeaves().get("subscriptionId");
            final DmiCmSubscriptionKey dmiCmSubscriptionKey =
                    new DmiCmSubscriptionKey(datastoreType.getDatastoreName(), cmHandleId, xpath);
            if (subscribers.size() > 1) {
                if (otherSubscibersPerDmi.containsKey(dmiServiceName)) {
                    otherSubscibersPerDmi.get(dmiServiceName).add(dmiCmSubscriptionKey);
                } else {
                    otherSubscibersPerDmi.put(dmiServiceName, Set.of(dmiCmSubscriptionKey));
                }
            } else {
                if (lastSubscibersPerDmi.containsKey(dmiServiceName)) {
                    lastSubscibersPerDmi.get(dmiServiceName).add(dmiCmSubscriptionKey);
                } else {
                    lastSubscibersPerDmi.put(dmiServiceName, Set.of(dmiCmSubscriptionKey));
                }
            }
        }
        final DmiCmSubscriptionTuple dmiCmSubscriptionTuple =
                new DmiCmSubscriptionTuple(lastSubscibersPerDmi, otherSubscibersPerDmi);
        return dmiCmSubscriptionTuple;
    }

    private DmiCmSubscriptionTuple getDmiCmNotificationTuplePerDmi1(final String clientSubscriptionId) {
        final Map<String, Collection<DmiCmSubscriptionKey>> lastSubscribersPerDmi = new HashMap<>();
        final Map<String, Collection<DmiCmSubscriptionKey>> otherSubscribersPerDmi = new HashMap<>();
        final Collection<DataNode> subscriptionNodes =
                cmNotificationSubscriptionPersistenceService.getAllNodesForSubscriptionId(clientSubscriptionId);

        for (final DataNode subscriptionNode : subscriptionNodes) {
            final String xpath = subscriptionNode.getXpath();
            final DatastoreType datastoreType = extractCmSubscriptionDatastoreFromXpath(xpath);
            final String cmHandleId = extractCmSubscriptionCmHandleIdFromXpath(xpath);
            final String dmiServiceName = inventoryPersistence.getYangModelCmHandle(cmHandleId).getDmiServiceName();
            final List<String> subscribers = (List<String>) subscriptionNode.getLeaves().get("subscriptionId");
            final DmiCmSubscriptionKey dmiCmSubscriptionKey =
                    new DmiCmSubscriptionKey(datastoreType.getDatastoreName(), cmHandleId, xpath);

            final Map<String, Collection<DmiCmSubscriptionKey>> targetMap =
                    subscribers.size() > 1 ? otherSubscribersPerDmi : lastSubscribersPerDmi;

            targetMap.computeIfAbsent(dmiServiceName, k -> new HashSet<>()).add(dmiCmSubscriptionKey);
        }

        return new DmiCmSubscriptionTuple(lastSubscribersPerDmi, otherSubscribersPerDmi);
    }


    private DatastoreType extractCmSubscriptionDatastoreFromXpath(final String xpath) {
        final Pattern pattern = Pattern.compile("/datastore\\[@name='(.*?)'\\]");
        final Matcher matcher = pattern.matcher(xpath);
        if (matcher.find()) {
            final String datastoreName = matcher.group(1);
            return DatastoreType.fromDatastoreName(datastoreName);
        }
        return null;
    }

    private String extractCmSubscriptionCmHandleIdFromXpath(final String xpath) {
        final Pattern pattern = Pattern.compile("/cm-handle\\[@id='(.*?)'\\]");
        final Matcher matcher = pattern.matcher(xpath);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

>>>>>>> Stashed changes
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
