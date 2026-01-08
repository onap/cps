/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 Deutsche Telekom AG
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

package org.onap.cps.api;

import java.util.List;
import java.util.Map;
import org.onap.cps.api.model.DeltaReport;
import org.onap.cps.api.parameters.FetchDescendantsOption;

public interface CpsDeltaService {

    /**
     * Retrieves the delta between two anchors by xpath within a dataspace. Returns a List of {@link DeltaReport}.
     * Each Delta Report contains following information: action, xpath, source-payload and/or target-payload.
     *
     * @param dataspaceName          dataspace name
     * @param sourceAnchorName       source anchor name
     * @param targetAnchorName       target anchor name
     * @param xpath                  xpath
     * @param fetchDescendantsOption defines the scope of data to fetch: either single node or all the descendant
     *                               nodes (recursively) as well
     * @param groupDataNodes         boolean flag to enable or disable grouping of data nodes in delta report.
     *                               If enabled, data nodes are grouped based on parent-child relationship, providing a
     *                               condensed version of delta report.
     * @return                       list containing {@link DeltaReport} objects
     */
    List<DeltaReport> getDeltaByDataspaceAndAnchors(String dataspaceName, String sourceAnchorName,
                                                    String targetAnchorName, String xpath,
                                                    FetchDescendantsOption fetchDescendantsOption,
                                                    boolean groupDataNodes);

    /**
     * Retrieves the delta between an anchor and JSON payload by xpath, using dataspace name and anchor name.
     * Returns a List of {@link DeltaReport}. Each Delta Report contains following information: action, xpath,
     * source-payload and/or target-payload.
     *
     * @param dataspaceName                source dataspace name
     * @param sourceAnchorName             source anchor name
     * @param xpath                        xpath
     * @param yangResourceContentPerName   YANG resources (files) map where key is a name and value is content
     * @param targetData                   target data to be compared in JSON string format
     * @param fetchDescendantsOption       defines the scope of data to fetch: defaulted to INCLUDE_ALL_DESCENDANTS
     * @param groupDataNodes               boolean flag to enable or disable grouping of data nodes in delta report.
     *                                     If enabled, data nodes are grouped based on parent-child relationship,
     *                                     providing a condensed version of delta report.
     *
     * @return                             list containing {@link DeltaReport} objects
     */
    List<DeltaReport> getDeltaByDataspaceAnchorAndPayload(String dataspaceName, String sourceAnchorName, String xpath,
                                                          Map<String, String> yangResourceContentPerName,
                                                          String targetData,
                                                          FetchDescendantsOption fetchDescendantsOption,
                                                          boolean groupDataNodes);

    /**
     * Apply the changes in the given delta report to an anchor. The delta report contains the difference between two
     * anchors or an anchor and a configuration.
     *
     * @param dataspaceName           dataspace name
     * @param anchorName              anchor name where the delta report is to be applied
     * @param deltaReportAsJsonString delta report in JSON string format
     */
    void applyChangesInDeltaReport(String dataspaceName, String anchorName, String deltaReportAsJsonString);
}
