/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation. All rights reserved.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.spi;

import java.util.Collection;
import java.util.Map;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.onap.cps.spi.model.DataNode;

/*
    Data Store interface that is responsible for handling yang data.
    Please follow guidelines in https://gerrit.nordix.org/#/c/onap/ccsdk/features/+/6698/19/cps/interface-proposal/src/main/java/cps/javadoc/spi/DataStoreService.java
    when adding methods.
 */
public interface CpsDataPersistenceService {

    /**
     * Store a datanode.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @param dataNode      data node
     */
    void storeDataNode(@NonNull String dataspaceName, @NonNull String anchorName,
        @NonNull DataNode dataNode);

    /**
     * Add a child to a Fragment.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @param parentXpath   parent xpath
     * @param dataNode      dataNode
     */
    void addChildDataNode(@NonNull String dataspaceName, @NonNull String anchorName, @NonNull String parentXpath,
        @NonNull DataNode dataNode);

    /**
     * Adds list node child elements to a Fragment.
     *
     * @param dataspaceName       dataspace name
     * @param anchorName          anchor name
     * @param parentNodeXpath     parent node xpath
     * @param dataNodesCollection collection of data nodes representing list node elements
     */

    void addListDataNodes(@NonNull String dataspaceName, @NonNull String anchorName, @NonNull String parentNodeXpath,
        @NonNull Collection<DataNode> dataNodesCollection);

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
     * Updates leaves for existing data node.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @param xpath         xpath
     * @param leaves        the leaves as a map where key is a leaf name and a value is a leaf value
     */
    void updateDataLeaves(@NonNull String dataspaceName, @NonNull String anchorName, @NonNull String xpath,
        @NonNull Map<String, Object> leaves);

    /**
     * Replaces existing data node content including descendants.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @param dataNode      data node
     */
    void replaceDataNodeTree(@NonNull String dataspaceName, @NonNull String anchorName, @NonNull DataNode dataNode);

    /**
     * Get a datanode by cps path.
     *
     * @param dataspaceName          dataspace name
     * @param anchorName             anchor name
     * @param cpsPath                cps path
     * @param fetchDescendantsOption defines whether the descendants of the node(s) found by the query should be
     *                               included in the output
     * @return the data nodes found i.e. 0 or more data nodes
     */
    Collection<DataNode> queryDataNodes(@NonNull String dataspaceName, @NonNull String anchorName,
        @NonNull String cpsPath, @NonNull FetchDescendantsOption fetchDescendantsOption);

}
