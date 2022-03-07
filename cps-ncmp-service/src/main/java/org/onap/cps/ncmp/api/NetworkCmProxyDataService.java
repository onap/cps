/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications Copyright (C) 2021-2022 Nordix Foundation
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

import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum;

import java.util.Collection;
import org.onap.cps.ncmp.api.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.models.DmiPluginRegistrationResponse;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.spi.model.ModuleReference;

/*
 * Datastore interface for handling CPS data.
 */
public interface NetworkCmProxyDataService {

    /**
     * Registration of New CM Handles.
     *
     * @param dmiPluginRegistration Dmi Plugin Registration
     * @return dmiPluginRegistrationResponse response only for failed operations
     */
    DmiPluginRegistrationResponse updateDmiRegistrationAndSyncModule(DmiPluginRegistration dmiPluginRegistration);

    /**
     * Get resource data for data store pass-through operational
     * using dmi.
     *
     * @param cmHandleId cm handle identifier
     * @param resourceIdentifier resource identifier
     * @param acceptParamInHeader accept param
     * @param optionsParamInQuery options query
     * @return {@code Object} resource data
     */
    Object getResourceDataOperationalForCmHandle(String cmHandleId,
                                                 String resourceIdentifier,
                                                 String acceptParamInHeader,
                                                 String optionsParamInQuery);

    /**
     * Get resource data for data store pass-through running
     * using dmi.
     *
     * @param cmHandleId cm handle identifier
     * @param resourceIdentifier resource identifier
     * @param acceptParamInHeader accept param
     * @param optionsParamInQuery options query
     * @return {@code Object} resource data
     */
    Object getResourceDataPassThroughRunningForCmHandle(String cmHandleId,
                                                        String resourceIdentifier,
                                                        String acceptParamInHeader,
                                                        String optionsParamInQuery);

    /**
     * Write resource data for data store pass-through running
     * using dmi for given cm-handle.
     *  @param cmHandleId cm handle identifier
     * @param resourceIdentifier resource identifier
     * @param operation required operation
     * @param requestBody request body to create resource
     * @param contentType content type in body
     * @return {@code Object} return data
     */
    Object writeResourceDataPassThroughRunningForCmHandle(String cmHandleId,
                                                        String resourceIdentifier,
                                                        OperationEnum operation,
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
     * Query cm handle identifiers for the given collection of module names.
     *
     * @param moduleNames module names.
     * @return a collection of cm handle identifiers. The schema set for each cm handle must include all the
     *         given module names
     */
    Collection<String> executeCmHandleHasAllModulesSearch(Collection<String> moduleNames);

    /**
     * Query cm handle details by cm handle's name.
     *
     * @param cmHandleId cm handle identifier
     * @return a collection of cm handle details.
     */
    NcmpServiceCmHandle getNcmpServiceCmHandle(String cmHandleId);

}
