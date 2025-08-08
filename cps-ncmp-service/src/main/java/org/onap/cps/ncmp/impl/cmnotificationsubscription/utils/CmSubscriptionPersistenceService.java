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
import org.onap.cps.ncmp.api.data.models.DatastoreType;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CmSubscriptionPersistenceService {

    private static final String NCMP_DATASPACE_NAME = "NCMP-Admin";
    private static final String CM_DATA_JOB_SUBSCRIPTIONS_ANCHOR_NAME = "cm-data-job-subscriptions";
    private static final String CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_AND_CMHANDLE = """
            /datastores/datastore[@name='%s']/cm-handles/cm-handle[@id='%s']
            """.trim();
    private static final String CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_FILTERS_WITH_DATASTORE_AND_CMHANDLE =
            CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_AND_CMHANDLE + "/filters";

    private static final String CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_CMHANDLE_AND_XPATH =
            CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_FILTERS_WITH_DATASTORE_AND_CMHANDLE + "/filter[@xpath='%s']";

    private static final String CPS_PATH_QUERY_FOR_CM_DATA_JOB_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE = """
                /dataJob/subscription[@alternateId='%s' and @dataTypeId='%s']
                """.trim();

    private static final String CPS_PATH_QUERY_FOR_CM_DATA_JOB_SUBSCRIPTION_WITH_DATA_JOB_ID = """
            //subscription/dataJobId[text()='%s']
            """.trim();


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
    public boolean isOngoingCmDataJobSubscription(final String dataType, final String alternateId) {
        return !getOngoingCmDataJobSubscriptionIds(dataType, alternateId).isEmpty();
    }

    /**
     * Check if the data job subscription ID is unique against ongoing subscriptions.
     *
     * @param dataJobSubscriptionId subscription ID
     * @return true if dataJobSubscriptionId is not used in active subscriptions, otherwise false
     */
    public boolean isUniqueDataJobSubscriptionId(final String dataJobSubscriptionId) {
        return cpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, CM_DATA_JOB_SUBSCRIPTIONS_ANCHOR_NAME,
                CPS_PATH_QUERY_FOR_CM_DATA_JOB_SUBSCRIPTION_WITH_DATA_JOB_ID.formatted(dataJobSubscriptionId),
                OMIT_DESCENDANTS).isEmpty();
    }

    /**
     * Get all ongoing cm data job subscription based on the parameters.
     *
     * @param dataType      the data type of the data job subscription
     * @param alternateId   the alternate id target of the data job subscription
     * @return              collection of subscription ids of ongoing cm notification subscription
     */
    public Collection<String> getOngoingCmDataJobSubscriptionIds(final String dataType,
                                                                 final String alternateId) {
        final String isOngoingCmDataJobSubscriptionCpsPathQuery =
                CPS_PATH_QUERY_FOR_CM_DATA_JOB_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE.formatted(
                        alternateId, dataType);
        final Collection<DataNode> existingNodes =
                cpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, CM_DATA_JOB_SUBSCRIPTIONS_ANCHOR_NAME,
                        isOngoingCmDataJobSubscriptionCpsPathQuery, OMIT_DESCENDANTS);
        if (existingNodes.isEmpty()) {
            return Collections.emptyList();
        }
        return (List<String>) existingNodes.iterator().next().getLeaves().get("dataJobId");
    }

    /**
     * Add cm notification subscription.
     *
     * @param datastoreType     the susbcription target datastore type
     * @param cmHandleId        the id of the cm handle for the susbcription
     * @param xpath             the target xpath
     * @param newSubscriptionId subscription id to be added
     */
    public void addCmSubscription(final DatastoreType datastoreType, final String cmHandleId,
            final String xpath, final String newSubscriptionId) {
        final Collection<String> subscriptionIds =
                getOngoingCmDataJobSubscriptionIds(datastoreType.getDatastoreName(), cmHandleId);
        if (subscriptionIds.isEmpty()) {
            addFirstSubscriptionForDatastoreCmHandleAndXpath(datastoreType, cmHandleId, xpath, newSubscriptionId);
        } else if (!subscriptionIds.contains(newSubscriptionId)) {
            subscriptionIds.add(newSubscriptionId);
            saveSubscriptionDetails(datastoreType, cmHandleId, xpath, subscriptionIds);
        }
    }

    /**
     * Remove cm notification Subscription.
     *
     * @param datastoreType  the subscription target datastore type
     * @param cmHandleId     the id of the cm handle for the subscription
     * @param xpath          the target xpath
     * @param subscriptionId subscription id to remove
     */
    public void removeCmSubscription(final DatastoreType datastoreType, final String cmHandleId,
            final String xpath, final String subscriptionId) {
        final Collection<String> subscriptionIds =
                getOngoingCmDataJobSubscriptionIds(datastoreType.getDatastoreName(), cmHandleId);
        if (subscriptionIds.remove(subscriptionId)) {
            saveSubscriptionDetails(datastoreType, cmHandleId, xpath, subscriptionIds);
            log.info("There are subscribers left for the following cps path {} :",
                    CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_CMHANDLE_AND_XPATH.formatted(
                            datastoreType.getDatastoreName(), cmHandleId, escapeQuotesByDoublingThem(xpath)));
            if (subscriptionIds.isEmpty()) {
                log.info("No subscribers left for the following cps path {} :",
                        CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_CMHANDLE_AND_XPATH.formatted(
                                datastoreType.getDatastoreName(), cmHandleId, escapeQuotesByDoublingThem(xpath)));
                deleteListOfSubscriptionsFor(datastoreType, cmHandleId);
            }
        }
    }

    /**
     * Retrieve all existing data nodes for given data job subscription id.
     *
     * @param subscriptionId  data job subscription id
     * @return                collection of DataNodes
     */
    public Collection<DataNode> getAllNodesForDataJobSubscriptionId(final String subscriptionId) {
        return cpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, CM_DATA_JOB_SUBSCRIPTIONS_ANCHOR_NAME,
                CPS_PATH_QUERY_FOR_CM_DATA_JOB_SUBSCRIPTION_WITH_DATA_JOB_ID.formatted(subscriptionId),
                OMIT_DESCENDANTS);
    }

    private void deleteListOfSubscriptionsFor(final DatastoreType datastoreType, final String cmHandleId) {
        cpsDataService.deleteDataNode(NCMP_DATASPACE_NAME, CM_DATA_JOB_SUBSCRIPTIONS_ANCHOR_NAME,
                CPS_PATH_QUERY_FOR_CM_DATA_JOB_SUBSCRIPTION_WITH_ALTERNATE_ID_AND_DATATYPE.formatted(cmHandleId,
                        datastoreType.getDatastoreName()),
                OffsetDateTime.now());
    }

    private void removeCmHandleFromDatastore(final String datastoreName, final String cmHandleId) {
        cpsDataService.deleteDataNode(NCMP_DATASPACE_NAME, CM_DATA_JOB_SUBSCRIPTIONS_ANCHOR_NAME,
                CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_AND_CMHANDLE.formatted(datastoreName, cmHandleId),
                OffsetDateTime.now());
    }

    private boolean isFirstSubscriptionForCmHandle(final DatastoreType datastoreType, final String cmHandleId) {
        return cpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, CM_DATA_JOB_SUBSCRIPTIONS_ANCHOR_NAME,
                CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_FILTERS_WITH_DATASTORE_AND_CMHANDLE.formatted(
                        datastoreType.getDatastoreName(), cmHandleId), OMIT_DESCENDANTS).isEmpty();
    }

    private void addFirstSubscriptionForDatastoreCmHandleAndXpath(final DatastoreType datastoreType,
            final String cmHandleId, final String xpath, final String subscriptionId) {
        final Collection<String> newSubscriptionList = Collections.singletonList(subscriptionId);
        final String subscriptionDetailsAsJson = getSubscriptionDetailsAsJson(xpath, newSubscriptionList);
        if (isFirstSubscriptionForCmHandle(datastoreType, cmHandleId)) {
            final String parentXpath =
                    "/datastores/datastore[@name='%s']/cm-handles".formatted(datastoreType.getDatastoreName());
            final String subscriptionAsJson =
                    String.format("{\"cm-handle\":[{\"id\":\"%s\",\"filters\":%s}]}", cmHandleId,
                            subscriptionDetailsAsJson);
            cpsDataService.saveData(NCMP_DATASPACE_NAME, CM_DATA_JOB_SUBSCRIPTIONS_ANCHOR_NAME, parentXpath,
                    subscriptionAsJson, OffsetDateTime.now(), ContentType.JSON);
        } else {
            cpsDataService.saveListElements(NCMP_DATASPACE_NAME, CM_DATA_JOB_SUBSCRIPTIONS_ANCHOR_NAME,
                    CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_FILTERS_WITH_DATASTORE_AND_CMHANDLE.formatted(
                            datastoreType.getDatastoreName(), cmHandleId), subscriptionDetailsAsJson,
                    OffsetDateTime.now(), ContentType.JSON);
        }
    }

    private void saveSubscriptionDetails(final DatastoreType datastoreType, final String cmHandleId, final String xpath,
            final Collection<String> subscriptionIds) {
        final String subscriptionDetailsAsJson = getSubscriptionDetailsAsJson(xpath, subscriptionIds);
        cpsDataService.updateNodeLeaves(NCMP_DATASPACE_NAME, CM_DATA_JOB_SUBSCRIPTIONS_ANCHOR_NAME,
                CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_FILTERS_WITH_DATASTORE_AND_CMHANDLE.formatted(
                        datastoreType.getDatastoreName(), cmHandleId), subscriptionDetailsAsJson, OffsetDateTime.now(),
                ContentType.JSON);
    }

    private String getSubscriptionDetailsAsJson(final String xpath, final Collection<String> subscriptionIds) {
        final Map<String, Serializable> subscriptionDetailsAsMap =
                Map.of("xpath", xpath, "subscriptionIds", (Serializable) subscriptionIds);
        return "{\"filter\":[" + jsonObjectMapper.asJsonString(subscriptionDetailsAsMap) + "]}";
    }

    private static String escapeQuotesByDoublingThem(final String inputXpath) {
        return inputXpath.replace("'", "''");
    }

}

