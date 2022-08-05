/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Bell Canada
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

package org.onap.cps.ncmp.api.impl;

import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum;
import static org.onap.cps.utils.CmHandleQueryRestParametersValidator.validateCmHandleQueryParameters;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.ncmp.api.NetworkCmProxyCmHandlerQueryService;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.impl.event.lcm.LcmEventsCmHandleStateHandler;
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations;
import org.onap.cps.ncmp.api.impl.operations.DmiOperations;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CmHandleState;
import org.onap.cps.ncmp.api.inventory.CompositeState;
import org.onap.cps.ncmp.api.inventory.CompositeStateUtils;
import org.onap.cps.ncmp.api.inventory.DataStoreSyncState;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.models.CmHandleQueryApiParameters;
import org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse;
import org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse.RegistrationError;
import org.onap.cps.ncmp.api.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.models.DmiPluginRegistrationResponse;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.CpsException;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.CmHandleQueryServiceParameters;
import org.onap.cps.spi.model.ModuleDefinition;
import org.onap.cps.spi.model.ModuleReference;
import org.onap.cps.utils.CpsValidator;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkCmProxyDataServiceImpl implements NetworkCmProxyDataService {

    private final JsonObjectMapper jsonObjectMapper;

    private final DmiDataOperations dmiDataOperations;

    private final NetworkCmProxyDataServicePropertyHandler networkCmProxyDataServicePropertyHandler;

    private final InventoryPersistence inventoryPersistence;

    private final NetworkCmProxyCmHandlerQueryService networkCmProxyCmHandlerQueryService;

    private final LcmEventsCmHandleStateHandler lcmEventsCmHandleStateHandler;

    private final CpsDataService cpsDataService;

    @Override
    public DmiPluginRegistrationResponse updateDmiRegistrationAndSyncModule(
            final DmiPluginRegistration dmiPluginRegistration) {
        dmiPluginRegistration.validateDmiPluginRegistration();
        final DmiPluginRegistrationResponse dmiPluginRegistrationResponse = new DmiPluginRegistrationResponse();
        dmiPluginRegistrationResponse.setRemovedCmHandles(
                parseAndRemoveCmHandlesInDmiRegistration(dmiPluginRegistration.getRemovedCmHandles()));
        if (!dmiPluginRegistration.getCreatedCmHandles().isEmpty()) {
            dmiPluginRegistrationResponse.setCreatedCmHandles(
                    parseAndCreateCmHandlesInDmiRegistrationAndSyncModules(dmiPluginRegistration));
        }
        if (!dmiPluginRegistration.getUpdatedCmHandles().isEmpty()) {
            dmiPluginRegistrationResponse.setUpdatedCmHandles(
                    networkCmProxyDataServicePropertyHandler
                            .updateCmHandleProperties(dmiPluginRegistration.getUpdatedCmHandles()));
        }
        return dmiPluginRegistrationResponse;
    }

    @Override
    public Object getResourceDataOperationalForCmHandle(final String cmHandleId,
                                                        final String resourceIdentifier,
                                                        final String optionsParamInQuery,
                                                        final String topicParamInQuery,
                                                        final String requestId) {
        final ResponseEntity<?> responseEntity = dmiDataOperations.getResourceDataFromDmi(cmHandleId,
            resourceIdentifier,
            optionsParamInQuery,
            DmiOperations.DataStoreEnum.PASSTHROUGH_OPERATIONAL,
            requestId, topicParamInQuery);
        return responseEntity.getBody();
    }

    @Override
    public Object getResourceDataPassThroughRunningForCmHandle(final String cmHandleId,
                                                               final String resourceIdentifier,
                                                               final String optionsParamInQuery,
                                                               final String topicParamInQuery,
                                                               final String requestId) {
        final ResponseEntity<?> responseEntity = dmiDataOperations.getResourceDataFromDmi(cmHandleId,
            resourceIdentifier,
            optionsParamInQuery,
            DmiOperations.DataStoreEnum.PASSTHROUGH_RUNNING,
            requestId, topicParamInQuery);
        return responseEntity.getBody();
    }

    @Override
    public Object writeResourceDataPassThroughRunningForCmHandle(final String cmHandleId,
                                                                 final String resourceIdentifier,
                                                                 final OperationEnum operation,
                                                                 final String requestData,
                                                                 final String dataType) {
        CpsValidator.validateNameCharacters(cmHandleId);
        return dmiDataOperations.writeResourceDataPassThroughRunningFromDmi(cmHandleId, resourceIdentifier, operation,
            requestData, dataType);
    }

    @Override
    public Collection<ModuleReference> getYangResourcesModuleReferences(final String cmHandleId) {
        CpsValidator.validateNameCharacters(cmHandleId);
        return inventoryPersistence.getYangResourcesModuleReferences(cmHandleId);
    }

    @Override
    public Collection<ModuleDefinition> getModuleDefinitionsByCmHandleId(final String cmHandleId) {
        CpsValidator.validateNameCharacters(cmHandleId);
        return inventoryPersistence.getModuleDefinitionsByCmHandleId(cmHandleId);
    }

    /**
     * Retrieve cm handles with details for the given query parameters.
     *
     * @param cmHandleQueryApiParameters cm handle query parameters
     * @return cm handles with details
     */
    @Override
    public Set<NcmpServiceCmHandle> executeCmHandleSearch(final CmHandleQueryApiParameters cmHandleQueryApiParameters) {
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters = jsonObjectMapper.convertToValueType(
                cmHandleQueryApiParameters, CmHandleQueryServiceParameters.class);

        validateCmHandleQueryParameters(cmHandleQueryServiceParameters);

        return networkCmProxyCmHandlerQueryService.queryCmHandles(cmHandleQueryServiceParameters);
    }

    /**
     * Retrieve cm handle ids for the given query parameters.
     *
     * @param cmHandleQueryApiParameters cm handle query parameters
     * @return cm handle ids
     */
    @Override
    public Set<String> executeCmHandleIdSearch(final CmHandleQueryApiParameters cmHandleQueryApiParameters) {
        final CmHandleQueryServiceParameters cmHandleQueryServiceParameters = jsonObjectMapper.convertToValueType(
                cmHandleQueryApiParameters, CmHandleQueryServiceParameters.class);

        validateCmHandleQueryParameters(cmHandleQueryServiceParameters);

        return networkCmProxyCmHandlerQueryService.queryCmHandleIds(cmHandleQueryServiceParameters);
    }

    /**
     * Set the data sync enabled flag, along with the data sync state
     * based on the data sync enabled boolean for the cm handle id provided.
     *
     * @param cmHandleId cm handle id
     * @param dataSyncEnabled data sync enabled flag
     */
    @Override
    public void setDataSyncEnabled(final String cmHandleId, final boolean dataSyncEnabled) {
        CpsValidator.validateNameCharacters(cmHandleId);
        final CompositeState compositeState = inventoryPersistence
            .getCmHandleState(cmHandleId);
        if (compositeState.getDataSyncEnabled().equals(dataSyncEnabled)) {
            log.info("Data-Sync Enabled flag is already: {} ", dataSyncEnabled);
        } else if (compositeState.getCmHandleState() != CmHandleState.READY) {
            throw new CpsException("State mismatch exception.", "Cm-Handle not in READY state. Cm handle state is: "
                + compositeState.getCmHandleState());
        } else {
            final DataStoreSyncState dataStoreSyncState = compositeState.getDataStores()
                .getOperationalDataStore().getDataStoreSyncState();
            if (!dataSyncEnabled && dataStoreSyncState == DataStoreSyncState.SYNCHRONIZED) {
                cpsDataService.deleteDataNode("NFP-Operational", cmHandleId,
                    "/netconf-state", OffsetDateTime.now());
            }
            CompositeStateUtils.setDataSyncEnabledFlagWithDataSyncState(dataSyncEnabled, compositeState);
            inventoryPersistence.saveCmHandleState(cmHandleId,
                compositeState);
        }
    }

    /**
     * Retrieve cm handle details for a given cm handle.
     *
     * @param cmHandleId cm handle identifier
     * @return cm handle details
     */
    @Override
    public NcmpServiceCmHandle getNcmpServiceCmHandle(final String cmHandleId) {
        CpsValidator.validateNameCharacters(cmHandleId);
        return YangDataConverter.convertYangModelCmHandleToNcmpServiceCmHandle(
                inventoryPersistence.getYangModelCmHandle(cmHandleId));
    }

    /**
     * Get cm handle public properties for a given cm handle id.
     *
     * @param cmHandleId cm handle identifier
     * @return cm handle public properties
     */
    @Override
    public Map<String, String> getCmHandlePublicProperties(final String cmHandleId) {
        CpsValidator.validateNameCharacters(cmHandleId);
        final YangModelCmHandle yangModelCmHandle =
            inventoryPersistence.getYangModelCmHandle(cmHandleId);
        final List<YangModelCmHandle.Property> yangModelPublicProperties = yangModelCmHandle.getPublicProperties();
        final Map<String, String> cmHandlePublicProperties = new HashMap<>();
        YangDataConverter.asPropertiesMap(yangModelPublicProperties, cmHandlePublicProperties);
        return cmHandlePublicProperties;
    }

    /**
     * Get cm handle composite state for a given cm handle id.
     *
     * @param cmHandleId cm handle identifier
     * @return cm handle state
     */
    @Override
    public CompositeState getCmHandleCompositeState(final String cmHandleId) {
        CpsValidator.validateNameCharacters(cmHandleId);
        return inventoryPersistence.getYangModelCmHandle(cmHandleId).getCompositeState();
    }

    /**
     * THis method registers a cm handle and initiates modules sync.
     *
     * @param dmiPluginRegistration dmi plugin registration information.
     * @return cm-handle registration response for create cm-handle requests.
     */
    public List<CmHandleRegistrationResponse> parseAndCreateCmHandlesInDmiRegistrationAndSyncModules(
            final DmiPluginRegistration dmiPluginRegistration) {
        List<CmHandleRegistrationResponse> cmHandleRegistrationResponses = new ArrayList<>();
        try {
            cmHandleRegistrationResponses = dmiPluginRegistration.getCreatedCmHandles().stream()
                .map(cmHandle ->
                    YangModelCmHandle.toYangModelCmHandle(
                        dmiPluginRegistration.getDmiPlugin(),
                        dmiPluginRegistration.getDmiDataPlugin(),
                        dmiPluginRegistration.getDmiModelPlugin(),
                        cmHandle)).map(this::registerNewCmHandle).collect(Collectors.toList());
        } catch (final DataValidationException dataValidationException) {
            cmHandleRegistrationResponses.add(CmHandleRegistrationResponse.createFailureResponse(dmiPluginRegistration
                            .getCreatedCmHandles().stream()
                            .map(NcmpServiceCmHandle::getCmHandleId).findFirst().orElse(null),
                    RegistrationError.CM_HANDLE_INVALID_ID));
        }
        return cmHandleRegistrationResponses;
    }

    protected List<CmHandleRegistrationResponse> parseAndRemoveCmHandlesInDmiRegistration(
            final List<String> tobeRemovedCmHandles) {
        final List<CmHandleRegistrationResponse> cmHandleRegistrationResponses =
                new ArrayList<>(tobeRemovedCmHandles.size());
        for (final String cmHandleId : tobeRemovedCmHandles) {
            try {
                CpsValidator.validateNameCharacters(cmHandleId);
                final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
                lcmEventsCmHandleStateHandler.updateCmHandleState(yangModelCmHandle,
                        CmHandleState.DELETING);
                deleteCmHandleByCmHandleId(cmHandleId);
                cmHandleRegistrationResponses.add(CmHandleRegistrationResponse.createSuccessResponse(cmHandleId));
                lcmEventsCmHandleStateHandler.updateCmHandleState(yangModelCmHandle,
                        CmHandleState.DELETED);
            } catch (final DataNodeNotFoundException dataNodeNotFoundException) {
                log.error("Unable to find dataNode for cmHandleId : {} , caused by : {}",
                        cmHandleId, dataNodeNotFoundException.getMessage());
                cmHandleRegistrationResponses.add(CmHandleRegistrationResponse
                        .createFailureResponse(cmHandleId, RegistrationError.CM_HANDLE_DOES_NOT_EXIST));
            } catch (final DataValidationException dataValidationException) {
                log.error("Unable to de-register cm-handle id: {}, caused by: {}",
                        cmHandleId, dataValidationException.getMessage());
                cmHandleRegistrationResponses.add(CmHandleRegistrationResponse
                        .createFailureResponse(cmHandleId, RegistrationError.CM_HANDLE_INVALID_ID));
            } catch (final Exception exception) {
                log.error("Unable to de-register cm-handle id : {} , caused by : {}",
                        cmHandleId, exception.getMessage());
                cmHandleRegistrationResponses.add(
                        CmHandleRegistrationResponse.createFailureResponse(cmHandleId, exception));
            }
        }
        return cmHandleRegistrationResponses;
    }

    private void deleteCmHandleByCmHandleId(final String cmHandleId) {
        inventoryPersistence.deleteSchemaSetWithCascade(cmHandleId);
        inventoryPersistence.deleteListOrListElement("/dmi-registry/cm-handles[@id='" + cmHandleId + "']");
    }

    private CmHandleRegistrationResponse registerNewCmHandle(final YangModelCmHandle yangModelCmHandle) {
        try {
            lcmEventsCmHandleStateHandler.updateCmHandleState(yangModelCmHandle, CmHandleState.ADVISED);
            return CmHandleRegistrationResponse.createSuccessResponse(yangModelCmHandle.getId());
        } catch (final AlreadyDefinedException alreadyDefinedException) {
            return CmHandleRegistrationResponse.createFailureResponse(
                    yangModelCmHandle.getId(), RegistrationError.CM_HANDLE_ALREADY_EXIST);
        } catch (final Exception exception) {
            return CmHandleRegistrationResponse.createFailureResponse(yangModelCmHandle.getId(), exception);
        }
    }
}
