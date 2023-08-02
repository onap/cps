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

package org.onap.cps.api.impl;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDeltaService;
import org.onap.cps.spi.model.DataNode;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CpsDeltaServiceImpl implements CpsDeltaService {
    private static final String ADD_ACTION = "add";
    private static final String REMOVE_ACTION = "remove";
    private static final String UPDATE_ACTION = "update";

    private static final String ACTION = "action";
    private static final String XPATH = "xpath";

    /**
     * Calculates delta/difference between DataNodes and returns the result as a Collection of Maps. Each map contains
     * information such as action, xpath, source-payload and target-payload. It performs the same operation recursively
     * for every child data node
     *
     * @param referenceDataNodes data nodes used as reference/source for generating delta
     * @param comparandDataNodes data nodes used as comparand value, and are compared against reference data nodes
     * @return Collection of Maps as the delta report between each data node
     */
    public List<Map<String, Object>> getDeltaBetweenDataNodes(final Collection<DataNode> referenceDataNodes,
                                                                     final Collection<DataNode> comparandDataNodes) {

        final List<Map<String, Object>> deltaReport = new ArrayList<>();

        final Map<String, DataNode> xpathToReferenceDataNodes = getXpathToDataNode(referenceDataNodes);
        final Map<String, DataNode> xpathToComparandDataNodes = getXpathToDataNode(comparandDataNodes);

        deltaReport.addAll(getRemovedAndUpdatedNodes(xpathToReferenceDataNodes, xpathToComparandDataNodes));

        deltaReport.addAll(getAddedNodes(xpathToReferenceDataNodes, xpathToComparandDataNodes));

        return Collections.unmodifiableList(deltaReport);
    }

    private static List<Map<String, Object>>  getRemovedAndUpdatedNodes(
                                                                final Map<String, DataNode> xpathToReferenceDataNodes,
                                                                final Map<String, DataNode> xpathToComparandDataNodes) {

        final List<Map<String, Object>> removedAndUpdatedNodes = new ArrayList<>();
        for (final Map.Entry<String, DataNode> entry: xpathToReferenceDataNodes.entrySet()) {
            final String xpath = entry.getKey();
            final DataNode referenceDataNode = entry.getValue();
            final DataNode comparandDataNode = xpathToComparandDataNodes.get(xpath);

            if (comparandDataNode == null) {
                removedAndUpdatedNodes.addAll(getRemovedNodes(xpath, referenceDataNode));
            } else {
                removedAndUpdatedNodes.addAll(getUpdatedNodes(xpath, referenceDataNode, comparandDataNode));
            }
        }
        return removedAndUpdatedNodes;
    }

    private static List<Map<String, Object>> getRemovedNodes(final String xpath, final DataNode referenceDataNode) {

        final List<Map<String, Object>> removedNodes = new ArrayList<>();

        final Map<String, Serializable> referenceDataNodeLeaves = referenceDataNode.getLeaves();

        if (!referenceDataNodeLeaves.isEmpty()) {
            removedNodes
                    .add(getDeltaReportEntities(REMOVE_ACTION, xpath, referenceDataNodeLeaves, Collections.emptyMap()));
        } else {
            removedNodes
                    .add(getDeltaReportEntities(REMOVE_ACTION, xpath, Collections.emptyMap(), Collections.emptyMap()));
        }
        return removedNodes;
    }

    private static List<Map<String, Object>> getUpdatedNodes(final String xpath, final DataNode referenceDataNode,
                                                                                final DataNode comparandDataNode) {

        final List<Map<String, Object>> updatedNodes = new ArrayList<>();
        final Map<String, Serializable> referenceDataNodeLeaves = referenceDataNode.getLeaves();
        final Map<String, Serializable> comparandDataNodeLeaves = comparandDataNode.getLeaves();

        if (!referenceDataNodeLeaves.isEmpty() && !comparandDataNodeLeaves.isEmpty()) {
            final Map<Map<String, Serializable>, Map<String, Serializable>> sourceLeavesToUpdatedLeaves =
                    getSourceLeavesAndUpdatedLeaves(referenceDataNodeLeaves, comparandDataNodeLeaves);
            addUpdatedLeavesToDeltaReport(xpath, updatedNodes, sourceLeavesToUpdatedLeaves);
        } else if (!referenceDataNodeLeaves.isEmpty()) {
            updatedNodes.add(
                    getDeltaReportEntities(UPDATE_ACTION, xpath, referenceDataNodeLeaves, Collections.emptyMap()));
        } else if (!comparandDataNodeLeaves.isEmpty()) {
            updatedNodes.add(
                    getDeltaReportEntities(UPDATE_ACTION, xpath, Collections.emptyMap(), comparandDataNodeLeaves));
        }
        return updatedNodes;
    }

    private static List<Map<String, Object>> getAddedNodes(final Map<String, DataNode> xpathToReferenceDataNodes,
                                                    final Map<String, DataNode> xpathToComparandDataNodes) {

        final List<Map<String, Object>> addedNodes = new ArrayList<>();
        final Map<String, DataNode> xpathToAddedNodes = new LinkedHashMap<>(xpathToComparandDataNodes);
        final var keySet = xpathToReferenceDataNodes.keySet();
        xpathToAddedNodes.keySet().removeAll(keySet);
        for (final Map.Entry<String, DataNode> entry: xpathToAddedNodes.entrySet()) {
            final String xpath = entry.getKey();
            final DataNode dataNode = entry.getValue();

            if (!dataNode.getLeaves().isEmpty()) {
                addedNodes.add(getDeltaReportEntities(ADD_ACTION, xpath, Collections.emptyMap(), dataNode.getLeaves()));
            } else {
                addedNodes.add(
                        getDeltaReportEntities(ADD_ACTION, xpath, Collections.emptyMap(), Collections.emptyMap()));
            }
        }
        return addedNodes;
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
            addReferenceAndComparandLeafToSourceAndUpdatedLeavesMap(sourceLeaves, updatedLeaves, key,
                    referenceLeaf, comparandLeaf);

        }
        for (final Map.Entry<String, Serializable> entry: uniqueLeavesOfComparandDataNode.entrySet()) {
            final String key = entry.getKey();
            final Serializable comparandLeaf = entry.getValue();
            final Serializable referenceLeaf = leavesOfReferenceDataNode.get(key);
            addReferenceAndComparandLeafToSourceAndUpdatedLeavesMap(sourceLeaves, updatedLeaves, key, referenceLeaf,
                    comparandLeaf);
        }
        if (!sourceLeaves.isEmpty() || !updatedLeaves.isEmpty()) {
            sourceToUpdatedLeaves.put(sourceLeaves, updatedLeaves);
        }
        return sourceToUpdatedLeaves;
    }

    private static void addReferenceAndComparandLeafToSourceAndUpdatedLeavesMap(
            final Map<String, Serializable> sourceLeaves, final Map<String, Serializable> updatedLeaves,
            final String key, final Serializable referenceLeaf, final Serializable comparandLeaf) {
        if (referenceLeaf != null && comparandLeaf != null) {
            if (!Objects.equals(referenceLeaf, comparandLeaf)) {
                sourceLeaves.put(key, referenceLeaf);
                updatedLeaves.put(key, comparandLeaf);
            }
        } else if (referenceLeaf != null) {
            sourceLeaves.put(key, referenceLeaf);
        } else if (comparandLeaf != null) {
            updatedLeaves.put(key, comparandLeaf);
        }
    }

    private static void addUpdatedLeavesToDeltaReport(final String xpath, final List<Map<String, Object>> updatedNodes,
                        final Map<Map<String, Serializable>, Map<String, Serializable>> sourceLeavesToUpdatedLeaves) {
        if (!sourceLeavesToUpdatedLeaves.isEmpty()) {
            for (final Map.Entry<Map<String, Serializable>, Map<String, Serializable>> entry:
                    sourceLeavesToUpdatedLeaves.entrySet()) {
                updatedNodes
                        .add(getDeltaReportEntities(UPDATE_ACTION, xpath, entry.getKey(), entry.getValue()));
            }
        }
    }

    private static Map<String, Object> getDeltaReportEntities(final String action, final String xpath,
                                                              final Map<String, Serializable> sourcePayload,
                                                              final Map<String, Serializable> targetPayload) {
        final Map<String, Object> actionEntity = new LinkedHashMap<>();
        if (action != null) {
            if (Objects.equals(action, ADD_ACTION) && !targetPayload.isEmpty()) {
                actionEntity.put(ACTION, action);
                actionEntity.put(XPATH, xpath);
                actionEntity.put("target-payload", targetPayload);
            } else if (Objects.equals(action, REMOVE_ACTION) && !sourcePayload.isEmpty()) {
                actionEntity.put(ACTION, action);
                actionEntity.put(XPATH, xpath);
                actionEntity.put("source-payload", sourcePayload);
            } else {
                actionEntity.put(ACTION, action);
                actionEntity.put(XPATH, xpath);
                if (!sourcePayload.values().stream().allMatch(Objects::isNull)) {
                    actionEntity.put("source-payload", sourcePayload);
                }
                if (!targetPayload.values().stream().allMatch(Objects::isNull)) {
                    actionEntity.put("target-payload", targetPayload);
                }
            }
        }
        return actionEntity;
    }

    private static Map<String, DataNode> getXpathToDataNode(final Collection<DataNode> dataNodes) {
        final Map<String, DataNode> xpathToDataNode = new LinkedHashMap<>();
        for (final DataNode dataNode : dataNodes) {
            xpathToDataNode.put(dataNode.getXpath(), dataNode);
            final Collection<DataNode> childDataNodes = dataNode.getChildDataNodes();
            if (!childDataNodes.isEmpty()) {
                xpathToDataNode.putAll(getXpathToDataNode(childDataNodes));
            }
        }
        return xpathToDataNode;
    }
}
