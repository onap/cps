/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
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

package org.onap.cps.api;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.DataNode;

/*
 * Datastore interface for handling CPS data.
 */
public interface CpsDataService {

    /**
     * Persists data for the given anchor and dataspace.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @param jsonData      json data
     * @throws DataValidationException when json data is invalid
     */
    void saveData(@NonNull String dataspaceName, @NonNull String anchorName, @NonNull String jsonData);

    /**
     * Persists child data fragment under existing data node for the given anchor and dataspace.
     *
     * @param dataspaceName   dataspace name
     * @param anchorName      anchor name
     * @param parentNodeXpath parent node xpath
     * @param jsonData        json data
     * @throws DataValidationException   when json data is invalid
     * @throws DataNodeNotFoundException when parent node cannot be found by parent node xpath
     * @throws AlreadyDefinedException   when child data node with same xpath already exists
     */
    void saveData(@NonNull String dataspaceName, @NonNull String anchorName, @NonNull String parentNodeXpath,
        @NonNull String jsonData);

    /**
     * Persists child data fragment representing list-node (with one or more elements) under existing data node
     * for the given anchor and dataspace.
     *
     * @param dataspaceName   dataspace name
     * @param anchorName      anchor name
     * @param parentNodeXpath parent node xpath
     * @param jsonData        json data representing list element
     * @throws DataValidationException   when json data is invalid (incl. list-node being empty)
     * @throws DataNodeNotFoundException when parent node cannot be found by parent node xpath
     * @throws AlreadyDefinedException   when any of child data nodes is having xpath of already existing node
     */
    void saveListNodeData(@NonNull String dataspaceName, @NonNull String anchorName, @NonNull String parentNodeXpath,
        @NonNull String jsonData);

    /**
     * Retrieves datanode by XPath for given dataspace and anchor.
     *
     * @param dataspaceName          dataspace name
     * @param anchorName             anchor name
     * @param xpath                  xpath
     * @param fetchDescendantsOption defines the scope of data to fetch: either single node or all the descendant nodes
     *                               (recursively) as well
     * @return data node object
     */
    DataNode getDataNode(@NonNull String dataspaceName, @NonNull String anchorName, @NonNull String xpath,
        @NonNull FetchDescendantsOption fetchDescendantsOption);

    /**
     * Updates data node for given dataspace and anchor using xpath to parent node.
     *
     * @param dataspaceName   dataspace name
     * @param anchorName      anchor name
     * @param parentNodeXpath xpath to parent node
     * @param jsonData        json data
     */
    void updateNodeLeaves(@NonNull String dataspaceName, @NonNull String anchorName, @NonNull String parentNodeXpath,
        @NonNull String jsonData);

    /**
     * Replaces existing data node content including descendants.
     *
     * @param dataspaceName   dataspace name
     * @param anchorName      anchor name
     * @param parentNodeXpath xpath to parent node
     * @param jsonData        json data
     */
    void replaceNodeTree(@NonNull String dataspaceName, @NonNull String anchorName, @NonNull String parentNodeXpath,
        @NonNull String jsonData);

    /**
     * Replaces (if exists) child data fragment representing list-node (with one or more elements)
     * under existing data node for the given anchor and dataspace.
     *
     * @param dataspaceName   dataspace name
     * @param anchorName      anchor name
     * @param parentNodeXpath parent node xpath
     * @param jsonData        json data representing list element
     * @throws DataValidationException   when json data is invalid (incl. list-node being empty)
     * @throws DataNodeNotFoundException when parent node cannot be found by parent node xpath
     */
    void replaceListNodeData(@NonNull String dataspaceName, @NonNull String anchorName, @NonNull String parentNodeXpath,
        @NonNull String jsonData);
}
