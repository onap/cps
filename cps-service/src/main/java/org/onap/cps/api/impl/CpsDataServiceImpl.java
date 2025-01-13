/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2020-2022 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2022-2024 TechMahindra Ltd.
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

import io.micrometer.core.annotation.Timed;
import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsDeltaService;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.events.CpsDataUpdateEventsService;
import org.onap.cps.events.model.Data.Operation;
import org.onap.cps.impl.utils.CpsValidator;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.DataNodeBuilder;
import org.onap.cps.spi.model.DeltaReport;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.DataMapUtils;
import org.onap.cps.utils.JsonObjectMapper;
import org.onap.cps.utils.PrefixResolver;
import org.onap.cps.utils.YangParser;
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class CpsDataServiceImpl implements CpsDataService {

    private static final String ROOT_NODE_XPATH = "/";
    private static final String PARENT_NODE_XPATH_FOR_ROOT_NODE_XPATH = "";
    private static final long DEFAULT_LOCK_TIMEOUT_IN_MILLISECONDS = 300L;
    private static final String NO_DATA_NODES = "No data nodes.";

    private final CpsDataPersistenceService cpsDataPersistenceService;
    private final CpsDataUpdateEventsService cpsDataUpdateEventsService;
    private final CpsAnchorService cpsAnchorService;

    private final CpsValidator cpsValidator;
    private final YangParser yangParser;
    private final CpsDeltaService cpsDeltaService;
    private final JsonObjectMapper jsonObjectMapper;
    private final PrefixResolver prefixResolver;

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
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final Collection<DataNode> dataNodes =
                buildDataNodesWithParentNodeXpath(anchor, ROOT_NODE_XPATH, nodeData, contentType);
        cpsDataPersistenceService.storeDataNodes(dataspaceName, anchorName, dataNodes);
        sendDataUpdatedEvent(anchor, ROOT_NODE_XPATH, Operation.CREATE, observedTimestamp);
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
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final Collection<DataNode> dataNodes =
                buildDataNodesWithParentNodeXpath(anchor, parentNodeXpath, nodeData, contentType);
        cpsDataPersistenceService.addChildDataNodes(dataspaceName, anchorName, parentNodeXpath, dataNodes);
        sendDataUpdatedEvent(anchor, parentNodeXpath, Operation.CREATE, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.list.element.save",
        description = "Time taken to save list elements")
    public void saveListElements(final String dataspaceName, final String anchorName,
                                 final String parentNodeXpath, final String nodeData,
                                 final OffsetDateTime observedTimestamp, final ContentType contentType) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final Collection<DataNode> listElementDataNodeCollection =
            buildDataNodesWithParentNodeXpath(anchor, parentNodeXpath, nodeData, contentType);
        if (isRootNodeXpath(parentNodeXpath)) {
            cpsDataPersistenceService.storeDataNodes(dataspaceName, anchorName, listElementDataNodeCollection);
        } else {
            cpsDataPersistenceService.addListElements(dataspaceName, anchorName, parentNodeXpath,
                                                      listElementDataNodeCollection);
        }
        sendDataUpdatedEvent(anchor, parentNodeXpath, Operation.UPDATE, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.get",
            description = "Time taken to get data nodes for an xpath")
    public Collection<DataNode> getDataNodes(final String dataspaceName, final String anchorName,
                                             final String xpath,
                                             final FetchDescendantsOption fetchDescendantsOption) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        return cpsDataPersistenceService.getDataNodes(dataspaceName, anchorName, xpath, fetchDescendantsOption);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.batch.get",
        description = "Time taken to get a batch of data nodes")
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
        final Collection<DataNode> dataNodesInPatch =
                buildDataNodesWithParentNodeXpath(anchor, parentNodeXpath, nodeData, contentType);
        final Map<String, Map<String, Serializable>> xpathToUpdatedLeaves = dataNodesInPatch.stream()
                .collect(Collectors.toMap(DataNode::getXpath, DataNode::getLeaves));
        cpsDataPersistenceService.batchUpdateDataLeaves(dataspaceName, anchorName, xpathToUpdatedLeaves);
        sendDataUpdatedEvent(anchor, parentNodeXpath, Operation.UPDATE, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.leaves.descendants.leaves.update",
        description = "Time taken to update data node leaves and existing descendants leaves")
    public void updateNodeLeavesAndExistingDescendantLeaves(final String dataspaceName, final String anchorName,
        final String parentNodeXpath,
        final String dataNodeUpdatesAsJson,
        final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final Collection<DataNode> dataNodeUpdates =
            buildDataNodesWithParentNodeXpath(anchor, parentNodeXpath, dataNodeUpdatesAsJson, ContentType.JSON);
        for (final DataNode dataNodeUpdate : dataNodeUpdates) {
            processDataNodeUpdate(anchor, dataNodeUpdate);
        }
        sendDataUpdatedEvent(anchor, parentNodeXpath, Operation.UPDATE, observedTimestamp);
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
    @Timed(value = "cps.data.service.get.delta",
            description = "Time taken to get delta between anchors")
    public List<DeltaReport> getDeltaByDataspaceAndAnchors(final String dataspaceName,
                                                           final String sourceAnchorName,
                                                           final String targetAnchorName, final String xpath,
                                                           final FetchDescendantsOption fetchDescendantsOption,
                                                            final boolean groupingEnabled) {

        final Collection<DataNode> sourceDataNodes = getDataNodesForMultipleXpaths(dataspaceName,
                sourceAnchorName, Collections.singletonList(xpath), fetchDescendantsOption);
        final Collection<DataNode> targetDataNodes = getDataNodesForMultipleXpaths(dataspaceName,
                targetAnchorName, Collections.singletonList(xpath), fetchDescendantsOption);

        return cpsDeltaService.getDeltaReports(sourceDataNodes, targetDataNodes, groupingEnabled);
    }

    @Timed(value = "cps.data.service.get.deltaBetweenAnchorAndPayload",
            description = "Time taken to get delta between anchor and a payload")
    @Override
    public List<DeltaReport> getDeltaByDataspaceAnchorAndPayload(final String dataspaceName,
                                                                final String sourceAnchorName, final String xpath,
                                                                final Map<String, String> yangResourcesNameToContentMap,
                                                                final String targetData,
                                                                final FetchDescendantsOption fetchDescendantsOption) {

        final Anchor sourceAnchor = cpsAnchorService.getAnchor(dataspaceName, sourceAnchorName);

        final Collection<DataNode> sourceDataNodes = getDataNodes(dataspaceName,
                sourceAnchorName, xpath, fetchDescendantsOption);

        final Collection<DataNode> sourceDataNodesRebuilt =
                new ArrayList<>(rebuildSourceDataNodes(xpath, sourceAnchor, sourceDataNodes));

        final Collection<DataNode> targetDataNodes =
                new ArrayList<>(buildTargetDataNodes(sourceAnchor, xpath, yangResourcesNameToContentMap, targetData));

        return cpsDeltaService.getDeltaReports(sourceDataNodesRebuilt, targetDataNodes, false);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.descendants.update",
        description = "Time taken to update a data node and descendants")
    public void updateDataNodeAndDescendants(final String dataspaceName, final String anchorName,
                                             final String parentNodeXpath, final String nodeData,
                                             final OffsetDateTime observedTimestamp, final ContentType contentType) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final Collection<DataNode> dataNodes =
                buildDataNodesWithParentNodeXpath(anchor, parentNodeXpath, nodeData, contentType);
        cpsDataPersistenceService.updateDataNodesAndDescendants(dataspaceName, anchorName, dataNodes);
        sendDataUpdatedEvent(anchor, parentNodeXpath, Operation.UPDATE, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.descendants.batch.update",
        description = "Time taken to update a batch of data nodes and descendants")
    public void updateDataNodesAndDescendants(final String dataspaceName, final String anchorName,
                                              final Map<String, String> nodeDataPerXPath,
                                              final OffsetDateTime observedTimestamp, final ContentType contentType) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final Collection<DataNode> dataNodes = buildDataNodesWithParentNodeXpath(anchor, nodeDataPerXPath, contentType);
        cpsDataPersistenceService.updateDataNodesAndDescendants(dataspaceName, anchorName, dataNodes);
        nodeDataPerXPath.keySet().forEach(nodeXpath ->
                sendDataUpdatedEvent(anchor, nodeXpath, Operation.UPDATE, observedTimestamp));
    }

    @Override
    @Timed(value = "cps.data.service.list.update",
        description = "Time taken to update a list")
    public void replaceListContent(final String dataspaceName, final String anchorName, final String parentNodeXpath,
            final String jsonData, final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final Collection<DataNode> newListElements =
            buildDataNodesWithParentNodeXpath(anchor, parentNodeXpath, jsonData, ContentType.JSON);
        replaceListContent(dataspaceName, anchorName, parentNodeXpath, newListElements, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.list.batch.update",
        description = "Time taken to update a batch of lists")
    public void replaceListContent(final String dataspaceName, final String anchorName, final String parentNodeXpath,
            final Collection<DataNode> dataNodes, final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        cpsDataPersistenceService.replaceListContent(dataspaceName, anchorName, parentNodeXpath, dataNodes);
        sendDataUpdatedEvent(anchor, parentNodeXpath, Operation.UPDATE, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.delete",
        description = "Time taken to delete a datanode")
    public void deleteDataNode(final String dataspaceName, final String anchorName, final String dataNodeXpath,
                               final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        cpsDataPersistenceService.deleteDataNode(dataspaceName, anchorName, dataNodeXpath);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        sendDataUpdatedEvent(anchor, dataNodeXpath, Operation.DELETE, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.batch.delete",
        description = "Time taken to delete a batch of datanodes")
    public void deleteDataNodes(final String dataspaceName, final String anchorName,
                                final Collection<String> dataNodeXpaths, final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        cpsDataPersistenceService.deleteDataNodes(dataspaceName, anchorName, dataNodeXpaths);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        dataNodeXpaths.forEach(dataNodeXpath ->
                sendDataUpdatedEvent(anchor, dataNodeXpath, Operation.DELETE, observedTimestamp));
    }


    @Override
    @Timed(value = "cps.data.service.datanode.delete.anchor",
        description = "Time taken to delete all datanodes for an anchor")
    public void deleteDataNodes(final String dataspaceName, final String anchorName,
                                final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        cpsDataPersistenceService.deleteDataNodes(dataspaceName, anchorName);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        sendDataUpdatedEvent(anchor, ROOT_NODE_XPATH, Operation.DELETE, observedTimestamp);
    }

    @Override
    @Timed(value = "cps.data.service.datanode.delete.anchor.batch",
        description = "Time taken to delete all datanodes for multiple anchors")
    public void deleteDataNodes(final String dataspaceName, final Collection<String> anchorNames,
                                final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName);
        cpsValidator.validateNameCharacters(anchorNames);
        cpsDataPersistenceService.deleteDataNodes(dataspaceName, anchorNames);
        for (final Anchor anchor : cpsAnchorService.getAnchors(dataspaceName, anchorNames)) {
            sendDataUpdatedEvent(anchor, ROOT_NODE_XPATH, Operation.DELETE, observedTimestamp);
        }
    }

    @Override
    @Timed(value = "cps.data.service.list.delete",
        description = "Time taken to delete a list or list element")
    public void deleteListOrListElement(final String dataspaceName, final String anchorName, final String listNodeXpath,
        final OffsetDateTime observedTimestamp) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        cpsDataPersistenceService.deleteListDataNode(dataspaceName, anchorName, listNodeXpath);
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        sendDataUpdatedEvent(anchor, listNodeXpath, Operation.DELETE, observedTimestamp);
    }

    @Override
    public void validateData(final String dataspaceName, final String anchorName, final String parentNodeXpath,
                             final String nodeData, final ContentType contentType) {
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final String xpath = ROOT_NODE_XPATH.equals(parentNodeXpath) ? PARENT_NODE_XPATH_FOR_ROOT_NODE_XPATH :
                CpsPathUtil.getNormalizedXpath(parentNodeXpath);
        yangParser.validateData(contentType, nodeData, anchor, xpath);
    }

    private Collection<DataNode> rebuildSourceDataNodes(final String xpath, final Anchor sourceAnchor,
                                                        final Collection<DataNode> sourceDataNodes) {

        final Collection<DataNode> sourceDataNodesRebuilt = new ArrayList<>();
        if (sourceDataNodes != null) {
            final String sourceDataNodesAsJson = getDataNodesAsJson(sourceAnchor, sourceDataNodes);
            sourceDataNodesRebuilt.addAll(
                    buildDataNodesWithAnchorAndXpath(sourceAnchor, xpath, sourceDataNodesAsJson, ContentType.JSON));
        }
        return sourceDataNodesRebuilt;
    }

    private Collection<DataNode> buildTargetDataNodes(final Anchor sourceAnchor, final String xpath,
                                                      final Map<String, String> yangResourcesNameToContentMap,
                                                      final String targetData) {
        if (yangResourcesNameToContentMap.isEmpty()) {
            return buildDataNodesWithAnchorAndXpath(sourceAnchor, xpath, targetData, ContentType.JSON);
        } else {
            return buildDataNodesWithYangResourceAndXpath(yangResourcesNameToContentMap, xpath,
                    targetData, ContentType.JSON);
        }
    }

    private String getDataNodesAsJson(final Anchor anchor, final Collection<DataNode> dataNodes) {

        final List<Map<String, Object>> prefixToDataNodes = prefixResolver(anchor, dataNodes);
        final Map<String, Object> targetDataAsJsonObject = getNodeDataAsJsonString(prefixToDataNodes);
        return jsonObjectMapper.asJsonString(targetDataAsJsonObject);
    }

    private Map<String, Object> getNodeDataAsJsonString(final List<Map<String, Object>> prefixToDataNodes) {
        final Map<String, Object>  nodeDataAsJson = new HashMap<>();
        for (final Map<String, Object> prefixToDataNode : prefixToDataNodes) {
            nodeDataAsJson.putAll(prefixToDataNode);
        }
        return nodeDataAsJson;
    }

    private List<Map<String, Object>> prefixResolver(final Anchor anchor, final Collection<DataNode> dataNodes) {
        final List<Map<String, Object>> prefixToDataNodes = new ArrayList<>(dataNodes.size());
        for (final DataNode dataNode: dataNodes) {
            final String prefix = prefixResolver.getPrefix(anchor, dataNode.getXpath());
            final Map<String, Object> prefixToDataNode = DataMapUtils.toDataMapWithIdentifier(dataNode, prefix);
            prefixToDataNodes.add(prefixToDataNode);
        }
        return prefixToDataNodes;
    }

    private Collection<DataNode> buildDataNodesWithParentNodeXpath(final Anchor anchor,
                                                                   final Map<String, String> nodesJsonData,
                                                                   final ContentType contentType) {
        final Collection<DataNode> dataNodes = new ArrayList<>();
        for (final Map.Entry<String, String> nodeJsonData : nodesJsonData.entrySet()) {
            dataNodes.addAll(buildDataNodesWithParentNodeXpath(anchor, nodeJsonData.getKey(),
                    nodeJsonData.getValue(), contentType));
        }
        return dataNodes;
    }

    private Collection<DataNode> buildDataNodesWithParentNodeXpath(final Anchor anchor, final String parentNodeXpath,
                                                                 final String nodeData, final ContentType contentType) {

        if (ROOT_NODE_XPATH.equals(parentNodeXpath)) {
            final ContainerNode containerNode = yangParser.parseData(contentType, nodeData,
                    anchor, PARENT_NODE_XPATH_FOR_ROOT_NODE_XPATH);
            final Collection<DataNode> dataNodes = new DataNodeBuilder()
                    .withContainerNode(containerNode)
                    .buildCollection();
            if (dataNodes.isEmpty()) {
                throw new DataValidationException(NO_DATA_NODES, "No data nodes provided");
            }
            return dataNodes;
        }
        final String normalizedParentNodeXpath = CpsPathUtil.getNormalizedXpath(parentNodeXpath);
        final ContainerNode containerNode =
            yangParser.parseData(contentType, nodeData, anchor, normalizedParentNodeXpath);
        final Collection<DataNode> dataNodes = new DataNodeBuilder()
            .withParentNodeXpath(normalizedParentNodeXpath)
            .withContainerNode(containerNode)
            .buildCollection();
        if (dataNodes.isEmpty()) {
            throw new DataValidationException(NO_DATA_NODES, "No data nodes provided");
        }
        return dataNodes;
    }

    private Collection<DataNode> buildDataNodesWithParentNodeXpath(
                                          final Map<String, String> yangResourcesNameToContentMap, final String xpath,
                                          final String nodeData, final ContentType contentType) {

        if (isRootNodeXpath(xpath)) {
            final ContainerNode containerNode = yangParser.parseData(contentType, nodeData,
                    yangResourcesNameToContentMap, PARENT_NODE_XPATH_FOR_ROOT_NODE_XPATH);
            final Collection<DataNode> dataNodes = new DataNodeBuilder()
                    .withContainerNode(containerNode)
                    .buildCollection();
            if (dataNodes.isEmpty()) {
                throw new DataValidationException(NO_DATA_NODES, "Data nodes were not found under the xpath " + xpath);
            }
            return dataNodes;
        }
        final String normalizedParentNodeXpath = CpsPathUtil.getNormalizedXpath(xpath);
        final ContainerNode containerNode =
                yangParser.parseData(contentType, nodeData, yangResourcesNameToContentMap, normalizedParentNodeXpath);
        final Collection<DataNode> dataNodes = new DataNodeBuilder()
                .withParentNodeXpath(normalizedParentNodeXpath)
                .withContainerNode(containerNode)
                .buildCollection();
        if (dataNodes.isEmpty()) {
            throw new DataValidationException(NO_DATA_NODES, "Data nodes were not found under the xpath " + xpath);
        }
        return dataNodes;
    }

    private Collection<DataNode> buildDataNodesWithAnchorAndXpath(final Anchor anchor, final String xpath,
                                                                  final String nodeData,
                                                                  final ContentType contentType) {

        if (!isRootNodeXpath(xpath)) {
            final String parentNodeXpath = CpsPathUtil.getNormalizedParentXpath(xpath);
            if (parentNodeXpath.isEmpty()) {
                return buildDataNodesWithParentNodeXpath(anchor, ROOT_NODE_XPATH, nodeData, contentType);
            }
            return buildDataNodesWithParentNodeXpath(anchor, parentNodeXpath, nodeData, contentType);
        }
        return buildDataNodesWithParentNodeXpath(anchor, xpath, nodeData, contentType);
    }

    private Collection<DataNode> buildDataNodesWithYangResourceAndXpath(
                                            final Map<String, String> yangResourcesNameToContentMap, final String xpath,
                                            final String nodeData, final ContentType contentType) {
        if (!isRootNodeXpath(xpath)) {
            final String parentNodeXpath = CpsPathUtil.getNormalizedParentXpath(xpath);
            if (parentNodeXpath.isEmpty()) {
                return buildDataNodesWithParentNodeXpath(yangResourcesNameToContentMap, ROOT_NODE_XPATH,
                        nodeData, contentType);
            }
            return buildDataNodesWithParentNodeXpath(yangResourcesNameToContentMap, parentNodeXpath,
                    nodeData, contentType);
        }
        return buildDataNodesWithParentNodeXpath(yangResourcesNameToContentMap, xpath, nodeData, contentType);
    }

    private static boolean isRootNodeXpath(final String xpath) {
        return ROOT_NODE_XPATH.equals(xpath);
    }

    private void processDataNodeUpdate(final Anchor anchor, final DataNode dataNodeUpdate) {
        cpsDataPersistenceService.batchUpdateDataLeaves(anchor.getDataspaceName(), anchor.getName(),
                Collections.singletonMap(dataNodeUpdate.getXpath(), dataNodeUpdate.getLeaves()));
        final Collection<DataNode> childDataNodeUpdates = dataNodeUpdate.getChildDataNodes();
        for (final DataNode childDataNodeUpdate : childDataNodeUpdates) {
            processDataNodeUpdate(anchor, childDataNodeUpdate);
        }
    }

    private void sendDataUpdatedEvent(final Anchor anchor, final String xpath,
                                      final Operation operation, final OffsetDateTime observedTimestamp) {
        try {
            cpsDataUpdateEventsService.publishCpsDataUpdateEvent(anchor, xpath, operation, observedTimestamp);
        } catch (final Exception exception) {
            log.error("Failed to send message to notification service", exception);
        }
    }
}
