/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2022 Bell Canada
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
import java.util.Map;
import org.onap.cps.ncmp.api.impl.inventory.CompositeState;
import org.onap.cps.ncmp.api.impl.operations.OperationType;
import org.onap.cps.ncmp.api.models.CmHandleQueryApiParameters;
import org.onap.cps.ncmp.api.models.CmHandleQueryServiceParameters;
import org.onap.cps.ncmp.api.models.DataOperationRequest;
import org.onap.cps.ncmp.api.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.models.DmiPluginRegistrationResponse;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.ModuleDefinition;
import org.onap.cps.spi.model.ModuleReference;

/*
 * Datastore interface for handling CPS data.
 */
public interface NetworkCmProxyDataService {

    /**
     * Registration of New CM Handles.
     *
     * @param dmiPluginRegistration Dmi Plugin Registration
     * @return dmiPluginRegistrationResponse
     */
    DmiPluginRegistrationResponse updateDmiRegistrationAndSyncModule(DmiPluginRegistration dmiPluginRegistration);

    /**
     * Get resource data for given data store using dmi.
     *
     * @param datastoreName       datastore name
     * @param cmHandleId          cm handle identifier
     * @param resourceIdentifier  resource identifier
     * @param optionsParamInQuery options query
     * @param topicParamInQuery   topic name for (triggering) async responses
     * @param requestId           unique requestId for async request
     * @return {@code Object} resource data
     */
    Object getResourceDataForCmHandle(String datastoreName,
                                      String cmHandleId,
                                      String resourceIdentifier,
                                      String optionsParamInQuery,
                                      String topicParamInQuery,
                                      String requestId);

    /**
     * Get resource data for operational.
     *
     * @param datastoreName      datastore name
     * @param cmHandleId         cm handle identifier
     * @param resourceIdentifier resource identifier
     * @Link FetchDescendantsOption fetch descendants option
     * @return {@code Object} resource data
     */
    Object getResourceDataForCmHandle(String datastoreName,
                                      String cmHandleId,
                                      String resourceIdentifier,
                                      FetchDescendantsOption fetchDescendantsOption);

    /**
     * Execute (async) data operation for group of cm handles using dmi.
     *
     * @param topicParamInQuery        topic name for (triggering) async responses
     * @param dataOperationRequest     contains a list of operation definitions(multiple operations)
     */
    void executeDataOperationForCmHandles(String topicParamInQuery,
                                          DataOperationRequest dataOperationRequest,
                                          String requestId);


    /**
     * Write resource data for data store pass-through running using dmi for given cm-handle.
     *
     * @param cmHandleId         cm handle identifier
     * @param resourceIdentifier resource identifier
     * @param operationType      required operation type
     * @param requestBody        request body to create resource
     * @param contentType        content type in body
     * @return {@code Object} return data
     */
    Object writeResourceDataPassThroughRunningForCmHandle(String cmHandleId,
                                                        String resourceIdentifier,
                                                        OperationType operationType,
                                                        String requestBody,
                                                        String contentType);

    /**
     * Retrieve module references for the given cm handle.
     *
     * @param cmHandleId cm handle identifier
     * @return a collection of modules names and revisions
     */
    Collection<ModuleReference> getYangResourcesModuleReferences(String cmHandleId);

    /**
     * Retrieve module definitions for the given cm handle.
     *
     * @param cmHandleId cm handle identifier
     * @return a collection of module definition (moduleName, revision and yang resource content)
     */
    Collection<ModuleDefinition> getModuleDefinitionsByCmHandleId(String cmHandleId);

    /**
     * Get module definitions for the given parameters.
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
     * Query cm handle details by cm handle's name.
     *
     * @param cmHandleId cm handle identifier
     * @return a collection of cm handle details.
     */
    NcmpServiceCmHandle getNcmpServiceCmHandle(String cmHandleId);

    /**
     * Get cm handle public properties by cm handle id.
     *
     * @param cmHandleId cm handle identifier
     * @return a collection of cm handle public properties.
     */
    Map<String, String> getCmHandlePublicProperties(String cmHandleId);

    /**
     * Get cm handle composite state by cm handle id.
     *
     * @param cmHandleId cm handle identifier
     * @return a cm handle composite state
     */
    CompositeState getCmHandleCompositeState(String cmHandleId);

    /**
     * Query and return cm handles that match the given query parameters.
     *
     * @param cmHandleQueryApiParameters the cm handle query parameters
     * @return collection of cm handles
     */
    Collection<NcmpServiceCmHandle> executeCmHandleSearch(CmHandleQueryApiParameters cmHandleQueryApiParameters);

    /**
     * Query and return cm handle ids that match the given query parameters.
     *
     * @param cmHandleQueryApiParameters the cm handle query parameters
     * @return collection of cm handle ids
     */
    Collection<String> executeCmHandleIdSearch(CmHandleQueryApiParameters cmHandleQueryApiParameters);

    /**
     * Set the data sync enabled flag, along with the data sync state to true or false based on the cm handle id.
     *
     * @param cmHandleId cm handle id
     * @param dataSyncEnabled data sync enabled flag
     */
    void setDataSyncEnabled(String cmHandleId, Boolean dataSyncEnabled);

    /**
     * Get all cm handle IDs by DMI plugin identifier.
     *
     * @param dmiPluginIdentifier DMI plugin identifier
     * @return collection of cm handle IDs
     */
    Collection<String> getAllCmHandleIdsByDmiPluginIdentifier(String dmiPluginIdentifier);

    /**
     * Get all cm handle IDs by various search criteria.
     *
     * @param cmHandleQueryServiceParameters cm handle query parameters
     * @return collection of cm handle IDs
     */
    Collection<String> executeCmHandleIdSearchForInventory(CmHandleQueryServiceParameters
                                                               cmHandleQueryServiceParameters);
}
