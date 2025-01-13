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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDeltaService;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.DeltaReport;
import org.onap.cps.spi.model.DeltaReportBuilder;
import org.onap.cps.utils.DataMapUtils;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CpsDeltaServiceImpl implements CpsDeltaService {

    @Override
    public List<DeltaReport> getDeltaReports(final Collection<DataNode> sourceDataNodes,
                                             final Collection<DataNode> targetDataNodes,
                                             final boolean groupingEnabled) {

        final List<DeltaReport> deltaReport = new ArrayList<>();

        if (groupingEnabled) {
            deltaReport.addAll(getCondensedDeltaReports(sourceDataNodes, targetDataNodes));
            deltaReport.addAll(getAddedDeltaReportsCondensed(sourceDataNodes, targetDataNodes));
        } else {
            final Map<String, DataNode> xpathToSourceDataNodes = convertToXPathToDataNodesMap(sourceDataNodes);
            final Map<String, DataNode> xpathToTargetDataNodes = convertToXPathToDataNodesMap(targetDataNodes);

            deltaReport.addAll(getRemovedAndUpdatedDeltaReports(xpathToSourceDataNodes, xpathToTargetDataNodes));
            deltaReport.addAll(getAddedDeltaReports(xpathToSourceDataNodes, xpathToTargetDataNodes));
        }
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

    private static Map<String, DataNode> convertToXPathToTopDataNodesMap(
            final Collection<DataNode> dataNodes) {
        final Map<String, DataNode> xpathToDataNode = new LinkedHashMap<>();
        for (final DataNode dataNode : dataNodes) {
            xpathToDataNode.put(dataNode.getXpath(), dataNode);
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
        final DeltaReport removedDeltaReportEntry = new DeltaReportBuilder().actionRemove().withXpath(xpath)
                .withSourceData(Collections.singletonList(sourceDataNode.getLeaves())).build();
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
                    .withXpath(xpath).withSourceData(Collections.singletonList(entry.getKey()))
                    .withTargetData(Collections.singletonList(entry.getValue())).build();
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
                                .withTargetData(Collections.singletonList(dataNode.getLeaves())).build();
            addedDeltaReportEntries.add(addedDataForDeltaReport);
        }
        return addedDeltaReportEntries;
    }

//    private static List<DeltaReport> getCondensedDeltaReports(final Collection<DataNode> sourceDataNodes,
//            final Collection<DataNode> targetDataNodes) {
//
//        final List<DeltaReport> removedAndUpdatedDeltaReportEntries = new ArrayList<>();
//        final Map<String, DataNode> xpathToTargetDataNodes = convertToXPathToTopDataNodesMap(targetDataNodes);
//        //1st for loop to check for removed and updated nodes
//        for (final DataNode sourceDataNode: sourceDataNodes) {
//            final String xpath = sourceDataNode.getXpath();
//            final DataNode targetDataNode = xpathToTargetDataNodes.get(xpath);
//            if (targetDataNode == null) {
//                final Collection<Map<String, Object>> jsonData = new ArrayList<>(getCondensedDeltaReports(sourceDataNode));
//                final Collection<Map<String, Serializable>> serializableJsonData = convertToSerializableCollection(jsonData);
//                final DeltaReport deltaReport = new DeltaReportBuilder().actionRemove().withXpath(xpath)
//                                                                    .withSourceData(serializableJsonData).build();
//                removedAndUpdatedDeltaReportEntries.add(deltaReport);
//            } else {
//                removedAndUpdatedDeltaReportEntries.addAll(getUpdatedDeltaReportsCondensed(xpath, sourceDataNode, targetDataNode));
//            }
//        }
//        return removedAndUpdatedDeltaReportEntries;
//    }

    private static List<DeltaReport> getCondensedDeltaReports(final Collection<DataNode> sourceDataNodes,
            final Collection<DataNode> targetDataNodes) {
        final Map<String, DataNode> xpathToSourceDataNodes = convertToXPathToTopDataNodesMap(sourceDataNodes);
        final Map<String, DataNode> xpathToTargetDataNodes = convertToXPathToTopDataNodesMap(targetDataNodes);
        String xpath = "";
        final List<DeltaReport> deltaReportEntries = new ArrayList<>();
        final Collection<Map<String, Object>> jsonDataRemoved = new LinkedList<>();
        for (Map.Entry<String, DataNode> entry: xpathToSourceDataNodes.entrySet()) {
            xpath = entry.getKey();
            final DataNode sourceDataNode = entry.getValue();
            final DataNode targetDataNode = xpathToTargetDataNodes.get(xpath);
            if (targetDataNode == null) {
                jsonDataRemoved.addAll(getCondensedDeltaReports(sourceDataNode));
            } else {
                deltaReportEntries.addAll(getUpdatedDeltaReportsCondensed(xpath, sourceDataNode, targetDataNode));
                getCondensedDeltaReportsForChildDataNodes(sourceDataNode, targetDataNode, deltaReportEntries);
            }
        }
        deltaReportEntries.addAll(getRemovedDeltaReportsCondensed(jsonDataRemoved, xpath));
        return deltaReportEntries;
    }

    private static void getCondensedDeltaReportsForChildDataNodes(DataNode sourceDataNode, DataNode targetDataNode,
            List<DeltaReport> deltaReportEntries) {
        final Collection<DataNode> childrenOfSourceDataNodes = sourceDataNode.getChildDataNodes();
        final Collection<DataNode> childrenOfTargetDataNodes = targetDataNode.getChildDataNodes();
        if (!childrenOfSourceDataNodes.isEmpty() || !childrenOfTargetDataNodes.isEmpty()) {
            deltaReportEntries.addAll(getCondensedDeltaReports(childrenOfSourceDataNodes, childrenOfTargetDataNodes));
            deltaReportEntries.addAll(getAddedDeltaReportsCondensed(childrenOfSourceDataNodes, childrenOfTargetDataNodes));
        }
    }

    private static List<DeltaReport> getRemovedDeltaReportsCondensed(final Collection<Map<String, Object>> jsonData, final String xpath) {
        final List<DeltaReport> removedDeltaReportEntries = new ArrayList<>();
        if (!jsonData.isEmpty()) {
            final String normalizedXpath = CpsPathUtil.getNormalizedParentXpath(xpath);
            final Collection<Map<String, Serializable>> serializableJsonData = convertToSerializableCollection(jsonData);
            final DeltaReport deltaReport = new DeltaReportBuilder().actionRemove().withXpath(normalizedXpath)
                                                    .withSourceData(serializableJsonData).build();
            removedDeltaReportEntries.add(deltaReport);
        }
        return removedDeltaReportEntries;
    }

    private static Collection<Map<String, Object>> getCondensedDeltaReports(final DataNode dataNode) {
        final Collection<Map<String, Object>> jsonData = new LinkedList<>();
        jsonData.add(DataMapUtils.toDataMap(dataNode));
        return jsonData;
    }

    private static List<DeltaReport> getUpdatedDeltaReportsCondensed(final String xpath, final DataNode sourceDataNode,
            final DataNode targetDataNode) {

        final List<DeltaReport> updatedDeltaReportEntries = new ArrayList<>();
        final Map<Map<String, Serializable>, Map<String, Serializable>> updatedLeaves = new HashMap<>(
                getUpdatedLeavesBetweenSourceAndTargetDataNode(sourceDataNode.getLeaves(), targetDataNode.getLeaves()));
        if (!updatedLeaves.isEmpty()) {
            addUpdatedLeavesToDeltaReport(xpath, updatedLeaves, updatedDeltaReportEntries);
        }
        return updatedDeltaReportEntries;
    }

    private static List<DeltaReport> getAddedDeltaReportsCondensed(final Collection<DataNode> sourceDataNodes,
            final Collection<DataNode> targetDataNodes) {

        final List<DeltaReport> addedDeltaReportEntries = new ArrayList<>();
        final Map<String, DataNode> xpathToSourceDataNodes = convertToXPathToTopDataNodesMap(sourceDataNodes);
        final Map<String, DataNode> xpathToTargetDataNodes = convertToXPathToTopDataNodesMap(targetDataNodes);
        final Collection<Map<String, Object>> jsonDataAdded = new ArrayList<>();
        String childXpathAdded ="";
        for (Map.Entry<String, DataNode> entry: xpathToTargetDataNodes.entrySet()) {
            childXpathAdded = entry.getKey();
            final DataNode childTarget = entry.getValue();
            final DataNode childSource = xpathToSourceDataNodes.get(childXpathAdded);
            if (childSource == null) {
                jsonDataAdded.addAll(getCondensedDeltaReports(childTarget));
            }
        }
        if (!jsonDataAdded.isEmpty()) {
            final String normalizedXpath = CpsPathUtil.getNormalizedParentXpath(childXpathAdded);
            final Collection<Map<String, Serializable>> serializableJsonData = convertToSerializableCollection(jsonDataAdded);
            final DeltaReport deltaReport;
            if (normalizedXpath.isEmpty()) {
                deltaReport = new DeltaReportBuilder().actionCreate().withXpath(childXpathAdded)
                                                    .withTargetData(serializableJsonData).build();
                addedDeltaReportEntries.add(deltaReport);
            } else{
                deltaReport = new DeltaReportBuilder().actionCreate().withXpath(normalizedXpath)
                                                        .withTargetData(serializableJsonData).build();
            }

            addedDeltaReportEntries.add(deltaReport);
        }
        return addedDeltaReportEntries;
    }

    private static Collection<Map<String, Serializable>> convertToSerializableCollection(Collection<Map<String, Object>> jsonData) {
        Collection<Map<String, Serializable>> serializableCollection = new ArrayList<>();
        for (Map<String, Object> map : jsonData) {
            Map<String, Serializable> serializableMap = new HashMap<>();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (entry.getValue() instanceof Serializable) {
                    serializableMap.put(entry.getKey(), (Serializable) entry.getValue());
                }
            }
            serializableCollection.add(serializableMap);
        }
        return serializableCollection;
    }

//    private static void addUpdatedLeavesToGroupedDeltaReport(String xpath, Map<Map<String, Serializable>, Map<String, Serializable>> groupedUpdatedLeaves, List<DeltaReport> updatedDeltaReportEntries) {
//
//        final Collection<Map<String, Serializable>> sourceLeaves = new ArrayList<>();
//        final Collection<Map<String, Serializable>> targetLeaves = new ArrayList<>();
//
//        for (Map.Entry<Map<String, Serializable>, Map<String, Serializable>> entry: groupedUpdatedLeaves.entrySet()) {
//            sourceLeaves.add(new HashMap<>(entry.getKey()));
//            targetLeaves.add(new HashMap<>(entry.getValue()));
//        }
//
//        final DeltaReport delta = new DeltaReportBuilder().actionReplace().withXpath(xpath).withSourceData(sourceLeaves).withTargetData(targetLeaves).build();
//        updatedDeltaReportEntries.add(delta);
//    }
}
