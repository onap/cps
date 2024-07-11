/*
 * ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.ncmp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.data.models.DatastoreType;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.cache.DmiCacheHandler;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.dmi.DmiCmSubscriptionDetailsPerDmiMapper;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.dmi.DmiInEventMapper;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.dmi.DmiInEventProducer;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.CmSubscriptionStatus;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionDetails;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionKey;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionPredicate;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.models.DmiCmSubscriptionTuple;
import org.onap.cps.ncmp.impl.cmnotificationsubscription.utils.CmSubscriptionPersistenceService;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.client_to_ncmp.Predicate;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_client.NcmpOutEvent;
import org.onap.cps.ncmp.impl.cmnotificationsubscription_1_0_0.ncmp_to_dmi.DmiInEvent;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.spi.model.DataNode;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CmSubscriptionHandlerImpl implements CmSubscriptionHandler {

    private static final Pattern DATASTORE_FROM_XPATH_PATTERN = Pattern.compile("/datastore\\[@name='(.*?)']");
    private static final Pattern FILTER_XPATH_FROM_XPATH_PATTERN = Pattern.compile("/filter\\[@xpath='(.*?)']");
    private static final Pattern CMHANDLE_ID_FROM_XPATH_PATTERN = Pattern.compile("/cm-handle\\[@id='(.*?)']");

    private final CmSubscriptionPersistenceService cmSubscriptionPersistenceService;
    private final CmSubscriptionComparator cmSubscriptionComparator;
    private final NcmpOutEventMapper ncmpOutEventMapper;
    private final DmiInEventMapper dmiInEventMapper;
    private final DmiCmSubscriptionDetailsPerDmiMapper dmiCmSubscriptionDetailsPerDmiMapper;
    private final NcmpOutEventProducer ncmpOutEventProducer;
    private final DmiInEventProducer dmiInEventProducer;
    private final DmiCacheHandler dmiCacheHandler;
    private final InventoryPersistence inventoryPersistence;

    @Override
    public void processSubscriptionCreateRequest(final String subscriptionId, final List<Predicate> predicates) {
        if (cmSubscriptionPersistenceService.isUniqueSubscriptionId(subscriptionId)) {
            dmiCacheHandler.add(subscriptionId, predicates);
            handleNewCmSubscription(subscriptionId);
            scheduleNcmpOutEventResponse(subscriptionId, "subscriptionCreateResponse");
        } else {
            rejectAndPublishCreateRequest(subscriptionId, predicates);
        }
    }

    @Override
    public void processSubscriptionDeleteRequest(final String subscriptionId) {
        final Collection<DataNode> subscriptionDataNodes =
                cmSubscriptionPersistenceService.getAllNodesForSubscriptionId(subscriptionId);
        final DmiCmSubscriptionTuple dmiCmSubscriptionTuple =
                getLastRemainingAndOverlappingSubscriptionsPerDmi(subscriptionDataNodes);
        dmiCacheHandler.add(subscriptionId, mergeDmiCmSubscriptionDetailsPerDmiMaps(dmiCmSubscriptionTuple));
        if (dmiCmSubscriptionTuple.lastRemainingSubscriptionsPerDmi().isEmpty()) {
            acceptAndPublishDeleteRequest(subscriptionId);
        } else {
            sendSubscriptionDeleteRequestToDmi(subscriptionId,
                    dmiCmSubscriptionDetailsPerDmiMapper.toDmiCmSubscriptionsPerDmi(
                            dmiCmSubscriptionTuple.lastRemainingSubscriptionsPerDmi()));
            scheduleNcmpOutEventResponse(subscriptionId, "subscriptionDeleteResponse");
        }
    }

    private Map<String, DmiCmSubscriptionDetails> mergeDmiCmSubscriptionDetailsPerDmiMaps(
            final DmiCmSubscriptionTuple dmiCmSubscriptionTuple) {
        final Map<String, DmiCmSubscriptionDetails> dmiCmSubscriptionDetailsMap =
                dmiCmSubscriptionDetailsPerDmiMapper.toDmiCmSubscriptionsPerDmi(
                dmiCmSubscriptionTuple.lastRemainingSubscriptionsPerDmi());
        final Map<String, DmiCmSubscriptionDetails> otherDmiCmSubscriptionDetailsMap =
                dmiCmSubscriptionDetailsPerDmiMapper.toDmiCmSubscriptionsPerDmi(
                dmiCmSubscriptionTuple.overlappingSubscriptionsPerDmi());
        final Map<String, DmiCmSubscriptionDetails> mergedDmiCmSubscriptionDetailsMap =
                new HashMap<>(dmiCmSubscriptionDetailsMap);
        otherDmiCmSubscriptionDetailsMap.forEach((dmiServiceName, dmiCmSubscriptionDetails) ->
                mergedDmiCmSubscriptionDetailsMap.merge(dmiServiceName, dmiCmSubscriptionDetails,
                        this::mergeDmiCmSubscriptionDetails));
        return mergedDmiCmSubscriptionDetailsMap;
    }

    private DmiCmSubscriptionDetails mergeDmiCmSubscriptionDetails(
            final DmiCmSubscriptionDetails dmiCmSubscriptionDetails,
            final DmiCmSubscriptionDetails otherDmiCmSubscriptionDetails) {
        final List<DmiCmSubscriptionPredicate> mergedDmiCmSubscriptionPredicates =
                new ArrayList<>(dmiCmSubscriptionDetails.getDmiCmSubscriptionPredicates());
        mergedDmiCmSubscriptionPredicates.addAll(otherDmiCmSubscriptionDetails.getDmiCmSubscriptionPredicates());
        return new DmiCmSubscriptionDetails(mergedDmiCmSubscriptionPredicates, CmSubscriptionStatus.PENDING);
    }

    private void scheduleNcmpOutEventResponse(final String subscriptionId, final String eventType) {
        ncmpOutEventProducer.publishNcmpOutEvent(subscriptionId, eventType, null, true);
    }

    private void rejectAndPublishCreateRequest(final String subscriptionId, final List<Predicate> predicates) {
        final Set<String> subscriptionTargetFilters =
                predicates.stream().flatMap(predicate -> predicate.getTargetFilter().stream())
                        .collect(Collectors.toSet());
        final NcmpOutEvent ncmpOutEvent = ncmpOutEventMapper.toNcmpOutEventForRejectedRequest(subscriptionId,
                new ArrayList<>(subscriptionTargetFilters));
        ncmpOutEventProducer.publishNcmpOutEvent(subscriptionId, "subscriptionCreateResponse", ncmpOutEvent, false);
    }

    private void acceptAndPublishDeleteRequest(final String subscriptionId) {
        final Set<String> dmiServiceNames = dmiCacheHandler.get(subscriptionId).keySet();
        for (final String dmiServiceName : dmiServiceNames) {
            dmiCacheHandler.updateDmiSubscriptionStatusPerDmi(subscriptionId, dmiServiceName,
                    CmSubscriptionStatus.ACCEPTED);
            dmiCacheHandler.removeFromDatabasePerDmi(subscriptionId, dmiServiceName);
        }
        final NcmpOutEvent ncmpOutEvent = ncmpOutEventMapper.toNcmpOutEvent(subscriptionId,
                dmiCacheHandler.get(subscriptionId));
        ncmpOutEventProducer.publishNcmpOutEvent(subscriptionId, "subscriptionDeleteResponse", ncmpOutEvent,
                false);
    }

    private void handleNewCmSubscription(final String subscriptionId) {
        final Map<String, DmiCmSubscriptionDetails> dmiSubscriptionsPerDmi =
                dmiCacheHandler.get(subscriptionId);
        dmiSubscriptionsPerDmi.forEach((dmiPluginName, dmiSubscriptionDetails) -> {
            final List<DmiCmSubscriptionPredicate> dmiCmSubscriptionPredicates =
                    cmSubscriptionComparator.getNewDmiSubscriptionPredicates(
                            dmiSubscriptionDetails.getDmiCmSubscriptionPredicates());

            if (dmiCmSubscriptionPredicates.isEmpty()) {
                acceptAndPublishNcmpOutEventPerDmi(subscriptionId, dmiPluginName);
            } else {
                publishDmiInEventPerDmi(subscriptionId, dmiPluginName, dmiCmSubscriptionPredicates);
            }
        });
    }

    private void publishDmiInEventPerDmi(final String subscriptionId, final String dmiPluginName,
                                         final List<DmiCmSubscriptionPredicate> dmiCmSubscriptionPredicates) {
        final DmiInEvent dmiInEvent = dmiInEventMapper.toDmiInEvent(dmiCmSubscriptionPredicates);
        dmiInEventProducer.publishDmiInEvent(subscriptionId, dmiPluginName,
                "subscriptionCreateRequest", dmiInEvent);
    }

    private void acceptAndPublishNcmpOutEventPerDmi(final String subscriptionId, final String dmiPluginName) {
        dmiCacheHandler.updateDmiSubscriptionStatusPerDmi(subscriptionId, dmiPluginName,
                CmSubscriptionStatus.ACCEPTED);
        dmiCacheHandler.persistIntoDatabasePerDmi(subscriptionId, dmiPluginName);
    }

    private void sendSubscriptionDeleteRequestToDmi(final String subscriptionId,
                                                    final Map<String, DmiCmSubscriptionDetails>
                                                            dmiCmSubscriptionsPerDmi) {
        dmiCmSubscriptionsPerDmi.forEach((dmiPluginName, dmiCmSubscriptionDetails) -> {
            final DmiInEvent dmiInEvent =
                    dmiInEventMapper.toDmiInEvent(
                            dmiCmSubscriptionDetails.getDmiCmSubscriptionPredicates());
            dmiInEventProducer.publishDmiInEvent(subscriptionId,
                    dmiPluginName, "subscriptionDeleteRequest", dmiInEvent);
        });
    }


    private DmiCmSubscriptionTuple getLastRemainingAndOverlappingSubscriptionsPerDmi(
            final Collection<DataNode> subscriptionNodes) {
        final Map<String, Collection<DmiCmSubscriptionKey>> lastRemainingSubscriptionsPerDmi = new HashMap<>();
        final Map<String, Collection<DmiCmSubscriptionKey>> overlappingSubscriptionsPerDmi = new HashMap<>();

        for (final DataNode subscriptionNode : subscriptionNodes) {
            final String xpath = subscriptionNode.getXpath();
            final DatastoreType datastoreType = extractCmSubscriptionDatastoreFromXpath(xpath);
            final String cmHandleId = extractCmSubscriptionCmHandleIdFromXpath(xpath);
            final String dmiServiceName = inventoryPersistence.getYangModelCmHandle(cmHandleId).getDmiServiceName();
            final List<String> subscribers = (List<String>) subscriptionNode.getLeaves().get("subscriptionIds");
            final DmiCmSubscriptionKey dmiCmSubscriptionKey =
                    new DmiCmSubscriptionKey(datastoreType.getDatastoreName(), cmHandleId,
                            extractCmSubscriptionFilterXpathFromXpath(xpath));
            populateDmiCmSubscriptionTuple(subscribers, overlappingSubscriptionsPerDmi,
                    lastRemainingSubscriptionsPerDmi, dmiServiceName, dmiCmSubscriptionKey);
        }
        return new DmiCmSubscriptionTuple(lastRemainingSubscriptionsPerDmi, overlappingSubscriptionsPerDmi);
    }

    private static void populateDmiCmSubscriptionTuple(final List<String> subscribers,
                                                       final Map<String, Collection<DmiCmSubscriptionKey>>
                                                               overlappingSubscriptionsPerDmi,
                                                       final Map<String, Collection<DmiCmSubscriptionKey>>
                                                               lastRemainingSubscriptionsPerDmi,
                                                       final String dmiServiceName,
                                                       final DmiCmSubscriptionKey dmiCmSubscriptionKey) {
        final Map<String, Collection<DmiCmSubscriptionKey>> targetMap =
                subscribers.size() > 1 ? overlappingSubscriptionsPerDmi : lastRemainingSubscriptionsPerDmi;
        targetMap.computeIfAbsent(dmiServiceName, dmiName -> new HashSet<>()).add(dmiCmSubscriptionKey);
    }


    private DatastoreType extractCmSubscriptionDatastoreFromXpath(final String xpath) {
        final Matcher matcher = DATASTORE_FROM_XPATH_PATTERN.matcher(xpath);
        if (matcher.find()) {
            final String datastoreName = matcher.group(1);
            return DatastoreType.fromDatastoreName(datastoreName);
        }
        return null;
    }

    private String extractCmSubscriptionFilterXpathFromXpath(final String xpath) {
        final Matcher matcher = FILTER_XPATH_FROM_XPATH_PATTERN.matcher(xpath);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    private String extractCmSubscriptionCmHandleIdFromXpath(final String xpath) {
        final Matcher matcher = CMHANDLE_ID_FROM_XPATH_PATTERN.matcher(xpath);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

}