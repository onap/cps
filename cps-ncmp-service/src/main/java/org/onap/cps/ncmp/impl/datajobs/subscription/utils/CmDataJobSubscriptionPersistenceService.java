/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2024 TechMahindra Ltd.
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

package org.onap.cps.ncmp.impl.datajobs.subscription.utils;

import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS;
import static org.onap.cps.ncmp.impl.datajobs.subscription.models.CmSubscriptionStatus.UNKNOWN;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsQueryService;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CmDataJobSubscriptionPersistenceService {

    private static final String DATASPACE = "NCMP-Admin";
    private static final String ANCHOR = "cm-data-job-subscriptions";

    private static final String PARENT_NODE_XPATH = "/dataJob";
    private static final String CPS_PATH_FOR_SUBSCRIPTION_NODE = "//subscription";
    private static final String CPS_PATH_TEMPLATE_FOR_SUBSCRIPTIONS_WITH_DATA_NODE_SELECTOR =
            CPS_PATH_FOR_SUBSCRIPTION_NODE + "[@dataNodeSelector='%s']";
    private static final String CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_DATA_JOB_ID =
            CPS_PATH_FOR_SUBSCRIPTION_NODE + "/dataJobId[text()='%s']";
    private static final String CPS_PATH_TEMPLATE_FOR_INACTIVE_SUBSCRIPTIONS =
            CPS_PATH_FOR_SUBSCRIPTION_NODE + "[@status='UNKNOWN' or @status='REJECTED']/dataJobId[text()='%s']";

    private final JsonObjectMapper jsonObjectMapper;
    private final CpsQueryService cpsQueryService;
    private final CpsDataService cpsDataService;

    /**
     * Check if we have a cm data job subscription for the given data node selector.
     *
     * @param dataNodeSelector the target of the data job subscription
     * @return true if the subscription details has at least one subscriber , otherwise false
     */
    public boolean hasAtLeastOneSubscription(final String dataNodeSelector) {
        return !getSubscriptionIds(dataNodeSelector).isEmpty();
    }

    /**
     * Check if the input is a new subscription ID against ongoing subscriptions.
     *
     * @param subscriptionId subscription ID
     * @return true if subscriptionId is not used in active subscriptions, otherwise false
     */
    public boolean isNewSubscriptionId(final String subscriptionId) {
        final String query = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_DATA_JOB_ID.formatted(subscriptionId);
        return cpsQueryService.queryDataNodes(DATASPACE, ANCHOR,
                query, OMIT_DESCENDANTS).isEmpty();
    }

    /**
     * Get the ids for the subscriptions for the given data node selector.
     *
     * @param dataNodeSelector the target of the data job subscription
     * @return collection of subscription ids of ongoing cm notification subscription
     */
    @SuppressWarnings("unchecked")
    public Collection<String> getSubscriptionIds(final String dataNodeSelector) {
        final String query = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTIONS_WITH_DATA_NODE_SELECTOR.formatted(dataNodeSelector);
        final Collection<DataNode> existingNodes =
                cpsQueryService.queryDataNodes(DATASPACE, ANCHOR,
                        query, OMIT_DESCENDANTS);
        if (existingNodes.isEmpty()) {
            return Collections.emptyList();
        }
        return (Collection<String>) existingNodes.iterator().next().getLeaves().get("dataJobId");
    }

    /**
     * Get data node selectors for subscriptions with subscription ID.
     *
     * @param subscriptionId subscription ID
     * @return a list of dataNodeSelectors, or empty list if none found
     */
    public Collection<String> getDataNodeSelectorsBySubscriptionId(final String subscriptionId) {
        final String query = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_DATA_JOB_ID.formatted(subscriptionId);
        final Collection<DataNode> subscriptionNodes =
                cpsQueryService.queryDataNodes(DATASPACE, ANCHOR, query, OMIT_DESCENDANTS);

        if (subscriptionNodes.isEmpty()) {
            return Collections.emptyList();
        }

        final List<String> dataNodeSelectors = new ArrayList<>();
        for (final DataNode subscriptionNode : subscriptionNodes) {
            final Object selector = subscriptionNode.getLeaves().get("dataNodeSelector");
            if (selector != null) {
                dataNodeSelectors.add(selector.toString());
            }
        }
        return dataNodeSelectors;
    }

    /**
     * Remove cm notification data job subscription.
     *
     * @param subscriptionId   data job subscription id to be deleted
     * @param dataNodeSelector the target of the data job subscription
     */
    public void remove(final String subscriptionId, final String dataNodeSelector) {
        final String query = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTIONS_WITH_DATA_NODE_SELECTOR.formatted(dataNodeSelector);
        final Collection<DataNode> dataNodes =
                cpsQueryService.queryDataNodes(DATASPACE, ANCHOR, query, OMIT_DESCENDANTS);

        if (dataNodes.isEmpty()) {
            log.warn("No subscription details found for dataNodeSelector={} when trying to remove subscriptionId={}",
                    dataNodeSelector, subscriptionId);
            return;
        }

        final Collection<String> subscriptionIds = getSubscriptionIds(dataNodeSelector);
        final String currentStatus = dataNodes.iterator().next().getLeaves().get("status").toString();

        if (!subscriptionIds.remove(subscriptionId)) {
            log.warn("SubscriptionId={} not found under dataNodeSelector={}", subscriptionId, dataNodeSelector);
            return;
        }

        if (subscriptionIds.isEmpty()) {
            log.info("Last subscriber removed for dataNodeSelector={}, setting status as UNKNOWN", dataNodeSelector);
            updateSubscriptionDetails(dataNodeSelector, Collections.emptyList(), UNKNOWN.name());
        } else {
            log.info("Removed subscriber {} from dataNodeSelector={}, remaining subscribers={}",
                    subscriptionId, dataNodeSelector, subscriptionIds);
            updateSubscriptionDetails(dataNodeSelector, subscriptionIds, currentStatus);
        }
    }

    /**
     * Get data node selectors for subscriptions with status UNKNOWN or REJECTED.
     *
     * @param subscriptionId subscription ID
     * @return a list of data node selectors
     */
    public List<String> getInactiveDataNodeSelectors(final String subscriptionId) {
        final String query = CPS_PATH_TEMPLATE_FOR_INACTIVE_SUBSCRIPTIONS.formatted(subscriptionId);
        final Collection<DataNode> dataNodes = cpsQueryService.queryDataNodes(DATASPACE, ANCHOR, query,
                OMIT_DESCENDANTS);
        final List<String> dataNodeSelectors = new ArrayList<>(dataNodes.size());
        for (final DataNode dataNode : dataNodes) {
            final String dataNodeSelector = dataNode.getLeaves().get("dataNodeSelector").toString();
            dataNodeSelectors.add(dataNodeSelector);
        }
        return dataNodeSelectors;
    }

    /**
     * Add cm notification data job subscription.
     *
     * @param subscriptionId   data job subscription id to be added
     * @param dataNodeSelector the target of the data job subscription
     */
    public void add(final String subscriptionId, final String dataNodeSelector) {
        final String query = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTIONS_WITH_DATA_NODE_SELECTOR.formatted(dataNodeSelector);
        final Collection<DataNode> dataNodes = cpsQueryService.queryDataNodes(DATASPACE, ANCHOR, query,
                OMIT_DESCENDANTS);
        if (dataNodes.isEmpty()) {
            addNewSubscriptionDetails(subscriptionId, dataNodeSelector);
        } else {
            final Collection<String> subscriptionIds = getSubscriptionIds(dataNodeSelector);
            final String status = dataNodes.iterator().next().getLeaves().get("status").toString();
            subscriptionIds.add(subscriptionId);
            updateSubscriptionDetails(dataNodeSelector, subscriptionIds, status);
        }
    }

    private void addNewSubscriptionDetails(final String subscriptionId,
                                           final String dataNodeSelector) {
        final Collection<String> newSubscriptionList = Collections.singletonList(subscriptionId);
        final String status = UNKNOWN.name();
        final String subscriptionDetailsAsJson = createSubscriptionDetailsAsJson(dataNodeSelector,
                newSubscriptionList, status);
        cpsDataService.saveData(DATASPACE, ANCHOR, subscriptionDetailsAsJson,
                OffsetDateTime.now(), ContentType.JSON);
    }

    private void updateSubscriptionDetails(final String dataNodeSelector, final Collection<String> subscriptionIds,
                                           final String status) {
        final String subscriptionDetailsAsJson = createSubscriptionDetailsAsJson(dataNodeSelector,
                subscriptionIds, status);
        cpsDataService.updateNodeLeaves(DATASPACE, ANCHOR,
                PARENT_NODE_XPATH, subscriptionDetailsAsJson, OffsetDateTime.now(),
                ContentType.JSON);
    }

    private String createSubscriptionDetailsAsJson(final String dataNodeSelector,
                                                   final Collection<String> subscriptionIds,
                                                   final String status) {
        final Map<String, Serializable> subscriptionDetailsAsMap =
                Map.of("dataNodeSelector", dataNodeSelector,
                        "dataJobId", (Serializable) subscriptionIds,
                        "status", status);
        return "{\"subscription\":[" + jsonObjectMapper.asJsonString(subscriptionDetailsAsMap) + "]}";
    }

    /**
     * Get all subscribers for a given dataNodeSelector.
     *
     * @param dataNodeSelector the selector key
     * @return list of subscriptionIds, or empty if none
     */
    public Collection<String> getSubscribers(final String dataNodeSelector) {
        return getSubscriptionIds(dataNodeSelector);
    }

    /**
     * Update only the status of a subscription record while keeping existing subscribers.
     *
     * @param dataNodeSelector selector key
     * @param status           new status (e.g., "UNKNOWN")
     */
    public void updateStatus(final String dataNodeSelector, final String status) {
        final Collection<String> subscriptionIds = getSubscriptionIds(dataNodeSelector);
        if (!subscriptionIds.isEmpty()) {
            final String subscriptionDetailsAsJson =
                    createSubscriptionDetailsAsJson(dataNodeSelector, subscriptionIds, status);
            cpsDataService.updateNodeLeaves(
                    DATASPACE,
                    ANCHOR,
                    PARENT_NODE_XPATH,
                    subscriptionDetailsAsJson,
                    OffsetDateTime.now(),
                    ContentType.JSON
            );
        } else {
            log.warn("Attempted to update status for {} but no subscribers found.", dataNodeSelector);
        }
    }

}

