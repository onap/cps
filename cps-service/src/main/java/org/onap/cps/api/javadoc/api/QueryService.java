/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.api.javadoc.api;

import org.onap.cps.api.javadoc.model.DataNode;

/*
 * Query interface for handling queries.
 */
public interface QueryService {

    /**
     * Get a data node given an anchor for the given dataspace (return just one level with just cpsPath references to
     * its children).
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @param cpsPath       cps path
     * @return datanode  data node name
     */
    DataNode getDataNodesCpsPathForAnchor(final String dataspaceName, final String anchorName, final String cpsPath);

    /**
     * Get a data node (under any anchor) given a cpsPath expression for the given dataspace.
     *
     * @param dataspaceName dataspace name
     * @param cpsPath       cps path
     * @return datanode  data node name
     */
    DataNode getDataNodesCpsPathKey(final String dataspaceName, final String cpsPath);


    /**
     * Query data nodes (under any anchor) given a cpsPath expression for the given dataspace.
     *
     * @param dataspaceName dataspace name
     * @param cpsPath       cps path
     * @return datanode  data node name
     */
    DataNode queryDataNodesCpsPath(final String dataspaceName, final String cpsPath);

    /**
     * Get all the relevant data nodes given a schema node identifier for the given dataspace.
     *
     * @param dataspaceName        dataspace name
     * @param schemaNodeIdentifier schema node identifier
     * @return datanode  data node name
     */
    DataNode getDataNodeSchemaNodeIdentifier(final String dataspaceName, final String schemaNodeIdentifier);

}