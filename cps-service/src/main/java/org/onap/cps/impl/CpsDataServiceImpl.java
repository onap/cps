/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2020-2022 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2022-2025 TechMahindra Ltd.
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

package org.onap.cps.impl;

import static org.onap.cps.cpspath.parser.CpsPathUtil.NO_PARENT_PATH;
import static org.onap.cps.cpspath.parser.CpsPathUtil.ROOT_NODE_XPATH;
import static org.onap.cps.events.model.EventPayload.Action.CREATE;
import static org.onap.cps.events.model.EventPayload.Action.REMOVE;
import static org.onap.cps.events.model.EventPayload.Action.REPLACE;
import static org.onap.cps.utils.ContentType.JSON;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.micrometer.core.annotation.Timed;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.DataNodeFactory;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.events.CpsDataUpdateEventsProducer;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.CpsValidator;
import org.onap.cps.utils.YangParser;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CpsDataServiceImpl implements CpsDataService {

    private static final long DEFAULT_LOCK_TIMEOUT_IN_MILLISECONDS = 300L;
    private static final String CREATE_ACTION = CREATE.value();
    private static final String REMOVE_ACTION = REMOVE.value();
    private static final String REPLACE_ACTION = REPLACE.value();

    private final CpsDataPersistenceService cpsDataPersistenceService;
    private final CpsDataUpdateEventsProducer cpsDataUpdateEventsProducer;
    private final CpsAnchorService cpsAnchorService;
    private final DataNodeFactory dataNodeFactory;
    private final CpsValidator cpsValidator;
    private final YangParser yangParser;

    @Override
    public void saveData(final String dataspaceName, final String anchorName, final String nodeData,
        final OffsetDateTime observedTimestamp) {
        saveData(dataspaceName, anchorName, nodeData, observedTimestamp, JSON);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.root.save", description = "Time taken to save a root data node")
    public void saveData(final String dataspaceName, final String anchorName, final String nodeData,
                         final OffsetDateTime observedTimestamp, final ContentType contentType) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final Collection<DataNode> dataNodes = dataNodeFactory
                .createDataNodesWithAnchorParentXpathAndNodeData(anchor, ROOT_NODE_XPATH, nodeData, contentType);
        cpsDataPersistenceService.storeDataNodes(dataspaceName, anchorName, dataNodes);
        sendDataUpdatedEvent(anchor, ROOT_NODE_XPATH, CREATE_ACTION, observedTimestamp);
    }

    @Override
    public void saveData(final String dataspaceName, final String anchorName, final String parentNodeXpath,
                         final String nodeData, final OffsetDateTime observedTimestamp) {
        saveData(dataspaceName, anchorName, parentNodeXpath, nodeData, observedTimestamp, JSON);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.child.save", description = "Time taken to save a child data node")
    public void saveData(final String dataspaceName, final String anchorName, final String parentNodeXpath,
                         final String nodeData, final OffsetDateTime observedTimestamp,
                         final ContentType contentType) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final Collection<DataNode> dataNodes = dataNodeFactory
                .createDataNodesWithAnchorParentXpathAndNodeData(anchor, parentNodeXpath, nodeData, contentType);
        cpsDataPersistenceService.addChildDataNodes(dataspaceName, anchorName, parentNodeXpath, dataNodes);
        sendDataUpdatedEvent(anchor, parentNodeXpath, CREATE_ACTION, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.list.element.save", description = "Time taken to save list elements")
    public void saveListElements(final String dataspaceName, final String anchorName,
                                 final String parentNodeXpath, final String nodeData,
                                 final OffsetDateTime observedTimestamp, final ContentType contentType) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final Collection<DataNode> listElementDataNodeCollection = dataNodeFactory
                    .createDataNodesWithAnchorParentXpathAndNodeData(anchor, parentNodeXpath, nodeData, contentType);
        if (ROOT_NODE_XPATH.equals(parentNodeXpath)) {
            cpsDataPersistenceService.storeDataNodes(dataspaceName, anchorName, listElementDataNodeCollection);
        } else {
            cpsDataPersistenceService.addListElements(dataspaceName, anchorName, parentNodeXpath,
                                                      listElementDataNodeCollection);
        }
        sendDataUpdatedEvent(anchor, parentNodeXpath, REPLACE_ACTION, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.get", description = "Time taken to get data nodes for an xpath")
    public Collection<DataNode> getDataNodes(final String dataspaceName, final String anchorName,
                                             final String xpath,
                                             final FetchDescendantsOption fetchDescendantsOption) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        return cpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, xpath, fetchDescendantsOption);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.batch.get", description = "Time taken to get a batch of data nodes")
    public Collection<DataNode> getDataNodesForMultipleXpaths(final String dataspaceName, final String anchorName,
                                                              final Collection<String> xpaths,
                                                              final FetchDescendantsOption fetchDescendantsOption) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        return cpsDataPersistenceService.getDataNodesForMultipleXpaths(dataspaceName, anchorName, xpaths,
                fetchDescendantsOption);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.leaves.update",
        description = "Time taken to update a batch of leaf data nodes")
    public void updateNodeLeaves(final String dataspaceName, final String anchorName, final String parentNodeXpath,
        final String nodeData, final OffsetDateTime observedTimestamp, final ContentType contentType) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final Collection<DataNode> dataNodesInPatch = dataNodeFactory
                .createDataNodesWithAnchorParentXpathAndNodeData(anchor, parentNodeXpath, nodeData, contentType);
        final Map<String, Map<String, Serializable>> xpathToUpdatedLeaves = dataNodesInPatch.stream()
                .collect(Collectors.toMap(DataNode::getXpath, DataNode::getLeaves));
        cpsDataPersistenceService.batchUpdateDataLeaves(dataspaceName, anchorName, xpathToUpdatedLeaves);
        sendDataUpdatedEvent(anchor, parentNodeXpath, REPLACE_ACTION, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.leaves.descendants.leaves.update",
        description = "Time taken to update data node leaves and existing descendants leaves")
    public void updateNodeLeavesAndExistingDescendantLeaves(final String dataspaceName,
                                                            final String anchorName,
                                                            final String parentNodeXpath,
                                                            final String dataNodeUpdatesAsJson,
                                                            final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final Collection<DataNode> dataNodeUpdates = dataNodeFactory
                .createDataNodesWithAnchorParentXpathAndNodeData(anchor, parentNodeXpath, dataNodeUpdatesAsJson, JSON);
        for (final DataNode dataNodeUpdate : dataNodeUpdates) {
            processDataNodeUpdate(anchor, dataNodeUpdate);
        }
        sendDataUpdatedEvent(anchor, parentNodeXpath, REPLACE_ACTION, observedTimestamp);
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
                                             final String parentNodeXpath, final String nodeData,
                                             final OffsetDateTime observedTimestamp, final ContentType contentType) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final Collection<DataNode> dataNodes = dataNodeFactory
                .createDataNodesWithAnchorParentXpathAndNodeData(anchor, parentNodeXpath, nodeData, contentType);
        if ("/".equals(parentNodeXpath)) {
            final JsonObject jsonObject = JsonParser.parseString(nodeData).getAsJsonObject();
            if (jsonObject.entrySet().isEmpty()) {
                throw new DataValidationException("Unsupported Normalized Node", "No valid node found");
            }
        } else if (CpsPathUtil.isPathToListElement(parentNodeXpath)) {
            cpsDataPersistenceService.replaceListContent(dataspaceName, anchorName, parentNodeXpath, dataNodes);
        }
        cpsDataPersistenceService.updateDataNodesAndDescendants(dataspaceName, anchorName, dataNodes);
        sendDataUpdatedEvent(anchor, parentNodeXpath, REPLACE_ACTION, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.descendants.batch.update",
        description = "Time taken to update a batch of data nodes and descendants")
    public void updateDataNodesAndDescendants(final String dataspaceName, final String anchorName,
                                              final Map<String, String> nodeDataPerParentNodeXPath,
                                              final OffsetDateTime observedTimestamp, final ContentType contentType) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final Collection<DataNode> dataNodes = dataNodeFactory
                .createDataNodesWithAnchorAndXpathToNodeData(anchor, nodeDataPerParentNodeXPath, contentType);
        cpsDataPersistenceService.updateDataNodesAndDescendants(dataspaceName, anchorName, dataNodes);
        nodeDataPerParentNodeXPath.keySet().forEach(nodeXpath ->
                sendDataUpdatedEvent(anchor, nodeXpath, REPLACE_ACTION, observedTimestamp));
    }

    @Override
    @Timed(value = "cps.data.service.list.update", description = "Time taken to update a list")
    public void replaceListContent(final String dataspaceName, final String anchorName, final String parentNodeXpath,
            final String nodeData, final OffsetDateTime observedTimestamp, final ContentType contentType) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final Collection<DataNode> newListElements = dataNodeFactory
                .createDataNodesWithAnchorParentXpathAndNodeData(anchor, parentNodeXpath, nodeData, contentType);
        replaceListContent(dataspaceName, anchorName, parentNodeXpath, newListElements, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.list.batch.update", description = "Time taken to update a batch of lists")
    public void replaceListContent(final String dataspaceName, final String anchorName, final String parentNodeXpath,
            final Collection<DataNode> dataNodes, final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        cpsDataPersistenceService.replaceListContent(dataspaceName, anchorName, parentNodeXpath, dataNodes);
        sendDataUpdatedEvent(anchor, parentNodeXpath, REPLACE_ACTION, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.delete", description = "Time taken to delete a datanode")
    public void deleteDataNode(final String dataspaceName, final String anchorName, final String dataNodeXpath,
                               final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        cpsDataPersistenceService.deleteDataNode(dataspaceName, anchorName, dataNodeXpath);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        sendDataUpdatedEvent(anchor, dataNodeXpath, REMOVE_ACTION, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.batch.delete", description = "Time taken to delete a batch of data nodes")
    public void deleteDataNodes(final String dataspaceName, final String anchorName,
                                final Collection<String> dataNodeXpaths, final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        cpsDataPersistenceService.deleteDataNodes(dataspaceName, anchorName, dataNodeXpaths);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        dataNodeXpaths.forEach(dataNodeXpath ->
                sendDataUpdatedEvent(anchor, dataNodeXpath, REMOVE_ACTION, observedTimestamp));
    }


    @Override
    @Timed(value = "cps.data.service.datanode.delete.anchor",
        description = "Time taken to delete all data nodes for an anchor")
    public void deleteDataNodes(final String dataspaceName, final String anchorName,
                                final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        cpsDataPersistenceService.deleteDataNodes(dataspaceName, anchorName);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        sendDataUpdatedEvent(anchor, ROOT_NODE_XPATH, REMOVE_ACTION, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.delete.anchor.batch",
        description = "Time taken to delete all data nodes for multiple anchors")
    public void deleteDataNodes(final String dataspaceName, final Collection<String> anchorNames,
                                final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName);
        cpsValidator.validateNameCharacters(anchorNames);
        cpsDataPersistenceService.deleteDataNodes(dataspaceName, anchorNames);
        for (final Anchor anchor : cpsAnchorService.getAnchors(dataspaceName, anchorNames)) {
            sendDataUpdatedEvent(anchor, ROOT_NODE_XPATH, REMOVE_ACTION, observedTimestamp);
        }
    }

    @Override
    @Timed(value = "cps.data.service.list.delete", description = "Time taken to delete a list or list element")
    public void deleteListOrListElement(final String dataspaceName, final String anchorName, final String listNodeXpath,
        final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        cpsDataPersistenceService.deleteListDataNode(dataspaceName, anchorName, listNodeXpath);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        sendDataUpdatedEvent(anchor, listNodeXpath, REMOVE_ACTION, observedTimestamp);
    }

    @Override
    public void validateData(final String dataspaceName, final String anchorName, final String parentNodeXpath,
                             final String nodeData, final ContentType contentType) {
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final String xpath = ROOT_NODE_XPATH.equals(parentNodeXpath) ? NO_PARENT_PATH :
                CpsPathUtil.getNormalizedXpath(parentNodeXpath);
        yangParser.validateData(contentType, nodeData, anchor, xpath);
    }

    private void processDataNodeUpdate(final Anchor anchor, final DataNode dataNodeUpdate) {
        cpsDataPersistenceService.batchUpdateDataLeaves(anchor.getDataspaceName(), anchor.getName(),
                Collections.singletonMap(dataNodeUpdate.getXpath(), dataNodeUpdate.getLeaves()));
        final Collection<DataNode> childDataNodeUpdates = dataNodeUpdate.getChildDataNodes();
        for (final DataNode childDataNodeUpdate : childDataNodeUpdates) {
            processDataNodeUpdate(anchor, childDataNodeUpdate);
        }
    }

    private void sendDataUpdatedEvent(final Anchor anchor,
                                      final String xpath,
                                      final String action,
                                      final OffsetDateTime observedTimestamp) {
        try {
            cpsDataUpdateEventsProducer.sendCpsDataUpdateEvent(anchor, xpath, action, observedTimestamp);
        } catch (final Exception exception) {
            log.error("Failed to send message to notification service", exception);
        }
    }
}
