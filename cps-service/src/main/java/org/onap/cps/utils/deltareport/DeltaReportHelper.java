/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Deutsche Telekom AG
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

package org.onap.cps.utils.deltareport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.model.DeltaReport;
import org.onap.cps.cpspath.parser.CpsPathQuery;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.impl.DataNodeBuilder;
import org.onap.cps.impl.DeltaReportBuilder;
import org.onap.cps.utils.DataMapUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DeltaReportHelper {

    /**
     * Get delta report entries for updates.
     *
     * @param xpath          the xpath of the data node
     * @param sourceDataNode the source data node
     * @param targetDataNode the target data node
     * @return               a list of delta report entries for updates
     */
    public List<DeltaReport> createDeltaReportsForUpdates(final String xpath, final DataNode sourceDataNode,
                                                          final DataNode targetDataNode) {
        final List<DeltaReport> deltaReportEntriesForUpdates = new ArrayList<>();
        final Map<Map<String, Serializable>, Map<String, Serializable>> updatedSourceDataToTargetData =
            getUpdatedSourceAndTargetDataNode(sourceDataNode, targetDataNode);
        if (!updatedSourceDataToTargetData.isEmpty()) {
            addUpdatedDataToDeltaReport(xpath, updatedSourceDataToTargetData, deltaReportEntriesForUpdates);
        }
        return deltaReportEntriesForUpdates;
    }

    private static Map<Map<String, Serializable>, Map<String, Serializable>> getUpdatedSourceAndTargetDataNode(
        final DataNode sourceDataNode,
        final DataNode targetDataNode) {
        final Map<String, Serializable> updatedLeavesInSourceData = new HashMap<>();
        final Map<String, Serializable> updatedLeavesInTargetData = new HashMap<>();
        processSourceAndTargetDataNode(sourceDataNode, targetDataNode,
            updatedLeavesInSourceData, updatedLeavesInTargetData);
        processUniqueDataInTargetDataNode(sourceDataNode, targetDataNode, updatedLeavesInTargetData);
        final Map<String, Serializable> updatedSourceData =
            getUpdatedNodeData(sourceDataNode, updatedLeavesInSourceData);
        final Map<String, Serializable> updatedTargetData =
            getUpdatedNodeData(targetDataNode, updatedLeavesInTargetData);
        if (updatedSourceData.isEmpty() && updatedTargetData.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.singletonMap(updatedSourceData, updatedTargetData);
    }

    private static void addUpdatedDataToDeltaReport(final String xpath,
                                                    final Map<Map<String, Serializable>,
                                                        Map<String, Serializable>> updatedSourceDataToTargetData,
                                                    final List<DeltaReport> deltaReportEntriesForUpdates) {
        for (final Map.Entry<Map<String, Serializable>, Map<String, Serializable>> entry:
            updatedSourceDataToTargetData.entrySet()) {
            final DeltaReport updatedDataForDeltaReport = new DeltaReportBuilder().actionReplace().withXpath(xpath)
                .withSourceData(entry.getKey()).withTargetData(entry.getValue()).build();
            deltaReportEntriesForUpdates.add(updatedDataForDeltaReport);
        }
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
        final Map<String, Serializable> leavesOfSourceDataNode = sourceDataNode.getLeaves();
        final Map<String, Serializable> uniqueLeavesOfTargetDataNode =
            new HashMap<>(targetDataNode.getLeaves());
        uniqueLeavesOfTargetDataNode.keySet().removeAll(leavesOfSourceDataNode.keySet());
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

    private static Map<String, Serializable> getUpdatedNodeData(final DataNode dataNode,
                                                                final Map<String, Serializable> updatedLeaves) {
        final Map<String, Serializable> updatedSourceData = new HashMap<>();
        if (!updatedLeaves.isEmpty()) {
            final String xpath = dataNode.getXpath();
            if (CpsPathUtil.isPathToListElement(xpath)) {
                addKeyLeavesToUpdatedData(xpath, updatedLeaves);
            }
            final Collection<DataNode> updatedDataNode = buildUpdatedDataNode(dataNode, updatedLeaves);
            updatedSourceData.putAll(getNodeNameToDataForDeltaReport(updatedDataNode));
        }
        return updatedSourceData;
    }

    private static void addKeyLeavesToUpdatedData(final String xpath,
                                                  final Map<String, Serializable> updatedLeaves) {
        final Map<String, Serializable> keyLeaves = new HashMap<>();
        final List<CpsPathQuery.LeafCondition> leafConditions = CpsPathUtil.getCpsPathQuery(xpath).getLeafConditions();
        for (final CpsPathQuery.LeafCondition leafCondition: leafConditions) {
            final String leafName = leafCondition.name();
            final Serializable leafValue = (Serializable) leafCondition.value();
            keyLeaves.put(leafName, leafValue);
        }
        updatedLeaves.putAll(keyLeaves);
    }

    private static Collection<DataNode> buildUpdatedDataNode(final DataNode dataNode,
                                                             final Map<String, Serializable> updatedLeaves) {
        final DataNode updatedDataNode = new DataNodeBuilder()
            .withXpath(dataNode.getXpath())
            .withModuleNamePrefix(dataNode.getModuleNamePrefix())
            .withLeaves(updatedLeaves)
            .build();
        return Collections.singletonList(updatedDataNode);
    }

    /**
     * Converts a collection of DataNodes to a map where the keys are the node names and the values are the node data.
     *
     * @param dataNodes the collection of DataNodes
     * @return          a map with node names as keys and their corresponding data as values
     */
    public static Map<String, Serializable> getNodeNameToDataForDeltaReport(final Collection<DataNode> dataNodes) {
        final DataNode containerNode = new DataNodeBuilder().withChildDataNodes(dataNodes).build();
        final Map<String, Object> condensedData = DataMapUtils.toDataMap(containerNode);
        return condensedData.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey,
            entry -> (Serializable) entry.getValue()));
    }
}
