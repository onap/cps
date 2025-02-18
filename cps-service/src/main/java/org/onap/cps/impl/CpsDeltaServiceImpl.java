/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 TechMahindra Ltd.
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

import static org.onap.cps.utils.ContentType.JSON;

import io.micrometer.core.annotation.Timed;
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
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsDeltaService;
import org.onap.cps.api.DataNodeFactory;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.model.DeltaReport;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.utils.DataMapUtils;
import org.onap.cps.utils.DataMapper;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CpsDeltaServiceImpl implements CpsDeltaService {

    private final CpsAnchorService cpsAnchorService;
    private final CpsDataService cpsDataService;
    private final DataNodeFactory dataNodeFactory;
    private final DataMapper dataMapper;
    private final JsonObjectMapper jsonObjectMapper;

    @Override
    @Timed(value = "cps.delta.service.get.delta",
        description = "Time taken to get delta between anchors")
    public List<DeltaReport> getDeltaByDataspaceAndAnchors(final String dataspaceName,
                                                           final String sourceAnchorName,
                                                           final String targetAnchorName,
                                                           final String xpath,
                                                           final FetchDescendantsOption fetchDescendantsOption,
                                                           final boolean groupDataNodes) {

        final Collection<DataNode> sourceDataNodes = cpsDataService.getDataNodesForMultipleXpaths(dataspaceName,
            sourceAnchorName, Collections.singletonList(xpath), fetchDescendantsOption);
        final Collection<DataNode> targetDataNodes = cpsDataService.getDataNodesForMultipleXpaths(dataspaceName,
            targetAnchorName, Collections.singletonList(xpath), fetchDescendantsOption);
        return getDeltaReports(sourceDataNodes, targetDataNodes, groupDataNodes);
    }

    @Timed(value = "cps.delta.service.get.delta",
        description = "Time taken to get delta between anchor and a payload")
    @Override
    public List<DeltaReport> getDeltaByDataspaceAnchorAndPayload(final String dataspaceName,
                                                                 final String sourceAnchorName,
                                                                 final String xpath,
                                                                 final Map<String, String> yangResourceContentPerName,
                                                                 final String targetData,
                                                                 final FetchDescendantsOption fetchDescendantsOption,
                                                                 final boolean groupDataNodes) {

        final Anchor sourceAnchor = cpsAnchorService.getAnchor(dataspaceName, sourceAnchorName);
        final Collection<DataNode> sourceDataNodes = cpsDataService.getDataNodesForMultipleXpaths(dataspaceName,
            sourceAnchorName, Collections.singletonList(xpath), fetchDescendantsOption);
        final Collection<DataNode> sourceDataNodesRebuilt =
            rebuildSourceDataNodes(xpath, sourceAnchor, sourceDataNodes);
        final Collection<DataNode> targetDataNodes = new ArrayList<>(
            buildTargetDataNodes(sourceAnchor, xpath, yangResourceContentPerName, targetData));
        return getDeltaReports(sourceDataNodesRebuilt, targetDataNodes, groupDataNodes);
    }

    private static List<DeltaReport> getDeltaReports(final Collection<DataNode> sourceDataNodes,
                                              final Collection<DataNode> targetDataNodes,
                                              final boolean groupDataNodes) {

        final List<DeltaReport> deltaReport = new ArrayList<>();
        if (groupDataNodes) {
            deltaReport.addAll(getCondensedDeltaReports(sourceDataNodes, targetDataNodes));
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

    private static List<DeltaReport> getRemovedAndUpdatedDeltaReports(
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

    private static List<DeltaReport> getRemovedDeltaReports(final String xpath, final DataNode sourceDataNode) {
        final List<DeltaReport> removedDeltaReportEntries = new ArrayList<>();
        final Map<String, Serializable> sourceDataNodeLeaves = sourceDataNode.getLeaves();
        final DeltaReport removedDeltaReportEntry = new DeltaReportBuilder().actionRemove().withXpath(xpath)
                .withSourceData(sourceDataNodeLeaves).build();
        removedDeltaReportEntries.add(removedDeltaReportEntry);
        return removedDeltaReportEntries;
    }

    private static List<DeltaReport> getUpdatedDeltaReports(final String xpath, final DataNode sourceDataNode,
                                                            final DataNode targetDataNode) {
        final List<DeltaReport> updatedDeltaReportEntries = new ArrayList<>();
        final Map<Map<String, Serializable>, Map<String, Serializable>> updatedSourceDataToTargetData =
                getUpdatedSourceAndTargetDataNode(sourceDataNode, targetDataNode);
        addUpdatedDataToDeltaReport(xpath, updatedSourceDataToTargetData, updatedDeltaReportEntries);
        return updatedDeltaReportEntries;
    }

    private static Map<Map<String, Serializable>, Map<String, Serializable>> getUpdatedSourceAndTargetDataNode(
                                                            final DataNode sourceDataNode,
                                                            final DataNode targetDataNode) {
        final Map<String, Serializable> updatedLeavesInSourceData = new HashMap<>();
        final Map<String, Serializable> updatedLeavesInTargetData = new HashMap<>();
        processSourceAndTargetDataNode(sourceDataNode, targetDataNode,
            updatedLeavesInSourceData, updatedLeavesInTargetData);
        processUniqueDataInTargetDataNode(sourceDataNode, targetDataNode, updatedLeavesInTargetData);
        return getUpdatedNodeData(sourceDataNode, targetDataNode, updatedLeavesInSourceData, updatedLeavesInTargetData);
    }

    private static void processSourceAndTargetDataNode(
                                                            final DataNode sourceDataNode,
                                                            final DataNode targetDataNode,
                                                            final Map<String, Serializable> sourceDataInDeltaReport,
                                                            final Map<String, Serializable> targetDataInDeltaReport) {
        final Map<String, Serializable> leavesOfSourceDataNode = sourceDataNode.getLeaves();
        final Map<String, Serializable> leavesOfTargetDataNode = targetDataNode.getLeaves();
        for (final Map.Entry<String, Serializable> entry: leavesOfSourceDataNode.entrySet()) {
            final String key = entry.getKey();
            final Serializable sourceLeaf = entry.getValue();
            final Serializable targetLeaf = leavesOfTargetDataNode.get(key);
            compareLeaves(key, sourceLeaf, targetLeaf, sourceDataInDeltaReport, targetDataInDeltaReport);
        }
    }

    private static void processUniqueDataInTargetDataNode(
                                                            final DataNode sourceDataNode,
                                                            final DataNode targetDataNode,
                                                            final Map<String, Serializable> targetDataInDeltaReport) {
        final Map<String, Serializable> uniqueLeavesOfTargetDataNode =
                new HashMap<>(targetDataNode.getLeaves());
        uniqueLeavesOfTargetDataNode.keySet().removeAll(sourceDataNode.getLeaves().keySet());
        targetDataInDeltaReport.putAll(uniqueLeavesOfTargetDataNode);
    }

    private static void compareLeaves(final String key,
                                      final Serializable sourceLeaf,
                                      final Serializable targetLeaf,
                                      final Map<String, Serializable> sourceDataInDeltaReport,
                                      final Map<String, Serializable> targetDataInDeltaReport) {
        if (targetLeaf != null) {
            if (!Objects.equals(sourceLeaf, targetLeaf)) {
                sourceDataInDeltaReport.put(key, sourceLeaf);
                targetDataInDeltaReport.put(key, targetLeaf);
            }
        } else {
            sourceDataInDeltaReport.put(key, sourceLeaf);
        }
    }

    private static Map<Map<String, Serializable>, Map<String, Serializable>> getUpdatedNodeData(
        final DataNode sourceDataNode,
        final DataNode targetDataNode,
        final Map<String, Serializable> updatedLeavesOfSourceData,
        final Map<String, Serializable> updatedLeavesOfTargetData) {
        final Map<Map<String, Serializable>, Map<String, Serializable>> updatedSourceDataToTargetData = new HashMap<>();

        if (!updatedLeavesOfSourceData.isEmpty() || !updatedLeavesOfTargetData.isEmpty()) {
            final String sourceDataNodeXpath = sourceDataNode.getXpath();
            if (CpsPathUtil.isPathToListElement(sourceDataNodeXpath)) {
                addKeyLeavesToUpdatedData(sourceDataNodeXpath, updatedLeavesOfSourceData, updatedLeavesOfTargetData);
            }
            final Collection<DataNode> updatedSourceDataNode =
                buildUpdatedDataNode(sourceDataNode, updatedLeavesOfSourceData);
            final Collection<DataNode> updatedTargetDataNode =
                buildUpdatedDataNode(targetDataNode, updatedLeavesOfTargetData);
            final Map<String, Serializable> updatedSourceData = getCondensedDataForDeltaReport(updatedSourceDataNode);
            final Map<String, Serializable> updatedTargetData = getCondensedDataForDeltaReport(updatedTargetDataNode);
            updatedSourceDataToTargetData.put(updatedSourceData, updatedTargetData);
        }
        return updatedSourceDataToTargetData;
    }

    private static void addKeyLeavesToUpdatedData(final String xpath,
                                                  final Map<String, Serializable> sourceDataInDeltaReport,
                                                  final Map<String, Serializable> targetDataInDeltaReport) {

        final Map<String, Serializable> keyLeaves = new HashMap<>();
        final List<CpsPathQuery.LeafCondition> leafConditions = CpsPathUtil.getCpsPathQuery(xpath).getLeafConditions();
        for (final CpsPathQuery.LeafCondition leafCondition: leafConditions) {
            final String leafName = leafCondition.name();
            final Serializable leafValue = (Serializable) leafCondition.value();
            keyLeaves.put(leafName, leafValue);
        }
        if (!sourceDataInDeltaReport.isEmpty() && !targetDataInDeltaReport.isEmpty()) {
            sourceDataInDeltaReport.putAll(keyLeaves);
            targetDataInDeltaReport.putAll(keyLeaves);
        } else if (sourceDataInDeltaReport.isEmpty()) {
            targetDataInDeltaReport.putAll(keyLeaves);
        } else {
            sourceDataInDeltaReport.putAll(keyLeaves);
        }
    }

    private static Collection<DataNode> buildUpdatedDataNode(final DataNode dataNode,
                                                 final Map<String, Serializable> updatedLeaves) {
        if (updatedLeaves.isEmpty()) {
            return Collections.emptyList();
        }
        final DataNode updatedDataNode = new DataNodeBuilder()
            .withXpath(dataNode.getXpath())
            .withModuleNamePrefix(dataNode.getModuleNamePrefix())
            .withLeaves(updatedLeaves)
            .build();
        return Collections.singletonList(updatedDataNode);
    }

    private static void addUpdatedDataToDeltaReport(final String xpath,
                        final Map<Map<String, Serializable>, Map<String, Serializable>> updatedSourceDataToTargetData,
                        final List<DeltaReport> updatedDeltaReportEntries) {
        for (final Map.Entry<Map<String, Serializable>, Map<String, Serializable>> entry:
            updatedSourceDataToTargetData.entrySet()) {
            final DeltaReport updatedDataForDeltaReport = new DeltaReportBuilder().actionReplace().withXpath(xpath)
                .withSourceData(entry.getKey()).withTargetData(entry.getValue()).build();
            updatedDeltaReportEntries.add(updatedDataForDeltaReport);
        }
    }

    private static List<DeltaReport> getAddedDeltaReports(final Map<String, DataNode> xpathToSourceDataNodes,
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

    private Collection<DataNode> rebuildSourceDataNodes(final String xpath,
                                                        final Anchor sourceAnchor,
                                                        final Collection<DataNode> sourceDataNodes) {
        final Collection<DataNode> sourceDataNodesRebuilt = new ArrayList<>();
        if (sourceDataNodes != null) {
            final Map<String, Object> sourceDataNodesAsMap = dataMapper.toFlatDataMap(sourceAnchor, sourceDataNodes);
            final String sourceDataNodesAsJson = jsonObjectMapper.asJsonString(sourceDataNodesAsMap);
            final Collection<DataNode> dataNodes = dataNodeFactory
                .createDataNodesWithAnchorXpathAndNodeData(sourceAnchor, xpath, sourceDataNodesAsJson, JSON);
            sourceDataNodesRebuilt.addAll(dataNodes);
        }
        return sourceDataNodesRebuilt;
    }

    private Collection<DataNode> buildTargetDataNodes(final Anchor sourceAnchor, final String xpath,
                                                      final Map<String, String> yangResourceContentPerName,
                                                      final String targetData) {
        if (yangResourceContentPerName.isEmpty()) {
            return dataNodeFactory
                .createDataNodesWithAnchorXpathAndNodeData(sourceAnchor, xpath, targetData, JSON);
        } else {
            return dataNodeFactory
                .createDataNodesWithYangResourceXpathAndNodeData(yangResourceContentPerName, xpath, targetData, JSON);
        }
    }

    private static List<DeltaReport> getCondensedDeltaReports(final Collection<DataNode> sourceDataNodes,
                                                              final Collection<DataNode> targetDataNodes) {

        final List<DeltaReport> deltaReportEntries = new ArrayList<>();
        final Map<String, DataNode> xpathToTargetDataNodes = flattenToXpathToFirstLevelDataNodeMap(targetDataNodes);

        deltaReportEntries.addAll(getCondensedRemovedDeltaReports(sourceDataNodes, xpathToTargetDataNodes));
        deltaReportEntries.addAll(getCondensedUpdatedDeltaReports(sourceDataNodes, xpathToTargetDataNodes));
        deltaReportEntries.addAll(getCondensedAddedDeltaReports(sourceDataNodes, targetDataNodes));
        return deltaReportEntries;
    }

    private static List<DeltaReport> getCondensedRemovedDeltaReports(final Collection<DataNode> sourceDataNodes,
                                                                   final Map<String, DataNode> xpathToTargetDataNodes) {

        final List<DeltaReport> removedDeltaReportEntries = new ArrayList<>();
        final Collection<DataNode> removedDataNodes =
            getDataNodesForDeltaReport(sourceDataNodes, xpathToTargetDataNodes);
        if (!removedDataNodes.isEmpty()) {
            final String xpath = getXpathForDeltaReport(removedDataNodes);
            removedDeltaReportEntries.add(new DeltaReportBuilder().actionRemove().withXpath(xpath)
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
        }
    }

    private static List<DeltaReport> getCondensedAddedDeltaReports(final Collection<DataNode> sourceDataNodes,
            final Collection<DataNode> targetDataNodes) {

        final List<DeltaReport> addedDeltaReportEntries = new ArrayList<>();
        final Collection<DataNode> addedDataNodes =
                getDataNodesForDeltaReport(targetDataNodes, flattenToXpathToFirstLevelDataNodeMap(sourceDataNodes));
        if (!addedDataNodes.isEmpty()) {
            final String xpath = getXpathForDeltaReport(addedDataNodes);
            addedDeltaReportEntries.add(new DeltaReportBuilder().actionCreate().withXpath(xpath)
                .withTargetData(getCondensedDataForDeltaReport(addedDataNodes)).build());
        }
        return addedDeltaReportEntries;
    }

    private static String getXpathForDeltaReport(final Collection<DataNode> dataNodes) {
        final String firstNodeXpath = dataNodes.iterator().next().getXpath();
        final String parentNodeXpath = CpsPathUtil.getNormalizedParentXpath(firstNodeXpath);
        return parentNodeXpath.isEmpty() ? firstNodeXpath : parentNodeXpath;
    }

    private static Collection<DataNode> getDataNodesForDeltaReport(final Collection<DataNode> dataNodes,
                                                                   final Map<String, DataNode> xpathToDataNodes) {
        return dataNodes.stream().filter(dataNode -> !xpathToDataNodes.containsKey(dataNode.getXpath())).toList();
    }

    private static Map<String, Serializable> getCondensedDataForDeltaReport(final Collection<DataNode> dataNodes) {
        final DataNode containerNode = new DataNodeBuilder().withChildDataNodes(dataNodes).build();
        final Map<String, Object> condensedData = DataMapUtils.toDataMap(containerNode);
        return condensedData.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
            entry -> (Serializable) entry.getValue()));
    }

    private static Map<String, DataNode> flattenToXpathToFirstLevelDataNodeMap(final Collection<DataNode> dataNodes) {
        return dataNodes.stream().collect(Collectors.toMap(DataNode::getXpath, dataNode -> dataNode));
    }
}
