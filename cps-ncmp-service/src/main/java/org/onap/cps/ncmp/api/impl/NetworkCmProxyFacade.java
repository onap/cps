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

package org.onap.cps.ncmp.api.impl;

import static org.onap.cps.ncmp.api.impl.utils.RestQueryParametersValidator.validateCmHandleQueryParameters;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.CpsDataService;
import org.onap.cps.ncmp.api.ParameterizedCmHandleQueryService;
import org.onap.cps.ncmp.api.impl.inventory.CompositeState;
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations;
import org.onap.cps.ncmp.api.impl.operations.OperationType;
import org.onap.cps.ncmp.api.impl.utils.CmHandleQueryConditions;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.models.CmHandleQueryApiParameters;
import org.onap.cps.ncmp.api.models.CmHandleQueryServiceParameters;
import org.onap.cps.ncmp.api.models.CmHandleRegistrationService;
import org.onap.cps.ncmp.api.models.CmResourceAddress;
import org.onap.cps.ncmp.api.models.DataOperationRequest;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.ModuleDefinition;
import org.onap.cps.spi.model.ModuleReference;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class NetworkCmProxyFacade {

    private final JsonObjectMapper jsonObjectMapper;
    private final DmiDataOperations dmiDataOperations;
    private final ParameterizedCmHandleQueryService parameterizedCmHandleQueryService;
    private final CpsDataService cpsDataService;

    /* TODO below services should not be used by this Facade.
       Related methods (7 of 11) can be moved to Inventory facade but then REST Controller needs to used both facades...
       Maybe that is better as then any inconsistencies are limited to the REST layer and not in the service layer too
     */
    private final CmHandleRegistrationService cmHandleRegistrationService;
    private final InventoryPersistence inventoryPersistence;

    /**
     * Fetches resource data for a given data store using DMI (Data Management Interface).
     * This method retrieves data based on the provided CmResourceAddress and additional query parameters.
     * It supports asynchronous processing and handles authorization if required.
     *
     * @param cmResourceAddress   The target data store, including the CM handle and resource identifier.
     *                            This parameter must not be null.
     * @param optionsParamInQuery Additional query parameters that may influence the data retrieval process,
     *                            such as filters or limits. This parameter can be null.
     * @param topicParamInQuery   The topic name for triggering asynchronous responses. If specified,
     *                            the response will be sent to this topic. This parameter can be null.
     * @param requestId           A unique identifier for the request, used for tracking and correlating
     *                            asynchronous operations. This parameter must not be null.
     * @param authorization       The contents of the Authorization header. This parameter can be null
     *                            if authorization is not required.
     * @return {@code Mono<Object>} A reactive Mono that emits the resource data on successful retrieval
     *     or an error signal if the operation fails. The Mono represents a single asynchronous
     *     computation result.
     */
    public Mono<Object> getResourceDataForCmHandle(final CmResourceAddress cmResourceAddress,
                                                   final String optionsParamInQuery,
                                                   final String topicParamInQuery,
                                                   final String requestId,
                                                   final String authorization) {
        return dmiDataOperations.getResourceDataFromDmi(cmResourceAddress, optionsParamInQuery, topicParamInQuery,
                requestId, authorization)
            .flatMap(responseEntity -> Mono.justOrEmpty(responseEntity.getBody()));
    }

    /**
     * Get resource data for operational.
     *
     * @param cmResourceAddress     target datastore, cm handle and resource identifier
     * @Link FetchDescendantsOption fetch descendants option
     * @return {@code Object} resource data
     */
    public Object getResourceDataForCmHandle(final CmResourceAddress cmResourceAddress,
                                             final FetchDescendantsOption fetchDescendantsOption) {
        return cpsDataService.getDataNodes(cmResourceAddress.datastoreName(),
                                           cmResourceAddress.cmHandleId(),
                                           cmResourceAddress.resourceIdentifier(),
                                           fetchDescendantsOption).iterator().next();
    }

    /**
     * Execute (async) data operation for group of cm handles using dmi.
     *
     * @param topicParamInQuery        topic name for (triggering) async responses
     * @param dataOperationRequest     contains a list of operation definitions(multiple operations)
     * @param requestId                request ID
     * @param authorization            contents of Authorization header, or null if not present
     */
    public void executeDataOperationForCmHandles(final String topicParamInQuery,
                                                 final DataOperationRequest dataOperationRequest,
                                                 final String requestId,
                                                 final String authorization) {
        dmiDataOperations.requestResourceDataFromDmi(topicParamInQuery, dataOperationRequest, requestId,
                authorization);
    }

    /**
     * Write resource data for data store pass-through running using dmi for given cm-handle.
     *
     * @param cmHandleId         cm handle identifier
     * @param resourceIdentifier resource identifier
     * @param operationType      required operation type
     * @param requestData        request body to create resource
     * @param dataType        content type in body
     * @param authorization       contents of Authorization header, or null if not present
     * @return {@code Object} return data
     */
    public Object writeResourceDataPassThroughRunningForCmHandle(final String cmHandleId,
                                                                 final String resourceIdentifier,
                                                                 final OperationType operationType,
                                                                 final String requestData,
                                                                 final String dataType,
                                                                 final String authorization) {
        return dmiDataOperations.writeResourceDataPassThroughRunningFromDmi(cmHandleId, resourceIdentifier,
            operationType, requestData, dataType, authorization);
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
        return parameterizedCmHandleQueryService.queryCmHandles(cmHandleQueryServiceParameters);
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
        return YangDataConverter.convertYangModelCmHandleToNcmpServiceCmHandle(
            inventoryPersistence.getYangModelCmHandle(cmHandleId));
    }

    /**
     * Get cm handle public properties for a given cm handle id.
     *
     * @param cmHandleId cm handle identifier
     * @return cm handle public properties
     */
    public Map<String, String> getCmHandlePublicProperties(final String cmHandleId) {
        final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
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
    public CompositeState getCmHandleCompositeState(final String cmHandleId) {
        return inventoryPersistence.getYangModelCmHandle(cmHandleId).getCompositeState();
    }

}
