/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Bell Canada
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

import static org.onap.cps.ncmp.impl.inventory.CmHandleQueryParametersValidator.validateCmHandleQueryParameters;

import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.inventory.models.CmHandleQueryApiParameters;
import org.onap.cps.ncmp.api.inventory.models.CmHandleQueryServiceParameters;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistrationResponse;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService;
import org.onap.cps.ncmp.impl.inventory.CmHandleRegistrationService;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.ParameterizedCmHandleQueryService;
import org.onap.cps.ncmp.impl.inventory.models.CmHandleQueryConditions;
import org.onap.cps.ncmp.impl.inventory.models.InventoryQueryConditions;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.inventory.trustlevel.TrustLevelManager;
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher;
import org.onap.cps.ncmp.impl.utils.YangDataConverter;
import org.onap.cps.spi.model.ModuleDefinition;
import org.onap.cps.spi.model.ModuleReference;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NetworkCmProxyInventoryFacade {

    private final CmHandleRegistrationService cmHandleRegistrationService;
    private final CmHandleQueryService cmHandleQueryService;
    private final ParameterizedCmHandleQueryService parameterizedCmHandleQueryService;
    private final InventoryPersistence inventoryPersistence;
    private final JsonObjectMapper jsonObjectMapper;
    private final TrustLevelManager trustLevelManager;
    private final AlternateIdMatcher alternateIdMatcher;



    /**
     * Registration of Created, Removed, Updated or Upgraded CM Handles.
     *
     * @param dmiPluginRegistration Dmi Plugin Registration details
     * @return dmiPluginRegistrationResponse
     */
    public DmiPluginRegistrationResponse updateDmiRegistration(final DmiPluginRegistration dmiPluginRegistration) {
        return cmHandleRegistrationService.updateDmiRegistration(dmiPluginRegistration);
    }

    /**
     * Get all cm handle references by DMI plugin identifier.
     *
     * @param dmiPluginIdentifier DMI plugin identifier
     * @param outputAlternateId   Boolean for cm handle reference type either
     *                            cm handle id (false) or alternate id (true)
     * @return collection of cm handle references
     */
    public Collection<String> getAllCmHandleReferencesByDmiPluginIdentifier(final String dmiPluginIdentifier,
                                                                     final boolean outputAlternateId) {
        return cmHandleQueryService.getCmHandleReferencesByDmiPluginIdentifier(dmiPluginIdentifier, outputAlternateId);
    }

    /**
     * Get all cm handle IDs by various properties.
     *
     * @param cmHandleQueryServiceParameters cm handle query parameters
     * @param outputAlternateId              Boolean for cm handle reference type either
     *                                       cm handle id (false) or alternate id (true)
     * @return                               collection of cm handle references
     */
    public Collection<String> executeParameterizedCmHandleIdSearch(
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters, final boolean outputAlternateId) {
        validateCmHandleQueryParameters(cmHandleQueryServiceParameters, InventoryQueryConditions.ALL_CONDITION_NAMES);

        return parameterizedCmHandleQueryService.queryCmHandleIdsForInventory(cmHandleQueryServiceParameters,
            outputAlternateId);
    }


    /**
     * Retrieve module references for the given cm handle reference.
     *
     * @param cmHandleReference cm handle or alternate id identifier
     * @return a collection of modules names and revisions
     */
    public Collection<ModuleReference> getYangResourcesModuleReferences(final String cmHandleReference) {
        final String cmHandleId = alternateIdMatcher.getCmHandleId(cmHandleReference);
        return inventoryPersistence.getYangResourcesModuleReferences(cmHandleId);
    }

    /**
     * Retrieve module definitions for the given cm handle.
     *
     * @param cmHandleReference cm handle or alternate id identifier
     * @return a collection of module definition (moduleName, revision and yang resource content)
     */
    public Collection<ModuleDefinition> getModuleDefinitionsByCmHandleReference(final String cmHandleReference) {
        final String cmHandleId = alternateIdMatcher.getCmHandleId(cmHandleReference);
        return inventoryPersistence.getModuleDefinitionsByCmHandleId(cmHandleId);
    }

    /**
     * Get module definitions for the given parameters.
     *
     * @param cmHandleReference  cm handle or alternate id identifier
     * @param moduleName         module name
     * @param moduleRevision     the revision of the module
     * @return list of module definitions (module name, revision, yang resource content)
     */
    public Collection<ModuleDefinition> getModuleDefinitionsByCmHandleAndModule(final String cmHandleReference,
                                                                                final String moduleName,
                                                                                final String moduleRevision) {
        final String cmHandleId = alternateIdMatcher.getCmHandleId(cmHandleReference);
        return inventoryPersistence.getModuleDefinitionsByCmHandleAndModule(cmHandleId, moduleName, moduleRevision);
    }

    /**
     * Retrieve cm handles with details for the given query parameters.
     *
     * @param cmHandleQueryApiParameters cm handle query parameters
     * @return cm handles with details
     */
    public Collection<NcmpServiceCmHandle> executeCmHandleSearch(
        final CmHandleQueryApiParameters cmHandleQueryApiParameters) {
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters = jsonObjectMapper.convertToValueType(
            cmHandleQueryApiParameters, CmHandleQueryServiceParameters.class);
        validateCmHandleQueryParameters(cmHandleQueryServiceParameters, CmHandleQueryConditions.ALL_CONDITION_NAMES);
        final Collection<NcmpServiceCmHandle> ncmpServiceCmHandles =
                parameterizedCmHandleQueryService.queryCmHandles(cmHandleQueryServiceParameters);
        ncmpServiceCmHandles.forEach(this::applyCurrentTrustLevel);
        return ncmpServiceCmHandles;
    }

    /**
     * Retrieve cm handle ids for the given query parameters.
     *
     * @param cmHandleQueryApiParameters cm handle query parameters
     * @param outputAlternateId Boolean for cm handle reference type either cmHandleId (false) or AlternateId (true)
     * @return cm handle ids
     */
    public Collection<String> executeCmHandleIdSearch(final CmHandleQueryApiParameters cmHandleQueryApiParameters,
                                                      final boolean outputAlternateId) {
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters = jsonObjectMapper.convertToValueType(
            cmHandleQueryApiParameters, CmHandleQueryServiceParameters.class);
        validateCmHandleQueryParameters(cmHandleQueryServiceParameters, CmHandleQueryConditions.ALL_CONDITION_NAMES);
        return parameterizedCmHandleQueryService.queryCmHandleReferenceIds(cmHandleQueryServiceParameters,
            outputAlternateId);
    }

    /**
     * Set the data sync enabled flag, along with the data sync state
     * based on the data sync enabled boolean for the cm handle id provided.
     *
     * @param cmHandleId                 cm handle id
     * @param dataSyncEnabledTargetValue data sync enabled flag
     */
    public void setDataSyncEnabled(final String cmHandleId, final Boolean dataSyncEnabledTargetValue) {
        cmHandleRegistrationService.setDataSyncEnabled(cmHandleId, dataSyncEnabledTargetValue);
    }

    /**
     * Retrieve cm handle details for a given cm handle reference.
     *
     * @param cmHandleReference cm handle or alternate identifier
     * @return cm handle details
     */
    public NcmpServiceCmHandle getNcmpServiceCmHandle(final String cmHandleReference) {
        final String cmHandleId = alternateIdMatcher.getCmHandleId(cmHandleReference);
        final NcmpServiceCmHandle ncmpServiceCmHandle = YangDataConverter.toNcmpServiceCmHandle(
                inventoryPersistence.getYangModelCmHandle(cmHandleId));
        applyCurrentTrustLevel(ncmpServiceCmHandle);
        return ncmpServiceCmHandle;
    }

    /**
     * Get cm handle public properties for a given cm handle or alternate id.
     *
     * @param cmHandleReference cm handle or alternate identifier
     * @return cm handle public properties
     */
    public Map<String, String> getCmHandlePublicProperties(final String cmHandleReference) {
        final String cmHandleId = alternateIdMatcher.getCmHandleId(cmHandleReference);
        final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
        return YangDataConverter.toPropertiesMap(yangModelCmHandle.getPublicProperties());
    }

    /**
     * Get cm handle composite state for a given cm handle id.
     *
     * @param cmHandleReference cm handle or alternate identifier
     * @return cm handle state
     */
    public CompositeState getCmHandleCompositeState(final String cmHandleReference) {
        final String cmHandleId = alternateIdMatcher.getCmHandleId(cmHandleReference);
        return inventoryPersistence.getYangModelCmHandle(cmHandleId).getCompositeState();
    }

    private void applyCurrentTrustLevel(final NcmpServiceCmHandle ncmpServiceCmHandle) {
        ncmpServiceCmHandle.setCurrentTrustLevel(trustLevelManager
                .getEffectiveTrustLevel(ncmpServiceCmHandle.getCmHandleId()));
    }

}