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

package org.onap.cps.ncmp.impl;

import static org.onap.cps.ncmp.impl.inventory.CmHandleQueryParametersValidator.validateCmHandleQueryParameters;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.model.ModuleDefinition;
import org.onap.cps.api.model.ModuleReference;
import org.onap.cps.ncmp.api.exceptions.CmHandleNotFoundException;
import org.onap.cps.ncmp.api.inventory.NetworkCmProxyInventoryFacade;
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
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NetworkCmProxyInventoryFacadeImpl implements NetworkCmProxyInventoryFacade {

    private final CmHandleRegistrationService cmHandleRegistrationService;
    private final CmHandleQueryService cmHandleQueryService;
    private final ParameterizedCmHandleQueryService parameterizedCmHandleQueryService;
    private final InventoryPersistence inventoryPersistence;
    private final JsonObjectMapper jsonObjectMapper;
    private final TrustLevelManager trustLevelManager;
    private final AlternateIdMatcher alternateIdMatcher;

    @Override
    public DmiPluginRegistrationResponse updateDmiRegistration(final DmiPluginRegistration dmiPluginRegistration) {
        return cmHandleRegistrationService.updateDmiRegistration(dmiPluginRegistration);
    }

    @Override
    public Collection<String> getAllCmHandleReferencesByDmiPluginIdentifier(final String dmiPluginIdentifier,
                                                                     final boolean outputAlternateId) {
        return cmHandleQueryService.getCmHandleReferencesByDmiPluginIdentifier(dmiPluginIdentifier, outputAlternateId);
    }

    @Override
    public Collection<String> executeParameterizedCmHandleIdSearch(
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters, final boolean outputAlternateId) {
        validateCmHandleQueryParameters(cmHandleQueryServiceParameters, InventoryQueryConditions.ALL_CONDITION_NAMES);

        return parameterizedCmHandleQueryService.queryCmHandleIdsForInventory(cmHandleQueryServiceParameters,
            outputAlternateId);
    }

    @Override
    public Collection<ModuleReference> getYangResourcesModuleReferences(final String cmHandleReference) {
        try {
            final String cmHandleId = alternateIdMatcher.getCmHandleId(cmHandleReference);
            return inventoryPersistence.getYangResourcesModuleReferences(cmHandleId);
        } catch (final CmHandleNotFoundException cmHandleNotFoundException) {
            return Collections.emptyList();
        }
    }

    @Override
    public Collection<ModuleDefinition> getModuleDefinitionsByCmHandleReference(final String cmHandleReference) {
        try {
            final String cmHandleId = alternateIdMatcher.getCmHandleId(cmHandleReference);
            // TODO Verify this works using moduleSetTag as SchemaSet name - it looks like no changes needed.
            return inventoryPersistence.getModuleDefinitionsByCmHandleId(cmHandleId);
        } catch (final CmHandleNotFoundException cmHandleNotFoundException) {
            return Collections.emptyList();
        }
    }

    @Override
    public Collection<ModuleDefinition> getModuleDefinitionsByCmHandleAndModule(final String cmHandleReference,
                                                                                final String moduleName,
                                                                                final String moduleRevision) {
        try {
            final String cmHandleId = alternateIdMatcher.getCmHandleId(cmHandleReference);
            return inventoryPersistence.getModuleDefinitionsByCmHandleAndModule(cmHandleId, moduleName, moduleRevision);
        } catch (final CmHandleNotFoundException cmHandleNotFoundException) {
            return Collections.emptyList();
        }
    }

    @Override
    public Collection<NcmpServiceCmHandle> executeCmHandleSearch(
            final CmHandleQueryApiParameters cmHandleQueryApiParameters) {
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters = jsonObjectMapper.convertToValueType(
                cmHandleQueryApiParameters, CmHandleQueryServiceParameters.class);
        validateCmHandleQueryParameters(cmHandleQueryServiceParameters, CmHandleQueryConditions.ALL_CONDITION_NAMES);
        final Collection<NcmpServiceCmHandle> ncmpServiceCmHandles =
                parameterizedCmHandleQueryService.queryCmHandles(cmHandleQueryServiceParameters);
        trustLevelManager.applyEffectiveTrustLevels(ncmpServiceCmHandles);
        return ncmpServiceCmHandles;
    }

    @Override
    public Collection<String> executeCmHandleIdSearch(final CmHandleQueryApiParameters cmHandleQueryApiParameters,
                                                      final boolean outputAlternateId) {
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters = jsonObjectMapper.convertToValueType(
            cmHandleQueryApiParameters, CmHandleQueryServiceParameters.class);
        validateCmHandleQueryParameters(cmHandleQueryServiceParameters, CmHandleQueryConditions.ALL_CONDITION_NAMES);
        return parameterizedCmHandleQueryService.queryCmHandleReferenceIds(cmHandleQueryServiceParameters,
            outputAlternateId);
    }

    @Override
    public void setDataSyncEnabled(final String cmHandleId, final Boolean dataSyncEnabledTargetValue) {
        cmHandleRegistrationService.setDataSyncEnabled(cmHandleId, dataSyncEnabledTargetValue);
    }

    @Override
    public NcmpServiceCmHandle getNcmpServiceCmHandle(final String cmHandleReference) {
        final String cmHandleId = alternateIdMatcher.getCmHandleId(cmHandleReference);
        final NcmpServiceCmHandle ncmpServiceCmHandle = YangDataConverter.toNcmpServiceCmHandle(
                inventoryPersistence.getYangModelCmHandle(cmHandleId));
        trustLevelManager.applyEffectiveTrustLevel(ncmpServiceCmHandle);
        return ncmpServiceCmHandle;
    }

    @Override
    public Map<String, String> getCmHandlePublicProperties(final String cmHandleReference) {
        final String cmHandleId = alternateIdMatcher.getCmHandleId(cmHandleReference);
        final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
        return YangDataConverter.toPropertiesMap(yangModelCmHandle.getPublicProperties());
    }

    @Override
    public CompositeState getCmHandleCompositeState(final String cmHandleReference) {
        final String cmHandleId = alternateIdMatcher.getCmHandleId(cmHandleReference);
        return inventoryPersistence.getYangModelCmHandle(cmHandleId).getCompositeState();
    }

}
