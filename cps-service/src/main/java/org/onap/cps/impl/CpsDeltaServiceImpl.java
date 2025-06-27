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
import java.util.List;
import java.util.Map;
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
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.utils.DataMapUtils;
import org.onap.cps.utils.DataMapper;
import org.onap.cps.utils.JsonObjectMapper;
import org.onap.cps.utils.deltareport.DeltaReportGenerator;
import org.onap.cps.utils.deltareport.DeltaReportHelper;
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
    private final DeltaReportHelper deltaReportHelper;
    private final DeltaReportGenerator deltaReportGenerator;

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

    private List<DeltaReport> getDeltaReports(final Collection<DataNode> sourceDataNodes,
                                              final Collection<DataNode> targetDataNodes,
                                              final boolean groupDataNodes) {

        final List<DeltaReport> deltaReport = new ArrayList<>();
        if (groupDataNodes) {
            deltaReport.addAll(getCondensedDeltaReports(sourceDataNodes, targetDataNodes));
        } else {
            deltaReport.addAll(deltaReportGenerator.getDeltaReports(sourceDataNodes, targetDataNodes));
        }
        return Collections.unmodifiableList(deltaReport);
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

    private List<DeltaReport> getCondensedDeltaReports(final Collection<DataNode> sourceDataNodes,
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

        final List<DeltaReport> deltaReportEntriesForRemove = new ArrayList<>();
        final Collection<DataNode> removedDataNodes =
            getDataNodesForDeltaReport(sourceDataNodes, xpathToTargetDataNodes);
        if (!removedDataNodes.isEmpty()) {
            final String xpath = getXpathForDeltaReport(removedDataNodes);
            deltaReportEntriesForRemove.add(new DeltaReportBuilder().actionRemove().withXpath(xpath)
                .withSourceData(getCondensedDataForDeltaReport(removedDataNodes)).build());
        }
        return deltaReportEntriesForRemove;
    }

    private List<DeltaReport> getCondensedUpdatedDeltaReports(final Collection<DataNode> sourceDataNodes,
                                                                   final Map<String, DataNode> xpathToTargetDataNodes) {
        final List<DeltaReport> deltaReportEntriesForUpdates = new ArrayList<>();
        for (final DataNode sourceDataNode : sourceDataNodes) {
            final String xpath = sourceDataNode.getXpath();
            if (xpathToTargetDataNodes.containsKey(xpath)) {
                final DataNode targetDataNode = xpathToTargetDataNodes.get(xpath);
                final List<DeltaReport> updatedDataForDeltaReport =
                        deltaReportHelper.getDeltaReportsForUpdates(xpath, sourceDataNode, targetDataNode);
                deltaReportEntriesForUpdates.addAll(updatedDataForDeltaReport);
                getCondensedDeltaReportsForChildDataNodes(sourceDataNode, targetDataNode, deltaReportEntriesForUpdates);
            }
        }
        return deltaReportEntriesForUpdates;
    }

    private void getCondensedDeltaReportsForChildDataNodes(final DataNode sourceDataNode,
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
