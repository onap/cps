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

package org.onap.cps.ncmp.api.impl.events.cmsubscription.service;

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS;
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsQueryService;
import org.onap.cps.ncmp.api.impl.operations.DatastoreType;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CmNotificationSubscriptionPersistenceServiceImpl implements CmNotificationSubscriptionPersistenceService {

    private static final String SUBSCRIPTION_ANCHOR_NAME = "cm-data-subscriptions";
    private static final String CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_AND_CMHANDLE = """
            /datastores/datastore[@name='%s']/cm-handles/cm-handle[@id='%s']/filters
            """.trim();
    private static final String CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_CMHANDLE_AND_XPATH =
            CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_AND_CMHANDLE + "/filter[@xpath='%s']";

    private static final String CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_ID = """
            //filter/subscriptionIds[text()='%s']
            """.trim();

    private final JsonObjectMapper jsonObjectMapper;
    private final CpsQueryService cpsQueryService;
    private final CpsDataService cpsDataService;

    @Override
    public boolean isOngoingCmNotificationSubscription(final DatastoreType datastoreType, final String cmHandleId,
                                                       final String xpath) {
        return !getOngoingCmNotificationSubscriptionIds(datastoreType, cmHandleId, xpath).isEmpty();
    }

    @Override
    public boolean isUniqueSubscriptionId(final String subscriptionId) {
        return cpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME,
                CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_ID.formatted(subscriptionId),
                OMIT_DESCENDANTS).isEmpty();
    }

    @Override
    public Collection<String> getOngoingCmNotificationSubscriptionIds(final DatastoreType datastoreType,
                                                                      final String cmHandleId, final String xpath) {

        final String isOngoingCmSubscriptionCpsPathQuery =
                CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_CMHANDLE_AND_XPATH.formatted(
                        datastoreType.getDatastoreName(), cmHandleId, escapeQuotesByDoublingThem(xpath));
        final Collection<DataNode> existingNodes =
                cpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, CM_SUBSCRIPTIONS_ANCHOR_NAME,
                        isOngoingCmSubscriptionCpsPathQuery, OMIT_DESCENDANTS);
        if (existingNodes.isEmpty()) {
            return Collections.emptyList();
        }
        return (List<String>) existingNodes.iterator().next().getLeaves().get("subscriptionIds");
    }

    private boolean isAdditionalSubscriptionForDatastoreCmHandleAndXPath(final DatastoreType datastoreType,
                                                                         final String cmHandleId,
                                                                         final String xpath,
                                                                         final String subscriptionId) {
        return isOngoingCmNotificationSubscription(datastoreType, cmHandleId, xpath)
                && (!getOngoingCmNotificationSubscriptionIds(datastoreType, cmHandleId, xpath)
                .contains(subscriptionId));
    }

    @Override
    public void addCmNotificationSubscription(final DatastoreType datastoreType, final String cmHandleId,
                                              final String xpath, final String subscriptionId) {
        if (isAdditionalSubscriptionForDatastoreCmHandleAndXPath(datastoreType, cmHandleId, xpath, subscriptionId)) {
            final Collection<String> subscriptionIds = getOngoingCmNotificationSubscriptionIds(datastoreType,
                    cmHandleId, xpath);
            subscriptionIds.add(subscriptionId);
            saveSubscriptionDetails(datastoreType, cmHandleId, xpath, subscriptionIds);
        } else {
            addFirstSubscriptionForDatastoreCmHandleAndXpath(datastoreType, cmHandleId, xpath, subscriptionId);
        }
    }

    @Override
    public void removeCmNotificationSubscription(final DatastoreType datastoreType, final String cmHandleId,
                                                 final String xpath, final String subscriptionId) {
        final Collection<String> subscriptionIds = getOngoingCmNotificationSubscriptionIds(datastoreType,
                cmHandleId, xpath);
        subscriptionIds.remove(subscriptionId);
        saveSubscriptionDetails(datastoreType, cmHandleId, xpath, subscriptionIds);
        if (isOngoingCmNotificationSubscription(datastoreType, cmHandleId, xpath)) {
            log.info("There are subscribers left for the following cps path {} :",
                    CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_CMHANDLE_AND_XPATH.formatted(
                            datastoreType.getDatastoreName(), cmHandleId, escapeQuotesByDoublingThem(xpath)));
        } else {
            log.info("No subscribers left for the following cps path {} :",
                    CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_CMHANDLE_AND_XPATH.formatted(
                            datastoreType.getDatastoreName(), cmHandleId, escapeQuotesByDoublingThem(xpath)));
            deleteListOfSubscriptionsFor(datastoreType, cmHandleId, xpath);
        }
    }

    private void deleteListOfSubscriptionsFor(final DatastoreType datastoreType, final String cmHandleId,
                                              final String xpath) {
        cpsDataService.deleteDataNode(NCMP_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME,
                CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_CMHANDLE_AND_XPATH.formatted(
                        datastoreType.getDatastoreName(), cmHandleId, escapeQuotesByDoublingThem(xpath)),
                OffsetDateTime.now());
    }

    private List<DataNode> getExistingDataNodesForDatastoreAndCmhandle(final DatastoreType datastoreType,
                                                                       final String cmHandleId) {
        final Collection<DataNode> existingDataNodes = cpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME,
                CM_SUBSCRIPTIONS_ANCHOR_NAME,
                CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_AND_CMHANDLE.formatted(
                        datastoreType.getDatastoreName(), cmHandleId),
                INCLUDE_ALL_DESCENDANTS);
        if (existingDataNodes == null) {
            return Collections.emptyList();
        }
        return existingDataNodes.stream().toList();
    }

    private void addFirstSubscriptionForDatastoreCmHandleAndXpath(final DatastoreType datastoreType,
                                                                  final String cmHandleId,
                                                                  final String xpath,
                                                                  final String subscriptionId) {
        final Collection<String> newSubscriptionList = new ArrayList<>(1);
        newSubscriptionList.add(subscriptionId);
        final String subscriptionDetailsAsJson = getSubscriptionDetailsAsJson(xpath, newSubscriptionList);
        final List<DataNode> existingDataNodesForDatastoreAndCmhandle =
                getExistingDataNodesForDatastoreAndCmhandle(datastoreType, cmHandleId);
        if (existingDataNodesForDatastoreAndCmhandle.isEmpty()) {
            final String parentXpath = "/datastores/datastore[@name='%s']/cm-handles"
                    .formatted(datastoreType.getDatastoreName());
            final String subscriptionAsJson = String.format("{\"cm-handle\":[{\"id\":\"%s\",\"filters\":%s}]}",
                    cmHandleId, subscriptionDetailsAsJson);
            cpsDataService.saveData(NCMP_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME, parentXpath, subscriptionAsJson,
                    OffsetDateTime.now(), ContentType.JSON);
        } else {
            cpsDataService.saveListElements(NCMP_DATASPACE_NAME, CM_SUBSCRIPTIONS_ANCHOR_NAME,
                    CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_AND_CMHANDLE.formatted(
                            datastoreType.getDatastoreName(), cmHandleId),
                    subscriptionDetailsAsJson, OffsetDateTime.now());
        }
    }

    private void saveSubscriptionDetails(final DatastoreType datastoreType, final String cmHandleId,
                                         final String xpath,
                                         final  Collection<String> subscriptionIds) {
        final String subscriptionDetailsAsJson = getSubscriptionDetailsAsJson(xpath, subscriptionIds);
        cpsDataService.updateNodeLeaves(NCMP_DATASPACE_NAME, CM_SUBSCRIPTIONS_ANCHOR_NAME,
                CPS_PATH_QUERY_FOR_CM_SUBSCRIPTION_WITH_DATASTORE_AND_CMHANDLE.formatted(
                        datastoreType.getDatastoreName(), cmHandleId), subscriptionDetailsAsJson, OffsetDateTime.now());
    }

    private String getSubscriptionDetailsAsJson(final String xpath, final Collection<String> subscriptionIds) {
        final Map<String, Serializable> subscriptionDetailsAsMap = new HashMap<>();
        subscriptionDetailsAsMap.put("xpath", xpath);
        subscriptionDetailsAsMap.put("subscriptionIds", (Serializable) subscriptionIds);
        return "{\"filter\":["
                + jsonObjectMapper.asJsonString(subscriptionDetailsAsMap).replace("'", "\"") + "]}";
    }

    private static String escapeQuotesByDoublingThem(final String inputXpath) {
        return inputXpath.replace("'", "''");
    }
}
