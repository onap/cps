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

import static org.onap.cps.utils.deltareport.DeltaReportHelper.getNodeNameToDataForDeltaReport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.model.DeltaReport;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.impl.DeltaReportBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupedDeltaReportGenerator {

    private final DeltaReportHelper deltaReportHelper;

    /**
     * Get condensed delta reports for the given source and target data nodes.
     *
     * @param sourceDataNodes the source data nodes
     * @param targetDataNodes the target data nodes
     * @return                a list of condensed delta reports
     */
    public List<DeltaReport> createCondensedDeltaReports(final Collection<DataNode> sourceDataNodes,
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
                .withSourceData(getNodeNameToDataForDeltaReport(removedDataNodes)).build());
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
                    deltaReportHelper.createDeltaReportsForUpdates(xpath, sourceDataNode, targetDataNode);
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
            deltaReportEntries
                .addAll(createCondensedDeltaReports(childrenOfSourceDataNodes, childrenOfTargetDataNodes));
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
                .withTargetData(getNodeNameToDataForDeltaReport(addedDataNodes)).build());
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

    private static Map<String, DataNode> flattenToXpathToFirstLevelDataNodeMap(final Collection<DataNode> dataNodes) {
        return dataNodes.stream().collect(Collectors.toMap(DataNode::getXpath, dataNode -> dataNode));
    }
}
