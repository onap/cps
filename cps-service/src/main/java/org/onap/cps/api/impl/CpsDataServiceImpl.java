/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2020-2022 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
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

import java.time.OffsetDateTime;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.notification.NotificationService;
import org.onap.cps.notification.Operation;
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
@Slf4j
public class CpsDataServiceImpl implements CpsDataService {

    private static final String ROOT_NODE_XPATH = "/";

    @Autowired
    private CpsDataPersistenceService cpsDataPersistenceService;

    @Autowired
    private CpsAdminService cpsAdminService;

    @Autowired
    private YangTextSchemaSourceSetCache yangTextSchemaSourceSetCache;

    @Autowired
    private NotificationService notificationService;

    @Override
    public void saveData(final String dataspaceName, final String anchorName, final String jsonData,
        final OffsetDateTime observedTimestamp) {
        final var dataNode = buildDataNode(dataspaceName, anchorName, ROOT_NODE_XPATH, jsonData);
        cpsDataPersistenceService.storeDataNode(dataspaceName, anchorName, dataNode);
        processDataUpdatedEventAsync(dataspaceName, anchorName, observedTimestamp, Operation.ROOT_NODE_CREATE);
    }

    @Override
    public void saveData(final String dataspaceName, final String anchorName, final String parentNodeXpath,
        final String jsonData, final OffsetDateTime observedTimestamp) {
        final var dataNode = buildDataNode(dataspaceName, anchorName, parentNodeXpath, jsonData);
        cpsDataPersistenceService.addChildDataNode(dataspaceName, anchorName, parentNodeXpath, dataNode);
        processDataUpdatedEventAsync(dataspaceName, anchorName, observedTimestamp, Operation.CHILD_NODE_CREATE);
    }

    @Override
    public void saveListElements(final String dataspaceName, final String anchorName,
        final String parentNodeXpath, final String jsonData, final OffsetDateTime observedTimestamp) {
        final Collection<DataNode> listElementDataNodeCollection =
            buildDataNodes(dataspaceName, anchorName, parentNodeXpath, jsonData);
        cpsDataPersistenceService.addListElements(dataspaceName, anchorName, parentNodeXpath,
            listElementDataNodeCollection);
        processDataUpdatedEventAsync(dataspaceName, anchorName, observedTimestamp, Operation.CHILD_NODE_UPDATE);
    }

    @Override
    public DataNode getDataNode(final String dataspaceName, final String anchorName, final String xpath,
        final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataPersistenceService.getDataNode(dataspaceName, anchorName, xpath, fetchDescendantsOption);
    }

    @Override
    public void updateNodeLeaves(final String dataspaceName, final String anchorName, final String parentNodeXpath,
        final String jsonData, final OffsetDateTime observedTimestamp) {
        final var dataNode = buildDataNode(dataspaceName, anchorName, parentNodeXpath, jsonData);
        cpsDataPersistenceService
            .updateDataLeaves(dataspaceName, anchorName, dataNode.getXpath(), dataNode.getLeaves());
        if (isRootXpath(parentNodeXpath)) {
            processDataUpdatedEventAsync(dataspaceName, anchorName, observedTimestamp, Operation.ROOT_NODE_UPDATE);
        } else {
            processDataUpdatedEventAsync(dataspaceName, anchorName, observedTimestamp, Operation.CHILD_NODE_UPDATE);
        }
    }

    @Override
    public void updateNodeLeavesAndExistingDescendantLeaves(final String dataspaceName, final String anchorName,
        final String parentNodeXpath,
        final String dataNodeUpdatesAsJson,
        final OffsetDateTime observedTimestamp) {
        final Collection<DataNode> dataNodeUpdates =
            buildDataNodes(dataspaceName, anchorName,
                parentNodeXpath, dataNodeUpdatesAsJson);
        for (final DataNode dataNodeUpdate : dataNodeUpdates) {
            processDataNodeUpdate(dataspaceName, anchorName, dataNodeUpdate);
        }
        if (isRootXpath(parentNodeXpath)) {
            processDataUpdatedEventAsync(dataspaceName, anchorName, observedTimestamp, Operation.ROOT_NODE_UPDATE);
        } else {
            processDataUpdatedEventAsync(dataspaceName, anchorName, observedTimestamp, Operation.CHILD_NODE_UPDATE);
        }
    }

    @Override
    public void replaceNodeTree(final String dataspaceName, final String anchorName, final String parentNodeXpath,
        final String jsonData, final OffsetDateTime observedTimestamp) {
        final var dataNode = buildDataNode(dataspaceName, anchorName, parentNodeXpath, jsonData);
        cpsDataPersistenceService.replaceDataNodeTree(dataspaceName, anchorName, dataNode);
        if (isRootXpath(parentNodeXpath)) {
            processDataUpdatedEventAsync(dataspaceName, anchorName, observedTimestamp, Operation.ROOT_NODE_UPDATE);
        } else {
            processDataUpdatedEventAsync(dataspaceName, anchorName, observedTimestamp, Operation.CHILD_NODE_UPDATE);
        }
    }

    @Override
    public void replaceListContent(final String dataspaceName, final String anchorName, final String parentNodeXpath,
                                   final String jsonData, final OffsetDateTime observedTimestamp) {
        final Collection<DataNode> newListElements =
            buildDataNodes(dataspaceName, anchorName, parentNodeXpath, jsonData);
        cpsDataPersistenceService.replaceListContent(dataspaceName, anchorName, parentNodeXpath, newListElements);
        processDataUpdatedEventAsync(dataspaceName, anchorName, observedTimestamp, Operation.CHILD_NODE_UPDATE);
    }

    @Override
    public void deleteDataNode(final String dataspaceName, final String anchorName, final String dataNodeXpath,
                               final OffsetDateTime observedTimestamp) {
        cpsDataPersistenceService.deleteDataNode(dataspaceName, anchorName, dataNodeXpath);
        if (isRootXpath(dataNodeXpath)) {
            processDataUpdatedEventAsync(dataspaceName, anchorName, observedTimestamp, Operation.ROOT_NODE_DELETE);
        } else {
            processDataUpdatedEventAsync(dataspaceName, anchorName, observedTimestamp, Operation.CHILD_NODE_DELETE);
        }
    }

    @Override
    public void deleteListOrListElement(final String dataspaceName, final String anchorName, final String listNodeXpath,
        final OffsetDateTime observedTimestamp) {
        cpsDataPersistenceService.deleteListDataNode(dataspaceName, anchorName, listNodeXpath);
        processDataUpdatedEventAsync(dataspaceName, anchorName, observedTimestamp, Operation.CHILD_NODE_DELETE);
    }

    private DataNode buildDataNode(final String dataspaceName, final String anchorName,
                                   final String parentNodeXpath, final String jsonData) {

        final var anchor = cpsAdminService.getAnchor(dataspaceName, anchorName);
        final var schemaContext = getSchemaContext(dataspaceName, anchor.getSchemaSetName());

        if (isRootXpath(parentNodeXpath)) {
            final NormalizedNode<?, ?> normalizedNode = YangUtils.parseJsonData(jsonData, schemaContext);
            return new DataNodeBuilder().withNormalizedNodeTree(normalizedNode).build();
        }

        final NormalizedNode<?, ?> normalizedNode = YangUtils.parseJsonData(jsonData, schemaContext, parentNodeXpath);
        return new DataNodeBuilder()
            .withParentNodeXpath(parentNodeXpath)
            .withNormalizedNodeTree(normalizedNode)
            .build();
    }

    private Collection<DataNode> buildDataNodes(final String dataspaceName,
                                                final String anchorName,
                                                final String parentNodeXpath,
                                                final String jsonData) {

        final var anchor = cpsAdminService.getAnchor(dataspaceName, anchorName);
        final var schemaContext = getSchemaContext(dataspaceName, anchor.getSchemaSetName());

        final NormalizedNode<?, ?> normalizedNode = YangUtils.parseJsonData(jsonData, schemaContext, parentNodeXpath);
        final Collection<DataNode> dataNodes = new DataNodeBuilder()
            .withParentNodeXpath(parentNodeXpath)
            .withNormalizedNodeTree(normalizedNode)
            .buildCollection();
        if (dataNodes.isEmpty()) {
            throw new DataValidationException("Invalid data.", "No data nodes provided");
        }
        return dataNodes;

    }

    private void processDataUpdatedEventAsync(final String dataspaceName, final String anchorName,
                                              final OffsetDateTime observedTimestamp, final Operation operation) {
        try {
            notificationService.processDataUpdatedEvent(dataspaceName, anchorName, observedTimestamp, operation);
        } catch (final Exception exception) {
            log.error("Failed to send message to notification service", exception);
        }
    }

    private SchemaContext getSchemaContext(final String dataspaceName, final String schemaSetName) {
        return yangTextSchemaSourceSetCache.get(dataspaceName, schemaSetName).getSchemaContext();
    }

    private void processDataNodeUpdate(final String dataspaceName, final String anchorName,
                                       final DataNode dataNodeUpdate) {
        if (dataNodeUpdate == null) {
            return;
        }
        cpsDataPersistenceService.updateDataLeaves(dataspaceName, anchorName, dataNodeUpdate.getXpath(),
            dataNodeUpdate.getLeaves());
        final Collection<DataNode> childDataNodeUpdates = dataNodeUpdate.getChildDataNodes();
        for (final DataNode childDataNodeUpdate : childDataNodeUpdates) {
            processDataNodeUpdate(dataspaceName, anchorName, childDataNodeUpdate);
        }
    }

    private boolean isRootXpath(final String xpath) {
        return ROOT_NODE_XPATH.equals(xpath);
    }
}
