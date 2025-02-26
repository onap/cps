/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 TechMahindra Ltd.
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

package org.onap.cps.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsNotificationService;
import org.onap.cps.api.exceptions.DataNodeNotFoundException;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.DataMapUtils;
import org.onap.cps.utils.PrefixResolver;
import org.onap.cps.utils.YangParser;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CpsNotificationServiceImpl implements CpsNotificationService {

    private final CpsAnchorService cpsAnchorService;

    private final CpsDataPersistenceService cpsDataPersistenceService;

    private final YangParser yangParser;

    private final PrefixResolver prefixResolver;

    private static final String ADMIN_DATASPACE = "CPS-Admin";
    private static final String ANCHOR_NAME = "cps-notification-subscriptions";
    private static final String DATASPACE_SUBSCRIPTION_XPATH_FORMAT = "/dataspaces/dataspace[@name='%s']";
    private static final String ANCHORS_SUBSCRIPTION_XPATH_FORMAT = "/dataspaces/dataspace[@name='%s']/anchors";
    private static final String ANCHOR_SUBSCRIPTION_XPATH_FORMAT =
            "/dataspaces/dataspace[@name='%s']/anchors/anchor[@name='%s']";

    @Override
    public void createNotificationSubscription(final String notificationSubscriptionAsJson, final String xpath) {

        final Anchor anchor = cpsAnchorService.getAnchor(ADMIN_DATASPACE, ANCHOR_NAME);
        final Collection<DataNode> dataNodes =
                buildDataNodesWithParentNodeXpath(anchor, xpath, notificationSubscriptionAsJson, ContentType.JSON);
        cpsDataPersistenceService.addListElements(ADMIN_DATASPACE, ANCHOR_NAME, xpath,
                dataNodes);
    }

    @Override
    public void deleteNotificationSubscription(final String xpath) {
        cpsDataPersistenceService.deleteDataNode(ADMIN_DATASPACE, ANCHOR_NAME, xpath);
    }

    @Override
    public List<Map<String, Object>> getNotificationSubscription(final String xpath) {
        final Collection<DataNode> dataNodes =
                cpsDataPersistenceService.getDataNodes(ADMIN_DATASPACE, ANCHOR_NAME, xpath,
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
        final List<Map<String, Object>> dataMaps = new ArrayList<>(dataNodes.size());
        final Anchor anchor = cpsAnchorService.getAnchor(ADMIN_DATASPACE, ANCHOR_NAME);
        for (final DataNode dataNode: dataNodes) {
            final String prefix = prefixResolver.getPrefix(anchor, dataNode.getXpath());
            final Map<String, Object> dataMap = DataMapUtils.toDataMapWithIdentifier(dataNode, prefix);
            dataMaps.add(dataMap);
        }
        return dataMaps;
    }

    @Override
    public boolean isNotificationEnabled(final String dataspaceName, final String anchorName) {
        return (isNotificationEnabledForAnchor(dataspaceName, anchorName)
                || notificationEnabledForAllAnchors(dataspaceName));
    }

    private boolean isNotificationEnabledForAnchor(final String dataspaceName, final String anchorName) {
        final String xpath = String.format(ANCHOR_SUBSCRIPTION_XPATH_FORMAT, dataspaceName, anchorName);
        return isNotificationEnabledForXpath(xpath);
    }

    private boolean isNotificationEnabledForXpath(final String xpath) {
        try {
            cpsDataPersistenceService.getDataNodes(ADMIN_DATASPACE, ANCHOR_NAME, xpath,
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS);
        } catch (final DataNodeNotFoundException e) {
            return false;
        }
        return true;
    }

    private boolean notificationEnabledForAllAnchors(final String dataspaceName) {
        final String dataspaceSubscriptionXpath = String.format(DATASPACE_SUBSCRIPTION_XPATH_FORMAT, dataspaceName);
        return (isNotificationEnabledForXpath(dataspaceSubscriptionXpath)
                && noIndividualAnchorEnabledInDataspace(dataspaceName));
    }

    private boolean noIndividualAnchorEnabledInDataspace(final String dataspaceName) {
        final String xpathForAnchors = String.format(ANCHORS_SUBSCRIPTION_XPATH_FORMAT, dataspaceName);
        return !isNotificationEnabledForXpath(xpathForAnchors);
    }


    private Collection<DataNode> buildDataNodesWithParentNodeXpath(final Anchor anchor, final String parentNodeXpath,
                                                                   final String nodeData,
                                                                   final ContentType contentType) {

        final String normalizedParentNodeXpath = CpsPathUtil.getNormalizedXpath(parentNodeXpath);
        final ContainerNode containerNode =
                yangParser.parseData(contentType, nodeData, anchor, normalizedParentNodeXpath);
        final Collection<DataNode> dataNodes = new DataNodeBuilder()
                .withParentNodeXpath(normalizedParentNodeXpath)
                .withContainerNode(containerNode)
                .buildCollection();
        return dataNodes;
    }
}