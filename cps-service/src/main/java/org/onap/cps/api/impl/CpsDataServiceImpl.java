/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2020 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021 Bell Canada.
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

package org.onap.cps.api.impl;

import java.util.Collection;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.notification.NotificationService;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.DataNodeBuilder;
import org.onap.cps.utils.YangUtils;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CpsDataServiceImpl implements CpsDataService {

    private static final String ROOT_NODE_XPATH = "/";

    @Autowired
    private CpsDataPersistenceService cpsDataPersistenceService;

    @Autowired
    private CpsAdminService cpsAdminService;

    @Autowired
    private CpsModuleService cpsModuleService;

    @Autowired
    private YangTextSchemaSourceSetCache yangTextSchemaSourceSetCache;

    @Autowired
    private NotificationService notificationService;

    @Override
    public void saveData(final String dataspaceName, final String anchorName, final String jsonData) {
        final var dataNode = buildDataNodeFromJson(dataspaceName, anchorName, ROOT_NODE_XPATH, jsonData);
        cpsDataPersistenceService.storeDataNode(dataspaceName, anchorName, dataNode);
        notificationService.processDataUpdatedEvent(dataspaceName, anchorName);
    }

    @Override
    public void saveData(final String dataspaceName, final String anchorName, final String parentNodeXpath,
        final String jsonData) {
        final var dataNode = buildDataNodeFromJson(dataspaceName, anchorName, parentNodeXpath, jsonData);
        cpsDataPersistenceService.addChildDataNode(dataspaceName, anchorName, parentNodeXpath, dataNode);
        notificationService.processDataUpdatedEvent(dataspaceName, anchorName);
    }

    @Override
    public void saveListNodeData(final String dataspaceName, final String anchorName,
        final String parentNodeXpath, final String jsonData) {
        final Collection<DataNode> dataNodesCollection =
            buildDataNodeCollectionFromJson(dataspaceName, anchorName, parentNodeXpath, jsonData);
        cpsDataPersistenceService.addListDataNodes(dataspaceName, anchorName, parentNodeXpath, dataNodesCollection);
        notificationService.processDataUpdatedEvent(dataspaceName, anchorName);
    }

    @Override
    public DataNode getDataNode(final String dataspaceName, final String anchorName, final String xpath,
        final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataPersistenceService.getDataNode(dataspaceName, anchorName, xpath, fetchDescendantsOption);
    }

    @Override
    public void updateNodeLeaves(final String dataspaceName, final String anchorName, final String parentNodeXpath,
        final String jsonData) {
        final var dataNode = buildDataNodeFromJson(dataspaceName, anchorName, parentNodeXpath, jsonData);
        cpsDataPersistenceService
            .updateDataLeaves(dataspaceName, anchorName, dataNode.getXpath(), dataNode.getLeaves());
        notificationService.processDataUpdatedEvent(dataspaceName, anchorName);
    }

    @Override
    public void replaceNodeTree(final String dataspaceName, final String anchorName, final String parentNodeXpath,
        final String jsonData) {
        final var dataNode = buildDataNodeFromJson(dataspaceName, anchorName, parentNodeXpath, jsonData);
        cpsDataPersistenceService.replaceDataNodeTree(dataspaceName, anchorName, dataNode);
        notificationService.processDataUpdatedEvent(dataspaceName, anchorName);
    }

    @Override
    public void replaceListNodeData(final String dataspaceName, final String anchorName, final String parentNodeXpath,
        final String jsonData) {
        final Collection<DataNode> dataNodes =
            buildDataNodeCollectionFromJson(dataspaceName, anchorName, parentNodeXpath, jsonData);
        cpsDataPersistenceService.replaceListDataNodes(dataspaceName, anchorName, parentNodeXpath, dataNodes);
        notificationService.processDataUpdatedEvent(dataspaceName, anchorName);
    }

    private DataNode buildDataNodeFromJson(final String dataspaceName, final String anchorName,
        final String parentNodeXpath, final String jsonData) {

        final var anchor = cpsAdminService.getAnchor(dataspaceName, anchorName);
        final var schemaContext = getSchemaContext(dataspaceName, anchor.getSchemaSetName());

        if (ROOT_NODE_XPATH.equals(parentNodeXpath)) {
            final NormalizedNode<?, ?> normalizedNode = YangUtils.parseJsonData(jsonData, schemaContext);
            return new DataNodeBuilder().withNormalizedNodeTree(normalizedNode).build();
        }

        final NormalizedNode<?, ?> normalizedNode = YangUtils.parseJsonData(jsonData, schemaContext, parentNodeXpath);
        return new DataNodeBuilder()
            .withParentNodeXpath(parentNodeXpath)
            .withNormalizedNodeTree(normalizedNode)
            .build();
    }

    private Collection<DataNode> buildDataNodeCollectionFromJson(final String dataspaceName, final String anchorName,
        final String parentNodeXpath, final String jsonData) {

        final var anchor = cpsAdminService.getAnchor(dataspaceName, anchorName);
        final var schemaContext = getSchemaContext(dataspaceName, anchor.getSchemaSetName());

        final NormalizedNode<?, ?> normalizedNode = YangUtils.parseJsonData(jsonData, schemaContext, parentNodeXpath);
        final Collection<DataNode> dataNodes = new DataNodeBuilder()
            .withParentNodeXpath(parentNodeXpath)
            .withNormalizedNodeTree(normalizedNode)
            .buildCollection();
        if (dataNodes.isEmpty()) {
            throw new DataValidationException("Invalid list data.", "List node is empty.");
        }
        return dataNodes;

    }

    private SchemaContext getSchemaContext(final String dataspaceName, final String schemaSetName) {
        return yangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName).getSchemaContext();
    }
}
