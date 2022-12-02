/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
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

import static org.onap.cps.notification.Operation.CREATE;
import static org.onap.cps.notification.Operation.DELETE;
import static org.onap.cps.notification.Operation.UPDATE;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.notification.NotificationService;
import org.onap.cps.notification.Operation;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.DataNodeBuilder;
import org.onap.cps.spi.utils.CpsValidator;
import org.onap.cps.utils.YangUtils;
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CpsDataServiceImpl implements CpsDataService {

    private static final String ROOT_NODE_XPATH = "/";
    private static final long DEFAULT_LOCK_TIMEOUT_IN_MILLISECONDS = 300L;

    private final CpsDataPersistenceService cpsDataPersistenceService;
    private final CpsAdminService cpsAdminService;
    private final YangTextSchemaSourceSetCache yangTextSchemaSourceSetCache;
    private final NotificationService notificationService;
    private final CpsValidator cpsValidator;

    @Override
    public void saveData(final String dataspaceName, final String anchorName, final String jsonData,
        final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final DataNode dataNode = buildDataNode(dataspaceName, anchorName, ROOT_NODE_XPATH, jsonData);
        cpsDataPersistenceService.storeDataNode(dataspaceName, anchorName, dataNode);
        processDataUpdatedEventAsync(dataspaceName, anchorName, ROOT_NODE_XPATH, CREATE, observedTimestamp);
    }

    @Override
    public void saveData(final String dataspaceName, final String anchorName, final String parentNodeXpath,
        final String jsonData, final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final DataNode dataNode = buildDataNode(dataspaceName, anchorName, parentNodeXpath, jsonData);
        cpsDataPersistenceService.addChildDataNode(dataspaceName, anchorName, parentNodeXpath, dataNode);
        processDataUpdatedEventAsync(dataspaceName, anchorName, parentNodeXpath, CREATE, observedTimestamp);
    }

    @Override
    public void saveListElements(final String dataspaceName, final String anchorName,
        final String parentNodeXpath, final String jsonData, final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Collection<DataNode> listElementDataNodeCollection =
            buildDataNodes(dataspaceName, anchorName, parentNodeXpath, jsonData);
        cpsDataPersistenceService.addListElements(dataspaceName, anchorName, parentNodeXpath,
            listElementDataNodeCollection);
        processDataUpdatedEventAsync(dataspaceName, anchorName, parentNodeXpath, UPDATE, observedTimestamp);
    }

    @Override
    public void saveListElementsBatch(final String dataspaceName, final String anchorName, final String parentNodeXpath,
            final Collection<String> jsonDataList, final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Collection<Collection<DataNode>> listElementDataNodeCollections =
                buildDataNodes(dataspaceName, anchorName, parentNodeXpath, jsonDataList);
        cpsDataPersistenceService.addMultipleLists(dataspaceName, anchorName, parentNodeXpath,
                listElementDataNodeCollections);
        processDataUpdatedEventAsync(dataspaceName, anchorName, parentNodeXpath, UPDATE, observedTimestamp);
    }

    @Override
    public DataNode getDataNode(final String dataspaceName, final String anchorName, final String xpath,
        final FetchDescendantsOption fetchDescendantsOption) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        return cpsDataPersistenceService.getDataNode(dataspaceName, anchorName, xpath, fetchDescendantsOption);
    }

    @Override
    public void updateNodeLeaves(final String dataspaceName, final String anchorName, final String parentNodeXpath,
        final String jsonData, final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final DataNode dataNode = buildDataNode(dataspaceName, anchorName, parentNodeXpath, jsonData);
        cpsDataPersistenceService
            .updateDataLeaves(dataspaceName, anchorName, dataNode.getXpath(), dataNode.getLeaves());
        processDataUpdatedEventAsync(dataspaceName, anchorName, parentNodeXpath, UPDATE, observedTimestamp);
    }

    @Override
    public void updateNodeLeavesAndExistingDescendantLeaves(final String dataspaceName, final String anchorName,
        final String parentNodeXpath,
        final String dataNodeUpdatesAsJson,
        final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Collection<DataNode> dataNodeUpdates =
            buildDataNodes(dataspaceName, anchorName,
                parentNodeXpath, dataNodeUpdatesAsJson);
        for (final DataNode dataNodeUpdate : dataNodeUpdates) {
            processDataNodeUpdate(dataspaceName, anchorName, dataNodeUpdate);
        }
        processDataUpdatedEventAsync(dataspaceName, anchorName, parentNodeXpath, UPDATE, observedTimestamp);
    }

    @Override
    public String startSession() {
        return cpsDataPersistenceService.startSession();
    }

    @Override
    public void closeSession(final String sessionId) {
        cpsDataPersistenceService.closeSession(sessionId);
    }

    @Override
    public void lockAnchor(final String sessionID, final String dataspaceName, final String anchorName) {
        lockAnchor(sessionID, dataspaceName, anchorName, DEFAULT_LOCK_TIMEOUT_IN_MILLISECONDS);
    }

    @Override
    public void lockAnchor(final String sessionID, final String dataspaceName,
                           final String anchorName, final Long timeoutInMilliseconds) {
        cpsDataPersistenceService.lockAnchor(sessionID, dataspaceName, anchorName, timeoutInMilliseconds);
    }

    @Override
    public void updateDataNodeAndDescendants(final String dataspaceName, final String anchorName,
                                             final String parentNodeXpath, final String jsonData,
                                             final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final DataNode dataNode = buildDataNode(dataspaceName, anchorName, parentNodeXpath, jsonData);
        cpsDataPersistenceService.updateDataNodeAndDescendants(dataspaceName, anchorName, dataNode);
        processDataUpdatedEventAsync(dataspaceName, anchorName, parentNodeXpath, UPDATE, observedTimestamp);
    }

    @Override
    public void updateDataNodesAndDescendants(final String dataspaceName, final String anchorName,
                                              final Map<String, String> nodesJsonData,
                                              final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final List<DataNode> dataNodes = buildDataNodes(dataspaceName, anchorName, nodesJsonData);
        cpsDataPersistenceService.updateDataNodesAndDescendants(dataspaceName, anchorName, dataNodes);
        nodesJsonData.keySet().forEach(nodeXpath ->
            processDataUpdatedEventAsync(dataspaceName, anchorName, nodeXpath,
                UPDATE, observedTimestamp));
    }

    @Override
    public void replaceListContent(final String dataspaceName, final String anchorName, final String parentNodeXpath,
            final String jsonData, final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Collection<DataNode> newListElements =
                buildDataNodes(dataspaceName, anchorName, parentNodeXpath, jsonData);
        replaceListContent(dataspaceName, anchorName, parentNodeXpath, newListElements, observedTimestamp);
    }

    @Override
    public void replaceListContent(final String dataspaceName, final String anchorName, final String parentNodeXpath,
            final Collection<DataNode> dataNodes, final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        cpsDataPersistenceService.replaceListContent(dataspaceName, anchorName, parentNodeXpath, dataNodes);
        processDataUpdatedEventAsync(dataspaceName, anchorName, parentNodeXpath, UPDATE, observedTimestamp);
    }

    @Override
    public void deleteDataNode(final String dataspaceName, final String anchorName, final String dataNodeXpath,
                               final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        cpsDataPersistenceService.deleteDataNode(dataspaceName, anchorName, dataNodeXpath);
        processDataUpdatedEventAsync(dataspaceName, anchorName, dataNodeXpath, DELETE, observedTimestamp);
    }

    @Override
    public void deleteDataNodes(final String dataspaceName, final String anchorName,
        final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        processDataUpdatedEventAsync(dataspaceName, anchorName, ROOT_NODE_XPATH, DELETE, observedTimestamp);
        cpsDataPersistenceService.deleteDataNodes(dataspaceName, anchorName);
    }

    @Override
    public void deleteListOrListElement(final String dataspaceName, final String anchorName, final String listNodeXpath,
        final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        cpsDataPersistenceService.deleteListDataNode(dataspaceName, anchorName, listNodeXpath);
        processDataUpdatedEventAsync(dataspaceName, anchorName, listNodeXpath, DELETE, observedTimestamp);
    }

    private DataNode buildDataNode(final String dataspaceName, final String anchorName,
                                   final String parentNodeXpath, final String jsonData) {

        final Anchor anchor = cpsAdminService.getAnchor(dataspaceName, anchorName);
        final SchemaContext schemaContext = getSchemaContext(dataspaceName, anchor.getSchemaSetName());

        if (ROOT_NODE_XPATH.equals(parentNodeXpath)) {
            final NormalizedNode normalizedNode = YangUtils.parseJsonData(jsonData, schemaContext);
            return new DataNodeBuilder().withNormalizedNodeTree(normalizedNode).build();
        }

        final NormalizedNode normalizedNode = YangUtils.parseJsonData(jsonData, schemaContext, parentNodeXpath);
        return new DataNodeBuilder()
            .withParentNodeXpath(parentNodeXpath)
            .withNormalizedNodeTree(normalizedNode)
            .build();
    }

    private List<DataNode> buildDataNodes(final String dataspaceName, final String anchorName,
                                          final Map<String, String> nodesJsonData) {
        return nodesJsonData.entrySet().stream().map(nodeJsonData ->
            buildDataNode(dataspaceName, anchorName, nodeJsonData.getKey(),
                nodeJsonData.getValue())).collect(Collectors.toList());
    }

    private Collection<DataNode> buildDataNodes(final String dataspaceName,
                                                final String anchorName,
                                                final String parentNodeXpath,
                                                final String jsonData) {

        final Anchor anchor = cpsAdminService.getAnchor(dataspaceName, anchorName);
        final SchemaContext schemaContext = getSchemaContext(dataspaceName, anchor.getSchemaSetName());

        final NormalizedNode normalizedNode = YangUtils.parseJsonData(jsonData, schemaContext, parentNodeXpath);
        final Collection<DataNode> dataNodes = new DataNodeBuilder()
            .withParentNodeXpath(parentNodeXpath)
            .withNormalizedNodeTree(normalizedNode)
            .buildCollection();
        if (dataNodes.isEmpty()) {
            throw new DataValidationException("Invalid data.", "No data nodes provided");
        }
        return dataNodes;

    }

    private Collection<Collection<DataNode>> buildDataNodes(final String dataspaceName, final String anchorName,
            final String parentNodeXpath, final Collection<String> jsonDataList) {
        return jsonDataList.stream()
                .map(jsonData -> buildDataNodes(dataspaceName, anchorName, parentNodeXpath, jsonData))
                .collect(Collectors.toList());
    }

    private void processDataUpdatedEventAsync(final String dataspaceName, final String anchorName, final String xpath,
            final Operation operation, final OffsetDateTime observedTimestamp) {
        try {
            notificationService.processDataUpdatedEvent(dataspaceName, anchorName, xpath, operation, observedTimestamp);
        } catch (final Exception exception) {
            //If async message can't be queued for notification service, the initial request should not failed.
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

}
