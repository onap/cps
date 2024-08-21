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
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.inventory.models.CmHandleQueryApiParameters;
import org.onap.cps.ncmp.api.inventory.models.CmHandleQueryServiceParameters;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistrationResponse;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.api.inventory.models.TrustLevel;
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService;
import org.onap.cps.ncmp.impl.inventory.CmHandleRegistrationService;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.ParameterizedCmHandleQueryService;
import org.onap.cps.ncmp.impl.inventory.models.CmHandleQueryConditions;
import org.onap.cps.ncmp.impl.inventory.models.InventoryQueryConditions;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.inventory.trustlevel.TrustLevelCacheConfig;
import org.onap.cps.ncmp.impl.utils.YangDataConverter;
import org.onap.cps.spi.model.ModuleDefinition;
import org.onap.cps.spi.model.ModuleReference;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkCmProxyInventoryFacade {

    private final CmHandleRegistrationService cmHandleRegistrationService;
    private final CmHandleQueryService cmHandleQueryService;
    private final ParameterizedCmHandleQueryService parameterizedCmHandleQueryService;
    private final InventoryPersistence inventoryPersistence;
    private final JsonObjectMapper jsonObjectMapper;

    @Qualifier(TrustLevelCacheConfig.TRUST_LEVEL_PER_CM_HANDLE)
    private final Map<String, TrustLevel> trustLevelPerCmHandle;

    /**
     * Registration of Created, Removed, Updated or Upgraded CM Handles.
     *
     * @param dmiPluginRegistration Dmi Plugin Registration details
     * @return dmiPluginRegistrationResponse
     */

    public DmiPluginRegistrationResponse updateDmiRegistrationAndSyncModule(
        final DmiPluginRegistration dmiPluginRegistration) {
        return cmHandleRegistrationService.updateDmiRegistrationAndSyncModule(dmiPluginRegistration);
    }

    /**
     * Get all cm handle IDs by DMI plugin identifier.
     *
     * @param dmiPluginIdentifier DMI plugin identifier
     * @return collection of cm handle IDs
     */
    public Collection<String> getAllCmHandleIdsByDmiPluginIdentifier(final String dmiPluginIdentifier) {
        return cmHandleQueryService.getCmHandleIdsByDmiPluginIdentifier(dmiPluginIdentifier);
    }

    /**
     * Get all cm handle IDs by various properties.
     *
     * @param cmHandleQueryServiceParameters cm handle query parameters
     * @return collection of cm handle IDs
     */
    public Collection<String> executeParameterizedCmHandleIdSearch(
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters) {
        validateCmHandleQueryParameters(cmHandleQueryServiceParameters, InventoryQueryConditions.ALL_CONDITION_NAMES);
        return parameterizedCmHandleQueryService.queryCmHandleIdsForInventory(cmHandleQueryServiceParameters);
    }


    /**
     * Retrieve module references for the given cm handle.
     *
     * @param cmHandleId cm handle identifier
     * @return a collection of modules names and revisions
     */
    public Collection<ModuleReference> getYangResourcesModuleReferences(final String cmHandleId) {
        return inventoryPersistence.getYangResourcesModuleReferences(cmHandleId);
    }

    /**
     * Retrieve module definitions for the given cm handle.
     *
     * @param cmHandleId cm handle identifier
     * @return a collection of module definition (moduleName, revision and yang resource content)
     */
    public Collection<ModuleDefinition> getModuleDefinitionsByCmHandleId(final String cmHandleId) {
        return inventoryPersistence.getModuleDefinitionsByCmHandleId(cmHandleId);
    }

    /**
     * Get module definitions for the given parameters.
     *
     * @param cmHandleId        cm-handle identifier
     * @param moduleName        module name
     * @param moduleRevision    the revision of the module
     * @return list of module definitions (module name, revision, yang resource content)
     */
    public Collection<ModuleDefinition> getModuleDefinitionsByCmHandleAndModule(final String cmHandleId,
                                                                                final String moduleName,
                                                                                final String moduleRevision) {
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
     * @return cm handle ids
     */
    public Collection<String> executeCmHandleIdSearch(final CmHandleQueryApiParameters cmHandleQueryApiParameters) {
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters = jsonObjectMapper.convertToValueType(
            cmHandleQueryApiParameters, CmHandleQueryServiceParameters.class);
        validateCmHandleQueryParameters(cmHandleQueryServiceParameters, CmHandleQueryConditions.ALL_CONDITION_NAMES);
        return parameterizedCmHandleQueryService.queryCmHandleIds(cmHandleQueryServiceParameters);
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
     * Retrieve cm handle details for a given cm handle.
     *
     * @param cmHandleId cm handle identifier
     * @return cm handle details
     */
    public NcmpServiceCmHandle getNcmpServiceCmHandle(final String cmHandleId) {
        final NcmpServiceCmHandle ncmpServiceCmHandle = YangDataConverter.toNcmpServiceCmHandle(
            inventoryPersistence.getYangModelCmHandle(cmHandleId));
        applyCurrentTrustLevel(ncmpServiceCmHandle);
        return ncmpServiceCmHandle;
    }

    /**
     * Get cm handle public properties for a given cm handle id.
     *
     * @param cmHandleId cm handle identifier
     * @return cm handle public properties
     */
    public Map<String, String> getCmHandlePublicProperties(final String cmHandleId) {
        final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
        return YangDataConverter.toPropertiesMap(yangModelCmHandle.getPublicProperties());
    }

    /**
     * Get cm handle composite state for a given cm handle id.
     *
     * @param cmHandleId cm handle identifier
     * @return cm handle state
     */
    public CompositeState getCmHandleCompositeState(final String cmHandleId) {
        return inventoryPersistence.getYangModelCmHandle(cmHandleId).getCompositeState();
    }

    private void applyCurrentTrustLevel(final NcmpServiceCmHandle ncmpServiceCmHandle) {
        ncmpServiceCmHandle.setCurrentTrustLevel(trustLevelPerCmHandle.get(ncmpServiceCmHandle.getCmHandleId()));
    }


}
