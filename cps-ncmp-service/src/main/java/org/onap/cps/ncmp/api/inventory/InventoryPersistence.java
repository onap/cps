/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

package org.onap.cps.ncmp.api.inventory;

import java.util.Collection;
import java.util.Map;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelSubscriptionEvent;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.ModuleDefinition;
import org.onap.cps.spi.model.ModuleReference;

public interface InventoryPersistence {

    /**
     * Get the Cm Handle Composite State from the data node.
     *
     * @param cmHandleId cm handle id
     * @return the cm handle composite state
     */
    CompositeState getCmHandleState(String cmHandleId);

    /**
     * Save the cm handles state.
     *
     * @param cmHandleId     cm handle id
     * @param compositeState composite state
     */
    void saveCmHandleState(String cmHandleId, CompositeState compositeState);

    /**
     * Save all cm handles states in batch.
     *
     * @param cmHandleStatePerCmHandleId contains cm handle id and updated state
     */
    void saveCmHandleStateBatch(Map<String, CompositeState> cmHandleStatePerCmHandleId);

    /**
     * Method to save subscription event.
     *
     * @param yangModelSubscriptionEvent subscription event represented as Yang Model
     */
    void saveYangModelSubscription(YangModelSubscriptionEvent yangModelSubscriptionEvent);

    /**
     * This method retrieves DMI service name, DMI properties and the state for a given cm handle.
     *
     * @param cmHandleId the id of the cm handle
     * @return yang model cm handle
     */
    YangModelCmHandle getYangModelCmHandle(String cmHandleId);

    /**
     * This method retrieves DMI service name, DMI properties and the state for a given cm handle.
     *
     * @param cmHandleIds a list of the ids of the cm handles
     * @return collection of yang model cm handles
     */
    Collection<YangModelCmHandle> getYangModelCmHandles(Collection<String> cmHandleIds);

    /**
     * Method to return module definitions by cmHandleId.
     *
     * @param cmHandleId cm handle ID
     * @return a collection of module definitions (moduleName, revision and yang resource content)
     */
    Collection<ModuleDefinition> getModuleDefinitionsByCmHandleId(String cmHandleId);

    /**
     * Method to return module references by cmHandleId.
     *
     * @param cmHandleId cm handle ID
     * @return a collection of module references (moduleName and revision)
     */
    Collection<ModuleReference> getYangResourcesModuleReferences(String cmHandleId);

    /**
     * Method to save cmHandle.
     *
     * @param yangModelCmHandle cmHandle represented as Yang Model
     */
    void saveCmHandle(YangModelCmHandle yangModelCmHandle);

    /**
     * Method to save batch of cm handles.
     *
     * @param yangModelCmHandles cm handle represented as Yang Models
     */
    void saveCmHandleBatch(Collection<YangModelCmHandle> yangModelCmHandles);

    /**
     * Method to delete a list or a list element.
     *
     * @param listElementXpath list element xPath
     */
    void deleteListOrListElement(String listElementXpath);

    /**
     * Method to delete a schema set.
     *
     * @param schemaSetName schema set name
     */
    void deleteSchemaSetWithCascade(String schemaSetName);

    /**
     * Method to delete multiple schema sets.
     *
     * @param schemaSetNames schema set names
     */
    void deleteSchemaSetsWithCascade(Collection<String> schemaSetNames);

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
     * @param xpath xpath
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
     * @param xpaths collection of xpaths
     * @param fetchDescendantsOption fetch descendants option
     * @return collection of data nodes
     */
    Collection<DataNode> getDataNodes(Collection<String> xpaths, FetchDescendantsOption fetchDescendantsOption);

    /**
     * Get data node of given cm handle.
     *
     * @param cmHandleId cmHandle ID
     * @return data node
     */
    Collection<DataNode> getCmHandleDataNode(String cmHandleId);

    /**
     * Get collection of data nodes of given cm handles.
     *
     * @param cmHandleIds collection of cmHandle IDs
     * @return collection of data nodes
     */
    Collection<DataNode> getCmHandleDataNodes(Collection<String> cmHandleIds);

    /**
     * get CM handles that has given module names.
     *
     * @param moduleNamesForQuery module names
     * @return Collection of CM handle Ids
     */
    Collection<String> getCmHandleIdsWithGivenModules(Collection<String> moduleNamesForQuery);

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
}
