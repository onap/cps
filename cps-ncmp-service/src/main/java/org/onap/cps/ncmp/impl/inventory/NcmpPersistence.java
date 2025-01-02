/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022-2025 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
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

package org.onap.cps.ncmp.impl.inventory;

import java.time.OffsetDateTime;
import java.util.Collection;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.parameters.FetchDescendantsOption;

public interface NcmpPersistence {

    String NCMP_DATASPACE_NAME = "NCMP-Admin";
    String NCMP_DMI_REGISTRY_ANCHOR = "ncmp-dmi-registry";
    String NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME = "NFP-Operational";
    String NCMP_DMI_REGISTRY_PARENT = "/dmi-registry";
    OffsetDateTime NO_TIMESTAMP = null;

    /**
     * Method to delete a list or a list element.
     *
     * @param listElementXpath list element xPath
     */
    void deleteListOrListElement(String listElementXpath);

    /**
     * Get data node via xpath.
     *
     * @param xpath xpath
     * @return data node
     */
    Collection<DataNode> getDataNode(String xpath);

    /**
     * Get data node via xpath.
     *
     * @param xpath                  xpath
     * @param fetchDescendantsOption fetch descendants option
     * @return data node
     */
    Collection<DataNode> getDataNode(String xpath, FetchDescendantsOption fetchDescendantsOption);

    /**
     * Get collection of data nodes via xpaths.
     *
     * @param xpaths collection of xpaths
     * @return collection of data nodes
     */
    Collection<DataNode> getDataNodes(Collection<String> xpaths);

    /**
     * Get collection of data nodes via xpaths.
     *
     * @param xpaths                 collection of xpaths
     * @param fetchDescendantsOption fetch descendants option
     * @return collection of data nodes
     */
    Collection<DataNode> getDataNodes(Collection<String> xpaths,
                                              FetchDescendantsOption fetchDescendantsOption);

    /**
     * Replaces list content by removing all existing elements and inserting the given new elements as data nodes.
     *
     * @param parentNodeXpath parent node xpath
     * @param dataNodes       datanodes representing the updated data
     */
    void replaceListContent(String parentNodeXpath, Collection<DataNode> dataNodes);

    /**
     * Deletes data node.
     *
     * @param dataNodeXpath data node xpath
     */
    void deleteDataNode(String dataNodeXpath);

    /**
     * Deletes multiple data nodes.
     *
     * @param dataNodeXpaths data node xpaths
     */
    void deleteDataNodes(Collection<String> dataNodeXpaths);

    /**
     * Deletes multiple anchors.
     *
     * @param anchorIds ids of the anchors to be deleted
     */
    void deleteAnchors(Collection<String> anchorIds);

}
