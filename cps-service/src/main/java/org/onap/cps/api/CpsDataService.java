/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Bell Canada
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

import java.time.OffsetDateTime;
import java.util.Collection;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.CmHandleQueryParameters;
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
     * @param observedTimestamp observedTimestamp
     */
    void saveData(String dataspaceName, String anchorName, String jsonData, OffsetDateTime observedTimestamp);

    /**
     * Persists child data fragment under existing data node for the given anchor and dataspace.
     *
     * @param dataspaceName   dataspace name
     * @param anchorName      anchor name
     * @param parentNodeXpath parent node xpath
     * @param jsonData        json data
     * @param observedTimestamp observedTimestamp
     */
    void saveData(String dataspaceName, String anchorName, String parentNodeXpath, String jsonData,
        OffsetDateTime observedTimestamp);

    /**
     * Persists child data fragment representing one or more list elements under existing data node for the
     * given anchor and dataspace.
     *
     * @param dataspaceName     dataspace name
     * @param anchorName        anchor name
     * @param parentNodeXpath   parent node xpath
     * @param jsonData          json data representing list element(s)
     * @param observedTimestamp observedTimestamp
     */
    void saveListElements(String dataspaceName, String anchorName, String parentNodeXpath, String jsonData,
        OffsetDateTime observedTimestamp);

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
    DataNode getDataNode(String dataspaceName, String anchorName, String xpath,
        FetchDescendantsOption fetchDescendantsOption);

    /**
     * Updates data node for given dataspace and anchor using xpath to parent node.
     *
     * @param dataspaceName   dataspace name
     * @param anchorName      anchor name
     * @param parentNodeXpath xpath to parent node
     * @param jsonData        json data
     * @param observedTimestamp observedTimestamp
     */
    void updateNodeLeaves(String dataspaceName, String anchorName, String parentNodeXpath, String jsonData,
        OffsetDateTime observedTimestamp);

    /**
     * Replaces existing data node content including descendants.
     *
     * @param dataspaceName   dataspace name
     * @param anchorName      anchor name
     * @param parentNodeXpath xpath to parent node
     * @param jsonData        json data
     * @param observedTimestamp observedTimestamp
     */
    void replaceNodeTree(String dataspaceName, String anchorName, String parentNodeXpath, String jsonData,
        OffsetDateTime observedTimestamp);

    /**
     * Replaces list content by removing all existing elements and inserting the given new elements as json
     * under given parent, anchor and dataspace.
     *
     * @param dataspaceName     dataspace name
     * @param anchorName        anchor name
     * @param parentNodeXpath   parent node xpath
     * @param jsonData          json data representing the new list elements
     * @param observedTimestamp observedTimestamp
     */
    void replaceListContent(String dataspaceName, String anchorName, String parentNodeXpath, String jsonData,
        OffsetDateTime observedTimestamp);

    /**
     * Replaces list content by removing all existing elements and inserting the given new elements as data nodes
     * under given parent, anchor and dataspace.
     *
     * @param dataspaceName     dataspace-name
     * @param anchorName        anchor name
     * @param parentNodeXpath   parent node xpath
     * @param dataNodes         datanodes representing the updated data
     * @param observedTimestamp observedTimestamp
     */
    void replaceListContent(String dataspaceName, String anchorName, String parentNodeXpath,
            Collection<DataNode> dataNodes, OffsetDateTime observedTimestamp);

    /**
     * Deletes data node for given anchor and dataspace.
     *
     * @param dataspaceName dataspace name
     * @param anchorName anchor name
     * @param dataNodeXpath data node xpath
     * @param observedTimestamp observed timestamp
     */
    void deleteDataNode(String dataspaceName, String anchorName, String dataNodeXpath,
        OffsetDateTime observedTimestamp);

    /**
     * Deletes all data nodes for a given anchor in a dataspace.
     *
     * @param dataspaceName     dataspace name
     * @param anchorName       anchor name
     * @param observedTimestamp observed timestamp
     */
    void deleteDataNodes(String dataspaceName, String anchorName, OffsetDateTime observedTimestamp);

    /**
     * Deletes a list or a list-element under given anchor and dataspace.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @param listElementXpath list element xpath
     * @param observedTimestamp observedTimestamp
     */
    void deleteListOrListElement(String dataspaceName, String anchorName, String listElementXpath,
        OffsetDateTime observedTimestamp);

    /**
     * Updates leaves of DataNode for given dataspace and anchor using xpath, along with the leaves of each Child Data
     * Node which already exists. This method will throw an exception if data node update or any descendant update does
     * not exist.
     *
     * @param dataspaceName         dataspace name
     * @param anchorName            anchor name
     * @param parentNodeXpath       xpath
     * @param dataNodeUpdatesAsJson json data representing data node updates
     * @param observedTimestamp observedTimestamp
     */
    void updateNodeLeavesAndExistingDescendantLeaves(String dataspaceName, String anchorName, String parentNodeXpath,
        String dataNodeUpdatesAsJson, OffsetDateTime observedTimestamp);

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
     *
     */
    void closeSession(String sessionId);

    /**
     * Lock anchor with default timeout.
     * To release locks(s), the session holding the lock(s) must be closed.
     *
     * @param sessionID session ID
     * @param dataspaceName dataspace name
     * @param anchorName anchor name
     */
    void lockAnchor(String sessionID, String dataspaceName, String anchorName);

    /**
     * Lock anchor with custom timeout.
     * To release locks(s), the session holding the lock(s) must be closed.
     *
     * @param sessionID session ID
     * @param dataspaceName dataspace name
     * @param anchorName anchor name
     * @param timeoutInMilliseconds lock attempt timeout in milliseconds
     */
    void lockAnchor(String sessionID, String dataspaceName, String anchorName, Long timeoutInMilliseconds);

    /**
     * Query and return cm handles that match the given query parameters.
     *
     * @param cmHandleQueryParameters the cm handle query parameters
     * @return collection of cm handles
     */
    Collection<DataNode> queryCmHandles(CmHandleQueryParameters cmHandleQueryParameters);
}
