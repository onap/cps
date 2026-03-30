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

import static org.onap.cps.utils.deltareport.DeltaReportHelper.getNodeNameToDataForDeltaReport;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
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

    /**
     * Generate delta reports between the given source and target data nodes.
     *
     * @param sourceDataNodes the source data nodes
     * @param targetDataNodes the target data nodes
     * @return                a list of delta reports
     */
    public List<DeltaReport> createDeltaReports(final Collection<DataNode> sourceDataNodes,
                                                final Collection<DataNode> targetDataNodes) {
        final CompletableFuture<Map<String, DataNode>> sourceFuture =
            CompletableFuture.supplyAsync(() -> convertToXPathToDataNodesMap(sourceDataNodes));
        final CompletableFuture<Map<String, DataNode>> targetFuture =
            CompletableFuture.supplyAsync(() -> convertToXPathToDataNodesMap(targetDataNodes));
        try {
            final Map<String, DataNode> xpathToSourceDataNodes = sourceFuture.join();
            final Map<String, DataNode> xpathToTargetDataNodes = targetFuture.join();

            final CompletableFuture<List<DeltaReport>> removalsAndUpdatesFuture =
                CompletableFuture.supplyAsync(() ->
                    getRemovedAndUpdatedDeltaReports(xpathToSourceDataNodes, xpathToTargetDataNodes));
            final CompletableFuture<List<DeltaReport>> additionsFuture =
                CompletableFuture.supplyAsync(() ->
                    getAddedDeltaReports(xpathToSourceDataNodes, xpathToTargetDataNodes));

            final List<DeltaReport> deltaReport =
                new ArrayList<>(xpathToSourceDataNodes.size() + xpathToTargetDataNodes.size());
            deltaReport.addAll(removalsAndUpdatesFuture.join());
            deltaReport.addAll(additionsFuture.join());
            return deltaReport;
        } catch (final CompletionException completionException) {
            throw CompletionExceptionConverter.convertCompletionException(completionException,
                "Failed to generate delta report",
                "Unexpected error during delta report generation");
        }
    }

    private static Map<String, DataNode> convertToXPathToDataNodesMap(final Collection<DataNode> dataNodes) {
        final Map<String, DataNode> xpathToDataNode = new LinkedHashMap<>();
        flattenDataNodes(dataNodes, xpathToDataNode);
        return xpathToDataNode;
    }

    private static void flattenDataNodes(final Collection<DataNode> dataNodes,
                                         final Map<String, DataNode> xpathToDataNode) {
        for (final DataNode dataNode : dataNodes) {
            final DataNode dataNodeWithoutChild = copyWithoutChildren(dataNode);
            xpathToDataNode.put(dataNodeWithoutChild.getXpath(), dataNodeWithoutChild);
            final Collection<DataNode> childDataNodes = dataNode.getChildDataNodes();
            if (!childDataNodes.isEmpty()) {
                flattenDataNodes(childDataNodes, xpathToDataNode);
            }
        }
    }

    private static DataNode copyWithoutChildren(final DataNode dataNode) {
        final DataNode dataNodeWithoutChild = new DataNode();
        dataNodeWithoutChild.setXpath(dataNode.getXpath());
        dataNodeWithoutChild.setModuleNamePrefix(dataNode.getModuleNamePrefix());
        dataNodeWithoutChild.setLeaves(dataNode.getLeaves());
        dataNodeWithoutChild.setChildDataNodes(Collections.emptyList());
        return dataNodeWithoutChild;
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
        final Map<String, Serializable> sourceDataNodeRemoved =
            getNodeNameToDataForDeltaReport(Collections.singletonList(sourceDataNode));
        final DeltaReport removedDeltaReportEntry = new DeltaReportBuilder().actionRemove().withXpath(xpath)
            .withSourceData(sourceDataNodeRemoved).build();
        return Collections.singletonList(removedDeltaReportEntry);
    }

    private static List<DeltaReport> getAddedDeltaReports(final Map<String, DataNode> xpathToSourceDataNodes,
                                                          final Map<String, DataNode> xpathToTargetDataNodes) {

        final List<DeltaReport> addedDeltaReportEntries = new ArrayList<>();
        for (final Map.Entry<String, DataNode> entry : xpathToTargetDataNodes.entrySet()) {
            final String xpath = entry.getKey();
            if (!xpathToSourceDataNodes.containsKey(xpath)) {
                final DataNode dataNode = entry.getValue();
                final Map<String, Serializable> targetData =
                    getNodeNameToDataForDeltaReport(Collections.singletonList(dataNode));
                final DeltaReport addedDataForDeltaReport = new DeltaReportBuilder().actionCreate().withXpath(xpath)
                    .withTargetData(targetData).build();
                addedDeltaReportEntries.add(addedDataForDeltaReport);
            }
        }
        return addedDeltaReportEntries;
    }

}
