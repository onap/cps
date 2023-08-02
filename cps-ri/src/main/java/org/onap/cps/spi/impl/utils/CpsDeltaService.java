/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.spi.impl.utils;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.model.DataNode;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CpsDeltaService {
    private static final String ADD_ACTION = "add";
    private static final String REMOVE_ACTION = "remove";
    private static final String UPDATE_ACTION = "update";

    /**
     * Calculates delta/difference between DataNodes and returns the result as a Collection of Maps. Each map contains
     * information such as action, xpath, source-payload and target-payload. It performs the same operation recursively
     * for every child data node
     *
     * @param dataspaceName       dataspace name
     * @param referenceAnchorName reference anchor name
     * @param comparandAnchorName comparand anchor name
     * @param xpath               parent node xpath
     * @param referenceDataNodes  data nodes used as reference/source for generating delta
     * @param comparandDataNodes  data nodes used as comparand value, and are compared against reference data nodes
     * @return                    Collection of Maps as the delta report between each data node
     */

    public static List<Map<String, Object>> getDeltaByDataspaceAndAnchors(final String dataspaceName,
                                                                   final String referenceAnchorName,
                                                                   final String comparandAnchorName, final String xpath,
                                                                   final Collection<DataNode> referenceDataNodes,
                                                                   final Collection<DataNode> comparandDataNodes) {

        if (referenceDataNodes.isEmpty() && comparandDataNodes.isEmpty()) {
            throw new DataNodeNotFoundException(dataspaceName, referenceAnchorName + "," + comparandAnchorName, xpath);
        }
        return getDeltaBetweenDataNodes(referenceDataNodes, comparandDataNodes);
    }

    private static List<Map<String, Object>> getDeltaBetweenDataNodes(
                                                                final Collection<DataNode> referenceDataNodes,
                                                                final Collection<DataNode> comparandDataNodes) {

        final List<Map<String, Object>> deltaReport = new ArrayList<>();

        final Map<String, DataNode> xpathToReferenceDataNodes = getXpathToDataNode(referenceDataNodes);
        final Map<String, DataNode> xpathToComparandDataNodes = getXpathToDataNode(comparandDataNodes);

        deltaReport.addAll(getRemovedAndUpdatedNodes(referenceDataNodes, xpathToComparandDataNodes));

        deltaReport.addAll(getAddedNodes(xpathToReferenceDataNodes, xpathToComparandDataNodes));

        return Collections.unmodifiableList(deltaReport);
    }

    private static List<Map<String, Object>>  getRemovedAndUpdatedNodes(final Collection<DataNode> referenceDataNodes,
                                                                final Map<String, DataNode> xpathToComparandDataNodes) {

        final List<Map<String, Object>> removedAndUpdatedNodes = new ArrayList<>();
        for (final DataNode referenceDataNode : referenceDataNodes) {
            final String xpath = referenceDataNode.getXpath();
            final DataNode comparandDataNode = xpathToComparandDataNodes.get(xpath);
            if (comparandDataNode == null) {
                final Map<String, Serializable> referenceDataNodeLeaves = referenceDataNode.getLeaves();
                final Collection<DataNode> referenceDataNodesChildren = referenceDataNode.getChildDataNodes();
                if (!referenceDataNodeLeaves.isEmpty() && !referenceDataNodesChildren.isEmpty()) {
                    removedAndUpdatedNodes.add(getDeltaReportEntities(REMOVE_ACTION, xpath, referenceDataNodeLeaves,
                            Collections.emptyMap()));
                    removedAndUpdatedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren,
                            Collections.emptyList()));
                } else if (!referenceDataNodeLeaves.isEmpty()) {
                    removedAndUpdatedNodes.add(getDeltaReportEntities(REMOVE_ACTION, xpath,
                            referenceDataNodeLeaves, Collections.emptyMap()));
                } else if (!referenceDataNode.getChildDataNodes().isEmpty()) {
                    removedAndUpdatedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren,
                            Collections.emptyList()));
                } else {
                    removedAndUpdatedNodes.add(getDeltaReportEntities(REMOVE_ACTION, xpath, Collections.emptyMap(),
                            Collections.emptyMap()));
                }
            } else {
                removedAndUpdatedNodes.addAll(getUpdatedNodes(xpath, referenceDataNode, comparandDataNode));
            }
        }
        return removedAndUpdatedNodes;
    }

    private static List<Map<String, Object>> getUpdatedNodes(final String xpath, final DataNode referenceDataNode,
                                                      final DataNode comparandDataNode) {

        final List<Map<String, Object>> updatedNodes = new ArrayList<>();

        final Map<String, Serializable> referenceDataNodeLeaves = referenceDataNode.getLeaves();
        final Collection<DataNode> referenceDataNodesChildren = referenceDataNode.getChildDataNodes();

        if (!referenceDataNodeLeaves.isEmpty() && !referenceDataNodesChildren.isEmpty()) {
            getUpdatedNodesWhenReferenceDataNodeHasLeavesAndChildDataNodes(xpath, referenceDataNodeLeaves,
                    referenceDataNodesChildren, comparandDataNode, updatedNodes);
        } else if (!referenceDataNodeLeaves.isEmpty()) {
            getUpdatedNodesWhenReferenceDataNodeHasLeaves(xpath, referenceDataNodeLeaves,
                    comparandDataNode, updatedNodes);
        } else if (!referenceDataNodesChildren.isEmpty()) {
            getUpdatedNodesWhenReferenceDataNodeHasChildDataNodes(xpath, referenceDataNodesChildren, comparandDataNode,
                    updatedNodes);
        } else {
            getUpdatedNodesWhenReferenceDataNodeIsEmpty(xpath, comparandDataNode, updatedNodes);
        }
        return updatedNodes;
    }

    private static List<Map<String, Object>> getAddedNodes(final Map<String, DataNode> xpathToReferenceDataNodes,
                                                    final Map<String, DataNode> xpathToComparandDataNodes) {

        final List<Map<String, Object>> addedNodes = new ArrayList<>();
        final Map<String, DataNode> xpathToAddedNodes = new LinkedHashMap<>(xpathToComparandDataNodes);
        xpathToAddedNodes.keySet().removeAll(xpathToReferenceDataNodes.keySet());
        for (final Map.Entry<String, DataNode> entry: xpathToAddedNodes.entrySet()) {
            final String xpath = entry.getKey();
            final DataNode dataNode = entry.getValue();
            if (!dataNode.getLeaves().isEmpty() && !dataNode.getChildDataNodes().isEmpty()) {
                addedNodes.add(getDeltaReportEntities(ADD_ACTION, xpath, Collections.emptyMap(), dataNode.getLeaves()));
                addedNodes.addAll(getDeltaBetweenDataNodes(Collections.emptyList(), dataNode.getChildDataNodes()));
            } else if (!dataNode.getLeaves().isEmpty()) {
                addedNodes.add(getDeltaReportEntities(ADD_ACTION, xpath, Collections.emptyMap(), dataNode.getLeaves()));
            } else if (!dataNode.getChildDataNodes().isEmpty()) {
                addedNodes.addAll(getDeltaBetweenDataNodes(Collections.emptyList(), dataNode.getChildDataNodes()));
            } else {
                addedNodes.add(
                        getDeltaReportEntities(ADD_ACTION, xpath, Collections.emptyMap(), Collections.emptyMap()));
            }
        }
        return addedNodes;
    }

    private static Map<String, Object> getDeltaReportEntities(final String action, final String xpath,
                                                       final Map<String, Serializable> sourcePayload,
                                                       final Map<String, Serializable> targetPayload) {
        final Map<String, Object> actionEntity = new LinkedHashMap<>();
        actionEntity.put("action", action);
        actionEntity.put("xpath", xpath);
        if (Objects.equals(action, ADD_ACTION)) {
            actionEntity.put("target-payload", targetPayload);
        } else if (Objects.equals(action, REMOVE_ACTION)) {
            actionEntity.put("source-payload", sourcePayload);
        } else {
            if (!sourcePayload.values().stream().allMatch(Objects::isNull)) {
                actionEntity.put("source-payload", sourcePayload);
            }
            if (!targetPayload.values().stream().allMatch(Objects::isNull)) {
                actionEntity.put("target-payload", targetPayload);
            }
        }
        return actionEntity;
    }

    private static Map<String, DataNode> getXpathToDataNode(final Collection<DataNode> dataNodes) {
        final Map<String, DataNode> xpathToDataNode = new LinkedHashMap<>();
        for (final DataNode dataNode : dataNodes) {
            xpathToDataNode.put(dataNode.getXpath(), dataNode);
        }
        return xpathToDataNode;
    }

    private static Map<Map<String, Serializable>, Map<String, Serializable>> getSourceLeavesAndUpdatedLeaves(
            final Map<String, Serializable> leavesOfReferenceDataNode,
            final Map<String, Serializable> leavesOfComparandDataNode) {

        final Map<Map<String, Serializable>, Map<String, Serializable>> sourceToUpdatedLeaves = new LinkedHashMap<>();
        final Map<String, Serializable> uniqueLeavesOfComparandDataNode =
                new LinkedHashMap<>(leavesOfComparandDataNode);
        uniqueLeavesOfComparandDataNode.keySet().removeAll(leavesOfReferenceDataNode.keySet());
        final Map<String, Serializable> sourceLeaves = new LinkedHashMap<>();
        final Map<String, Serializable> updatedLeaves = new LinkedHashMap<>();

        for (final Map.Entry<String, Serializable> entry: leavesOfReferenceDataNode.entrySet()) {
            final String key = entry.getKey();
            final Serializable referenceLeaf = entry.getValue();
            final Serializable comparandLeaf = leavesOfComparandDataNode.get(key);
            if ((referenceLeaf != null && comparandLeaf != null) && (!Objects.equals(referenceLeaf, comparandLeaf))) {
                sourceLeaves.put(key, referenceLeaf);
                updatedLeaves.put(key, comparandLeaf);
            } else if (referenceLeaf != null && comparandLeaf == null) {
                sourceLeaves.put(key, referenceLeaf);
            }
        }

        for (final Map.Entry<String, Serializable> entry: uniqueLeavesOfComparandDataNode.entrySet()) {
            final String key = entry.getKey();
            final Serializable comparandLeaf = entry.getValue();
            final Serializable referenceLeaf = leavesOfReferenceDataNode.get(key);
            if ((referenceLeaf != null && comparandLeaf != null) && (!Objects.equals(comparandLeaf, referenceLeaf))) {
                sourceLeaves.put(key, referenceLeaf);
                updatedLeaves.put(key, comparandLeaf);
            } else if (referenceLeaf == null && comparandLeaf != null) {
                updatedLeaves.put(key, comparandLeaf);
            }
        }
        if (!sourceLeaves.isEmpty() || !updatedLeaves.isEmpty()) {
            sourceToUpdatedLeaves.put(sourceLeaves, updatedLeaves);
        }
        return sourceToUpdatedLeaves;
    }

    private static void getUpdatedNodesWhenReferenceDataNodeHasLeavesAndChildDataNodes(final String xpath,
                                                                final Map<String, Serializable> referenceDataNodeLeaves,
                                                                final Collection<DataNode> referenceDataNodesChildren,
                                                                final DataNode comparandDataNode,
                                                                final List<Map<String, Object>> updatedNodes) {

        final Map<String, Serializable> comparandDataNodeLeaves = comparandDataNode.getLeaves();
        final Collection<DataNode> comparandDataNodesChildren = comparandDataNode.getChildDataNodes();

        if (!comparandDataNodeLeaves.isEmpty() && !comparandDataNodesChildren.isEmpty()) {
            final Map<Map<String, Serializable>, Map<String, Serializable>> sourceLeavesToUpdatedLeaves =
                    getSourceLeavesAndUpdatedLeaves(referenceDataNodeLeaves, comparandDataNodeLeaves);
            addUpdatedLeavesToDeltaReport(xpath, updatedNodes, sourceLeavesToUpdatedLeaves);
            updatedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren, comparandDataNodesChildren));
        } else if (!comparandDataNodeLeaves.isEmpty()) {
            final Map<Map<String, Serializable>, Map<String, Serializable>> sourceLeavesToUpdatedLeaves =
                    getSourceLeavesAndUpdatedLeaves(referenceDataNodeLeaves, comparandDataNodeLeaves);
            addUpdatedLeavesToDeltaReport(xpath, updatedNodes, sourceLeavesToUpdatedLeaves);
            updatedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren, Collections.emptyList()));
        } else if (!comparandDataNodesChildren.isEmpty()) {
            updatedNodes.add(
                    getDeltaReportEntities(UPDATE_ACTION, xpath, referenceDataNodeLeaves, Collections.emptyMap()));
            updatedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren, comparandDataNodesChildren));
        } else {
            updatedNodes.add(
                    getDeltaReportEntities(UPDATE_ACTION, xpath, referenceDataNodeLeaves, Collections.emptyMap()));
            updatedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren, Collections.emptyList()));
        }
    }

    private static void getUpdatedNodesWhenReferenceDataNodeHasLeaves(final String xpath,
                                                  final Map<String, Serializable> referenceDataNodeLeaves,
                                                  final DataNode comparandDataNode,
                                                  final List<Map<String, Object>> updatedNodes) {

        final Map<String, Serializable> comparandDataNodeLeaves = comparandDataNode.getLeaves();
        final Collection<DataNode> comparandDataNodesChildren = comparandDataNode.getChildDataNodes();

        if (!comparandDataNodeLeaves.isEmpty() && !comparandDataNodesChildren.isEmpty()) {
            final Map<Map<String, Serializable>, Map<String, Serializable>> sourceLeavesToUpdatedLeaves =
                    getSourceLeavesAndUpdatedLeaves(referenceDataNodeLeaves, comparandDataNodeLeaves);
            addUpdatedLeavesToDeltaReport(xpath, updatedNodes, sourceLeavesToUpdatedLeaves);
            updatedNodes.addAll(getDeltaBetweenDataNodes(Collections.emptyList(), comparandDataNodesChildren));
        } else if (!comparandDataNodeLeaves.isEmpty()) {
            final Map<Map<String, Serializable>, Map<String, Serializable>> sourceLeavesToUpdatedLeaves =
                    getSourceLeavesAndUpdatedLeaves(referenceDataNodeLeaves, comparandDataNodeLeaves);
            addUpdatedLeavesToDeltaReport(xpath, updatedNodes, sourceLeavesToUpdatedLeaves);
        } else if (!comparandDataNodesChildren.isEmpty()) {
            updatedNodes.add(
                    getDeltaReportEntities(UPDATE_ACTION, xpath, referenceDataNodeLeaves, Collections.emptyMap()));
            updatedNodes.addAll(getDeltaBetweenDataNodes(Collections.emptyList(), comparandDataNodesChildren));
        } else {
            updatedNodes.add(
                    getDeltaReportEntities(UPDATE_ACTION, xpath, referenceDataNodeLeaves, Collections.emptyMap()));
        }
    }

    private static void getUpdatedNodesWhenReferenceDataNodeHasChildDataNodes(final String xpath,
                                                        final Collection<DataNode> referenceDataNodesChildren,
                                                        final DataNode comparandDataNode,
                                                        final List<Map<String, Object>> updatedNodes) {

        final Map<String, Serializable> comparandDataNodeLeaves = comparandDataNode.getLeaves();
        final Collection<DataNode> comparandDataNodesChildren = comparandDataNode.getChildDataNodes();

        if (!comparandDataNodeLeaves.isEmpty() && !comparandDataNodesChildren.isEmpty()) {
            updatedNodes.add(
                    getDeltaReportEntities(UPDATE_ACTION, xpath, Collections.emptyMap(), comparandDataNodeLeaves));
            updatedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren, comparandDataNodesChildren));
        } else if (!comparandDataNode.getLeaves().isEmpty()) {
            updatedNodes.add(
                    getDeltaReportEntities(UPDATE_ACTION, xpath, Collections.emptyMap(), comparandDataNodeLeaves));
            updatedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren, Collections.emptyList()));
        } else if (!comparandDataNodesChildren.isEmpty()) {
            updatedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren, comparandDataNodesChildren));
        } else {
            updatedNodes.addAll(getDeltaBetweenDataNodes(referenceDataNodesChildren, Collections.emptyList()));
        }
    }

    private static void getUpdatedNodesWhenReferenceDataNodeIsEmpty(final String xpath,
                                                                    final DataNode comparandDataNode,
                                                                    final List<Map<String, Object>> updatedNodes) {

        final Map<String, Serializable> comparandDataNodeLeaves = comparandDataNode.getLeaves();
        final Collection<DataNode> comparandDataNodesChildren = comparandDataNode.getChildDataNodes();

        if (!comparandDataNodeLeaves.isEmpty() && !comparandDataNodesChildren.isEmpty()) {
            updatedNodes.add(
                    getDeltaReportEntities(UPDATE_ACTION, xpath, Collections.emptyMap(), comparandDataNodeLeaves));
            updatedNodes.addAll(getDeltaBetweenDataNodes(Collections.emptyList(), comparandDataNodesChildren));
        } else if (!comparandDataNodeLeaves.isEmpty()) {
            updatedNodes.add(
                    getDeltaReportEntities(UPDATE_ACTION, xpath, Collections.emptyMap(), comparandDataNodeLeaves));
        } else if (!comparandDataNodesChildren.isEmpty()) {
            updatedNodes.addAll(getDeltaBetweenDataNodes(Collections.emptyList(), comparandDataNodesChildren));
        }
    }

    private static void addUpdatedLeavesToDeltaReport(String xpath, final List<Map<String, Object>> updatedNodes,
                        Map<Map<String, Serializable>, Map<String, Serializable>> sourceLeavesToUpdatedLeaves) {
        if (!sourceLeavesToUpdatedLeaves.isEmpty()) {
            for (final Map.Entry<Map<String, Serializable>, Map<String, Serializable>> entry:
                    sourceLeavesToUpdatedLeaves.entrySet()) {
                updatedNodes
                        .add(getDeltaReportEntities(UPDATE_ACTION, xpath, entry.getKey(), entry.getValue()));
            }
        }
    }
}
