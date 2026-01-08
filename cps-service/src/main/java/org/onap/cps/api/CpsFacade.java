/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Nordix Foundation
 *  Modifications Copyright (C) 2025 Deutsche Telekom AG
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
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.api.parameters.PaginationOption;

public interface CpsFacade {

    /**
     * Get the first data node for a given dataspace, anchor and xpath.
     *
     * @param dataspaceName          the name of the dataspace
     * @param anchorName             the name of the anchor
     * @param xpath                  the xpath
     * @param fetchDescendantsOption control what level of descendants should be returned
     * @return                       a map representing the data node and its descendants
     */
    Map<String, Object> getFirstDataNodeByAnchor(String dataspaceName,
                                                 String anchorName,
                                                 String xpath,
                                                 FetchDescendantsOption fetchDescendantsOption);

    /**
     * Get data nodes for a given dataspace, anchor and xpath.
     *
     * @param dataspaceName          the name of the dataspace
     * @param anchorName             the name of the anchor
     * @param xpath                  the xpath
     * @param fetchDescendantsOption control what level of descendants should be returned
     * @return                       a map representing the data nodes and their descendants
     */
    List<Map<String, Object>> getDataNodesByAnchor(String dataspaceName,
                                                   String anchorName,
                                                   String xpath,
                                                   FetchDescendantsOption fetchDescendantsOption);

    /**
     * Get data nodes for a given dataspace, anchor and xpath.
     * This method ensures that list nodes are returned as a single entry with their list items
     * grouped under the list node name.
     *
     *
     * @param dataspaceName          the name of the dataspace
     * @param anchorName             the name of the anchor
     * @param xpath                  the xpath
     * @param fetchDescendantsOption control what level of descendants should be returned
     * @return                       a map where each key represents a data node name (e.g., container or list),
     *                               and each value is either:
     *                              - a leaf values as key-value pairs,
     *                              - a nested map (for containers),
     *                              - or a list of maps (for lists containing multiple elements)
     */

    Map<String, Object> getDataNodesByAnchorV3(String dataspaceName,
                                               String anchorName,
                                               String xpath,
                                               FetchDescendantsOption fetchDescendantsOption);

    /**
     * Query the given anchor using a cps path expression.
     *
     * @param dataspaceName          the name of the dataspace
     * @param anchorName             the name of the anchor
     * @param cpsPath                the xpath i.e. query
     * @param fetchDescendantsOption control what level of descendants should be returned
     * @return                       a map representing the data nodes and their descendants
     */
    List<Map<String, Object>> executeAnchorQuery(String dataspaceName,
                                                 String anchorName,
                                                 String cpsPath,
                                                 FetchDescendantsOption fetchDescendantsOption);

    /**
     * Query the given dataspace (all anchors) using a cps path expression.
     *
     * @param dataspaceName          the name of the dataspace
     * @param cpsPath                the xpath i.e. query
     * @param fetchDescendantsOption control what level of descendants should be returned
     * @return                        a map representing the data nodes and their descendants
     */
    List<Map<String, Object>> executeDataspaceQuery(String dataspaceName,
                                                    String cpsPath,
                                                    FetchDescendantsOption fetchDescendantsOption,
                                                    PaginationOption paginationOption);

    /**
     * Query how many anchors wil be returned for the given dataspace and a cps path query.
     *
     * @param dataspaceName    the name of the dataspace
     * @param cpsPath          the xpath i.e. query
     * @param paginationOption the options for pagination
     * @return                 the number of anchors involved in the output
     */
    int countAnchorsInDataspaceQuery(String dataspaceName,
                                     String cpsPath,
                                     PaginationOption paginationOption);
}
