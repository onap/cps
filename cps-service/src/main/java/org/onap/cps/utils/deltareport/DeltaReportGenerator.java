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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.model.DeltaReport;
import org.onap.cps.impl.DeltaReportBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeltaReportGenerator {
    private final DeltaReportHelper deltaReportHelper;

    /**`
     * Generate delta reports between the given source and target data nodes.
     *
     * @param sourceDataNodes the source data nodes
     * @param targetDataNodes the target data nodes
     * @return                a list of delta reports
     */
    public List<DeltaReport> createDeltaReports(final Collection<DataNode> sourceDataNodes,
                                                final Collection<DataNode> targetDataNodes) {
        final List<DeltaReport> deltaReport = new ArrayList<>();
        final Map<String, DataNode> xpathToSourceDataNodes = convertToXPathToDataNodesMap(sourceDataNodes);
        final Map<String, DataNode> xpathToTargetDataNodes = convertToXPathToDataNodesMap(targetDataNodes);
        deltaReport.addAll(getRemovedAndUpdatedDeltaReports(xpathToSourceDataNodes, xpathToTargetDataNodes));
        deltaReport.addAll(getAddedDeltaReports(xpathToSourceDataNodes, xpathToTargetDataNodes));
        return deltaReport;
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
        return clearChildDataNodes(xpathToDataNode);
    }

    private static Map<String, DataNode> clearChildDataNodes(final Map<String, DataNode> xpathToDataNodes) {
        for (final DataNode dataNode : xpathToDataNodes.values()) {
            dataNode.setChildDataNodes(Collections.emptyList());
        }
        return xpathToDataNodes;
    }

    private List<DeltaReport> getRemovedAndUpdatedDeltaReports(final Map<String, DataNode> xpathToSourceDataNodes,
                                                               final Map<String, DataNode> xpathToTargetDataNodes) {
        final List<DeltaReport> removedAndUpdatedDeltaReportEntries = new ArrayList<>();
        for (final Map.Entry<String, DataNode> entry: xpathToSourceDataNodes.entrySet()) {
            final String xpath = entry.getKey();
            final DataNode sourceDataNode = entry.getValue();
            final DataNode targetDataNode = xpathToTargetDataNodes.get(xpath);
            final List<DeltaReport> deltaReports;
            if (targetDataNode == null) {
                deltaReports = getDeltaReportsForRemove(xpath, sourceDataNode);
            } else {
                deltaReports = deltaReportHelper.createDeltaReportsForUpdates(xpath, sourceDataNode, targetDataNode);
            }
            removedAndUpdatedDeltaReportEntries.addAll(deltaReports);
        }
        return removedAndUpdatedDeltaReportEntries;
    }

    private static List<DeltaReport> getDeltaReportsForRemove(final String xpath, final DataNode sourceDataNode) {
        final List<DeltaReport> deltaReportEntriesForRemove = new ArrayList<>();
        final Map<String, Serializable> sourceDataNodeRemoved =
            getNodeNameToDataForDeltaReport(Collections.singletonList(sourceDataNode));
        final DeltaReport removedDeltaReportEntry = new DeltaReportBuilder().actionRemove().withXpath(xpath)
            .withSourceData(sourceDataNodeRemoved).build();
        deltaReportEntriesForRemove.add(removedDeltaReportEntry);
        return deltaReportEntriesForRemove;
    }

    private static List<DeltaReport> getAddedDeltaReports(final Map<String, DataNode> xpathToSourceDataNodes,
                                                          final Map<String, DataNode> xpathToTargetDataNodes) {

        final List<DeltaReport> addedDeltaReportEntries = new ArrayList<>();
        final Map<String, DataNode> xpathToAddedNodes = new LinkedHashMap<>(xpathToTargetDataNodes);
        xpathToAddedNodes.keySet().removeAll(xpathToSourceDataNodes.keySet());
        for (final Map.Entry<String, DataNode> entry: xpathToAddedNodes.entrySet()) {
            final String xpath = entry.getKey();
            final DataNode dataNode = entry.getValue();
            final Map<String, Serializable> targetData =
                getNodeNameToDataForDeltaReport(Collections.singletonList(dataNode));
            final DeltaReport addedDataForDeltaReport = new DeltaReportBuilder().actionCreate().withXpath(xpath)
                .withTargetData(targetData).build();
            addedDeltaReportEntries.add(addedDataForDeltaReport);
        }
        return addedDeltaReportEntries;
    }

}
