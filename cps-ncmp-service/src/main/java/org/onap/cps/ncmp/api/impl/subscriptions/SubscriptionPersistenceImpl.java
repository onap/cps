/*
 *  ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.api.impl.subscriptions;

import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NO_TIMESTAMP;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.ncmp.api.impl.utils.DataNodeHelper;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class SubscriptionPersistenceImpl implements SubscriptionPersistence {

    private static final String SUBSCRIPTION_DATASPACE_NAME = "NCMP-Admin";
    private static final String SUBSCRIPTION_ANCHOR_NAME = "AVC-Subscriptions";
    private static final String SUBSCRIPTION_REGISTRY_PARENT = "/subscription-registry";

    private final JsonObjectMapper jsonObjectMapper;
    private final CpsDataService cpsDataService;

    @Override
    public void saveSubscriptionEvent(final YangModelSubscriptionEvent yangModelSubscriptionEvent) {
        final String clientId = yangModelSubscriptionEvent.getClientId();
        final String subscriptionName = yangModelSubscriptionEvent.getSubscriptionName();

        final Collection<DataNode> dataNodes = cpsDataService.getDataNodes(SUBSCRIPTION_DATASPACE_NAME,
                SUBSCRIPTION_ANCHOR_NAME, SUBSCRIPTION_REGISTRY_PARENT, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);

        if (isSubscriptionRegistryEmptyOrNonExist(dataNodes, clientId, subscriptionName)) {
            saveSubscriptionEventYangModel(createSubscriptionEventJsonData(
                    jsonObjectMapper.asJsonString(yangModelSubscriptionEvent)));
        } else {
            findDeltaCmHandlesAddOrUpdateInDatabase(yangModelSubscriptionEvent, clientId, subscriptionName, dataNodes);
        }
    }

    private void findDeltaCmHandlesAddOrUpdateInDatabase(final YangModelSubscriptionEvent yangModelSubscriptionEvent,
                                                         final String clientId, final String subscriptionName,
                                                         final Collection<DataNode> dataNodes) {
        final Map<String, SubscriptionStatus> cmHandleIdsFromYangModel =
                extractCmHandleFromYangModelAsMap(yangModelSubscriptionEvent);
        final Map<String, SubscriptionStatus> cmHandleIdsFromDatabase =
                extractCmHandleFromDbAsMap(dataNodes);

        final Map<String, SubscriptionStatus> newCmHandles =
                mapDifference(cmHandleIdsFromYangModel, cmHandleIdsFromDatabase);
        traverseCmHandleList(newCmHandles, clientId, subscriptionName, true);

        final Map<String, SubscriptionStatus> existingCmHandles =
                mapDifference(cmHandleIdsFromYangModel, newCmHandles);
        traverseCmHandleList(existingCmHandles, clientId, subscriptionName, false);
    }

    private boolean isSubscriptionRegistryEmptyOrNonExist(final Collection<DataNode> dataNodes,
                                                          final String clientId, final String subscriptionName) {
        final Optional<DataNode> dataNodeFirst = dataNodes.stream().findFirst();
        return ((dataNodeFirst.isPresent() && dataNodeFirst.get().getChildDataNodes().isEmpty())
                || getCmHandlesForSubscriptionEvent(clientId, subscriptionName).isEmpty());
    }

    private void traverseCmHandleList(final Map<String, SubscriptionStatus> cmHandleMap,
                                      final String clientId,
                                      final String subscriptionName,
                                      final boolean isAddListElementOperation) {
        final List<YangModelSubscriptionEvent.TargetCmHandle> cmHandleList =
                targetCmHandlesAsList(cmHandleMap);
        for (final YangModelSubscriptionEvent.TargetCmHandle targetCmHandle : cmHandleList) {
            final String targetCmHandleAsJson =
                    createTargetCmHandleJsonData(jsonObjectMapper.asJsonString(targetCmHandle));
            addOrReplaceCmHandlePredicateListElement(targetCmHandleAsJson, clientId, subscriptionName,
                    isAddListElementOperation);
        }
    }

    private void addOrReplaceCmHandlePredicateListElement(final String targetCmHandleAsJson,
                                                          final String clientId,
                                                          final String subscriptionName,
                                                          final boolean isAddListElementOperation) {
        if (isAddListElementOperation) {
            log.info("targetCmHandleAsJson to be added into DB {}", targetCmHandleAsJson);
            cpsDataService.saveListElements(SUBSCRIPTION_DATASPACE_NAME,
                    SUBSCRIPTION_ANCHOR_NAME, createCmHandleXpathPredicates(clientId, subscriptionName),
                    targetCmHandleAsJson, NO_TIMESTAMP);
        } else {
            log.info("targetCmHandleAsJson to be updated into DB {}", targetCmHandleAsJson);
            cpsDataService.updateNodeLeaves(SUBSCRIPTION_DATASPACE_NAME,
                    SUBSCRIPTION_ANCHOR_NAME, createCmHandleXpathPredicates(clientId, subscriptionName),
                    targetCmHandleAsJson, NO_TIMESTAMP);
        }
    }

    private void saveSubscriptionEventYangModel(final String subscriptionEventJsonData) {
        log.info("SubscriptionEventJsonData to be saved into DB {}", subscriptionEventJsonData);
        cpsDataService.saveListElements(SUBSCRIPTION_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME,
                SUBSCRIPTION_REGISTRY_PARENT, subscriptionEventJsonData, NO_TIMESTAMP);
    }

    @Override
    public Collection<DataNode> getDataNodesForSubscriptionEvent() {
        return cpsDataService.getDataNodes(SUBSCRIPTION_DATASPACE_NAME,
                SUBSCRIPTION_ANCHOR_NAME, SUBSCRIPTION_REGISTRY_PARENT,
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
    }

    @Override
    public Collection<DataNode> getCmHandlesForSubscriptionEvent(final String clientId, final String subscriptionName) {
        return cpsDataService.getDataNodesForMultipleXpaths(SUBSCRIPTION_DATASPACE_NAME,
                SUBSCRIPTION_ANCHOR_NAME, Arrays.asList(createCmHandleXpath(clientId, subscriptionName)),
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
    }

    private static Map<String, SubscriptionStatus> extractCmHandleFromDbAsMap(final Collection<DataNode> dataNodes) {
        final List<Map<String, Serializable>> dataNodeLeaves = DataNodeHelper.getDataNodeLeaves(dataNodes);
        final List<Collection<Serializable>> cmHandleIdToStatus = DataNodeHelper.getCmHandleIdToStatus(dataNodeLeaves);
        return DataNodeHelper.getCmHandleIdToStatusMap(cmHandleIdToStatus);
    }

    private static Map<String, SubscriptionStatus> extractCmHandleFromYangModelAsMap(
            final YangModelSubscriptionEvent yangModelSubscriptionEvent) {
        return yangModelSubscriptionEvent.getPredicates().getTargetCmHandles()
                .stream().collect(Collectors.toMap(
                        YangModelSubscriptionEvent.TargetCmHandle::getCmHandleId,
                        YangModelSubscriptionEvent.TargetCmHandle::getStatus));
    }

    private static List<YangModelSubscriptionEvent.TargetCmHandle> targetCmHandlesAsList(
            final Map<String, SubscriptionStatus> newCmHandles) {
        return newCmHandles.entrySet().stream().map(entry ->
                new YangModelSubscriptionEvent.TargetCmHandle(entry.getKey(),
                        entry.getValue())).collect(Collectors.toList());
    }

    private static String createSubscriptionEventJsonData(final String yangModelSubscriptionAsJson) {
        return "{\"subscription\":[" + yangModelSubscriptionAsJson + "]}";
    }

    private static String createTargetCmHandleJsonData(final String targetCmHandleAsJson) {
        return "{\"targetCmHandles\":[" + targetCmHandleAsJson + "]}";
    }

    private static String createCmHandleXpathPredicates(final String clientId, final String subscriptionName) {
        return "/subscription-registry/subscription[@clientID='" + clientId
                + "' and @subscriptionName='" + subscriptionName + "']/predicates";
    }

    private static String createCmHandleXpath(final String clientId, final String subscriptionName) {
        return "/subscription-registry/subscription[@clientID='" + clientId
                + "' and @subscriptionName='" + subscriptionName + "']";
    }

    private static <K, V> Map<K, V> mapDifference(final Map<? extends K, ? extends V> left,
                                                  final Map<? extends K, ? extends V> right) {
        final Map<K, V> difference = new HashMap<>();
        difference.putAll(left);
        difference.putAll(right);
        difference.entrySet().removeAll(right.entrySet());
        return difference;
    }
}
