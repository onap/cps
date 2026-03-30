/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 Deutsche Telekom AG
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
     * Holds the updated source and target data for a delta report entry.
     *
     * @param sourceData the updated source data (may be empty)
     * @param targetData the updated target data (may be empty)
     */
    record UpdatedData(Map<String, Serializable> sourceData, Map<String, Serializable> targetData) {

        boolean isEmpty() {
            return sourceData.isEmpty() && targetData.isEmpty();
        }
    }

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
        final UpdatedData updatedData = getUpdatedSourceAndTargetData(sourceDataNode, targetDataNode);
        if (!updatedData.isEmpty()) {
            final DeltaReport deltaReport = new DeltaReportBuilder().actionReplace().withXpath(xpath)
                .withSourceData(updatedData.sourceData()).withTargetData(updatedData.targetData()).build();
            return Collections.singletonList(deltaReport);
        }
        return Collections.emptyList();
    }

    private static UpdatedData getUpdatedSourceAndTargetData(final DataNode sourceDataNode,
                                                             final DataNode targetDataNode) {
        final Map<String, Serializable> updatedLeavesInSourceData = new HashMap<>();
        final Map<String, Serializable> updatedLeavesInTargetData = new HashMap<>();
        processLeafDifferences(sourceDataNode, targetDataNode,
            updatedLeavesInSourceData, updatedLeavesInTargetData);
        final Map<String, Serializable> updatedSourceData =
            getUpdatedNodeData(sourceDataNode, updatedLeavesInSourceData);
        final Map<String, Serializable> updatedTargetData =
            getUpdatedNodeData(targetDataNode, updatedLeavesInTargetData);
        return new UpdatedData(updatedSourceData, updatedTargetData);
    }

    private static void processLeafDifferences(final DataNode sourceDataNode,
                                               final DataNode targetDataNode,
                                               final Map<String, Serializable> sourceDataInDeltaReport,
                                               final Map<String, Serializable> targetDataInDeltaReport) {
        final Map<String, Serializable> leavesOfSourceDataNode = sourceDataNode.getLeaves();
        final Map<String, Serializable> leavesOfTargetDataNode = targetDataNode.getLeaves();
        for (final Map.Entry<String, Serializable> entry : leavesOfSourceDataNode.entrySet()) {
            final String key = entry.getKey();
            final Serializable sourceLeaf = entry.getValue();
            final Serializable targetLeaf = leavesOfTargetDataNode.get(key);
            if (targetLeaf == null) {
                sourceDataInDeltaReport.put(key, sourceLeaf);
            } else if (!Objects.equals(sourceLeaf, targetLeaf)) {
                sourceDataInDeltaReport.put(key, sourceLeaf);
                targetDataInDeltaReport.put(key, targetLeaf);
            }
        }
        for (final Map.Entry<String, Serializable> entry : leavesOfTargetDataNode.entrySet()) {
            if (!leavesOfSourceDataNode.containsKey(entry.getKey())) {
                targetDataInDeltaReport.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private static Map<String, Serializable> getUpdatedNodeData(final DataNode dataNode,
                                                                final Map<String, Serializable> updatedLeaves) {
        if (updatedLeaves.isEmpty()) {
            return Collections.emptyMap();
        }
        final String xpath = dataNode.getXpath();
        if (xpath.endsWith("]")) {
            final CpsPathQuery cpsPathQuery = CpsPathUtil.getCpsPathQuery(xpath);
            if (cpsPathQuery.isPathToListElement()) {
                addKeyLeavesToUpdatedData(cpsPathQuery, updatedLeaves);
            }
        }
        final Collection<DataNode> updatedDataNode = buildUpdatedDataNode(dataNode, updatedLeaves);
        return getNodeNameToDataForDeltaReport(updatedDataNode);
    }

    private static void addKeyLeavesToUpdatedData(final CpsPathQuery cpsPathQuery,
                                                  final Map<String, Serializable> updatedLeaves) {
        for (final CpsPathQuery.LeafCondition leafCondition : cpsPathQuery.getLeafConditions()) {
            updatedLeaves.put(leafCondition.name(), (Serializable) leafCondition.value());
        }
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
