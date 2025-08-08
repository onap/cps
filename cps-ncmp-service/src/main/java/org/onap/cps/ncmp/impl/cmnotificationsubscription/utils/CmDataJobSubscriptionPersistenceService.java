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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.utils;

import static org.onap.cps.api.parameters.FetchDescendantsOption.OMIT_DESCENDANTS;

import java.io.Serializable;
import java.time.OffsetDateTime;
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

    private static final String NCMP_DATASPACE_NAME = "NCMP-Admin";
    private static final String CM_DATA_JOB_SUBSCRIPTIONS_ANCHOR_NAME = "cm-data-job-subscriptions";
    private static final String CM_DATA_JOB_SUBSCRIPTIONS_PARENT_NODE_XPATH = "/dataJob";
    private static final String CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE =
            "/dataJob/subscription[@alternateId='%s' and @dataTypeId='%s']";
    private static final String CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_DATA_JOB_ID =
            "//subscription/dataJobId[text()='%s']";

    private final JsonObjectMapper jsonObjectMapper;
    private final CpsQueryService cpsQueryService;
    private final CpsDataService cpsDataService;

    /**
     * Check if we have an ongoing cm data job subscription based on the parameters.
     *
     * @param dataType      the data type of the data job subscription
     * @param alternateId   the alternate id target of the data job subscription
     * @return              true for ongoing cm data job subscription , otherwise false
     */
    public boolean isOngoingSubscriptionDetails(final String dataType, final String alternateId) {
        return !getOngoingSubscriptionIds(dataType, alternateId).isEmpty();
    }

    /**
     * Check if the input is a new subscription ID against ongoing subscriptions.
     *
     * @param subscriptionId subscription ID
     * @return true if subscriptionId is not used in active subscriptions, otherwise false
     */
    public boolean isNewSubscriptionId(final String subscriptionId) {
        final String query = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_DATA_JOB_ID.formatted(subscriptionId);
        return cpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, CM_DATA_JOB_SUBSCRIPTIONS_ANCHOR_NAME,
                query, OMIT_DESCENDANTS).isEmpty();
    }

    /**
     * Get all ongoing cm data job subscription based on the parameters.
     *
     * @param dataType      the data type of the data job subscription
     * @param alternateId   the alternate id target of the data job subscription
     * @return              collection of subscription ids of ongoing cm notification subscription
     */
    public Collection<String> getOngoingSubscriptionIds(final String dataType,
                                                        final String alternateId) {
        final String query = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE.formatted(
                alternateId, dataType);
        final Collection<DataNode> existingNodes =
                cpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, CM_DATA_JOB_SUBSCRIPTIONS_ANCHOR_NAME,
                        query, OMIT_DESCENDANTS);
        if (existingNodes.isEmpty()) {
            return Collections.emptyList();
        }
        return (List<String>) existingNodes.iterator().next().getLeaves().get("dataJobId");
    }

    /**
     * Add cm notification data job subscription.
     *
     * @param dataType          the data type of the data job subscription
     * @param alternateId       the alternate id target of the data job subscription
     * @param newSubscriptionId data job subscription id to be added
     */
    public void addSubscription(final String dataType,
                                final String alternateId,
                                final String newSubscriptionId) {
        final Collection<String> subscriptionIds =
                getOngoingSubscriptionIds(dataType, alternateId);
        if (subscriptionIds.isEmpty()) {
            addNewSubscriptionDetails(dataType, alternateId, newSubscriptionId);
        } else {
            subscriptionIds.add(newSubscriptionId);
            updateSubscriptionDetails(subscriptionIds, dataType, alternateId);
        }
    }

    /**
     * Remove cm notification data job Subscription.
     *
     * @param dataType          the data type of the data job subscription
     * @param alternateId       the alternate id target of the data job subscription
     * @param subscriptionId    data subscription id to remove
     */
    public void removeSubscription(final String dataType,
                                   final String alternateId,
                                   final String subscriptionId) {
        final Collection<String> subscriptionIds =
                getOngoingSubscriptionIds(dataType, alternateId);
        if (subscriptionIds.remove(subscriptionId)) {
            updateSubscriptionDetails(subscriptionIds, dataType, alternateId);
            final String query = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE.formatted(
                    alternateId, dataType);
            log.info("There are subscribers left for the following cps path {} :", query);
            if (subscriptionIds.isEmpty()) {
                log.info("No subscribers left for the following cps path {} :", query);
                deleteUnusedSubscriptionDetails(dataType, alternateId);
            }
        }
    }

    /**
     * Retrieve all existing data nodes for given data job subscription id.
     *
     * @param subscriptionId  data job subscription id
     * @return                collection of DataNodes
     */
    public Collection<DataNode> getAffectedDataNodes(final String subscriptionId) {
        final String query = CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_DATA_JOB_ID.formatted(subscriptionId);
        return cpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, CM_DATA_JOB_SUBSCRIPTIONS_ANCHOR_NAME,
                query, OMIT_DESCENDANTS);
    }

    private void deleteUnusedSubscriptionDetails(final String dataType, final String alternateId) {
        final String deleteListOfSubscriptionCpsPathQuery =
                CPS_PATH_TEMPLATE_FOR_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE.formatted(alternateId,
                        dataType);
        cpsDataService.deleteDataNode(NCMP_DATASPACE_NAME, CM_DATA_JOB_SUBSCRIPTIONS_ANCHOR_NAME,
                deleteListOfSubscriptionCpsPathQuery, OffsetDateTime.now());
    }

    private void addNewSubscriptionDetails(final String dataType,
                                           final String alternateId,
                                           final String subscriptionId) {
        final Collection<String> newSubscriptionList = Collections.singletonList(subscriptionId);
        final String subscriptionDetailsAsJson = getSubscriptionDetailsAsJson(newSubscriptionList, dataType,
                alternateId);
        cpsDataService.saveData(NCMP_DATASPACE_NAME, CM_DATA_JOB_SUBSCRIPTIONS_ANCHOR_NAME, subscriptionDetailsAsJson,
                OffsetDateTime.now(), ContentType.JSON);
    }

    private void updateSubscriptionDetails(final Collection<String> subscriptionIds, final String dataType,
                                           final String alternateId) {
        final String subscriptionDetailsAsJson = getSubscriptionDetailsAsJson(subscriptionIds, dataType, alternateId);
        cpsDataService.updateNodeLeaves(NCMP_DATASPACE_NAME, CM_DATA_JOB_SUBSCRIPTIONS_ANCHOR_NAME,
                CM_DATA_JOB_SUBSCRIPTIONS_PARENT_NODE_XPATH, subscriptionDetailsAsJson, OffsetDateTime.now(),
                ContentType.JSON);
    }

    private String getSubscriptionDetailsAsJson(final Collection<String> subscriptionIds,
                                                final String dataTypeId,
                                                final String alternateId) {
        final Map<String, Serializable> subscriptionDetailsAsMap =
                Map.of("dataTypeId", dataTypeId,
                        "alternateId", alternateId,
                        "dataJobId", (Serializable) subscriptionIds);
        return "{\"subscription\":[" + jsonObjectMapper.asJsonString(subscriptionDetailsAsMap) + "]}";
    }

}

