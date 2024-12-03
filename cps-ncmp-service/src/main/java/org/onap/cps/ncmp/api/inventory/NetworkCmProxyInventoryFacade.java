/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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
import org.onap.cps.api.model.ModuleDefinition;
import org.onap.cps.api.model.ModuleReference;
import org.onap.cps.ncmp.api.inventory.models.CmHandleQueryApiParameters;
import org.onap.cps.ncmp.api.inventory.models.CmHandleQueryServiceParameters;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistrationResponse;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;

public interface NetworkCmProxyInventoryFacade {

    /**
     * Registration of Created, Removed, Updated or Upgraded CM Handles.
     *
     * @param dmiPluginRegistration Dmi Plugin Registration details
     * @return dmiPluginRegistrationResponse
     */
    DmiPluginRegistrationResponse updateDmiRegistration(final DmiPluginRegistration dmiPluginRegistration);

    /**
     * Get all cm handle references by DMI plugin identifier.
     *
     * @param dmiPluginIdentifier DMI plugin identifier
     * @param outputAlternateId   boolean for cm handle reference type either
     *                            cm handle id (false) or alternate id (true)
     * @return collection of cm handle references
     */
    Collection<String> getAllCmHandleReferencesByDmiPluginIdentifier(final String dmiPluginIdentifier,
                                                                     final boolean outputAlternateId);

    /**
     * Get all cm handle IDs by various properties.
     *
     * @param cmHandleQueryServiceParameters cm handle query parameters
     * @param outputAlternateId              boolean for cm handle reference type either
     *                                       cm handle id (false) or alternate id (true)
     * @return                               collection of cm handle references
     */
    Collection<String> executeParameterizedCmHandleIdSearch(final CmHandleQueryServiceParameters
                                                                  cmHandleQueryServiceParameters,
                                                            final boolean outputAlternateId);

    /**
     * Retrieve module references for the given cm handle reference.
     *
     * @param cmHandleReference cm handle or alternate id identifier
     * @return a collection of modules names and revisions
     */
    Collection<ModuleReference> getYangResourcesModuleReferences(final String cmHandleReference);

    /**
     * Retrieve module definitions for the given cm handle.
     *
     * @param cmHandleReference cm handle or alternate id identifier
     * @return a collection of module definition (moduleName, revision and yang resource content)
     */
    Collection<ModuleDefinition> getModuleDefinitionsByCmHandleReference(final String cmHandleReference);

    /**
     * Get module definitions for the given parameters.
     *
     * @param cmHandleReference  cm handle or alternate id identifier
     * @param moduleName         module name
     * @param moduleRevision     the revision of the module
     * @return list of module definitions (module name, revision, yang resource content)
     */
    Collection<ModuleDefinition> getModuleDefinitionsByCmHandleAndModule(final String cmHandleReference,
                                                                         final String moduleName,
                                                                         final String moduleRevision);

    /**
     * Retrieve cm handles with details for the given query parameters.
     *
     * @param cmHandleQueryApiParameters cm handle query parameters
     * @return cm handles with details
     */
    Collection<NcmpServiceCmHandle> executeCmHandleSearch(final CmHandleQueryApiParameters cmHandleQueryApiParameters);

    /**
     * Retrieve cm handle ids for the given query parameters.
     *
     * @param cmHandleQueryApiParameters cm handle query parameters
     * @param outputAlternateId boolean for cm handle reference type either cmHandleId (false) or AlternateId (true)
     * @return cm handle ids
     */
    Collection<String> executeCmHandleIdSearch(final CmHandleQueryApiParameters cmHandleQueryApiParameters,
                                               final boolean outputAlternateId);

    /**
     * Set the data sync enabled flag, along with the data sync state
     * based on the data sync enabled boolean for the cm handle id provided.
     *
     * @param cmHandleId                 cm handle id
     * @param dataSyncEnabledTargetValue data sync enabled flag
     */
    void setDataSyncEnabled(final String cmHandleId, final Boolean dataSyncEnabledTargetValue);

    /**
     * Retrieve cm handle details for a given cm handle reference.
     *
     * @param cmHandleReference cm handle or alternate identifier
     * @return cm handle details
     */
    NcmpServiceCmHandle getNcmpServiceCmHandle(final String cmHandleReference);

    /**
     * Get cm handle public properties for a given cm handle or alternate id.
     *
     * @param cmHandleReference cm handle or alternate identifier
     * @return cm handle public properties
     */
    Map<String, String> getCmHandlePublicProperties(final String cmHandleReference);

    /**
     * Get cm handle composite state for a given cm handle id.
     *
     * @param cmHandleReference cm handle or alternate identifier
     * @return cm handle state
     */
    CompositeState getCmHandleCompositeState(final String cmHandleReference);
}
