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
import org.onap.cps.ncmp.impl.datajobs.subscription.models.CmSubscriptionStatus;
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
     * Get data node selectors for subscriptions with status UNKNOWN or REJECTED.
     *
     * @param subscriptionId    subscription ID
     * @return                  a list of data node selectors
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
            final String cmSubscriptionStatusName = dataNodes.iterator().next().getLeaves().get("status").toString();
            subscriptionIds.add(subscriptionId);
            updateSubscriptionDetails(dataNodeSelector, subscriptionIds, cmSubscriptionStatusName);
        }
    }

    /**
     * Update status of a subscription.
     *
     * @param dataNodeSelector     data node selector
     * @param cmSubscriptionStatus cm subscription status
     */
    public void updateCmSubscriptionStatus(final String dataNodeSelector,
                                           final CmSubscriptionStatus cmSubscriptionStatus) {
        final Collection<String> subscriptionIds = getSubscriptionIds(dataNodeSelector);
        updateSubscriptionDetails(dataNodeSelector, subscriptionIds, cmSubscriptionStatus.name());
    }

    private void addNewSubscriptionDetails(final String subscriptionId,
                                           final String dataNodeSelector) {
        final Collection<String> newSubscriptionList = Collections.singletonList(subscriptionId);
        final String cmSubscriptionStatus = UNKNOWN.name();
        final String subscriptionDetailsAsJson = createSubscriptionDetailsAsJson(dataNodeSelector,
                newSubscriptionList, cmSubscriptionStatus);
        cpsDataService.saveData(DATASPACE, ANCHOR, subscriptionDetailsAsJson,
            OffsetDateTime.now(), ContentType.JSON);
    }

    private void updateSubscriptionDetails(final String dataNodeSelector, final Collection<String> subscriptionIds,
                                           final String cmSubscriptionStatusName) {
        final String subscriptionDetailsAsJson = createSubscriptionDetailsAsJson(dataNodeSelector,
                subscriptionIds, cmSubscriptionStatusName);
        cpsDataService.updateNodeLeaves(DATASPACE, ANCHOR,
                PARENT_NODE_XPATH, subscriptionDetailsAsJson, OffsetDateTime.now(),
            ContentType.JSON);
    }

    private String createSubscriptionDetailsAsJson(final String dataNodeSelector,
                                                   final Collection<String> subscriptionIds,
                                                   final String cmSubscriptionStatusName) {
        final Map<String, Serializable> subscriptionDetailsAsMap =
            Map.of("dataNodeSelector", dataNodeSelector,
                "dataJobId", (Serializable) subscriptionIds,
                "status", cmSubscriptionStatusName);
        return "{\"subscription\":[" + jsonObjectMapper.asJsonString(subscriptionDetailsAsMap) + "]}";
    }
}

