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
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDeltaService;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.DeltaReport;
import org.onap.cps.spi.model.DeltaReportBuilder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@NoArgsConstructor
public class CpsDeltaServiceImpl implements CpsDeltaService {

    @Override
    public List<DeltaReport> getDeltaReports(final Collection<DataNode> sourceDataNodes,
                                             final Collection<DataNode> targetDataNodes) {

        final List<DeltaReport> deltaReport = new ArrayList<>();

        final Map<String, DataNode> xpathToSourceDataNodes = convertToXPathToDataNodesMap(sourceDataNodes);
        final Map<String, DataNode> xpathToTargetDataNodes = convertToXPathToDataNodesMap(targetDataNodes);

        deltaReport.addAll(getRemovedDeltaReports(xpathToSourceDataNodes, xpathToTargetDataNodes));

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

    private static List<DeltaReport> getRemovedDeltaReports(
                                                            final Map<String, DataNode> xpathToSourceDataNodes,
                                                            final Map<String, DataNode> xpathToTargetDataNodes) {

        final List<DeltaReport> removedDeltaReportEntries = new ArrayList<>();
        for (final Map.Entry<String, DataNode> entry: xpathToSourceDataNodes.entrySet()) {
            final String xpath = entry.getKey();
            final DataNode sourceDataNode = entry.getValue();
            final DataNode targetDataNode = xpathToTargetDataNodes.get(xpath);

            if (targetDataNode == null) {
                final Map<String, Serializable> sourceDataNodeLeaves = sourceDataNode.getLeaves();
                final DeltaReport removedData = new DeltaReportBuilder().actionRemove().withXpath(xpath)
                        .withSourceData(sourceDataNodeLeaves).build();
                removedDeltaReportEntries.add(removedData);
            }
        }
        return removedDeltaReportEntries;
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
}
