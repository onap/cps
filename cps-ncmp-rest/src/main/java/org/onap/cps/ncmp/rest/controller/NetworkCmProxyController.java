/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2021 highstreet technologies GmbH
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

package org.onap.cps.ncmp.rest.controller;

import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.OPERATIONAL;
import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_RUNNING;
import static org.onap.cps.ncmp.api.impl.operations.OperationType.CREATE;
import static org.onap.cps.ncmp.api.impl.operations.OperationType.DELETE;
import static org.onap.cps.ncmp.api.impl.operations.OperationType.PATCH;
import static org.onap.cps.ncmp.api.impl.operations.OperationType.UPDATE;

import io.micrometer.core.annotation.Timed;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.impl.exception.InvalidDatastoreException;
import org.onap.cps.ncmp.api.impl.inventory.CompositeState;
import org.onap.cps.ncmp.api.impl.operations.DatastoreType;
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevel;
import org.onap.cps.ncmp.api.models.CmHandleQueryApiParameters;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.rest.api.NetworkCmProxyApi;
import org.onap.cps.ncmp.rest.controller.handlers.NcmpCachedResourceRequestHandler;
import org.onap.cps.ncmp.rest.controller.handlers.NcmpDatastoreRequestHandler;
import org.onap.cps.ncmp.rest.controller.handlers.NcmpPassthroughResourceRequestHandler;
import org.onap.cps.ncmp.rest.mapper.CmHandleStateMapper;
import org.onap.cps.ncmp.rest.mapper.DataOperationRequestMapper;
import org.onap.cps.ncmp.rest.model.CmHandlePublicProperties;
import org.onap.cps.ncmp.rest.model.CmHandleQueryParameters;
import org.onap.cps.ncmp.rest.model.DataOperationRequest;
import org.onap.cps.ncmp.rest.model.RestModuleDefinition;
import org.onap.cps.ncmp.rest.model.RestModuleReference;
import org.onap.cps.ncmp.rest.model.RestOutputCmHandle;
import org.onap.cps.ncmp.rest.model.RestOutputCmHandleCompositeState;
import org.onap.cps.ncmp.rest.model.RestOutputCmHandlePublicProperties;
import org.onap.cps.ncmp.rest.util.DeprecationHelper;
import org.onap.cps.spi.model.ModuleDefinition;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("${rest.api.ncmp-base-path}")
@RequiredArgsConstructor
public class NetworkCmProxyController implements NetworkCmProxyApi {

    private static final String NO_BODY = null;
    private final NetworkCmProxyDataService networkCmProxyDataService;
    private final JsonObjectMapper jsonObjectMapper;
    private final DeprecationHelper deprecationHelper;
    private final NcmpRestInputMapper ncmpRestInputMapper;
    private final CmHandleStateMapper cmHandleStateMapper;
    private final NcmpCachedResourceRequestHandler ncmpCachedResourceRequestHandler;
    private final NcmpPassthroughResourceRequestHandler ncmpPassthroughResourceRequestHandler;
    private final DataOperationRequestMapper dataOperationRequestMapper;
    private final Map<String, TrustLevel> trustLevelPerCmHandle;

    /**
     * Get resource data from datastore.
     *
     * @param datastoreName        name of the datastore
     * @param cmHandle             cm handle identifier
     * @param resourceIdentifier   resource identifier
     * @param optionsParamInQuery  options query parameter
     * @param topicParamInQuery    topic query parameter
     * @param includeDescendants   whether to include descendants or not
     * @return {@code ResponseEntity} response from dmi plugin
     */
    @Override
    @Timed(value = "cps.ncmp.controller.get", description = "Time taken to get resource data from datastore")
    public ResponseEntity<Object> getResourceDataForCmHandle(final String datastoreName,
                                                             final String cmHandle,
                                                             final String resourceIdentifier,
                                                             final String optionsParamInQuery,
                                                             final String topicParamInQuery,
                                                             final Boolean includeDescendants) {
        final NcmpDatastoreRequestHandler ncmpDatastoreRequestHandler = getNcmpDatastoreRequestHandler(datastoreName);
        return ncmpDatastoreRequestHandler.executeRequest(datastoreName, cmHandle, resourceIdentifier,
                optionsParamInQuery, topicParamInQuery, includeDescendants);
    }

    @Override
    public ResponseEntity<Object> executeDataOperationForCmHandles(final String topicParamInQuery,
                                                                  final DataOperationRequest
                                                                          dataOperationRequest) {
        return ncmpPassthroughResourceRequestHandler.executeRequest(topicParamInQuery,
                dataOperationRequestMapper.toDataOperationRequest(dataOperationRequest));
    }

    /**
     * Query resource data from datastore.
     *
     * @param datastoreName        name of the datastore
     * @param cmHandle             cm handle identifier
     * @param cpsPath              CPS Path
     * @param optionsParamInQuery  options query parameter
     * @param topicParamInQuery    topic query parameter
     * @param includeDescendants   whether to include descendants or not
     * @return {@code ResponseEntity} response from dmi plugin
     */

    @Override
    public ResponseEntity<Object> queryResourceDataForCmHandle(final String datastoreName,
                                                               final String cmHandle,
                                                               final String cpsPath,
                                                               final String optionsParamInQuery,
                                                               final String topicParamInQuery,
                                                               final Boolean includeDescendants) {
        validateDataStore(OPERATIONAL, datastoreName);
        return ncmpCachedResourceRequestHandler.executeRequest(cmHandle, cpsPath, includeDescendants);
    }

    /**
     * Patch resource data from passthrough-running.
     *
     * @param datastoreName      name of the datastore
     * @param cmHandle           cm handle identifier
     * @param resourceIdentifier resource identifier
     * @param requestBody        the request body
     * @param contentType        content type of body
     * @return {@code ResponseEntity} response from dmi plugin
     */

    @Override
    public ResponseEntity<Object> patchResourceDataRunningForCmHandle(final String datastoreName,
                                                                      final String cmHandle,
                                                                      final String resourceIdentifier,
                                                                      final Object requestBody,
                                                                      final String contentType) {

        validateDataStore(PASSTHROUGH_RUNNING, datastoreName);

        final Object responseObject = networkCmProxyDataService
                .writeResourceDataPassThroughRunningForCmHandle(
                        cmHandle, resourceIdentifier, PATCH,
                        jsonObjectMapper.asJsonString(requestBody), contentType);
        return ResponseEntity.ok(responseObject);
    }

    /**
     * Create resource data in datastore pass-through running for given cm-handle.
     *
     * @param datastoreName      name of the datastore
     * @param cmHandle           cm handle identifier
     * @param resourceIdentifier resource identifier
     * @param requestBody        the request body
     * @param contentType        content type of body
     * @return {@code ResponseEntity} response from dmi plugin
     */
    @Override
    public ResponseEntity<Void> createResourceDataRunningForCmHandle(final String datastoreName,
                                                                     final String cmHandle,
                                                                     final String resourceIdentifier,
                                                                     final Object requestBody,
                                                                     final String contentType) {

        validateDataStore(PASSTHROUGH_RUNNING, datastoreName);

        networkCmProxyDataService.writeResourceDataPassThroughRunningForCmHandle(cmHandle,
                resourceIdentifier, CREATE, jsonObjectMapper.asJsonString(requestBody), contentType);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Update resource data in datastore pass-through running for given cm-handle.
     *
     * @param datastoreName      name of the datastore
     * @param cmHandle           cm handle identifier
     * @param resourceIdentifier resource identifier
     * @param requestBody        the request body
     * @param contentType        content type of the body
     * @return response entity
     */

    @Override
    public ResponseEntity<Object> updateResourceDataRunningForCmHandle(final String datastoreName,
                                                                       final String cmHandle,
                                                                       final String resourceIdentifier,
                                                                       final Object requestBody,
                                                                       final String contentType) {
        validateDataStore(PASSTHROUGH_RUNNING, datastoreName);

        networkCmProxyDataService.writeResourceDataPassThroughRunningForCmHandle(cmHandle,
                resourceIdentifier, UPDATE, jsonObjectMapper.asJsonString(requestBody), contentType);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Delete resource data in datastore pass-through running for a given cm-handle.
     *
     * @param datastoreName      name of the datastore
     * @param cmHandle           cm handle identifier
     * @param resourceIdentifier resource identifier
     * @param contentType        content type of the body
     * @return response entity no content if request is successful
     */
    @Override
    public ResponseEntity<Void> deleteResourceDataRunningForCmHandle(final String datastoreName,
                                                                     final String cmHandle,
                                                                     final String resourceIdentifier,
                                                                     final String contentType) {

        validateDataStore(PASSTHROUGH_RUNNING, datastoreName);

        networkCmProxyDataService.writeResourceDataPassThroughRunningForCmHandle(cmHandle,
                resourceIdentifier, DELETE, NO_BODY, contentType);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Query and return cm handles that match the given query parameters.
     *
     * @param cmHandleQueryParameters the cm handle query parameters
     * @return collection of cm handles
     */
    @Override
    @SuppressWarnings("deprecation") // mapOldConditionProperties method will be removed in Release 12
    public ResponseEntity<List<RestOutputCmHandle>> searchCmHandles(
            final CmHandleQueryParameters cmHandleQueryParameters) {
        final CmHandleQueryApiParameters cmHandleQueryApiParameters =
                deprecationHelper.mapOldConditionProperties(cmHandleQueryParameters);
        final Collection<NcmpServiceCmHandle> cmHandles = networkCmProxyDataService
                .executeCmHandleSearch(cmHandleQueryApiParameters);
        final List<RestOutputCmHandle> outputCmHandles =
                cmHandles.stream().map(this::toRestOutputCmHandle).collect(Collectors.toList());
        return ResponseEntity.ok(outputCmHandles);
    }

    /**
     * Query and return cm handle ids that match the given query parameters.
     *
     * @param cmHandleQueryParameters the cm handle query parameters
     * @return collection of cm handle ids
     */
    @Override
    public ResponseEntity<List<String>> searchCmHandleIds(
            final CmHandleQueryParameters cmHandleQueryParameters) {
        final CmHandleQueryApiParameters cmHandleQueryApiParameters =
                jsonObjectMapper.convertToValueType(cmHandleQueryParameters, CmHandleQueryApiParameters.class);
        final Collection<String> cmHandleIds
            = networkCmProxyDataService.executeCmHandleIdSearch(cmHandleQueryApiParameters);
        return ResponseEntity.ok(List.copyOf(cmHandleIds));
    }

    /**
     * Search for Cm Handle and Properties by Name.
     *
     * @param cmHandleId cm-handle identifier
     * @return cm handle and its properties
     */
    @Override
    public ResponseEntity<RestOutputCmHandle> retrieveCmHandleDetailsById(final String cmHandleId) {
        final NcmpServiceCmHandle ncmpServiceCmHandle = networkCmProxyDataService.getNcmpServiceCmHandle(cmHandleId);
        final RestOutputCmHandle restOutputCmHandle = toRestOutputCmHandle(ncmpServiceCmHandle);
        return ResponseEntity.ok(restOutputCmHandle);
    }

    /**
     * Get Cm Handle Properties by Cm Handle Id.
     *
     * @param cmHandleId cm-handle identifier
     * @return cm handle properties
     */
    @Override
    public ResponseEntity<RestOutputCmHandlePublicProperties> getCmHandlePublicPropertiesByCmHandleId(
            final String cmHandleId) {
        final CmHandlePublicProperties cmHandlePublicProperties = new CmHandlePublicProperties();
        cmHandlePublicProperties.add(networkCmProxyDataService.getCmHandlePublicProperties(cmHandleId));
        final RestOutputCmHandlePublicProperties restOutputCmHandlePublicProperties =
                new RestOutputCmHandlePublicProperties();
        restOutputCmHandlePublicProperties.setPublicCmHandleProperties(cmHandlePublicProperties);
        return ResponseEntity.ok(restOutputCmHandlePublicProperties);
    }

    /**
     * Get Cm Handle State by Cm Handle Id.
     *
     * @param cmHandleId cm-handle identifier
     * @return cm handle state
     */
    @Override
    public ResponseEntity<RestOutputCmHandleCompositeState> getCmHandleStateByCmHandleId(
            final String cmHandleId) {
        final CompositeState cmHandleState = networkCmProxyDataService.getCmHandleCompositeState(cmHandleId);
        final RestOutputCmHandleCompositeState restOutputCmHandleCompositeState =
                new RestOutputCmHandleCompositeState();
        restOutputCmHandleCompositeState.setState(
                cmHandleStateMapper.toCmHandleCompositeStateExternalLockReason(cmHandleState));
        return ResponseEntity.ok(restOutputCmHandleCompositeState);
    }

    /**
     * Return module definitions.
     *
     * @param cmHandleId    cm-handle identifier
     * @param moduleName    module name
     * @param revision      the revision of the module
     * @return list of module definitions (module name, revision, yang resource content)
     */
    @Override
    public ResponseEntity<List<RestModuleDefinition>> getModuleDefinitions(final String cmHandleId,
                                                                           final String moduleName,
                                                                           final String revision) {
        final Collection<ModuleDefinition> moduleDefinitions;
        if (StringUtils.hasText(moduleName)) {
            moduleDefinitions =
                networkCmProxyDataService.getModuleDefinitionsByCmHandleAndModule(cmHandleId, moduleName, revision);
        } else {
            moduleDefinitions = networkCmProxyDataService.getModuleDefinitionsByCmHandleId(cmHandleId);
            if (StringUtils.hasText(revision)) {
                log.warn("Ignoring revision filter as no module name is provided");
            }
        }
        final List<RestModuleDefinition> response = new ArrayList<>();
        for (final ModuleDefinition moduleDefinition: moduleDefinitions) {
            response.add(ncmpRestInputMapper.toRestModuleDefinition(moduleDefinition));
        }
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    /**
     * Return module references for a cm handle.
     *
     * @param cmHandle the cm handle
     * @return module references for cm handle. Namespace will be always blank because restConf does not include this.
     */
    public ResponseEntity<List<RestModuleReference>> getModuleReferencesByCmHandle(final String cmHandle) {
        final List<RestModuleReference> restModuleReferences =
                networkCmProxyDataService.getYangResourcesModuleReferences(cmHandle).stream()
                        .map(ncmpRestInputMapper::toRestModuleReference)
                        .collect(Collectors.toList());
        return new ResponseEntity<>(restModuleReferences, HttpStatus.OK);
    }

    /**
     * Set the data sync enabled flag, along with the data sync state for the specified cm handle.
     *
     * @param cmHandleId          cm handle id
     * @param dataSyncEnabledFlag data sync enabled flag
     * @return response entity ok if request is successful
     */
    @Override
    public ResponseEntity<Object> setDataSyncEnabledFlagForCmHandle(final String cmHandleId,
                                                                    final Boolean dataSyncEnabledFlag) {
        networkCmProxyDataService.setDataSyncEnabled(cmHandleId, dataSyncEnabledFlag);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    private RestOutputCmHandle toRestOutputCmHandle(final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final RestOutputCmHandle restOutputCmHandle = new RestOutputCmHandle();
        final CmHandlePublicProperties cmHandlePublicProperties = new CmHandlePublicProperties();
        final TrustLevel cmHandleCurrentTrustLevel = trustLevelPerCmHandle.get(ncmpServiceCmHandle.getCmHandleId());
        restOutputCmHandle.setCmHandle(ncmpServiceCmHandle.getCmHandleId());
        cmHandlePublicProperties.add(ncmpServiceCmHandle.getPublicProperties());
        restOutputCmHandle.setPublicCmHandleProperties(cmHandlePublicProperties);
        restOutputCmHandle.setState(cmHandleStateMapper.toCmHandleCompositeStateExternalLockReason(
                ncmpServiceCmHandle.getCompositeState()));
        if (cmHandleCurrentTrustLevel != null) {
            restOutputCmHandle.setTrustLevel(cmHandleCurrentTrustLevel.toString());
        }
        return restOutputCmHandle;
    }

    private void validateDataStore(final DatastoreType acceptableDataStoreType, final String requestedDatastoreName) {
        final DatastoreType datastoreType = DatastoreType.fromDatastoreName(requestedDatastoreName);

        if (acceptableDataStoreType != datastoreType) {
            throw new InvalidDatastoreException(requestedDatastoreName + " is not supported");
        }
    }

    private NcmpDatastoreRequestHandler getNcmpDatastoreRequestHandler(final String datastoreName) {
        if (OPERATIONAL.equals(DatastoreType.fromDatastoreName(datastoreName))) {
            return ncmpCachedResourceRequestHandler;
        }
        return ncmpPassthroughResourceRequestHandler;
    }


}

