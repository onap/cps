/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation
 *  Modifications Copyright (C) 2020-2022 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2022 TechMahindra Ltd.
 *  Modifications Copyright (C) 2022 Deutsche Telekom AG
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

import io.micrometer.core.annotation.Timed;
import java.time.OffsetDateTime;
import java.util.ArrayList;
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
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.TimedYangParser;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
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
    private final TimedYangParser timedYangParser;

    @Override
    public void saveData(final String dataspaceName, final String anchorName, final String nodeData,
        final OffsetDateTime observedTimestamp) {
        saveData(dataspaceName, anchorName, nodeData, observedTimestamp, ContentType.JSON);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.root.save",
        description = "Time taken to save a root data node")
    public void saveData(final String dataspaceName, final String anchorName, final String nodeData,
                         final OffsetDateTime observedTimestamp, final ContentType contentType) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Collection<DataNode> dataNodes =
                buildDataNodes(dataspaceName, anchorName, ROOT_NODE_XPATH, nodeData, contentType);
        cpsDataPersistenceService.storeDataNodes(dataspaceName, anchorName, dataNodes);
        processDataUpdatedEventAsync(dataspaceName, anchorName, ROOT_NODE_XPATH, CREATE, observedTimestamp);
    }

    @Override
    public void saveData(final String dataspaceName, final String anchorName, final String parentNodeXpath,
                         final String nodeData, final OffsetDateTime observedTimestamp) {
        saveData(dataspaceName, anchorName, parentNodeXpath, nodeData, observedTimestamp, ContentType.JSON);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.child.save",
        description = "Time taken to save a child data node")
    public void saveData(final String dataspaceName, final String anchorName, final String parentNodeXpath,
                         final String nodeData, final OffsetDateTime observedTimestamp,
                         final ContentType contentType) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Collection<DataNode> dataNodes =
                buildDataNodes(dataspaceName, anchorName, parentNodeXpath, nodeData, contentType);
        cpsDataPersistenceService.addChildDataNodes(dataspaceName, anchorName, parentNodeXpath, dataNodes);
        processDataUpdatedEventAsync(dataspaceName, anchorName, parentNodeXpath, CREATE, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.list.element.save",
        description = "Time taken to save a list element")
    public void saveListElements(final String dataspaceName, final String anchorName,
        final String parentNodeXpath, final String jsonData, final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Collection<DataNode> listElementDataNodeCollection =
            buildDataNodes(dataspaceName, anchorName, parentNodeXpath, jsonData, ContentType.JSON);
        cpsDataPersistenceService.addListElements(dataspaceName, anchorName, parentNodeXpath,
            listElementDataNodeCollection);
        processDataUpdatedEventAsync(dataspaceName, anchorName, parentNodeXpath, UPDATE, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.list.element.batch.save",
        description = "Time taken to save a batch of list elements")
    public void saveListElementsBatch(final String dataspaceName, final String anchorName, final String parentNodeXpath,
            final Collection<String> jsonDataList, final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Collection<Collection<DataNode>> listElementDataNodeCollections =
                buildDataNodes(dataspaceName, anchorName, parentNodeXpath, jsonDataList, ContentType.JSON);
        cpsDataPersistenceService.addMultipleLists(dataspaceName, anchorName, parentNodeXpath,
                listElementDataNodeCollections);
        processDataUpdatedEventAsync(dataspaceName, anchorName, parentNodeXpath, UPDATE, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.get",
        description = "Time taken to get a data node")
    public DataNode getDataNode(final String dataspaceName, final String anchorName, final String xpath,
        final FetchDescendantsOption fetchDescendantsOption) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        return cpsDataPersistenceService.getDataNode(dataspaceName, anchorName, xpath, fetchDescendantsOption);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.batch.get",
        description = "Time taken to get a batch of data nodes")
    public Collection<DataNode> getDataNodes(final String dataspaceName, final String anchorName,
                                             final Collection<String> xpaths,
                                final FetchDescendantsOption fetchDescendantsOption) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        return cpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, xpaths, fetchDescendantsOption);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.leaves.update",
        description = "Time taken to get a batch of data nodes")
    public void updateNodeLeaves(final String dataspaceName, final String anchorName, final String parentNodeXpath,
        final String jsonData, final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final DataNode dataNode = buildDataNode(dataspaceName, anchorName, parentNodeXpath, jsonData, ContentType.JSON);
        cpsDataPersistenceService
            .updateDataLeaves(dataspaceName, anchorName, dataNode.getXpath(), dataNode.getLeaves());
        processDataUpdatedEventAsync(dataspaceName, anchorName, parentNodeXpath, UPDATE, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.leaves.descendants.leaves.update",
        description = "Time taken to update data node leaves and existing descendants leaves")
    public void updateNodeLeavesAndExistingDescendantLeaves(final String dataspaceName, final String anchorName,
        final String parentNodeXpath,
        final String dataNodeUpdatesAsJson,
        final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Collection<DataNode> dataNodeUpdates =
            buildDataNodes(dataspaceName, anchorName,
                parentNodeXpath, dataNodeUpdatesAsJson, ContentType.JSON);
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
    @Timed(value = "cps.data.service.datanode.descendants.update",
        description = "Time taken to update a data node and descendants")
    public void updateDataNodeAndDescendants(final String dataspaceName, final String anchorName,
                                             final String parentNodeXpath, final String jsonData,
                                             final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Collection<DataNode> dataNodes =
                buildDataNodes(dataspaceName, anchorName, parentNodeXpath, jsonData, ContentType.JSON);
        final ArrayList<DataNode> nodes = new ArrayList<>(dataNodes);
        cpsDataPersistenceService.updateDataNodesAndDescendants(dataspaceName, anchorName, nodes);
        processDataUpdatedEventAsync(dataspaceName, anchorName, parentNodeXpath, UPDATE, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.descendants.batch.update",
        description = "Time taken to update a batch of data nodes and descendants")
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
    @Timed(value = "cps.data.service.list.update",
        description = "Time taken to update a list")
    public void replaceListContent(final String dataspaceName, final String anchorName, final String parentNodeXpath,
            final String jsonData, final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Collection<DataNode> newListElements =
                buildDataNodes(dataspaceName, anchorName, parentNodeXpath, jsonData, ContentType.JSON);
        replaceListContent(dataspaceName, anchorName, parentNodeXpath, newListElements, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.list.batch.update",
        description = "Time taken to update a batch of lists")
    public void replaceListContent(final String dataspaceName, final String anchorName, final String parentNodeXpath,
            final Collection<DataNode> dataNodes, final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        cpsDataPersistenceService.replaceListContent(dataspaceName, anchorName, parentNodeXpath, dataNodes);
        processDataUpdatedEventAsync(dataspaceName, anchorName, parentNodeXpath, UPDATE, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.delete",
        description = "Time taken to delete a datanode")
    public void deleteDataNode(final String dataspaceName, final String anchorName, final String dataNodeXpath,
                               final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        cpsDataPersistenceService.deleteDataNode(dataspaceName, anchorName, dataNodeXpath);
        processDataUpdatedEventAsync(dataspaceName, anchorName, dataNodeXpath, DELETE, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.batch.delete",
        description = "Time taken to delete a batch of datanodes")
    public void deleteDataNodes(final String dataspaceName, final String anchorName,
        final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        processDataUpdatedEventAsync(dataspaceName, anchorName, ROOT_NODE_XPATH, DELETE, observedTimestamp);
        cpsDataPersistenceService.deleteDataNodes(dataspaceName, anchorName);
    }

    @Override
    @Timed(value = "cps.data.service.list.delete",
        description = "Time taken to delete a list or list element")
    public void deleteListOrListElement(final String dataspaceName, final String anchorName, final String listNodeXpath,
        final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        cpsDataPersistenceService.deleteListDataNode(dataspaceName, anchorName, listNodeXpath);
        processDataUpdatedEventAsync(dataspaceName, anchorName, listNodeXpath, DELETE, observedTimestamp);
    }

    private DataNode buildDataNode(final String dataspaceName, final String anchorName,
                                   final String parentNodeXpath, final String nodeData,
                                   final ContentType contentType) {

        final Anchor anchor = cpsAdminService.getAnchor(dataspaceName, anchorName);
        final SchemaContext schemaContext = getSchemaContext(dataspaceName, anchor.getSchemaSetName());

        if (ROOT_NODE_XPATH.equals(parentNodeXpath)) {
            final ContainerNode containerNode = timedYangParser.parseData(contentType, nodeData, schemaContext);
            return new DataNodeBuilder().withContainerNode(containerNode).build();
        }

        final ContainerNode containerNode =
            timedYangParser.parseData(contentType, nodeData, schemaContext, parentNodeXpath);

        return new DataNodeBuilder()
                .withParentNodeXpath(parentNodeXpath)
                .withContainerNode(containerNode)
                .build();
    }

    private List<DataNode> buildDataNodes(final String dataspaceName, final String anchorName,
                                          final Map<String, String> nodesJsonData) {
        return nodesJsonData.entrySet().stream().map(nodeJsonData ->
            buildDataNode(dataspaceName, anchorName, nodeJsonData.getKey(),
                nodeJsonData.getValue(), ContentType.JSON)).collect(Collectors.toList());
    }

    private Collection<DataNode> buildDataNodes(final String dataspaceName,
                                                final String anchorName,
                                                final String parentNodeXpath,
                                                final String nodeData,
                                                final ContentType contentType) {

        final Anchor anchor = cpsAdminService.getAnchor(dataspaceName, anchorName);
        final SchemaContext schemaContext = getSchemaContext(dataspaceName, anchor.getSchemaSetName());

        if (ROOT_NODE_XPATH.equals(parentNodeXpath)) {
            final ContainerNode containerNode = timedYangParser.parseData(contentType, nodeData, schemaContext);
            final Collection<DataNode> dataNodes = new DataNodeBuilder()
                    .withContainerNode(containerNode)
                    .buildCollection();
            if (dataNodes.isEmpty()) {
                throw new DataValidationException("Invalid data.", "No data nodes provided");
            }
            return dataNodes;
        }
        final ContainerNode containerNode =
            timedYangParser.parseData(contentType, nodeData, schemaContext, parentNodeXpath);
        final Collection<DataNode> dataNodes = new DataNodeBuilder()
            .withParentNodeXpath(parentNodeXpath)
            .withContainerNode(containerNode)
            .buildCollection();
        if (dataNodes.isEmpty()) {
            throw new DataValidationException("Invalid data.", "No data nodes provided");
        }
        return dataNodes;
    }

    private Collection<Collection<DataNode>> buildDataNodes(final String dataspaceName, final String anchorName,
            final String parentNodeXpath, final Collection<String> nodeDataList, final ContentType contentType) {
        return nodeDataList.stream()
                .map(nodeData -> buildDataNodes(dataspaceName, anchorName, parentNodeXpath, nodeData, contentType))
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
