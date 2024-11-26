/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2024 Nordix Foundation
 *  Modifications Copyright (C) 2022-2023 TechMahindra Ltd.
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
