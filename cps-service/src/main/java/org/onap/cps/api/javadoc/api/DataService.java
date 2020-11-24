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


import org.onap.cps.api.javadoc.model.Anchor;
import org.onap.cps.api.javadoc.model.Dataspace;

/*
 * Data interface for handling CPS data.
 */
public interface DataService {

    /**
     * Create a dataspace.
     *
     * @param dataspaceName dataspace name
     */
    void createDataspace(String dataspaceName);

    /**
     * Read all dataspaces in the system.
     *
     * @return dataspace names
     */
    Dataspace getDataspaces();

    /**
     * Read the attributes of one dataspace.
     *
     * @param dataspaceName dataspace name
     * @return dataspace names
     */
    Dataspace getDataspace(String dataspaceName);

    /**
     * Delete a dataspace. This will delete the everything associated with this dataspace.
     *
     * @param dataspaceName dataspace name
     */
    void deleteDataspace(String dataspaceName);

    /**
     * Create an anchor in the given dataspace.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     */
    void createAnchor(String dataspaceName, String anchorName);

    /**
     * Read all anchors in the given a dataspace.
     *
     * @param dataspaceName dataspace name
     * @return anchors
     */
    Anchor getAnchors(String dataspaceName);


    /**
     * Read an anchor and the associated attributes given a anchor name and a dataspace.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @return an anchor and the associated attributes
     */
    Anchor getAnchor(String dataspaceName, String anchorName);

    /**
     * Delete an anchor in the given dataspace.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     */
    void deleteAnchor(String dataspaceName, String anchorName);

    /**
     * Create a data node.
     *
     * @param anchorName    the anchor name
     * @param dataspaceName the dataspace name
     * @param dataNodeName  the data node name
     */
    void createDataNode(String dataspaceName, String dataNodeName, String anchorName);

    /**
     * Associate anchor to a module set.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @param moduleSet     module set
     */
    void associateAnchorToModuleSet(String dataspaceName, String moduleSet, String anchorName);

    /**
     * Associate a data node To a dataspace.
     *
     * @param dataspaceName dataspace name
     * @param anchorName    anchor name
     * @param dataNodeName  data node name
     */
    void associateAnchorToDataspace(String dataspaceName, String anchorName, String dataNodeName);

    /**
     * Associate a data node To a anchor.
     *
     * @param dataspaceName dataspace name
     * @param namespace     namespace
     * @param dataNodeName  data node name
     */
    void associateDataNodeToAnchor(String dataspaceName, String namespace, String dataNodeName);
}
