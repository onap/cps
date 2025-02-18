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

package org.onap.cps.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDeltaService;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.model.DeltaReport;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.utils.DataMapUtils;
import org.onap.cps.utils.PrefixResolver;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CpsDeltaServiceImpl implements CpsDeltaService {

    private final PrefixResolver prefixResolver;
    private final CpsAnchorService cpsAnchorService;

    private static final String ROOT_NODE_XPATH = "/";


    @Override
    public List<DeltaReport> getDeltaReports(final Collection<DataNode> sourceDataNodes,
                                             final Collection<DataNode> targetDataNodes,
                                             final boolean groupingEnabled) {

        final List<DeltaReport> deltaReport = new ArrayList<>();
        if (groupingEnabled) {
            deltaReport.addAll(getCondensedDeltaReports(sourceDataNodes, targetDataNodes));
            deltaReport.addAll(getCondensedAddedDeltaReports(sourceDataNodes, targetDataNodes));
        } else {
            final Map<String, DataNode> xpathToSourceDataNodes = convertToXPathToDataNodesMap(sourceDataNodes);
            final Map<String, DataNode> xpathToTargetDataNodes = convertToXPathToDataNodesMap(targetDataNodes);
            deltaReport.addAll(getRemovedAndUpdatedDeltaReports(xpathToSourceDataNodes, xpathToTargetDataNodes));
            deltaReport.addAll(getAddedDeltaReports(xpathToSourceDataNodes, xpathToTargetDataNodes));
        }
        return Collections.unmodifiableList(deltaReport);
    }

    private static Map<String, DataNode> convertToXPathToDataNodesMap(final Collection<DataNode> dataNodes) {
        final Map<String, DataNode> xpathToDataNode = new LinkedHashMap<>();
        for (final DataNode dataNode : dataNodes) {
            xpathToDataNode.put(dataNode.getXpath(), dataNode);
            final Collection<DataNode> childDataNodes = dataNode.getChildDataNodes();
            if (!childDataNodes.isEmpty()) {
                xpathToDataNode.putAll(convertToXPathToDataNodesMap(childDataNodes));
            }
        }
        return xpathToDataNode;
    }

    private List<DeltaReport> getRemovedAndUpdatedDeltaReports(
                                                                final Map<String, DataNode> xpathToSourceDataNodes,
                                                                final Map<String, DataNode> xpathToTargetDataNodes) {
        final List<DeltaReport> removedAndUpdatedDeltaReportEntries = new ArrayList<>();
        for (final Map.Entry<String, DataNode> entry: xpathToSourceDataNodes.entrySet()) {
            final String xpath = entry.getKey();
            final DataNode sourceDataNode = entry.getValue();
            final DataNode targetDataNode = xpathToTargetDataNodes.get(xpath);
            final List<DeltaReport> deltaReports;
            if (targetDataNode == null) {
                deltaReports = getRemovedDeltaReports(xpath, sourceDataNode);
            } else {
                deltaReports = getUpdatedDeltaReports(xpath, sourceDataNode, targetDataNode);
            }
            removedAndUpdatedDeltaReportEntries.addAll(deltaReports);
        }
        return removedAndUpdatedDeltaReportEntries;
    }

    private List<DeltaReport> getRemovedDeltaReports(final String xpath, final DataNode sourceDataNode) {
        final List<DeltaReport> removedDeltaReportEntries = new ArrayList<>();
        final DeltaReport removedDeltaReportEntry = new DeltaReportBuilder().actionRemove().withXpath(xpath)
                .withSourceData(getPrefixToNodeData(Collections.singletonList(sourceDataNode))).build();
        removedDeltaReportEntries.add(removedDeltaReportEntry);
        return removedDeltaReportEntries;
    }

    private List<DeltaReport> getUpdatedDeltaReports(final String xpath, final DataNode sourceDataNode,
                                                            final DataNode targetDataNode) {
        final List<DeltaReport> updatedDeltaReportEntries = new ArrayList<>();
        final Map<DataNode, DataNode> updatedSourceDataNodeToTargetDataNode =
                getUpdatedSourceAndTargetDataNode(xpath, sourceDataNode, targetDataNode);
        addUpdatedDataNodesToDeltaReport(xpath, updatedSourceDataNodeToTargetDataNode, updatedDeltaReportEntries);
        return updatedDeltaReportEntries;
    }

    private static Map<DataNode, DataNode> getUpdatedSourceAndTargetDataNode( final String xpath,
                                                            final DataNode sourceDataNode,
                                                            final DataNode targetDataNode) {
        final Map<DataNode, DataNode> updatedSourceDataNodeToTargetDataNode =
                new LinkedHashMap<>();
        final Map<String, Serializable> leavesInSourceData = new HashMap<>();
        final Map<String, Serializable> leavesInTargetData = new HashMap<>();
        final DataNode updatedSourceDataNode = sourceDataNode;
        final DataNode updatedTargetDataNode = targetDataNode;
        processLeavesPresentInSourceAndTargetDataNode(sourceDataNode.getLeaves(), targetDataNode.getLeaves(),
                leavesInSourceData, leavesInTargetData);
        processLeavesUniqueInTargetDataNode(sourceDataNode.getLeaves(), targetDataNode.getLeaves(), leavesInSourceData,
                leavesInTargetData);
        final boolean isUpdatedDataInDeltaReport = !leavesInSourceData.isEmpty() || !leavesInTargetData.isEmpty();
        if (isUpdatedDataInDeltaReport) {
            updatedSourceDataNode.setLeaves(leavesInSourceData);
            updatedSourceDataNode.setChildDataNodes(Collections.emptyList());
            updatedTargetDataNode.setLeaves(leavesInTargetData);
            updatedTargetDataNode.setChildDataNodes(Collections.emptyList());
            updatedSourceDataNodeToTargetDataNode.put(updatedSourceDataNode, updatedTargetDataNode);
        }
        return updatedSourceDataNodeToTargetDataNode;
    }

    private static void processLeavesPresentInSourceAndTargetDataNode(
                                                            final Map<String, Serializable> leavesOfSourceDataNode,
                                                            final Map<String, Serializable> leavesOfTargetDataNode,
                                                            final Map<String, Serializable> sourceDataInDeltaReport,
                                                            final Map<String, Serializable> targetDataInDeltaReport) {
        for (final Map.Entry<String, Serializable> entry: leavesOfSourceDataNode.entrySet()) {
            final String key = entry.getKey();
            final Serializable sourceLeaf = entry.getValue();
            final Serializable targetLeaf = leavesOfTargetDataNode.get(key);
            compareLeaves(key, sourceLeaf, targetLeaf, sourceDataInDeltaReport, targetDataInDeltaReport);
        }
    }

    private static void processLeavesUniqueInTargetDataNode(
                                                            final Map<String, Serializable> leavesOfSourceDataNode,
                                                            final Map<String, Serializable> leavesOfTargetDataNode,
                                                            final Map<String, Serializable> sourceDataInDeltaReport,
                                                            final Map<String, Serializable> targetDataInDeltaReport) {
        final Map<String, Serializable> uniqueLeavesOfTargetDataNode =
                new LinkedHashMap<>(leavesOfTargetDataNode);
        uniqueLeavesOfTargetDataNode.keySet().removeAll(leavesOfSourceDataNode.keySet());
        for (final Map.Entry<String, Serializable> entry: uniqueLeavesOfTargetDataNode.entrySet()) {
            final String key = entry.getKey();
            final Serializable targetLeaf = entry.getValue();
            final Serializable sourceLeaf = leavesOfSourceDataNode.get(key);
            compareLeaves(key, sourceLeaf, targetLeaf, sourceDataInDeltaReport, targetDataInDeltaReport);
        }
    }

    private static void compareLeaves(final String key,
                                      final Serializable sourceLeaf,
                                      final Serializable targetLeaf,
                                      final Map<String, Serializable> sourceDataInDeltaReport,
                                      final Map<String, Serializable> targetDataInDeltaReport) {
        if (sourceLeaf != null && targetLeaf != null) {
            if (!Objects.equals(sourceLeaf, targetLeaf)) {
                sourceDataInDeltaReport.put(key, sourceLeaf);
                targetDataInDeltaReport.put(key, targetLeaf);
            }
        } else if (sourceLeaf == null) {
            targetDataInDeltaReport.put(key, targetLeaf);

        } else {
            sourceDataInDeltaReport.put(key, sourceLeaf);
        }
    }

    private void addUpdatedDataNodesToDeltaReport(final String xpath,
                                               final Map<DataNode, DataNode> updatedSourceDataNodeToTargetDataNode,
                                               final List<DeltaReport> updatedDeltaReportEntries) {
        for (final Map.Entry<DataNode, DataNode> entry: updatedSourceDataNodeToTargetDataNode.entrySet()) {
            final DataNode updatedSourceDataNode = entry.getKey();
            final DataNode updatedTargetDataNode = entry.getValue();
            final Map<String, List<Map<String, Object>>> prefixToUpdatedSourceData = new HashMap<>();
            final Map<String, List<Map<String, Object>>> prefixToUpdatedTargetData = new HashMap<>();
            if (!updatedSourceDataNode.getLeaves().isEmpty()) {
                prefixToUpdatedSourceData.putAll(getPrefixToNodeData(Collections.singletonList(
                        updatedSourceDataNode)));
            }
            if (!updatedTargetDataNode.getLeaves().isEmpty()) {
                prefixToUpdatedTargetData.putAll(getPrefixToNodeData(Collections.singletonList(
                        updatedTargetDataNode)));
            }
            final DeltaReport updatedDataForDeltaReport =
                    new DeltaReportBuilder().actionReplace().withXpath(xpath).withSourceData(prefixToUpdatedSourceData)
                            .withTargetData(prefixToUpdatedTargetData).build();
            updatedDeltaReportEntries.add(updatedDataForDeltaReport);
        }
    }

    private List<DeltaReport> getAddedDeltaReports(final Map<String, DataNode> xpathToSourceDataNodes,
                                                          final Map<String, DataNode> xpathToTargetDataNodes) {

        final List<DeltaReport> addedDeltaReportEntries = new ArrayList<>();
        final Map<String, DataNode> xpathToAddedNodes = new LinkedHashMap<>(xpathToTargetDataNodes);
        xpathToAddedNodes.keySet().removeAll(xpathToSourceDataNodes.keySet());
        for (final Map.Entry<String, DataNode> entry: xpathToAddedNodes.entrySet()) {
            final String xpath = entry.getKey();
            final DataNode dataNode = entry.getValue();
            final DeltaReport addedDataForDeltaReport = new DeltaReportBuilder().actionCreate().withXpath(xpath)
                                .withTargetData(getPrefixToNodeData(Collections.singletonList(dataNode))).build();
            addedDeltaReportEntries.add(addedDataForDeltaReport);
        }
        return addedDeltaReportEntries;
    }

    private List<DeltaReport> getCondensedDeltaReports(final Collection<DataNode> sourceDataNodes,
            final Collection<DataNode> targetDataNodes) {

        final List<DeltaReport> deltaReportEntries = new ArrayList<>();
        final Map<String, DataNode> xpathToTargetDataNodes = convertToXPathToFirstLevelDataNodesMap(targetDataNodes);
        String xpath = "";
        final List<DataNode> removedDataNodes = new ArrayList<>();
        for (final DataNode sourceDataNode : sourceDataNodes) {
            xpath = sourceDataNode.getXpath();
            final DataNode targetDataNode = xpathToTargetDataNodes.get(xpath);
            if (targetDataNode == null) {
                removedDataNodes.add(sourceDataNode);
            } else {
                deltaReportEntries.addAll(getUpdatedDeltaReports(xpath, sourceDataNode, targetDataNode));
                getCondensedDeltaReportsForChildDataNodes(sourceDataNode, targetDataNode, deltaReportEntries);
            }
        }
        if (!sourceDataNodes.isEmpty() && !removedDataNodes.isEmpty()) {
            final Map<String, List<Map<String, Object>>> sourceDataInDeltaReport =
                    getPrefixToNodeData(removedDataNodes);
            final String parentNodeXpath = CpsPathUtil.getNormalizedParentXpath(xpath);
            final String xpathForDeltaReport = parentNodeXpath.isEmpty() ? ROOT_NODE_XPATH : parentNodeXpath;
            final DeltaReport removedDeltaReportEntries = new DeltaReportBuilder().actionRemove()
                                                                  .withXpath(xpathForDeltaReport)
                                                                  .withSourceData(sourceDataInDeltaReport).build();
            deltaReportEntries.add(removedDeltaReportEntries);
        }
        return deltaReportEntries;
    }

    private void getCondensedDeltaReportsForChildDataNodes(final DataNode sourceDataNode,
            final DataNode targetDataNode,
            final List<DeltaReport> deltaReportEntries) {
        final Collection<DataNode> childrenOfSourceDataNodes = sourceDataNode.getChildDataNodes();
        final Collection<DataNode> childrenOfTargetDataNodes = targetDataNode.getChildDataNodes();
        if (!childrenOfSourceDataNodes.isEmpty() || !childrenOfTargetDataNodes.isEmpty()) {
            deltaReportEntries.addAll(getCondensedDeltaReports(childrenOfSourceDataNodes, childrenOfTargetDataNodes));
            deltaReportEntries.addAll(
                    getCondensedAddedDeltaReports(childrenOfSourceDataNodes, childrenOfTargetDataNodes));
        }
    }

    private List<DeltaReport> getCondensedAddedDeltaReports(final Collection<DataNode> sourceDataNodes,
            final Collection<DataNode> targetDataNodes) {

        final List<DeltaReport> addedDeltaReportEntries = new ArrayList<>();
        final Map<String, DataNode> xpathToSourceDataNodes = convertToXPathToFirstLevelDataNodesMap(sourceDataNodes);
        String xpath = "";
        final List<DataNode> addedNodes = new ArrayList<>();
        if (!targetDataNodes.isEmpty()) {
            for (final DataNode targetDataNode : targetDataNodes) {
                xpath = targetDataNode.getXpath();
                final DataNode sourceDataNode = xpathToSourceDataNodes.get(xpath);
                if (sourceDataNode == null) {
                    addedNodes.add(targetDataNode);
                }
            }
        }
        if (!addedNodes.isEmpty()) {
            final Map<String, List<Map<String, Object>>> targetDataInDeltaReport = getPrefixToNodeData(addedNodes);
            final String parentNodeXpath = CpsPathUtil.getNormalizedParentXpath(xpath);
            final String xpathForDeltaReport = parentNodeXpath.isEmpty() ? ROOT_NODE_XPATH : parentNodeXpath;
            addedDeltaReportEntries.add(new DeltaReportBuilder().actionCreate().withXpath(xpathForDeltaReport)
                        .withTargetData(targetDataInDeltaReport).build());
        }
        return addedDeltaReportEntries;
    }

    private Map<String, List<Map<String, Object>>> getPrefixToNodeData(final List<DataNode> dataNodes) {
        final Map<String, List<Map<String, Object>>> prefixToNodeData = new HashMap<>();
        for (final DataNode dataNode: dataNodes) {
            final Anchor anchor = cpsAnchorService.getAnchor(dataNode.getDataspace(), dataNode.getAnchorName());
            final String prefix = prefixResolver.getPrefix(anchor, dataNode.getXpath());
            DataMapUtils.listDataNodes(dataNode, prefix, prefixToNodeData);
        }
        return prefixToNodeData;
    }

    private Map<String, DataNode> convertToXPathToFirstLevelDataNodesMap(final Collection<DataNode> dataNodes) {
        final Map<String, DataNode> xpathToDataNode = new LinkedHashMap<>();
        for (final DataNode dataNode : dataNodes) {
            xpathToDataNode.put(dataNode.getXpath(), dataNode);
        }
        return xpathToDataNode;
    }
}
