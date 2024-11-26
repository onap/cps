/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2024 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Bell Canada
 *  Modifications Copyright (C) 2022 Deutsche Telekom AG
 *  Modifications Copyright (C) 2023-2024 TechMahindra Ltd.
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
import java.util.List;
import java.util.Map;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.model.DeltaReport;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.utils.ContentType;

/*
 * Datastore interface for handling CPS data.
 */
public interface CpsDataService {

    /**
     * Persists data for the given anchor and dataspace.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @param nodeData      node data
     * @param observedTimestamp observedTimestamp
     */
    void saveData(String dataspaceName, String anchorName, String nodeData, OffsetDateTime observedTimestamp);

    /**
     * Persists data for the given anchor and dataspace.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @param nodeData      node data
     * @param observedTimestamp observedTimestamp
     * @param contentType       JSON/XML content type
     */
    void saveData(String dataspaceName, String anchorName, String nodeData, OffsetDateTime observedTimestamp,
                  ContentType contentType);

    /**
     * Persists child data fragment under existing data node for the given anchor and dataspace.
     *
     * @param dataspaceName   dataspace name
     * @param anchorName      anchor name
     * @param parentNodeXpath parent node xpath
     * @param nodeData        node data
     * @param observedTimestamp observedTimestamp
     */
    void saveData(String dataspaceName, String anchorName, String parentNodeXpath, String nodeData,
                  OffsetDateTime observedTimestamp);

    /**
     * Persists child data fragment under existing data node for the given anchor, dataspace and content type.
     *
     * @param dataspaceName     dataspace name
     * @param anchorName        anchor name
     * @param parentNodeXpath   parent node xpath
     * @param nodeData          node data
     * @param observedTimestamp observedTimestamp
     * @param contentType       JSON/XML content type
     *
     */
    void saveData(String dataspaceName, String anchorName, String parentNodeXpath, String nodeData,
                  OffsetDateTime observedTimestamp, ContentType contentType);

    /**
     * Persists child data fragment representing one or more list elements under existing data node for the
     * given anchor and dataspace.
     *
     * @param dataspaceName     dataspace name
     * @param anchorName        anchor name
     * @param parentNodeXpath   parent node xpath
     * @param nodeData          node data representing list element(s)
     * @param observedTimestamp observedTimestamp
     * @param contentType       JSON/XML content type
     */
    void saveListElements(String dataspaceName, String anchorName, String parentNodeXpath, String nodeData,
        OffsetDateTime observedTimestamp, ContentType contentType);

    /**
     * Retrieves all the datanodes by XPath for given dataspace and anchor.
     *
     * @param dataspaceName           dataspace name
     * @param anchorName              anchor name
     * @param xpath                   xpath
     * @param fetchDescendantsOption  defines the scope of data to fetch: either single node or all the descendant nodes
     *                                (recursively) as well
     * @return collection of data node objects
     */
    Collection<DataNode> getDataNodes(String dataspaceName, String anchorName, String xpath,
                                      FetchDescendantsOption fetchDescendantsOption);

    /**
     * Retrieves all the datanodes for multiple XPaths for given dataspace and anchor.
     *
     * @param dataspaceName           dataspace name
     * @param anchorName              anchor name
     * @param xpaths                  collection of xpaths
     * @param fetchDescendantsOption  defines the scope of data to fetch: either single node or all the descendant nodes
     *                                (recursively) as well
     * @return collection of data node objects
     */
    Collection<DataNode> getDataNodesForMultipleXpaths(String dataspaceName, String anchorName,
                                                       Collection<String> xpaths,
                                                       FetchDescendantsOption fetchDescendantsOption);

    /**
     * Updates multiple data nodes for given dataspace and anchor using xpath to parent node.
     *
     * @param dataspaceName   dataspace name
     * @param anchorName      anchor name
     * @param parentNodeXpath xpath to parent node
     * @param nodeData        node data
     * @param observedTimestamp observedTimestamp
     * @param contentType       JSON/XML content type
     */
    void updateNodeLeaves(String dataspaceName, String anchorName, String parentNodeXpath, String nodeData,
        OffsetDateTime observedTimestamp, ContentType contentType);

    /**
     * Replaces an existing data node's content including descendants.
     *
     * @param dataspaceName     dataspace name
     * @param anchorName        anchor name
     * @param parentNodeXpath   xpath to parent node
     * @param nodeData          node data
     * @param observedTimestamp observedTimestamp
     * @param contentType       JSON/XML content type
     */
    void updateDataNodeAndDescendants(String dataspaceName, String anchorName, String parentNodeXpath, String nodeData,
                                       OffsetDateTime observedTimestamp, ContentType contentType);

    /**
     * Replaces multiple existing data nodes' content including descendants in a batch operation.
     *
     * @param dataspaceName   dataspace name
     * @param anchorName      anchor name
     * @param nodeDataPerXPath   map of xpath and node JSON/XML data
     * @param observedTimestamp observedTimestamp
     * @param contentType       JSON/XML content type
     */
    void updateDataNodesAndDescendants(String dataspaceName, String anchorName, Map<String, String> nodeDataPerXPath,
                                       OffsetDateTime observedTimestamp, ContentType contentType);

    /**
     * Replaces list content by removing all existing elements and inserting the given new elements as json
     * under given parent, anchor and dataspace.
     *
     * @param dataspaceName     dataspace name
     * @param anchorName        anchor name
     * @param parentNodeXpath   parent node xpath
     * @param nodeData          node data representing the new list elements
     * @param observedTimestamp observedTimestamp
     * @param contentType       JSON/XML content type
     */
    void replaceListContent(String dataspaceName, String anchorName, String parentNodeXpath, String nodeData,
        OffsetDateTime observedTimestamp, ContentType contentType);

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
     * @param dataspaceName     dataspace name
     * @param anchorName        anchor name
     * @param dataNodeXpath     data node xpath
     * @param observedTimestamp observed timestamp
     */
    void deleteDataNode(String dataspaceName, String anchorName, String dataNodeXpath,
        OffsetDateTime observedTimestamp);

    /**
     * Deletes multiple data nodes for given anchor and dataspace.
     *
     * @param dataspaceName     dataspace name
     * @param anchorName        anchor name
     * @param dataNodeXpaths    data node xpaths
     * @param observedTimestamp observed timestamp
     */
    void deleteDataNodes(String dataspaceName, String anchorName, Collection<String> dataNodeXpaths,
                         OffsetDateTime observedTimestamp);

    /**
     * Deletes all data nodes for a given anchor in a dataspace.
     *
     * @param dataspaceName     dataspace name
     * @param anchorName        anchor name
     * @param observedTimestamp observed timestamp
     */
    void deleteDataNodes(String dataspaceName, String anchorName, OffsetDateTime observedTimestamp);

    /**
     * Deletes all data nodes for multiple anchors in a dataspace.
     *
     * @param dataspaceName     dataspace name
     * @param anchorNames       anchor names
     * @param observedTimestamp observed timestamp
     */
    void deleteDataNodes(String dataspaceName, Collection<String> anchorNames, OffsetDateTime observedTimestamp);

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
     * Retrieves the delta between two anchors by xpath within a dataspace.
     *
     * @param dataspaceName          dataspace name
     * @param sourceAnchorName       source anchor name
     * @param targetAnchorName       target anchor name
     * @param xpath                  xpath
     * @param fetchDescendantsOption defines the scope of data to fetch: either single node or all the descendant
     *                               nodes (recursively) as well
     * @return                       list containing {@link DeltaReport} objects
     */
    List<DeltaReport> getDeltaByDataspaceAndAnchors(String dataspaceName, String sourceAnchorName,
                                                    String targetAnchorName, String xpath,
                                                    FetchDescendantsOption fetchDescendantsOption);

    /**
     * Retrieves the delta between an anchor and JSON payload by xpath, using dataspace name and anchor name.
     *
     * @param dataspaceName                     source dataspace name
     * @param sourceAnchorName                  source anchor name
     * @param xpath                             xpath
     * @param yangResourcesNameToContentMap     YANG resources (files) map where key is a name and value is content
     * @param targetData                        target data to be compared in JSON string format
     * @param fetchDescendantsOption            defines the scope of data to fetch: defaulted to INCLUDE_ALL_DESCENDANTS
     * @return                                  list containing {@link DeltaReport} objects
     */
    List<DeltaReport> getDeltaByDataspaceAnchorAndPayload(String dataspaceName, String sourceAnchorName, String xpath,
                                                          Map<String, String> yangResourcesNameToContentMap,
                                                          String targetData,
                                                          FetchDescendantsOption fetchDescendantsOption);


    /**
     * Validates JSON or XML data by parsing it using the schema associated to an anchor within the given dataspace.
     * Validation is performed without persisting the data.
     *
     * @param dataspaceName     the name of the dataspace where the anchor is located.
     * @param anchorName        the name of the anchor used to validate the data.
     * @param parentNodeXpath   the xpath of the parent node where the data is to be validated.
     * @param nodeData          the JSON or XML data to be validated.
     * @param contentType       the content type of the data (e.g., JSON or XML).
     */
    void validateData(String dataspaceName, String anchorName, String parentNodeXpath, String nodeData,
                                 ContentType contentType);
}
