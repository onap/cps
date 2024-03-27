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
import org.onap.cps.cpspath.parser.CpsPathUtil;
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
    private static final String CM_SUBSCRIPTION_CPS_PATH_QUERY = """
            /datastores/datastore[@name='%s']/cm-handles/cm-handle[@id='%s']/filters/filter[@xpath='%s']
            """.trim();
    private static final String SUBSCRIPTION_IDS_CPS_PATH_QUERY = """
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
                SUBSCRIPTION_IDS_CPS_PATH_QUERY.formatted(subscriptionId),
                OMIT_DESCENDANTS).isEmpty();
    }

    @Override
    public Collection<String> getOngoingCmNotificationSubscriptionIds(final DatastoreType datastoreType,
                                                                      final String cmHandleId, final String xpath) {

        final String isOngoingCmSubscriptionCpsPathQuery =
                CM_SUBSCRIPTION_CPS_PATH_QUERY.formatted(datastoreType.getDatastoreName(), cmHandleId,
                        escapeQuotesByDoublingThem(xpath));
        final Collection<DataNode> existingNodes =
                cpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, CM_SUBSCRIPTIONS_ANCHOR_NAME,
                        isOngoingCmSubscriptionCpsPathQuery, OMIT_DESCENDANTS);
        if (existingNodes.isEmpty()) {
            return Collections.emptyList();
        }
        return (List<String>) existingNodes.iterator().next().getLeaves().get("subscriptionIds");
    }

    @Override
    public void addCmNotificationSubscription(final DatastoreType datastoreType, final String cmHandleId,
                                              final String xpath, final String subscriptionId) {
        if (isOngoingCmNotificationSubscription(datastoreType, cmHandleId, xpath)
                && (!getOngoingCmNotificationSubscriptionIds(datastoreType, cmHandleId, xpath)
                .contains(subscriptionId))) {
            addOrRemoveCmNotificationSubscription(datastoreType, cmHandleId, xpath, subscriptionId, true);
        } else {
            addNewSubscriptionViaDatastore(datastoreType, cmHandleId, xpath, subscriptionId);
        }
    }

    @Override
    public void removeCmNotificationSubscription(final DatastoreType datastoreType, final String cmHandleId,
                                                 final String xpath, final String subscriptionId) {
        if (isOngoingCmNotificationSubscription(datastoreType, cmHandleId, xpath)
                && (getOngoingCmNotificationSubscriptionIds(datastoreType, cmHandleId, xpath)
                .contains(subscriptionId))) {
            addOrRemoveCmNotificationSubscription(datastoreType, cmHandleId, xpath, subscriptionId, false);
            refreshOngoingCmNotificationSubscriptions(datastoreType, cmHandleId, xpath);
        }
    }

    private void refreshOngoingCmNotificationSubscriptions(final DatastoreType datastoreType, final String cmHandleId,
                                                    final String xpath) {
        if (getOngoingCmNotificationSubscriptionIds(datastoreType, cmHandleId, xpath).isEmpty()) {
            cpsDataService.deleteDataNode(NCMP_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME,
                    CM_SUBSCRIPTION_CPS_PATH_QUERY.formatted(datastoreType.getDatastoreName(), cmHandleId,
                            escapeQuotesByDoublingThem(xpath)),
                    OffsetDateTime.now());
        }
    }

    private void addOrRemoveCmNotificationSubscription(final DatastoreType datastoreType, final String cmHandleId,
                                                       final String xpath, final String subscriptionId,
                                                       final boolean isAdd) {
        final DataNode existingFilterNode =
                cpsQueryService.queryDataNodes(NCMP_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME,
                        CM_SUBSCRIPTION_CPS_PATH_QUERY.formatted(datastoreType.getDatastoreName(), cmHandleId,
                                escapeQuotesByDoublingThem(xpath)),
                        OMIT_DESCENDANTS).iterator().next();
        final Collection<String> existingSubscriptionIds = getOngoingCmNotificationSubscriptionIds(datastoreType,
                cmHandleId, xpath);
        final List<String> updatedSubscribers = new ArrayList<>(existingSubscriptionIds);
        if (isAdd) {
            updatedSubscribers.add(subscriptionId);
        } else {
            updatedSubscribers.remove(subscriptionId);
        }
        final Map<String, Serializable> updatedLeaves = new HashMap<>();
        updatedLeaves.put("xpath", existingFilterNode.getLeaves().get("xpath"));
        updatedLeaves.put("subscriptionIds", (Serializable) updatedSubscribers);
        queryAndUpdateListOfSubscribers(existingFilterNode, updatedLeaves);
    }

    private void addNewSubscriptionViaDatastore(final DatastoreType datastoreType, final String cmHandleId,
                                                final String xpath, final String newSubscriptionId) {
        final String parentXpath = "/datastores/datastore[@name='%s']/cm-handles"
                .formatted(datastoreType.getDatastoreName());
        final String updatedJson = String.format("{\"cm-handle\":[{\"id\":\"%s\",\"filters\":{\"filter\":"
                + "[{\"xpath\":\"%s\",\"subscriptionIds\":[\"%s\"]}]}}]}", cmHandleId, xpath, newSubscriptionId);
        cpsDataService.saveData(NCMP_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME, parentXpath, updatedJson,
                OffsetDateTime.now(), ContentType.JSON);
    }

    private void queryAndUpdateListOfSubscribers(final DataNode existingFilterNode,
                                                 final Map<String, Serializable> nodeLeaves) {
        final String parentXpath = CpsPathUtil.getNormalizedParentXpath(existingFilterNode.getXpath());
        final String updatedJson = "{\"filter\":["
                + jsonObjectMapper.asJsonString(nodeLeaves).replace("'", "\"") + "]}";
        cpsDataService.updateNodeLeaves(NCMP_DATASPACE_NAME, SUBSCRIPTION_ANCHOR_NAME, parentXpath, updatedJson,
                OffsetDateTime.now());
    }

    private static String escapeQuotesByDoublingThem(final String inputXpath) {
        return inputXpath.replace("'", "''");
    }
}
