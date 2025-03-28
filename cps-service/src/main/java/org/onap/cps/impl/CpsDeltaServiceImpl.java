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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.onap.cps.utils.DataMapper;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CpsDeltaServiceImpl implements CpsDeltaService {

    private static final String XPATH_REGEX =
        "@([a-zA-Z_][a-zA-Z0-9_\\-.]*)=(?:\"([^\"]*)\"|'([^']*)'|(.*))\\]$";
        //"@([a-zA-Z_][a-zA-Z0-9_\\-.]*)=(?:\"([^\"]*)\"|'([^']*)'|([^\\]\\s]+))\\]$";

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
                                                           final FetchDescendantsOption fetchDescendantsOption) {

        final Collection<DataNode> sourceDataNodes = cpsDataService.getDataNodesForMultipleXpaths(dataspaceName,
            sourceAnchorName, Collections.singletonList(xpath), fetchDescendantsOption);
        final Collection<DataNode> targetDataNodes = cpsDataService.getDataNodesForMultipleXpaths(dataspaceName,
            targetAnchorName, Collections.singletonList(xpath), fetchDescendantsOption);
        return getDeltaReports(sourceDataNodes, targetDataNodes);
    }

    @Timed(value = "cps.delta.service.get.delta",
        description = "Time taken to get delta between anchor and a payload")
    @Override
    public List<DeltaReport> getDeltaByDataspaceAnchorAndPayload(final String dataspaceName,
                                                                 final String sourceAnchorName,
                                                                 final String xpath,
                                                                 final Map<String, String> yangResourceContentPerName,
                                                                 final String targetData,
                                                                 final FetchDescendantsOption fetchDescendantsOption) {

        final Anchor sourceAnchor = cpsAnchorService.getAnchor(dataspaceName, sourceAnchorName);
        final Collection<DataNode> sourceDataNodes = cpsDataService.getDataNodesForMultipleXpaths(dataspaceName,
            sourceAnchorName, Collections.singletonList(xpath), fetchDescendantsOption);
        final Collection<DataNode> sourceDataNodesRebuilt =
            rebuildSourceDataNodes(xpath, sourceAnchor, sourceDataNodes);
        final Collection<DataNode> targetDataNodes = new ArrayList<>(
            buildTargetDataNodes(sourceAnchor, xpath, yangResourceContentPerName, targetData));
        return getDeltaReports(sourceDataNodesRebuilt, targetDataNodes);
    }

    private List<DeltaReport> getDeltaReports(final Collection<DataNode> sourceDataNodes,
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
        for (final Map.Entry<String, Serializable> entry : uniqueLeavesOfTargetDataNode.entrySet()) {
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

    @Override
    public void applyDelta(final String dataspaceName, final String anchorName, final String xpathForAnchor,
                           final String deltaReportData, final OffsetDateTime offsetDateTime) {

        final List<DeltaReport> deltaReports = jsonObjectMapper.convertToJsonArray(deltaReportData, DeltaReport.class);
        for (final DeltaReport deltaReport: deltaReports) {
            final String action = deltaReport.getAction();
            final String xpath = deltaReport.getXpath();
            if (action.equals(DeltaReport.REPLACE_ACTION)) {
                final String updatedData = jsonObjectMapper.asJsonString(deltaReport.getTargetData());
                updateDataNodesUsingDelta(dataspaceName, anchorName, xpath, updatedData, offsetDateTime);
            } else if (action.equals(DeltaReport.REMOVE_ACTION)) {
                deleteDataNodesUsingDelta(dataspaceName, anchorName, xpath, deltaReport, offsetDateTime);
            } else {
                //call create
                final String addData = jsonObjectMapper.asJsonString(deltaReport.getTargetData());
                cpsDataService.saveListElements(dataspaceName, anchorName, xpath, addData, offsetDateTime, JSON);
            }
        }
    }

    private void updateDataNodesUsingDelta(final String dataspaceName, final String anchorName, final String xpath,
                                           final String updatedData, final OffsetDateTime offsetDateTime) {
        final Map<String, Serializable> keyLeaf = new HashMap<>();
        if (xpath.endsWith("]")) {
            keyLeaf.putAll(getKeyLeaf(xpath));
        }
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final Collection<DataNode> updatedNodes = dataNodeFactory
            .createDataNodesWithAnchorXpathAndNodeData(anchor, xpath, updatedData, JSON);
        for (final DataNode updatedNode : updatedNodes) {
            updatedNode.getLeaves().putAll(keyLeaf);
        }
        final String updatedDataAsJson = jsonObjectMapper.asJsonString(updatedNodes);
        cpsDataService.updateNodeLeavesAndExistingDescendantLeaves(dataspaceName, anchorName, xpath, updatedDataAsJson, offsetDateTime);
    }

    private Map<String, Serializable> getKeyLeaf(String xpath) {
        final Pattern regex = Pattern.compile(XPATH_REGEX);
        final Matcher matchXpath = regex.matcher(xpath);
        final Map<String, Serializable> keyLeaf = new LinkedHashMap<>();
        if (matchXpath.find()) {
            final String key = matchXpath.group(1);
            final Serializable value = matchXpath.group(2) != null ? matchXpath.group(2) :
                matchXpath.group(3) != null ? matchXpath.group(3) : matchXpath.group(4);
            keyLeaf.put(key, value);
        }
        return keyLeaf;
    }

    private void deleteDataNodesUsingDelta (final String dataspaceName, final String anchorName, final String xpathFromDeltaReport,
                                            final DeltaReport deltaReport, final OffsetDateTime offsetDateTime) {
        final Anchor anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final String deleteData = jsonObjectMapper.asJsonString(deltaReport.getSourceData());
        final Collection<DataNode> dataNodesToDelete =
            dataNodeFactory.createDataNodesWithAnchorParentXpathAndNodeData(anchor, xpathFromDeltaReport, deleteData, JSON);
        final Collection<String> xpaths = new ArrayList<>();
        dataNodesToDelete.forEach(dataNodeToDelete -> {
            xpaths.add(dataNodeToDelete.getXpath());
        });
        cpsDataService.deleteDataNodes(dataspaceName, anchorName, xpaths, offsetDateTime);
    }
}
