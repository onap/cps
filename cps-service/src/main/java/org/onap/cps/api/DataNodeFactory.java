/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 Deutsche Telekom AG
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
import java.util.Map;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.utils.ContentType;

public interface DataNodeFactory {

    /**
     * Create data nodes using an anchor, xpath, and JSON/XML string.
     *
     * @param anchor        name of Anchor sharing same schema structure as the JSON/XML string
     * @param xpath         xpath of the data node
     * @param nodeData      JSON/XML data string
     * @param contentType   JSON or XML content type
     * @return              a collection of {@link DataNode}
     */
    Collection<DataNode> createDataNodesWithAnchorXpathAndNodeData(Anchor anchor, String xpath,
                                                                   String nodeData, ContentType contentType);

    /**
     * Create data nodes using an anchor, parent data node xpath, and JSON/XML string.
     *
     * @param anchor            name of Anchor sharing same schema structure as the JSON/XML string
     * @param parentNodeXpath   xpath of the parent data node
     * @param nodeData          JSON/XML data string
     * @param contentType       JSON or XML content type
     * @return                  a collection of {@link DataNode}
     */
    Collection<DataNode> createDataNodesWithAnchorParentXpathAndNodeData(Anchor anchor,
                                                                         String parentNodeXpath,
                                                                         String nodeData,
                                                                         ContentType contentType);

    /**
     * Create data nodes using a map of xpath to JSON/XML data, and anchor name.
     *
     * @param anchor      name of Anchor sharing same schema structure as the JSON/XML string
     * @param nodesData   map of xpath and node JSON/XML data
     * @param contentType JSON or XML content type
     * @return            a collection of {@link DataNode}
     */
    Collection<DataNode> createDataNodesWithAnchorAndXpathToNodeData(Anchor anchor,
                                                                     Map<String, String> nodesData,
                                                                     ContentType contentType);

    /**
     * Create data nodes using a map of YANG resource name to content, xpath, and JSON/XML string.
     *
     * @param yangResourcesNameToContentMap map of YANG resource name to content
     * @param xpath                         xpath of the data node
     * @param nodeData                      JSON/XML data string
     * @param contentType                   JSON or XML content type
     * @return                              a collection of {@link DataNode}
     */
    Collection<DataNode> createDataNodesWithYangResourceXpathAndNodeData(
                                                        Map<String, String> yangResourcesNameToContentMap,
                                                        String xpath, String nodeData,
                                                        ContentType contentType);

}
