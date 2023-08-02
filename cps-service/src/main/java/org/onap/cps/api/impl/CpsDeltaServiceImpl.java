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
    public List<DeltaReport> getDeltaReport(final Collection<DataNode> sourceDataNodes,
                                            final Collection<DataNode> targetDataNodes) {

        final List<DeltaReport> deltaReport = new ArrayList<>();

        final Map<String, Map<String, Serializable>> xpathToSourceLeaves = convertToXpathToLeavesMap(sourceDataNodes);
        final Map<String, Map<String, Serializable>> xpathToTargetLeaves = convertToXpathToLeavesMap(targetDataNodes);

        deltaReport.addAll(getRemovedDeltaReports(xpathToSourceLeaves, xpathToTargetLeaves));

        deltaReport.addAll(getAddedDeltaReports(xpathToSourceLeaves, xpathToTargetLeaves));

        return Collections.unmodifiableList(deltaReport);
    }

    private static Map<String, Map<String, Serializable>> convertToXpathToLeavesMap(
                                                                    final Collection<DataNode> dataNodes) {
        final Map<String, Map<String, Serializable>> xpathToDataNode = new LinkedHashMap<>();
        for (final DataNode dataNode : dataNodes) {
            xpathToDataNode.put(dataNode.getXpath(), dataNode.getLeaves());
            final Collection<DataNode> childDataNodes = dataNode.getChildDataNodes();
            if (!childDataNodes.isEmpty()) {
                xpathToDataNode.putAll(convertToXpathToLeavesMap(childDataNodes));
            }
        }
        return xpathToDataNode;
    }

    private static List<DeltaReport> getRemovedDeltaReports(
                                                    final Map<String, Map<String, Serializable>> xpathToSourceLeaves,
                                                    final Map<String, Map<String, Serializable>> xpathToTargetLeaves) {

        final List<DeltaReport> removedDeltaReportEntries = new ArrayList<>();
        for (final Map.Entry<String, Map<String, Serializable>> entry: xpathToSourceLeaves.entrySet()) {
            final String xpath = entry.getKey();
            final Map<String, Serializable> sourceLeaves = entry.getValue();
            final Map<String, Serializable> targetLeaves = xpathToTargetLeaves.get(xpath);
            if (targetLeaves == null) {
                final DeltaReport removedData = new DeltaReportBuilder().actionRemove().withXpath(xpath)
                        .withSourceData(sourceLeaves).build();
                removedDeltaReportEntries.add(removedData);
            }
        }
        return removedDeltaReportEntries;
    }

    private static List<DeltaReport> getAddedDeltaReports(
                                                    final Map<String, Map<String, Serializable>> xpathToSourceLeaves,
                                                    final Map<String, Map<String, Serializable>> xpathToTargetLeaves) {

        final List<DeltaReport> addedDeltaReportEntries = new ArrayList<>();
        final Map<String, Map<String, Serializable>> xpathToAddedLeaves = new LinkedHashMap<>(xpathToTargetLeaves);
        xpathToAddedLeaves.keySet().removeAll(xpathToSourceLeaves.keySet());
        for (final Map.Entry<String, Map<String, Serializable>> entry: xpathToAddedLeaves.entrySet()) {
            final String xpath = entry.getKey();
            final Map<String, Serializable> addedLeaves = entry.getValue();
            final DeltaReport addedDataForDeltaReport = new DeltaReportBuilder().actionAdd().withXpath(xpath)
                                .withTargetData(addedLeaves).build();
            addedDeltaReportEntries.add(addedDataForDeltaReport);
        }
        return addedDeltaReportEntries;
    }
}
