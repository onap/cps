/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2025 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2022-2023 Deutsche Telekom AG
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

import java.util.Collection;
import java.util.Set;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.api.parameters.PaginationOption;

/*
 * Query interface for handling cps queries.
 */
public interface CpsQueryService {

    int NO_LIMIT = 0;

    /**
     * Get data nodes for the given dataspace and anchor by cps path.
     *
     * @param dataspaceName          dataspace name
     * @param anchorName             anchor name
     * @param cpsPath                cps path
     * @param fetchDescendantsOption defines whether the descendants of the node(s) found by the query should be
     *                               included in the output
     * @return a collection of data nodes
     */
    Collection<DataNode> queryDataNodes(String dataspaceName, String anchorName,
                                        String cpsPath, FetchDescendantsOption fetchDescendantsOption);

    /**
     * Retrieves a collection of data nodes based on the specified CPS path query.
     *
     * @param dataspaceName the name of the dataspace (must not be null or empty)
     * @param anchorName the name of the anchor (must not be null or empty)
     * @param cpsPath the CPS path used for querying (must not be null or empty)
     * @param fetchDescendantsOption specifies whether to include descendant nodes in the output
     * @param queryResultLimit the maximum number of data nodes to return; if less than 1, returns all matching nodes
     *
     * @return a collection of matching {@link DataNode} instances (can be empty if no nodes are found)
     */
    Collection<DataNode> queryDataNodes(String dataspaceName, String anchorName,
                                        String cpsPath, FetchDescendantsOption fetchDescendantsOption,
                                        int queryResultLimit);

    /**
     * Get data leaf for the given dataspace and anchor by cps path.
     *
     * @param dataspaceName          dataspace name
     * @param anchorName             anchor name
     * @param cpsPath                cps path
     * @param targetClass            class of the expected data type
     * @return a collection of data objects of expected type
     */
    <T> Set<T> queryDataLeaf(String dataspaceName, String anchorName, String cpsPath, Class<T> targetClass);

    /**
     * Get data nodes for the given dataspace across all anchors by cps path.
     *
     * @param dataspaceName dataspace name
     * @param cpsPath CPS path
     * @param fetchDescendantsOption defines whether the descendants of the node(s) found by the query should be
     *                               included in the output
     * @param paginationOption pagination option
     * @return a collection of data nodes
     */
    Collection<DataNode> queryDataNodesAcrossAnchors(String dataspaceName, String cpsPath,
                                                     FetchDescendantsOption fetchDescendantsOption,
                                                     PaginationOption paginationOption);

    /**
     * Query total number of anchors for given dataspace name and cps path.
     * @param dataspaceName dataspace name
     * @param cpsPath cps path
     * @return total number of anchors for given dataspace name and cps path.
     */
    Integer countAnchorsForDataspaceAndCpsPath(String dataspaceName, String cpsPath);
}
