/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 TechMahindra Ltd.
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

import static org.onap.cps.cpspath.parser.CpsPathUtil.ROOT_NODE_XPATH;
import static org.onap.cps.utils.ContentType.JSON;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.DataNodeFactory;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.model.DeltaReport;
import org.onap.cps.cpspath.parser.CpsPathUtil;
import org.onap.cps.cpspath.parser.PathParsingException;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeltaReportExecutor {

    private static final OffsetDateTime NO_TIMESTAMP = null;

    private final CpsAnchorService cpsAnchorService;
    private final CpsDataPersistenceService cpsDataPersistenceService;
    private final DataNodeFactory dataNodeFactory;
    private final JsonObjectMapper jsonObjectMapper;

    /**
     * Applies the delta report to the specified dataspace and anchor.
     *
     * @param dataspaceName     the name of the dataspace
     * @param anchorName        the name of the anchor
     * @param deltaReportAsJsonString the delta report as a JSON string
     */
    @Transactional
    public void applyChangesInDeltaReport(final String dataspaceName, final String anchorName,
                                   final String deltaReportAsJsonString) {
        final List<DeltaReport> deltaReports =
            jsonObjectMapper.convertToJsonArray(deltaReportAsJsonString, DeltaReport.class);
        for (final DeltaReport deltaReport: deltaReports) {
            final String action = deltaReport.getAction();
            final String xpath = deltaReport.getXpath();
            if (action.equals(DeltaReport.REPLACE_ACTION)) {
                final String dataForUpdate = jsonObjectMapper.asJsonString(deltaReport.getTargetData());
                updateDataNodes(dataspaceName, anchorName, xpath, dataForUpdate);
            } else if (action.equals(DeltaReport.REMOVE_ACTION)) {
                final String dataForDelete = jsonObjectMapper.asJsonString(deltaReport.getSourceData());
                deleteDataNodesUsingDelta(dataspaceName, anchorName, xpath, dataForDelete);
            } else if (action.equals(DeltaReport.CREATE_ACTION)) {
                final String dataForCreate = jsonObjectMapper.asJsonString(deltaReport.getTargetData());
                addDataNodesUsingDelta(dataspaceName, anchorName, xpath, dataForCreate);
            } else {
                throw new DataValidationException("Data Validation Failed",
                    "Failed to parse JSON data at: <invalid-action>");
            }
        }
    }

    private void updateDataNodes(final String dataspaceName, final String anchorName, final String xpath,
                                 final String updatedData) {
        final String parentNodeXpath;
        try {
            parentNodeXpath = CpsPathUtil.getNormalizedParentXpath(xpath);
        } catch (final PathParsingException pathParsingException) {
            throw new DataValidationException("Error while parsing xpath expression",
                pathParsingException.getMessage());
        }
        final Collection<DataNode> dataNodesToUpdate =
            createDataNodes(dataspaceName, anchorName, parentNodeXpath, updatedData);
        cpsDataPersistenceService.updateDataNodesAndDescendants(dataspaceName, anchorName, dataNodesToUpdate);
    }

    private void deleteDataNodesUsingDelta(final String dataspaceName, final String anchorName, final String xpath,
                                           final String dataToDelete) {
        final Collection<DataNode> dataNodesToDelete;
        try {
            dataNodesToDelete = createDataNodes(dataspaceName, anchorName, xpath, dataToDelete);
        } catch (final PathParsingException pathParsingException) {
            throw new DataValidationException("Error while parsing xpath expression",
                pathParsingException.getMessage());
        }
        final Collection<String> xpathsToDelete = dataNodesToDelete.stream().map(DataNode::getXpath).toList();
        cpsDataPersistenceService.deleteDataNodes(dataspaceName, anchorName, xpathsToDelete);
    }

    private void addDataNodesUsingDelta(final String dataspaceName, final String anchorName, final String xpath,
                                        final String dataToAdd) {
        final String xpathToAdd;
        try {
            xpathToAdd = isRootListNodeXpath(xpath) ? CpsPathUtil.ROOT_NODE_XPATH : xpath;
        } catch (final PathParsingException pathParsingException) {
            throw new DataValidationException("Error while parsing xpath expression",
                pathParsingException.getMessage());
        }
        final Collection<DataNode> dataNodesToAdd = createDataNodes(dataspaceName, anchorName, xpathToAdd, dataToAdd);
        if (ROOT_NODE_XPATH.equals(xpathToAdd)) {
            cpsDataPersistenceService.storeDataNodes(dataspaceName, anchorName, dataNodesToAdd);
        } else {
            cpsDataPersistenceService.addListElements(dataspaceName, anchorName, xpathToAdd, dataNodesToAdd);
        }
    }

    private boolean isRootListNodeXpath(final String xpath) {
        return CpsPathUtil.getNormalizedParentXpath(xpath).isEmpty() && CpsPathUtil.isPathToListElement(xpath);
    }

    private Collection<DataNode> createDataNodes(final String datasapceName, final String anchorName,
                                                 final String xpath, final String nodeData) {
        final Anchor anchor = cpsAnchorService.getAnchor(datasapceName, anchorName);
        return dataNodeFactory.createDataNodesWithAnchorParentXpathAndNodeData(anchor, xpath, nodeData, JSON);
    }
}
