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

import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.impl.exception.HttpClientRequestException;
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations;
import org.onap.cps.ncmp.api.impl.operations.DmiOperations;
import org.onap.cps.ncmp.api.impl.operations.YangModelCmHandleRetriever;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
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

    private final YangModelCmHandleRetriever yangModelCmHandleRetriever;

    private final ModuleSyncService moduleSyncService;

    @Override
    public DmiPluginRegistrationResponse updateDmiRegistrationAndSyncModule(
        final DmiPluginRegistration dmiPluginRegistration) {
        dmiPluginRegistration.validateDmiPluginRegistration();
        final var dmiPluginRegistrationResponse = new DmiPluginRegistrationResponse();
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
        CpsValidator.validateNameCharacters(cmHandleId);
        return getResourceDataResponse(cmHandleId, resourceIdentifier,
                DmiOperations.DataStoreEnum.PASSTHROUGH_OPERATIONAL, optionsParamInQuery, topicParamInQuery, requestId);
    }

    @Override
    public Object getResourceDataPassThroughRunningForCmHandle(final String cmHandleId,
                                                               final String resourceIdentifier,
                                                               final String optionsParamInQuery,
                                                               final String topicParamInQuery,
                                                               final String requestId) {
        CpsValidator.validateNameCharacters(cmHandleId);
        return getResourceDataResponse(cmHandleId, resourceIdentifier,
                DmiOperations.DataStoreEnum.PASSTHROUGH_RUNNING, optionsParamInQuery, topicParamInQuery, requestId);
    }

    @Override
    public Object writeResourceDataPassThroughRunningForCmHandle(final String cmHandleId,
                                                               final String resourceIdentifier,
                                                               final OperationEnum operation,
                                                               final String requestData,
                                                               final String dataType) {
        CpsValidator.validateNameCharacters(cmHandleId);
        return handleResponse(
                dmiDataOperations.writeResourceDataPassThroughRunningFromDmi(cmHandleId, resourceIdentifier, operation,
                        requestData, dataType), operation);
    }


    @Override
    public Collection<ModuleReference> getYangResourcesModuleReferences(final String cmHandleId) {
        CpsValidator.validateNameCharacters(cmHandleId);
        return cpsModuleService.getYangResourcesModuleReferences(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, cmHandleId);
    }

    /**
     * Retrieve cm handle identifiers for the given list of module names.
     *
     * @param moduleNames module names.
     * @return a collection of anchor identifiers
     */
    @Override
    public Collection<String> executeCmHandleHasAllModulesSearch(final Collection<String> moduleNames) {
        return cpsAdminService.queryAnchorNames(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, moduleNames);
    }

    @Override
    public Set<String> queryCmHandles(final CmHandleQueryApiParameters cmHandleQueryApiParameters) {

        cmHandleQueryApiParameters.getPublicProperties().forEach((key, value) -> {
            if (Strings.isNullOrEmpty(key)) {
                throw new DataValidationException("Invalid Query Parameter.",
                    "Missing property name - please supply a valid name.");
            }
        });

        return cpsAdminService.queryCmHandles(jsonObjectMapper.convertToValueType(cmHandleQueryApiParameters,
                org.onap.cps.spi.model.CmHandleQueryParameters.class));
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
        final NcmpServiceCmHandle ncmpServiceCmHandle = new NcmpServiceCmHandle();
        final YangModelCmHandle yangModelCmHandle =
            yangModelCmHandleRetriever.getYangModelCmHandle(cmHandleId);
        final List<YangModelCmHandle.Property> dmiProperties = yangModelCmHandle.getDmiProperties();
        final List<YangModelCmHandle.Property> publicProperties = yangModelCmHandle.getPublicProperties();
        ncmpServiceCmHandle.setCmHandleId(yangModelCmHandle.getId());
        setDmiProperties(dmiProperties, ncmpServiceCmHandle);
        setPublicProperties(publicProperties, ncmpServiceCmHandle);
        return ncmpServiceCmHandle;
    }

    /**
     * Retrieve cm handle public properties for a given cm handle id.
     *
     * @param cmHandleId cm handle identifier
     * @return cm handle details
     */
    @Override
    public Map<String, String> getCmHandlePublicProperties(final String cmHandleId) {
        CpsValidator.validateNameCharacters(cmHandleId);
        final YangModelCmHandle yangModelCmHandle =
            yangModelCmHandleRetriever.getYangModelCmHandle(cmHandleId);
        final List<YangModelCmHandle.Property> yangModelPublicProperties = yangModelCmHandle.getPublicProperties();
        final Map<String, String> cmHandlePublicProperties = new HashMap<>();
        asPropertiesMap(yangModelPublicProperties, cmHandlePublicProperties);
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
                .map(cmHandle ->
                    YangModelCmHandle.toYangModelCmHandle(
                        dmiPluginRegistration.getDmiPlugin(),
                        dmiPluginRegistration.getDmiDataPlugin(),
                        dmiPluginRegistration.getDmiModelPlugin(), cmHandle)
                )
                .map(this::registerAndSyncNewCmHandle)
                .collect(Collectors.toList());
        } catch (final DataValidationException dataValidationException) {
            cmHandleRegistrationResponses.add(CmHandleRegistrationResponse.createFailureResponse(dmiPluginRegistration
                    .getCreatedCmHandles().stream()
                    .map(NcmpServiceCmHandle::getCmHandleId).findFirst().orElse(null),
                RegistrationError.CM_HANDLE_INVALID_ID));
        }
        return cmHandleRegistrationResponses;
    }

    protected void syncModulesAndCreateAnchor(final YangModelCmHandle yangModelCmHandle) {
        final String schemaSetName = moduleSyncService.syncAndCreateSchemaSet(yangModelCmHandle);
        final String anchorName = yangModelCmHandle.getId();
        cpsAdminService.createAnchor(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, schemaSetName,
                anchorName);
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

    private Object getResourceDataResponse(final String cmHandleId,
                                           final String resourceIdentifier,
                                           final DmiOperations.DataStoreEnum dataStore,
                                           final String optionsParamInQuery,
                                           final String topicParamInQuery,
                                           final String requestId) {
        final ResponseEntity<?> responseEntity = dmiDataOperations.getResourceDataFromDmi(
                cmHandleId, resourceIdentifier, optionsParamInQuery, dataStore, requestId, topicParamInQuery);
        return handleResponse(responseEntity, OperationEnum.READ);
    }

    private void setDmiProperties(final List<YangModelCmHandle.Property> dmiProperties,
                                  final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final Map<String, String> dmiPropertiesMap = new LinkedHashMap<>(dmiProperties.size());
        asPropertiesMap(dmiProperties, dmiPropertiesMap);
        ncmpServiceCmHandle.setDmiProperties(dmiPropertiesMap);
    }

    private void setPublicProperties(final List<YangModelCmHandle.Property> publicProperties,
                                     final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final Map<String, String> publicPropertiesMap = new LinkedHashMap<>();
        asPropertiesMap(publicProperties, publicPropertiesMap);
        ncmpServiceCmHandle.setPublicProperties(publicPropertiesMap);
    }

    private void asPropertiesMap(final List<YangModelCmHandle.Property> properties,
                                 final Map<String, String> propertiesMap) {
        for (final YangModelCmHandle.Property property: properties) {
            propertiesMap.put(property.getName(), property.getValue());
        }
    }


    private CmHandleRegistrationResponse registerAndSyncNewCmHandle(final YangModelCmHandle yangModelCmHandle) {
        try {
            final String cmHandleJsonData = String.format("{\"cm-handles\":[%s]}",
                jsonObjectMapper.asJsonString(yangModelCmHandle));
            cpsDataService.saveListElements(NCMP_DATASPACE_NAME, NCMP_DMI_REGISTRY_ANCHOR, NCMP_DMI_REGISTRY_PARENT,
                cmHandleJsonData, NO_TIMESTAMP);
            syncModulesAndCreateAnchor(yangModelCmHandle);
            return CmHandleRegistrationResponse.createSuccessResponse(yangModelCmHandle.getId());
        } catch (final AlreadyDefinedException alreadyDefinedException) {
            return CmHandleRegistrationResponse.createFailureResponse(
                yangModelCmHandle.getId(), RegistrationError.CM_HANDLE_ALREADY_EXIST);
        } catch (final Exception exception) {
            return CmHandleRegistrationResponse.createFailureResponse(yangModelCmHandle.getId(), exception);
        }
    }

    private static Object handleResponse(final ResponseEntity<?> responseEntity, final OperationEnum operation) {
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            return responseEntity.getBody();
        } else {
            final String exceptionMessage = "Unable to " + operation.toString() + " resource data.";
            throw new HttpClientRequestException(exceptionMessage, (String) responseEntity.getBody(),
                responseEntity.getStatusCodeValue());
        }
    }

}