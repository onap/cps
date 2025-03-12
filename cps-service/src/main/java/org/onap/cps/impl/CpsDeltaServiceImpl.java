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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsDeltaService;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.model.DeltaReport;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.utils.ContentType;
import org.onap.cps.utils.DataMapUtils;
import org.onap.cps.utils.JsonObjectMapper;
import org.onap.cps.utils.PrefixResolver;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CpsDeltaServiceImpl implements CpsDeltaService {

    private final CpsAnchorService cpsAnchorService;
    private final CpsDataService cpsDataService;
    private final JsonObjectMapper jsonObjectMapper;
    private final PrefixResolver prefixResolver;

    @Override
    @Timed(value = "cps.data.service.get.delta",
        description = "Time taken to get delta between anchors")
    public List<DeltaReport> getDeltaByDataspaceAndAnchors(final String dataspaceName,
                                                           final String sourceAnchorName,
                                                           final String targetAnchorName, final String xpath,
                                                           final FetchDescendantsOption fetchDescendantsOption) {

        final Collection<DataNode> sourceDataNodes = cpsDataService.getDataNodesForMultipleXpaths(dataspaceName,
            sourceAnchorName, Collections.singletonList(xpath), fetchDescendantsOption);
        final Collection<DataNode> targetDataNodes = cpsDataService.getDataNodesForMultipleXpaths(dataspaceName,
            targetAnchorName, Collections.singletonList(xpath), fetchDescendantsOption);

        return getDeltaReports(sourceDataNodes, targetDataNodes);
    }

    @Timed(value = "cps.data.service.get.deltaBetweenAnchorAndPayload",
        description = "Time taken to get delta between anchor and a payload")
    @Override
    public List<DeltaReport> getDeltaByDataspaceAnchorAndPayload(final String dataspaceName,
                                                                 final String sourceAnchorName, final String xpath,
                                                                 final Map<String, String> yangResourceContentPerName,
                                                                 final String targetData,
                                                                 final FetchDescendantsOption fetchDescendantsOption) {

        final Anchor sourceAnchor = cpsAnchorService.getAnchor(dataspaceName, sourceAnchorName);

        final Collection<DataNode> sourceDataNodes = cpsDataService.getDataNodes(dataspaceName,
            sourceAnchorName, xpath, fetchDescendantsOption);

        final Collection<DataNode> sourceDataNodesRebuilt =
            new ArrayList<>(rebuildSourceDataNodes(xpath, sourceAnchor, sourceDataNodes));

        final Collection<DataNode> targetDataNodes =
            new ArrayList<>(buildTargetDataNodes(sourceAnchor, xpath, yangResourceContentPerName, targetData));

        return getDeltaReports(sourceDataNodesRebuilt, targetDataNodes);
    }

    @Override
    public List<DeltaReport> getDeltaReports(final Collection<DataNode> sourceDataNodes,
                                             final Collection<DataNode> targetDataNodes) {

        final List<DeltaReport> deltaReport = new ArrayList<>();

        final Map<String, DataNode> xpathToSourceDataNodes = convertToXPathToDataNodesMap(sourceDataNodes);
        final Map<String, DataNode> xpathToTargetDataNodes = convertToXPathToDataNodesMap(targetDataNodes);

        deltaReport.addAll(getRemovedAndUpdatedDeltaReports(xpathToSourceDataNodes, xpathToTargetDataNodes));

        deltaReport.addAll(getAddedDeltaReports(xpathToSourceDataNodes, xpathToTargetDataNodes));

        return Collections.unmodifiableList(deltaReport);
    }

    private static Map<String, DataNode> convertToXPathToDataNodesMap(
                                                                    final Collection<DataNode> dataNodes) {
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
        final Map<Map<String, Serializable>, Map<String, Serializable>> updatedLeavesAsSourceDataToTargetData =
                getUpdatedLeavesBetweenSourceAndTargetDataNode(sourceDataNode.getLeaves(), targetDataNode.getLeaves());
        addUpdatedLeavesToDeltaReport(xpath, updatedLeavesAsSourceDataToTargetData, updatedDeltaReportEntries);
        return updatedDeltaReportEntries;
    }

    private static Map<Map<String, Serializable>,
            Map<String, Serializable>> getUpdatedLeavesBetweenSourceAndTargetDataNode(
                                                            final Map<String, Serializable> leavesOfSourceDataNode,
                                                            final Map<String, Serializable> leavesOfTargetDataNode) {
        final Map<Map<String, Serializable>, Map<String, Serializable>> updatedLeavesAsSourceDataToTargetData =
                new LinkedHashMap<>();
        final Map<String, Serializable> sourceDataInDeltaReport = new LinkedHashMap<>();
        final Map<String, Serializable> targetDataInDeltaReport = new LinkedHashMap<>();
        processLeavesPresentInSourceAndTargetDataNode(leavesOfSourceDataNode, leavesOfTargetDataNode,
                sourceDataInDeltaReport, targetDataInDeltaReport);
        processLeavesUniqueInTargetDataNode(leavesOfSourceDataNode, leavesOfTargetDataNode,
                sourceDataInDeltaReport, targetDataInDeltaReport);
        final boolean isUpdatedDataInDeltaReport =
                !sourceDataInDeltaReport.isEmpty() || !targetDataInDeltaReport.isEmpty();
        if (isUpdatedDataInDeltaReport) {
            updatedLeavesAsSourceDataToTargetData.put(sourceDataInDeltaReport, targetDataInDeltaReport);
        }
        return updatedLeavesAsSourceDataToTargetData;
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

    private static void addUpdatedLeavesToDeltaReport(final String xpath,
                                                      final Map<Map<String, Serializable>, Map<String,
                                                              Serializable>> updatedLeavesAsSourceDataToTargetData,
                                                      final List<DeltaReport> updatedDeltaReportEntries) {
        for (final Map.Entry<Map<String, Serializable>, Map<String, Serializable>> entry:
                updatedLeavesAsSourceDataToTargetData.entrySet()) {
            final DeltaReport updatedDataForDeltaReport = new DeltaReportBuilder().actionReplace()
                    .withXpath(xpath).withSourceData(entry.getKey()).withTargetData(entry.getValue()).build();
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

    private Collection<DataNode> rebuildSourceDataNodes(final String xpath, final Anchor sourceAnchor,
                                                        final Collection<DataNode> sourceDataNodes) {

        final Collection<DataNode> sourceDataNodesRebuilt = new ArrayList<>();
        if (sourceDataNodes != null) {
            final String sourceDataNodesAsJson = getDataNodesAsJson(sourceAnchor, sourceDataNodes);
//            sourceDataNodesRebuilt.addAll(
//                //TODO:
//                //buildDataNodesWithAnchorAndXpath(sourceAnchor, xpath, sourceDataNodesAsJson, ContentType.JSON)
//            );
        }
        return sourceDataNodesRebuilt;
    }

    private Collection<DataNode> buildTargetDataNodes(final Anchor sourceAnchor, final String xpath,
                                                      final Map<String, String> yangResourceContentPerName,
                                                      final String targetData) {
        if (yangResourceContentPerName.isEmpty()) {
            //TODO
            //return buildDataNodesWithAnchorAndXpath(sourceAnchor, xpath, targetData, ContentType.JSON);
        } else {
            //TODO
            //return buildDataNodesWithYangResourceAndXpath(yangResourceContentPerName, xpath,
                targetData, ContentType.JSON);
        }
        return null;
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
}
