/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.inventory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.model.ModuleDefinition;
import org.onap.cps.api.model.ModuleReference;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;

public interface InventoryPersistence extends NcmpPersistence {

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
     * This method retrieves DMI service name, DMI properties and the state for a given cm handle.
     *
     * @param cmHandleId the id of the cm handle
     * @return yang model cm handle
     */
    YangModelCmHandle getYangModelCmHandle(String cmHandleId);

    /**
     * This method retrieves YangModelCmHandles for a given collection of cm handle ids.
     *
     * @param cmHandleIds a list of the ids of the cm handles
     * @return collection of yang model cm handles
     */
    Collection<YangModelCmHandle> getYangModelCmHandles(Collection<String> cmHandleIds);

    /**
     * This method retrieves YangModelCmHandles for a given collection of cm handle ids.
     * This variant does not include the state, additional and private properties.
     *
     * @param cmHandleIds a list of the ids of the cm handles
     * @return collection of yang model cm handles
     */
    Collection<YangModelCmHandle> getYangModelCmHandlesWithoutProperties(Collection<String> cmHandleIds);

    /**
     * Method to return module definitions by cmHandleId.
     *
     * @param cmHandleId cm handle ID
     * @return a collection of module definitions (moduleName, revision and yang resource content)
     */
    Collection<ModuleDefinition> getModuleDefinitionsByCmHandleId(String cmHandleId);

    /**
     * Method to return module definitions for the given parameters.
     *
     * @param cmHandleId        cm-handle identifier
     * @param moduleName        module name
     * @param moduleRevision    the revision of the module
     * @return list of module definitions (module name, revision, yang resource content)
     */
    Collection<ModuleDefinition> getModuleDefinitionsByCmHandleAndModule(String cmHandleId,
                                                                         String moduleName,
                                                                         String moduleRevision);

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
    void saveCmHandleBatch(List<YangModelCmHandle> yangModelCmHandles);

    /**
     * Get data node with the given cm handle id.
     *
     * @param cmHandleId cmHandle ID
     * @param fetchDescendantsOption fetch descendants option
     * @return data node
     */
    Collection<DataNode> getCmHandleDataNodeByCmHandleId(String cmHandleId,
                                                         FetchDescendantsOption fetchDescendantsOption);

    /**
     * Get collection of data nodes of given cm handles.
     *
     * @param cmHandleIds collection of cmHandle IDs
     * @param fetchDescendantsOption fetch descendants option
     * @return collection of data nodes
     */
    Collection<DataNode> getCmHandleDataNodes(Collection<String> cmHandleIds,
                                              FetchDescendantsOption fetchDescendantsOption);

    /**
     * get CM handles that has given module names.
     *
     * @param moduleNamesForQuery module names
     * @param outputAlternateId   boolean for cm handle reference type either
     *                            cm handle id (false or null) or alternate id (true)
     * @return Collection of CM handle references
     */
    Collection<String> getCmHandleReferencesWithGivenModules(Collection<String> moduleNamesForQuery,
                                                             boolean outputAlternateId);

    /**
     * Check database if cm handle id exists if not return false.
     *
     * @param cmHandleId cmHandle Id
     * @return boolean
     */
    boolean isExistingCmHandleId(String cmHandleId);
}
