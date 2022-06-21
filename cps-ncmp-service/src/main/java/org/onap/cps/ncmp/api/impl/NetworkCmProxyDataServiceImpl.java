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

import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NCMP_DATASPACE_NAME;
import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NCMP_DMI_REGISTRY_ANCHOR;
import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NCMP_DMI_REGISTRY_PARENT;
import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME;
import static org.onap.cps.ncmp.api.impl.constants.DmiRegistryConstants.NO_TIMESTAMP;
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum;
import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED;
import static org.onap.cps.utils.CmHandleQueryRestParametersValidator.validateCmHandleQueryParameters;
import static org.onap.ncmp.cmhandle.lcm.event.Event.Operation.DELETE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.api.NetworkCmProxyCmHandlerQueryService;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.impl.event.NcmpEventsService;
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations;
import org.onap.cps.ncmp.api.impl.operations.DmiOperations;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CmHandleState;
import org.onap.cps.ncmp.api.inventory.CompositeState;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.inventory.sync.ModuleSyncService;
import org.onap.cps.ncmp.api.models.CmHandleQueryApiParameters;
import org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse;
import org.onap.cps.ncmp.api.models.CmHandleRegistrationResponse.RegistrationError;
import org.onap.cps.ncmp.api.models.DmiPluginRegistration;
import org.onap.cps.ncmp.api.models.DmiPluginRegistrationResponse;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.exceptions.SchemaSetNotFoundException;
import org.onap.cps.spi.model.CmHandleQueryServiceParameters;
import org.onap.cps.spi.model.ModuleReference;
import org.onap.cps.utils.CpsValidator;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkCmProxyDataServiceImpl implements NetworkCmProxyDataService {

    private final CpsDataService cpsDataService;

    private final JsonObjectMapper jsonObjectMapper;

    private final DmiDataOperations dmiDataOperations;

    private final CpsModuleService cpsModuleService;

    private final CpsAdminService cpsAdminService;

    private final NetworkCmProxyDataServicePropertyHandler networkCmProxyDataServicePropertyHandler;

    private final InventoryPersistence inventoryPersistence;

    private final ModuleSyncService moduleSyncService;

    private final NetworkCmProxyCmHandlerQueryService networkCmProxyCmHandlerQueryService;

    private final NcmpEventsService ncmpEventsService;

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
        return cpsModuleService.getYangResourcesModuleReferences(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId);
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

        return networkCmProxyCmHandlerQueryService.queryCmHandles(cmHandleQueryServiceParameters).stream()
                .map(dataNode -> YangDataConverter
                        .convertCmHandleToYangModel(dataNode, dataNode.getLeaves().get("id").toString()))
                .map(YangDataConverter::convertYangModelCmHandleToNcmpServiceCmHandle).collect(Collectors.toSet());
    }

    /**
     * Retrieve cm handle ids for the given query parameters.
     *
     * @param cmHandleQueryApiParameters cm handle query parameters
     * @return cm handle ids
     */
    @Override
    public Set<String> executeCmHandleIdSearch(final CmHandleQueryApiParameters cmHandleQueryApiParameters) {
        return executeCmHandleSearch(cmHandleQueryApiParameters).stream().map(NcmpServiceCmHandle::getCmHandleId)
                .collect(Collectors.toSet());
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
                .map(cmHandle -> {
                    cmHandle.setCompositeState(new CompositeState());
                    cmHandle.getCompositeState().setCmHandleState(CmHandleState.ADVISED);
                    cmHandle.getCompositeState().setLastUpdateTimeNow();
                    return YangModelCmHandle.toYangModelCmHandle(
                        dmiPluginRegistration.getDmiPlugin(),
                        dmiPluginRegistration.getDmiDataPlugin(),
                        dmiPluginRegistration.getDmiModelPlugin(),
                        cmHandle);
                    }
                )
                .map(this::registerNewCmHandle)
                .collect(Collectors.toList());
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
        for (final String cmHandle : tobeRemovedCmHandles) {
            try {
                CpsValidator.validateNameCharacters(cmHandle);
                deleteSchemaSetWithCascade(cmHandle);
                cpsDataService.deleteListOrListElement(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR,
                        "/dmi-registry/cm-handles[@id='" + cmHandle + "']", NO_TIMESTAMP);
                cmHandleRegistrationResponses.add(CmHandleRegistrationResponse.createSuccessResponse(cmHandle));
                log.debug("Publishing LCM Delete Event for cmHandleId : {}", cmHandle);
                ncmpEventsService.publishNcmpEvent(cmHandle, DELETE);
            } catch (final DataNodeNotFoundException dataNodeNotFoundException) {
                log.error("Unable to find dataNode for cmHandleId : {} , caused by : {}",
                        cmHandle, dataNodeNotFoundException.getMessage());
                cmHandleRegistrationResponses.add(CmHandleRegistrationResponse
                        .createFailureResponse(cmHandle, RegistrationError.CM_HANDLE_DOES_NOT_EXIST));
            } catch (final DataValidationException dataValidationException) {
                log.error("Unable to de-register cm-handle id: {}, caused by: {}",
                        cmHandle, dataValidationException.getMessage());
                cmHandleRegistrationResponses.add(CmHandleRegistrationResponse
                        .createFailureResponse(cmHandle, RegistrationError.CM_HANDLE_INVALID_ID));
            } catch (final Exception exception) {
                log.error("Unable to de-register cm-handle id : {} , caused by : {}",
                        cmHandle, exception.getMessage());
                cmHandleRegistrationResponses.add(
                        CmHandleRegistrationResponse.createFailureResponse(cmHandle, exception));
            }
        }
        return cmHandleRegistrationResponses;
    }

    private void deleteSchemaSetWithCascade(final String schemaSetName) {
        try {
            cpsModuleService.deleteSchemaSet(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, schemaSetName,
                    CASCADE_DELETE_ALLOWED);
        } catch (final SchemaSetNotFoundException schemaSetNotFoundException) {
            log.warn("Schema set {} does not exist or already deleted", schemaSetName);
        }
    }

    private CmHandleRegistrationResponse registerNewCmHandle(final YangModelCmHandle yangModelCmHandle) {
        try {
            final String cmHandleJsonData = String.format("{\"cm-handles\":[%s]}",
                    jsonObjectMapper.asJsonString(yangModelCmHandle));
            cpsDataService.saveListElements(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NCMP_DMI_REGISTRY_PARENT,
                    cmHandleJsonData, NO_TIMESTAMP);
            return CmHandleRegistrationResponse.createSuccessResponse(yangModelCmHandle.getId());
        } catch (final AlreadyDefinedException alreadyDefinedException) {
            return CmHandleRegistrationResponse.createFailureResponse(
                    yangModelCmHandle.getId(), RegistrationError.CM_HANDLE_ALREADY_EXIST);
        } catch (final Exception exception) {
            return CmHandleRegistrationResponse.createFailureResponse(yangModelCmHandle.getId(), exception);
        }
    }
}
