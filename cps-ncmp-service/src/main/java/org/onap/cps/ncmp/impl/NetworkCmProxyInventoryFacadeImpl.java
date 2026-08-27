/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications Copyright (C) 2021-2026 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Bell Canada
 *  Modifications Copyright (C) 2023 Deutsche Telekom AG
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.api.model.ModuleDefinition;
import org.onap.cps.api.model.ModuleReference;
import org.onap.cps.ncmp.api.exceptions.CmHandleNotFoundException;
import org.onap.cps.ncmp.api.exceptions.ServerNcmpException;
import org.onap.cps.ncmp.api.inventory.NetworkCmProxyInventoryFacade;
import org.onap.cps.ncmp.api.inventory.models.CmHandleQueryApiParameters;
import org.onap.cps.ncmp.api.inventory.models.CmHandleQueryServiceParameters;
import org.onap.cps.ncmp.api.inventory.models.CmHandleState;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.inventory.models.DmiPluginRegistrationResponse;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.api.inventory.models.RefreshCmHandle;
import org.onap.cps.ncmp.exceptions.NoAlternateIdMatchFoundException;
import org.onap.cps.ncmp.impl.dmi.DmiPluginUrlValidator;
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService;
import org.onap.cps.ncmp.impl.inventory.CmHandleRegistrationService;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.ParameterizedCmHandleQueryService;
import org.onap.cps.ncmp.impl.inventory.models.NorthboundCmHandleQuerySupportedConditions;
import org.onap.cps.ncmp.impl.inventory.models.SouthboundCmHandleQuerySupportedConditions;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.inventory.trustlevel.TrustLevelManager;
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher;
import org.onap.cps.ncmp.impl.utils.YangDataConverter;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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
    private final DmiPluginUrlValidator dmiPluginUrlValidator;

    @Override
    public DmiPluginRegistrationResponse updateDmiRegistration(final DmiPluginRegistration dmiPluginRegistration) {
        dmiPluginUrlValidator.validateDmiPluginUrls(dmiPluginRegistration);
        return cmHandleRegistrationService.updateDmiRegistration(dmiPluginRegistration);
    }

    @Override
    public Collection<String> getAllCmHandleReferencesByDmiPluginIdentifier(final String dmiPluginIdentifier,
                                                                     final boolean outputAlternateId) {
        return cmHandleQueryService.getCmHandleReferencesByDmiPluginIdentifier(dmiPluginIdentifier, outputAlternateId);
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
    public Map<String, List<RefreshCmHandle>> refreshModules(
        final CmHandleQueryApiParameters cmHandleQueryApiParameters) {
        final Map<String, List<NcmpServiceCmHandle>> cmHandlesByModuleSetTag = new LinkedHashMap<>();
        southboundCmHandleSearchLightweight(cmHandleQueryApiParameters).toIterable().forEach(ncmpServiceCmHandle ->
            cmHandlesByModuleSetTag.computeIfAbsent(ncmpServiceCmHandle.getModuleSetTag(), tag -> new ArrayList<>())
                .add(ncmpServiceCmHandle));

        final Map<String, List<RefreshCmHandle>> refreshCmHandlesByModuleSetTag = new LinkedHashMap<>();
        final List<String> selectedSampleCmHandleIds = new ArrayList<>();
        cmHandlesByModuleSetTag.forEach((moduleSetTag, ncmpServiceCmHandles) -> {
            final NcmpServiceCmHandle sampleCmHandle = firstReadyCmHandle(ncmpServiceCmHandles);
            final List<RefreshCmHandle> refreshCmHandles = new ArrayList<>(ncmpServiceCmHandles.size());
            for (final NcmpServiceCmHandle ncmpServiceCmHandle : ncmpServiceCmHandles) {
                refreshCmHandles.add(new RefreshCmHandle(
                    ncmpServiceCmHandle.getCmHandleId(),
                    getCmHandleReference(ncmpServiceCmHandle),
                    getCmHandleStatusOrThrow(ncmpServiceCmHandle),
                    ncmpServiceCmHandle == sampleCmHandle));
            }
            refreshCmHandlesByModuleSetTag.put(moduleSetTag, refreshCmHandles);
            if (sampleCmHandle != null) {
                selectedSampleCmHandleIds.add(sampleCmHandle.getCmHandleId());
            }
        });
        if (!selectedSampleCmHandleIds.isEmpty()) {
            cmHandleRegistrationService.setCmHandlesToLockedForModuleRefresh(selectedSampleCmHandleIds);
        }
        return refreshCmHandlesByModuleSetTag;
    }

    @Override
    public Collection<String> northboundCmHandleIdSearch(final CmHandleQueryApiParameters cmHandleQueryApiParameters,
                                                         final boolean outputAlternateId) {
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters = jsonObjectMapper.convertToValueType(
            cmHandleQueryApiParameters, CmHandleQueryServiceParameters.class);
        validateCmHandleQueryParameters(cmHandleQueryServiceParameters,
            NorthboundCmHandleQuerySupportedConditions.CONDITION_NAMES);
        return parameterizedCmHandleQueryService.queryCmHandleReferenceIds(cmHandleQueryServiceParameters,
            outputAlternateId);
    }

    @Override
    public Flux<NcmpServiceCmHandle> northboundCmHandleSearch(
        final CmHandleQueryApiParameters cmHandleQueryApiParameters) {
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters =
                jsonObjectMapper.convertToValueType(cmHandleQueryApiParameters, CmHandleQueryServiceParameters.class);
        validateCmHandleQueryParameters(cmHandleQueryServiceParameters,
            NorthboundCmHandleQuerySupportedConditions.CONDITION_NAMES);
        return parameterizedCmHandleQueryService.queryCmHandles(cmHandleQueryServiceParameters);
    }

    @Override
    public Flux<NcmpServiceCmHandle> northboundCmHandleSearchLightweight(
            final CmHandleQueryApiParameters cmHandleQueryApiParameters) {
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters =
                jsonObjectMapper.convertToValueType(cmHandleQueryApiParameters, CmHandleQueryServiceParameters.class);
        validateCmHandleQueryParameters(cmHandleQueryServiceParameters,
            NorthboundCmHandleQuerySupportedConditions.CONDITION_NAMES);
        return parameterizedCmHandleQueryService.queryCmHandlesLightweight(cmHandleQueryServiceParameters);
    }

    @Override
    public Flux<NcmpServiceCmHandle> southboundCmHandleSearch(
            final CmHandleQueryApiParameters cmHandleQueryApiParameters) {
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters =
                jsonObjectMapper.convertToValueType(cmHandleQueryApiParameters, CmHandleQueryServiceParameters.class);
        validateCmHandleQueryParameters(cmHandleQueryServiceParameters,
            SouthboundCmHandleQuerySupportedConditions.CONDITION_NAMES);
        return parameterizedCmHandleQueryService.queryInventoryForCmHandles(cmHandleQueryServiceParameters);
    }

    @Override
    public Flux<NcmpServiceCmHandle> southboundCmHandleSearchLightweight(
            final CmHandleQueryApiParameters cmHandleQueryApiParameters) {
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters =
                jsonObjectMapper.convertToValueType(cmHandleQueryApiParameters, CmHandleQueryServiceParameters.class);
        validateCmHandleQueryParameters(cmHandleQueryServiceParameters,
            SouthboundCmHandleQuerySupportedConditions.CONDITION_NAMES);
        return parameterizedCmHandleQueryService.queryInventoryForCmHandlesLightweight(
                cmHandleQueryServiceParameters);
    }

    @Override
    public Collection<String> southboundCmHandleIdSearch(
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters, final boolean outputAlternateId) {
        validateCmHandleQueryParameters(cmHandleQueryServiceParameters,
            SouthboundCmHandleQuerySupportedConditions.CONDITION_NAMES);
        return parameterizedCmHandleQueryService.queryCmHandleIdsForInventory(cmHandleQueryServiceParameters,
            outputAlternateId);
    }

    @Override
    public void setDataSyncEnabled(final String cmHandleId, final Boolean dataSyncEnabledTargetValue) {
        cmHandleRegistrationService.setDataSyncEnabled(cmHandleId, dataSyncEnabledTargetValue);
    }

    @Override
    public NcmpServiceCmHandle getNcmpServiceCmHandle(final String cmHandleReference) {
        final String cmHandleId = getCmHandleIdByReference(cmHandleReference);
        final NcmpServiceCmHandle ncmpServiceCmHandle = YangDataConverter.toNcmpServiceCmHandle(
                inventoryPersistence.getYangModelCmHandle(cmHandleId));
        trustLevelManager.applyEffectiveTrustLevel(ncmpServiceCmHandle);
        return ncmpServiceCmHandle;
    }

    @Override
    public Map<String, String> getPublicCmHandleProperties(final String cmHandleReference) {
        final String cmHandleId = alternateIdMatcher.getCmHandleId(cmHandleReference);
        final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
        return YangDataConverter.toPropertiesMap(yangModelCmHandle.getPublicProperties());
    }

    @Override
    public CompositeState getCmHandleCompositeState(final String cmHandleReference) {
        final String cmHandleId = alternateIdMatcher.getCmHandleId(cmHandleReference);
        return inventoryPersistence.getYangModelCmHandle(cmHandleId).getCompositeState();
    }

    private String getCmHandleIdByReference(final String cmHandleReference) {
        try {
            return alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(cmHandleReference, "/");
        } catch (final NoAlternateIdMatchFoundException ignored) {
            return alternateIdMatcher.getCmHandleId(cmHandleReference);
        }
    }

    private static String getCmHandleReference(final NcmpServiceCmHandle ncmpServiceCmHandle) {
        return StringUtils.isNotBlank(ncmpServiceCmHandle.getAlternateId())
            ? ncmpServiceCmHandle.getAlternateId() : ncmpServiceCmHandle.getCmHandleId();
    }

    private static String getCmHandleStatusOrThrow(final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final String cmHandleStatus = ncmpServiceCmHandle.getCmHandleStatus();
        if (StringUtils.isBlank(cmHandleStatus)) {
            throw new ServerNcmpException("Module refresh is not supported by the current inventory data model",
                "The top-level CM handle status is not available; this feature requires inventory model version "
                    + "r20260423 or later.");
        }
        return cmHandleStatus;
    }

    private static NcmpServiceCmHandle firstReadyCmHandle(final List<NcmpServiceCmHandle> ncmpServiceCmHandles) {
        for (final NcmpServiceCmHandle ncmpServiceCmHandle : ncmpServiceCmHandles) {
            if (CmHandleState.READY.name().equals(getCmHandleStatusOrThrow(ncmpServiceCmHandle))) {
                return ncmpServiceCmHandle;
            }
        }
        return null;
    }

}
