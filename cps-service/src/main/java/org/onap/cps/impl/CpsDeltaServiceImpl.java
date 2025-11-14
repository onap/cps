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

import static org.onap.cps.cpspath.parser.CpsPathUtil.ROOT_NODE_XPATH;
import static org.onap.cps.utils.ContentType.JSON;

import io.micrometer.core.annotation.Timed;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsDeltaService;
import org.onap.cps.api.DataNodeFactory;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.model.DeltaReport;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.cpspath.parser.PathParsingException;
import org.onap.cps.utils.CpsValidator;
import org.onap.cps.utils.DataMapper;
import org.onap.cps.utils.JsonObjectMapper;
import org.onap.cps.utils.deltareport.DeltaReportExecutor;
import org.onap.cps.utils.deltareport.DeltaReportGenerator;
import org.onap.cps.utils.deltareport.GroupedDeltaReportGenerator;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CpsDeltaServiceImpl implements CpsDeltaService {

    private final DeltaReportExecutor deltaReportExecutor;
    private final CpsAnchorService cpsAnchorService;
    private final CpsValidator cpsValidator;
    private final CpsDataService cpsDataService;
    private final DataNodeFactory dataNodeFactory;
    private final DataMapper dataMapper;
    private final JsonObjectMapper jsonObjectMapper;
    private final DeltaReportGenerator deltaReportGenerator;
    private final GroupedDeltaReportGenerator groupedDeltaReportGenerator;

    @Override
    @Timed(value = "cps.delta.service.get.delta",
        description = "Time taken to get delta between anchors")
    public List<DeltaReport> getDeltaByDataspaceAndAnchors(final String dataspaceName,
                                                           final String sourceAnchorName,
                                                           final String targetAnchorName,
                                                           final String xpath,
                                                           final FetchDescendantsOption fetchDescendantsOption,
                                                           final boolean groupDataNodes) {

        final String xpathForDeltaReport = validateXpath(xpath);
        final Collection<DataNode> sourceDataNodes = cpsDataService.getDataNodesForMultipleXpaths(dataspaceName,
            sourceAnchorName, Collections.singletonList(xpathForDeltaReport), fetchDescendantsOption);
        final Collection<DataNode> targetDataNodes = cpsDataService.getDataNodesForMultipleXpaths(dataspaceName,
            targetAnchorName, Collections.singletonList(xpathForDeltaReport), fetchDescendantsOption);
        return getDeltaReports(sourceDataNodes, targetDataNodes, groupDataNodes);
    }

    @Timed(value = "cps.delta.service.get.delta",
        description = "Time taken to get delta between anchor and a payload")
    @Override
    public List<DeltaReport> getDeltaByDataspaceAnchorAndPayload(final String dataspaceName,
                                                                 final String sourceAnchorName,
                                                                 final String xpath,
                                                                 final Map<String, String> yangResourceContentPerName,
                                                                 final String targetData,
                                                                 final FetchDescendantsOption fetchDescendantsOption,
                                                                 final boolean groupDataNodes) {

        final Anchor sourceAnchor = cpsAnchorService.getAnchor(dataspaceName, sourceAnchorName);
        final Collection<DataNode> sourceDataNodes = cpsDataService.getDataNodesForMultipleXpaths(dataspaceName,
            sourceAnchorName, Collections.singletonList(xpath), fetchDescendantsOption);
        final Collection<DataNode> sourceDataNodesRebuilt =
            rebuildSourceDataNodes(xpath, sourceAnchor, sourceDataNodes);
        final Collection<DataNode> targetDataNodes = new ArrayList<>(
            buildTargetDataNodes(sourceAnchor, xpath, yangResourceContentPerName, targetData));
        return getDeltaReports(sourceDataNodesRebuilt, targetDataNodes, groupDataNodes);
    }

    /**
     * Apply the delta report to the data nodes.
     *
     * @param dataspaceName      name of the dataspace
     * @param anchorName         name of the anchor
     * @param deltaReportAsJsonString  JSON string representing the delta report
     */
    @Override
    public void applyChangesInDeltaReport(final String dataspaceName, final String anchorName,
                                          final String deltaReportAsJsonString) {
        cpsValidator.validateNameCharacters(dataspaceName, anchorName);
        deltaReportExecutor.applyChangesInDeltaReport(dataspaceName, anchorName, deltaReportAsJsonString);
    }

    private List<DeltaReport> getDeltaReports(final Collection<DataNode> sourceDataNodes,
                                              final Collection<DataNode> targetDataNodes,
                                              final boolean groupDataNodes) {

        final List<DeltaReport> deltaReport = new ArrayList<>();
        if (groupDataNodes) {
            deltaReport
                .addAll(groupedDeltaReportGenerator.createCondensedDeltaReports(sourceDataNodes, targetDataNodes));
        } else {
            deltaReport.addAll(deltaReportGenerator.createDeltaReports(sourceDataNodes, targetDataNodes));
        }
        return Collections.unmodifiableList(deltaReport);
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

    private String validateXpath(final String xpath) {
        try {
            return xpath.equals(ROOT_NODE_XPATH) ? ROOT_NODE_XPATH : CpsPathUtil.getNormalizedXpath(xpath);
        } catch (final PathParsingException pathParsingException) {
            throw new DataValidationException("Invalid xpath: " + xpath, pathParsingException.getMessage(),
                pathParsingException);
        }
    }

}
