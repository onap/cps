/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
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

package org.onap.cps.ncmp.api;

import java.util.Collection;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.onap.cps.ncmp.api.models.DmiPluginRegistration;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;

/*
 * Datastore interface for handling CPS data.
 */
public interface NetworkCmProxyDataService {

    /**
     * Retrieves datanode by XPath for a given cm handle.
     *
     * @param cmHandle               The identifier for a network function, network element, subnetwork or any other cm
     *                               object by managed Network CM Proxy
     * @param xpath                  xpath
     * @param fetchDescendantsOption defines the scope of data to fetch: either single node or all the descendant nodes
     *                               (recursively) as well
     * @return data node object
     */
    DataNode getDataNode(@NonNull String cmHandle, @NonNull String xpath,
        @NonNull FetchDescendantsOption fetchDescendantsOption);

    /**
     * Get datanodes for the given cm handle by cps path.
     *
     * @param cmHandle               The identifier for a network function, network element, subnetwork or any other cm
     *                               object by managed Network CM Proxy
     * @param cpsPath                cps path
     * @param fetchDescendantsOption defines whether the descendants of the node(s) found by the query should be
     *                               included in the output
     * @return a collection of datanodes
     */
    Collection<DataNode> queryDataNodes(@NonNull String cmHandle, @NonNull String cpsPath,
        @NonNull FetchDescendantsOption fetchDescendantsOption);

    /**
     * Creates data node with descendants at root level or under existing node (if parent node xpath is provided).
     *
     * @param cmHandle        The identifier for a network function, network element, subnetwork or any other cm
     *                        object managed by Network CM Proxy
     * @param parentNodeXpath xpath to parent node or '/' for root level
     * @param jsonData        data as JSON string
     */
    void createDataNode(@NonNull String cmHandle, @NonNull String parentNodeXpath, @NonNull String jsonData);

    /**
     * Creates one or more child node elements with descendants under existing node from list-node data fragment.
     *
     * @param cmHandle        The identifier for a network function, network element, subnetwork or any other cm
     *                        object managed by Network CM Proxy
     * @param parentNodeXpath xpath to parent node
     * @param jsonData        data as JSON string
     */
    void addListNodeElements(@NonNull String cmHandle, @NonNull String parentNodeXpath, @NonNull String jsonData);

    /**
     * Updates data node for given cm handle using xpath to parent node.
     *
     * @param cmHandle        The identifier for a network function, network element, subnetwork or any other cm object
     *                        by managed Network CM Proxy
     * @param parentNodeXpath xpath to parent node
     * @param jsonData        json data
     */
    void updateNodeLeaves(@NonNull String cmHandle, @NonNull String parentNodeXpath, @NonNull String jsonData);

    /**
     * Replaces existing data node content including descendants.
     *
     * @param cmHandle        The identifier for a network function, network element, subnetwork or any other cm object
     *                        by managed Network CM Proxy
     * @param parentNodeXpath xpath to parent node
     * @param jsonData        json data
     */
    void replaceNodeTree(@NonNull String cmHandle, @NonNull String parentNodeXpath, @NonNull String jsonData);

    /**
     * Registration of New CM Handles.
     *
     * @param dmiPluginRegistration Dmi Plugin Registration
     */
    void updateDmiPluginRegistration(DmiPluginRegistration dmiPluginRegistration);

}
