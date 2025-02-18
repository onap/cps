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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDeltaService;
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
                .withSourceData(sourceDataNode.getLeaves()).build();
        removedDeltaReportEntries.add(removedDeltaReportEntry);
        return removedDeltaReportEntries;
    }

    private static List<DeltaReport> getUpdatedDeltaReports(final String xpath, final DataNode sourceDataNode,
                                                            final DataNode targetDataNode) {
        final List<DeltaReport> updatedDeltaReportEntries = new ArrayList<>();
        final Map<DataNode, DataNode> updatedSourceDataNodeToTargetDataNode =
                getUpdatedSourceAndTargetDataNode(xpath, sourceDataNode, targetDataNode);
        addUpdatedDataNodesToDeltaReport(xpath, updatedSourceDataNodeToTargetDataNode, updatedDeltaReportEntries);
        return updatedDeltaReportEntries;
    }

    private static Map<DataNode, DataNode> getUpdatedSourceAndTargetDataNode(final String xpath,
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

    private static void addUpdatedDataNodesToDeltaReport(final String xpath,
                                               final Map<DataNode, DataNode> updatedSourceDataNodeToTargetDataNode,
                                               final List<DeltaReport> updatedDeltaReportEntries) {
        for (final Map.Entry<DataNode, DataNode> entry: updatedSourceDataNodeToTargetDataNode.entrySet()) {
            final DeltaReport updatedDataForDeltaReport =
                    new DeltaReportBuilder().actionReplace().withXpath(xpath).withSourceData(entry.getKey().getLeaves())
                            .withTargetData(entry.getValue().getLeaves()).build();
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
                                .withTargetData(dataNode.getLeaves()).build();
            addedDeltaReportEntries.add(addedDataForDeltaReport);
        }
        return addedDeltaReportEntries;
    }

    private static List<DeltaReport> getCondensedDeltaReports(final Collection<DataNode> sourceDataNodes,
            final Collection<DataNode> targetDataNodes) {

        final List<DeltaReport> deltaReportEntries = new ArrayList<>();
        final Map<String, DataNode> xpathToTargetDataNodes = flattenToXpathToDataNodeMap(targetDataNodes);

        deltaReportEntries.addAll(getCondensedRemovedDeltaReports(sourceDataNodes, xpathToTargetDataNodes));
        deltaReportEntries.addAll(getCondensedUpdatedDeltaReports(sourceDataNodes, xpathToTargetDataNodes));
        return deltaReportEntries;
    }

    private static List<DeltaReport> getCondensedRemovedDeltaReports(final Collection<DataNode> sourceDataNodes,
                                                            final Map<String, DataNode> xpathToTargetDataNodes) {

        final List<DeltaReport> removedDeltaReportEntries = new ArrayList<>();
        final Collection<DataNode> removedDataNodes = dataNodesForDeltaReport(sourceDataNodes, xpathToTargetDataNodes);

        if (!sourceDataNodes.isEmpty() && !removedDataNodes.isEmpty()) {
            final String firstNodeXpath = removedDataNodes.iterator().next().getXpath();
            final String parentNodeXpath = CpsPathUtil.getNormalizedParentXpath(firstNodeXpath);
            final String xpathForDeltaReport = parentNodeXpath.isEmpty() ? firstNodeXpath : parentNodeXpath;
            removedDeltaReportEntries.add(new DeltaReportBuilder().actionRemove().withXpath(xpathForDeltaReport)
                .withSourceData(getCondensedDataForDeltaReport(removedDataNodes)).build());
        }
        return removedDeltaReportEntries;
    }

    private static List<DeltaReport> getCondensedUpdatedDeltaReports(final Collection<DataNode> sourceDataNodes,
                                                            final Map<String, DataNode> xpathToTargetDataNodes) {
        final List<DeltaReport> updatedDeltaReportEntries = new ArrayList<>();
        for (final DataNode sourceDataNode : sourceDataNodes) {
            final String xpath = sourceDataNode.getXpath();
            if (xpathToTargetDataNodes.containsKey(xpath)) {
                final DataNode targetDataNode = xpathToTargetDataNodes.get(xpath);
                updatedDeltaReportEntries.addAll(getUpdatedDeltaReports(xpath, sourceDataNode, targetDataNode));
                getCondensedDeltaReportsForChildDataNodes(sourceDataNode, targetDataNode, updatedDeltaReportEntries);
            }
        }
        return updatedDeltaReportEntries;
    }

    private static void getCondensedDeltaReportsForChildDataNodes(final DataNode sourceDataNode,
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

    private static List<DeltaReport> getCondensedAddedDeltaReports(final Collection<DataNode> sourceDataNodes,
                                                                   final Collection<DataNode> targetDataNodes) {

        final List<DeltaReport> addedDeltaReportEntries = new ArrayList<>();
        final Collection<DataNode> addedDataNodes = new ArrayList<>(
            dataNodesForDeltaReport(targetDataNodes, flattenToXpathToDataNodeMap(sourceDataNodes)));
        if (!addedDataNodes.isEmpty()) {
            final String firstNodeXpath = addedDataNodes.iterator().next().getXpath();
            final String parentNodeXpath = CpsPathUtil.getNormalizedParentXpath(firstNodeXpath);
            final String xpathForDeltaReport = parentNodeXpath.isEmpty() ? firstNodeXpath : parentNodeXpath;
            addedDeltaReportEntries.add(new DeltaReportBuilder().actionCreate().withXpath(xpathForDeltaReport)
                .withTargetData(getCondensedDataForDeltaReport(addedDataNodes)).build());
        }
        return addedDeltaReportEntries;
    }

    private static Collection<DataNode> dataNodesForDeltaReport(final Collection<DataNode> dataNodes,
                                                                final Map<String, DataNode> xpathToDataNodes) {
        final Collection<DataNode> dataNodesInDeltaReport = new ArrayList<>();
        for (final DataNode dataNode : dataNodes) {
            final String xpath = dataNode.getXpath();
            if (!xpathToDataNodes.containsKey(xpath)) {
                dataNodesInDeltaReport.add(dataNode);
            }
        }
        return dataNodesInDeltaReport;
    }

    private static Map<String, Serializable> getCondensedDataForDeltaReport(final Collection<DataNode> dataNodes) {
        final Map<String, Object> condensedData = new HashMap<>();
        final Map<String, List<Map<String, Object>>> groupedLists = new HashMap<>();
        for (final DataNode dataNode : dataNodes) {
            final String prefix = dataNode.getModuleNamePrefix();
            DataMapUtils.listDataNodes(dataNode, prefix, groupedLists);
            condensedData.putAll(groupedLists);
        }
        return condensedData.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> (Serializable) entry.getValue()));
    }

    private static Map<String, DataNode> flattenToXpathToDataNodeMap(final Collection<DataNode> dataNodes) {
        return dataNodes.stream().collect(Collectors.toMap(DataNode::getXpath, dataNode -> dataNode));
    }
}
