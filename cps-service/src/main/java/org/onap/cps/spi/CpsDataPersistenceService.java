/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2024 Nordix Foundation.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2022 Bell Canada
 *  Modifications Copyright (C) 2022-2023 TechMahindra Ltd.
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

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.api.parameters.PaginationOption;

/*
    Data Store interface that is responsible for handling yang data.
    Please follow guidelines in https://gerrit.nordix.org/#/c/onap/ccsdk/features/+/6698/19/cps/interface-proposal/src/main/java/cps/javadoc/spi/DataStoreService.java
    when adding methods.
 */
public interface CpsDataPersistenceService {

    /**
     * Store multiple datanodes at once.
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @param dataNodes     data nodes
     */
    void storeDataNodes(String dataspaceName, String anchorName, Collection<DataNode> dataNodes);


    /**
     * Add multiple children to a Fragment.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @param parentXpath   parent xpath
     * @param dataNodes     collection of dataNodes
     */
    void addChildDataNodes(String dataspaceName, String anchorName, String parentXpath, Collection<DataNode> dataNodes);

    /**
     * Adds list child elements to a Fragment.
     *
     * @param dataspaceName          dataspace name
     * @param anchorName             anchor name
     * @param parentNodeXpath        parent node xpath
     * @param listElementsCollection collection of data nodes representing list elements
     */
    void addListElements(String dataspaceName, String anchorName, String parentNodeXpath,
        Collection<DataNode> listElementsCollection);

    /**
     * Retrieves multiple datanodes for a single XPath for given dataspace and anchor.
     * Multiple data nodes are returned when xPath is set to root '/', otherwise single data node
     * is returned when a specific xpath is used (Example: /bookstore).
     *
     * @param dataspaceName          dataspace name
     * @param anchorName             anchor name
     * @param xpath                  one xpath
     * @param fetchDescendantsOption defines the scope of data to fetch: either single node or all the descendant nodes
     *                               (recursively) as well
     * @return collection of data node object
     */
    Collection<DataNode> getDataNodes(String dataspaceName, String anchorName, String xpath,
                                      FetchDescendantsOption fetchDescendantsOption);

    /**
     * Retrieves multiple datanodes for multiple XPaths, given a dataspace and anchor.
     *
     * @param dataspaceName           dataspace name
     * @param anchorName              anchor name
     * @param xpaths                  collection of xpaths
     * @param fetchDescendantsOption  defines the scope of data to fetch: either single node or all the descendant nodes
     *                                (recursively) as well
     * @return collection of data node object
     */
    Collection<DataNode> getDataNodesForMultipleXpaths(String dataspaceName, String anchorName,
                                                       Collection<String> xpaths,
                                                       FetchDescendantsOption fetchDescendantsOption);

    /**
     * Updates data leaves for multiple data nodes.
     *
     * @param dataspaceName              dataspace name
     * @param anchorName                 anchor name
     * @param updatedLeavesPerXPath      Map of xPaths to updated leaf nodes
     */
    void batchUpdateDataLeaves(String dataspaceName, String anchorName,
                               Map<String, Map<String, Serializable>> updatedLeavesPerXPath);

    /**
     * Replaces multiple existing data nodes' content including descendants in a batch operation.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @param dataNodes     data nodes
     */
    void updateDataNodesAndDescendants(String dataspaceName, String anchorName, final Collection<DataNode> dataNodes);

    /**
     * Replaces list content by removing all existing elements and inserting the given new elements
     * under given parent, anchor and dataspace.
     *
     * @param dataspaceName   dataspace name
     * @param anchorName      anchor name
     * @param parentNodeXpath parent node xpath
     * @param newListElements collection of data nodes representing the new list content
     */
    void replaceListContent(String dataspaceName, String anchorName,
                            String parentNodeXpath, Collection<DataNode> newListElements);

    /**
     * Deletes any dataNode, yang container or yang list or yang list element.
     *
     * @param dataspaceName   dataspace name
     * @param anchorName      anchor name
     * @param targetXpath     xpath to list or list element (include [@key=value] to delete a single list element)
     */
    void deleteDataNode(String dataspaceName, String anchorName, String targetXpath);

    /**
     * Deletes multiple dataNode, yang container or yang list or yang list element.
     *
     * @param dataspaceName   dataspace name
     * @param anchorName      anchor name
     * @param targetXpaths    xpaths of nodes to delete
     */
    void deleteDataNodes(String dataspaceName, String anchorName, Collection<String> targetXpaths);

    /**
     * Deletes all dataNodes in a given anchor.
     *
     * @param dataspaceName   dataspace name
     * @param anchorName      anchor name
     */
    void deleteDataNodes(String dataspaceName, String anchorName);

    /**
     * Deletes all dataNodes in multiple anchors.
     *
     * @param dataspaceName   dataspace name
     * @param anchorNames     anchor names
     */
    void deleteDataNodes(String dataspaceName, Collection<String> anchorNames);

    /**
     * Deletes a single existing list element or the whole list.
     *
     * @param dataspaceName   dataspace name
     * @param anchorName      anchor name
     * @param targetXpath     xpath to list or list element (include [@key=value] to delete a single list element)
     */
    void deleteListDataNode(String dataspaceName, String anchorName, String targetXpath);

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
    List<DataNode> queryDataNodes(String dataspaceName, String anchorName,
                                  String cpsPath, FetchDescendantsOption fetchDescendantsOption);

    /**
     * Get a datanode by dataspace name and cps path across all anchors.
     *
     * @param dataspaceName          dataspace name
     * @param cpsPath                cps path
     * @param fetchDescendantsOption defines whether the descendants of the node(s) found by the query should be
     *                               included in the output
     * @param paginationOption pagination option
     * @return the data nodes found i.e. 0 or more data nodes
     */
    List<DataNode> queryDataNodesAcrossAnchors(String dataspaceName,
                                  String cpsPath, FetchDescendantsOption fetchDescendantsOption,
                                  PaginationOption paginationOption);

    /**
     * Starts a session which allows use of locks and batch interaction with the persistence service.
     *
     * @return Session ID string
     */
    String startSession();

    /**
     * Close session.
     *
     * @param sessionId session ID
     */
    void closeSession(String sessionId);

    /**
     * Lock anchor.
     * To release locks(s), the session holding the lock(s) must be closed.
     *
     * @param sessionID session ID
     * @param dataspaceName dataspace name
     * @param anchorName anchor name
     * @param timeoutInMilliseconds lock attempt timeout in milliseconds
     */
    void lockAnchor(String sessionID, String dataspaceName, String anchorName, Long timeoutInMilliseconds);

    /**
     * Query total anchors for dataspace name and cps path.
     * @param dataspaceName datasoace name
     * @param cpsPath cps path
     * @return total anchors for dataspace name and cps path
     */
    Integer countAnchorsForDataspaceAndCpsPath(String dataspaceName, String cpsPath);
}
