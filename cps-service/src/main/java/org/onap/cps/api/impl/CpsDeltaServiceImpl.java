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
import org.onap.cps.spi.model.DeltaReport;
import org.onap.cps.spi.model.DeltaReportBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CpsDeltaServiceImpl implements CpsDeltaService {

    /**
     * Calculates delta/difference between source and target data nodes and returns the result as a List of
     * {@link DeltaReport}. Each Delta Report contains information such as action, xpath, source-payload and
     * target-payload.
     *
     * @param sourceDataNodes data nodes used as reference for generating delta
     * @param targetDataNodes data nodes used as target value, and are compared against source data nodes
     * @return                Collection of Maps as the delta report between each data node
     */
    public List<DeltaReport> getDeltaReport(final Collection<DataNode> sourceDataNodes,
                                            final Collection<DataNode> targetDataNodes) {

        final List<DeltaReport> deltaReport = new ArrayList<>();

        final Map<String, DataNode> xpathToSourceDataNodes = convertToXPathToDataNodesMap(sourceDataNodes);
        final Map<String, DataNode> xpathToTargetDataNodes = convertToXPathToDataNodesMap(targetDataNodes);

        deltaReport.addAll(getRemovedAndUpdatedDeltaReports(xpathToSourceDataNodes, xpathToTargetDataNodes));

        deltaReport.addAll(getAddedDeltaReports(xpathToSourceDataNodes, xpathToTargetDataNodes));

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

            if (targetDataNode == null) {
                removedAndUpdatedDeltaReportEntries.addAll(getRemovedDeltaReports(xpath, sourceDataNode));
            } else {
                removedAndUpdatedDeltaReportEntries
                        .addAll(getUpdatedDeltaReports(xpath, sourceDataNode, targetDataNode));
            }
        }
        return removedAndUpdatedDeltaReportEntries;
    }

    private static List<DeltaReport> getRemovedDeltaReports(final String xpath, final DataNode sourceDataNode) {

        final List<DeltaReport> removedDeltaReportEntries = new ArrayList<>();

        final Map<String, Serializable> sourceDataNodeLeaves = sourceDataNode.getLeaves();
        final DeltaReport removedData = new DeltaReportBuilder().actionRemove().withXpath(xpath)
                                                                    .withSourceData(sourceDataNodeLeaves).build();

        removedDeltaReportEntries.add(removedData);
        return removedDeltaReportEntries;
    }

    private static List<DeltaReport> getUpdatedDeltaReports(final String xpath, final DataNode sourceDataNode,
                                                            final DataNode targetDataNode) {

        final List<DeltaReport> updatedDeltaReportEntries = new ArrayList<>();
        final Map<String, Serializable> sourceDataNodeLeaves = sourceDataNode.getLeaves();
        final Map<String, Serializable> targetDataNodeLeaves = targetDataNode.getLeaves();

        final Map<Map<String, Serializable>, Map<String, Serializable>> updatedLeavesAsSourceDataToTargetData =
                    compareLeavesData(sourceDataNodeLeaves, targetDataNodeLeaves);

        addUpdatedLeavesToDeltaReport(xpath, updatedDeltaReportEntries, updatedLeavesAsSourceDataToTargetData);
        return updatedDeltaReportEntries;
    }

    private static List<DeltaReport> getAddedDeltaReports(final Map<String, DataNode> xpathToSourceDataNodes,
                                                          final Map<String, DataNode> xpathToTargetDataNodes) {

        final List<DeltaReport> addedDeltaReportEntries = new ArrayList<>();
        final Map<String, DataNode> xpathToAddedNodes = new LinkedHashMap<>(xpathToTargetDataNodes);
        xpathToAddedNodes.keySet().removeAll(xpathToSourceDataNodes.keySet());
        for (final Map.Entry<String, DataNode> entry: xpathToAddedNodes.entrySet()) {
            final String xpath = entry.getKey();
            final DataNode dataNode = entry.getValue();
            final DeltaReport addedDataForDeltaReport = new DeltaReportBuilder().actionAdd().withXpath(xpath)
                                .withTargetData(dataNode.getLeaves()).build();
            addedDeltaReportEntries.add(addedDataForDeltaReport);
        }
        return addedDeltaReportEntries;
    }

    private static Map<Map<String, Serializable>, Map<String, Serializable>> compareLeavesData(
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

        if (!sourceDataInDeltaReport.isEmpty() || !targetDataInDeltaReport.isEmpty()) {
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
            compareLeafDataToGetSourceAndTargetDataForDeltaReport(key, sourceLeaf, targetLeaf,
                    sourceDataInDeltaReport, targetDataInDeltaReport);
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
            compareLeafDataToGetSourceAndTargetDataForDeltaReport(key, sourceLeaf, targetLeaf,
                    sourceDataInDeltaReport, targetDataInDeltaReport);
        }
    }

    private static void compareLeafDataToGetSourceAndTargetDataForDeltaReport(final String key,
                                                              final Serializable sourceLeaf,
                                                              final Serializable targetLeaf,
                                                              final Map<String, Serializable> sourceDataInDeltaReport,
                                                              final Map<String, Serializable> targetDataInDeltaReport) {
        if (sourceLeaf != null && targetLeaf != null) {
            if (!Objects.equals(sourceLeaf, targetLeaf)) {
                sourceDataInDeltaReport.put(key, sourceLeaf);
                targetDataInDeltaReport.put(key, targetLeaf);
            }
        } else if (sourceLeaf != null) {
            sourceDataInDeltaReport.put(key, sourceLeaf);
        } else if (targetLeaf != null) {
            targetDataInDeltaReport.put(key, targetLeaf);
        }
    }

    private static void addUpdatedLeavesToDeltaReport(final String xpath,
                                            final List<DeltaReport> updatedDeltaReportEntries,
                                            final Map<Map<String, Serializable>,
                                                    Map<String, Serializable>> updatedLeavesAsSourceDataToTargetData) {
        if (!updatedLeavesAsSourceDataToTargetData.isEmpty()) {
            for (final Map.Entry<Map<String, Serializable>, Map<String, Serializable>> entry:
                    updatedLeavesAsSourceDataToTargetData.entrySet()) {
                final DeltaReport updatedDataForDeltaReport = new DeltaReportBuilder().actionUpdate().withXpath(xpath)
                        .withSourceData(entry.getKey())
                                .withTargetData(entry.getValue()).build();
                updatedDeltaReportEntries.add(updatedDataForDeltaReport);
            }
        }
    }
}
