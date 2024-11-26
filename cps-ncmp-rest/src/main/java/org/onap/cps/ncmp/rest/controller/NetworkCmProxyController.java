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

import static org.onap.cps.ncmp.api.data.models.DatastoreType.OPERATIONAL;
import static org.onap.cps.ncmp.api.data.models.DatastoreType.PASSTHROUGH_RUNNING;
import static org.onap.cps.ncmp.api.data.models.OperationType.CREATE;
import static org.onap.cps.ncmp.api.data.models.OperationType.DELETE;
import static org.onap.cps.ncmp.api.data.models.OperationType.PATCH;
import static org.onap.cps.ncmp.api.data.models.OperationType.UPDATE;

import io.micrometer.core.annotation.Timed;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.model.ModuleDefinition;
import org.onap.cps.ncmp.api.data.exceptions.InvalidDatastoreException;
import org.onap.cps.ncmp.api.data.models.CmResourceAddress;
import org.onap.cps.ncmp.api.data.models.DatastoreType;
import org.onap.cps.ncmp.api.inventory.NetworkCmProxyInventoryFacade;
import org.onap.cps.ncmp.api.inventory.models.CmHandleQueryApiParameters;
import org.onap.cps.ncmp.api.inventory.models.CompositeState;
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.impl.data.NetworkCmProxyFacade;
import org.onap.cps.ncmp.rest.api.NetworkCmProxyApi;
import org.onap.cps.ncmp.rest.model.CmHandlePublicProperties;
import org.onap.cps.ncmp.rest.model.CmHandleQueryParameters;
import org.onap.cps.ncmp.rest.model.DataOperationRequest;
import org.onap.cps.ncmp.rest.model.RestModuleDefinition;
import org.onap.cps.ncmp.rest.model.RestModuleReference;
import org.onap.cps.ncmp.rest.model.RestOutputCmHandle;
import org.onap.cps.ncmp.rest.model.RestOutputCmHandleCompositeState;
import org.onap.cps.ncmp.rest.model.RestOutputCmHandlePublicProperties;
import org.onap.cps.ncmp.rest.util.CmHandleStateMapper;
import org.onap.cps.ncmp.rest.util.DataOperationRequestMapper;
import org.onap.cps.ncmp.rest.util.DeprecationHelper;
import org.onap.cps.ncmp.rest.util.NcmpRestInputMapper;
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
    private final NetworkCmProxyFacade networkCmProxyFacade;
    private final NetworkCmProxyInventoryFacade networkCmProxyInventoryFacade;
    private final JsonObjectMapper jsonObjectMapper;
    private final DeprecationHelper deprecationHelper;
    private final NcmpRestInputMapper ncmpRestInputMapper;
    private final CmHandleStateMapper cmHandleStateMapper;
    private final DataOperationRequestMapper dataOperationRequestMapper;

    /**
     * Get resource data from datastore.
     *
     * @param datastoreName        name of the datastore
     * @param cmHandleReference    cm handle or alternate id identifier
     * @param resourceIdentifier   resource identifier
     * @param optionsParamInQuery  options query parameter
     * @param topicParamInQuery    topic query parameter
     * @param includeDescendants   whether to include descendants or not
     * @param authorization        contents of Authorization header, or null if not present
     * @return {@code ResponseEntity} response from dmi plugin
     */
    @Override
    @Timed(value = "cps.ncmp.controller.get", description = "Time taken to get resource data from datastore")
    public ResponseEntity<Object> getResourceDataForCmHandle(final String datastoreName,
                                                             final String cmHandleReference,
                                                             final String resourceIdentifier,
                                                             final String optionsParamInQuery,
                                                             final String topicParamInQuery,
                                                             final Boolean includeDescendants,
                                                             final String authorization) {
        final CmResourceAddress cmResourceAddress = new CmResourceAddress(datastoreName, cmHandleReference,
            resourceIdentifier);
        final Object result = networkCmProxyFacade.getResourceDataForCmHandle(cmResourceAddress, optionsParamInQuery,
            topicParamInQuery, includeDescendants, authorization);
        return ResponseEntity.ok(result);
    }

    @Override
    public ResponseEntity<Object> executeDataOperationForCmHandles(final String topicParamInQuery,
                                                                   final DataOperationRequest dataOperationRequest,
                                                                   final String authorization) {
        final Object result = networkCmProxyFacade.executeDataOperationForCmHandles(topicParamInQuery,
                dataOperationRequestMapper.toDataOperationRequest(dataOperationRequest), authorization);
        return ResponseEntity.ok(result);
    }

    /**
     * Query resource data from datastore.
     *
     * @param datastoreName        name of the datastore (currently only supports "ncmp-datastore:operational")
     * @param cmHandle             cm handle identifier
     * @param cpsPath              CPS Path
     * @param optionsParamInQuery  options query parameter
     * @param topicParamInQuery    topic query parameter
     * @param includeDescendants   whether to include descendants or not
     * @return {@code ResponseEntity} response. Body contains a collection of DataNodes
     */

    @Override
    public ResponseEntity<Object> queryResourceDataForCmHandle(final String datastoreName,
                                                               final String cmHandle,
                                                               final String cpsPath,
                                                               final String optionsParamInQuery,
                                                               final String topicParamInQuery,
                                                               final Boolean includeDescendants) {
        validateDataStore(OPERATIONAL, datastoreName);
        final Collection<DataNode> dataNodes = networkCmProxyFacade.queryResourceDataForCmHandle(cmHandle, cpsPath,
            includeDescendants);
        return ResponseEntity.ok(dataNodes);
    }

    /**
     * Patch resource data.
     *
     * @param datastoreName      name of the datastore (currently only supports "ncmp-datastore:passthrough-running")
     * @param cmHandleReference  cm handle or alternate identifier
     * @param resourceIdentifier resource identifier
     * @param requestBody        the request body
     * @param contentType        content type of body
     * @param authorization      contents of Authorization header, or null if not present
     * @return {@code ResponseEntity} response from dmi plugin
     */

    @Override
    public ResponseEntity<Object> patchResourceDataRunningForCmHandle(final String datastoreName,
                                                                      final String cmHandleReference,
                                                                      final String resourceIdentifier,
                                                                      final Object requestBody,
                                                                      final String contentType,
                                                                      final String authorization) {

        validateDataStore(PASSTHROUGH_RUNNING, datastoreName);

        final Object responseObject = networkCmProxyFacade
                .writeResourceDataPassThroughRunningForCmHandle(
                        cmHandleReference, resourceIdentifier, PATCH,
                        jsonObjectMapper.asJsonString(requestBody), contentType, authorization);
        return ResponseEntity.ok(responseObject);
    }

    /**
     * Create resource data for given cm-handle.
     *
     * @param datastoreName      name of the datastore (currently only supports "ncmp-datastore:passthrough-running")
     * @param cmHandleReference  cm handle or alternate identifier
     * @param resourceIdentifier resource identifier
     * @param requestBody        the request body
     * @param contentType        content type of body
     * @param authorization      contents of Authorization header, or null if not present
     * @return {@code ResponseEntity} response from dmi plugin
     */
    @Override
    public ResponseEntity<Void> createResourceDataRunningForCmHandle(final String datastoreName,
                                                                     final String cmHandleReference,
                                                                     final String resourceIdentifier,
                                                                     final Object requestBody,
                                                                     final String contentType,
                                                                     final String authorization) {
        validateDataStore(PASSTHROUGH_RUNNING, datastoreName);

        networkCmProxyFacade.writeResourceDataPassThroughRunningForCmHandle(cmHandleReference,
                resourceIdentifier, CREATE, jsonObjectMapper.asJsonString(requestBody), contentType, authorization);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Update resource data for given cm-handle.
     *
     * @param datastoreName      name of the datastore (currently only supports "ncmp-datastore:passthrough-running")
     * @param cmHandleReference  cm handle or alternate identifier
     * @param resourceIdentifier resource identifier
     * @param requestBody        the request body
     * @param contentType        content type of the body
     * @param authorization      contents of Authorization header, or null if not present
     * @return response entity
     */

    @Override
    public ResponseEntity<Object> updateResourceDataRunningForCmHandle(final String datastoreName,
                                                                       final String cmHandleReference,
                                                                       final String resourceIdentifier,
                                                                       final Object requestBody,
                                                                       final String contentType,
                                                                       final String authorization) {
        validateDataStore(PASSTHROUGH_RUNNING, datastoreName);

        networkCmProxyFacade.writeResourceDataPassThroughRunningForCmHandle(cmHandleReference,
                resourceIdentifier, UPDATE, jsonObjectMapper.asJsonString(requestBody), contentType, authorization);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Delete resource data for a given cm-handle.
     *
     * @param datastoreName      name of the datastore (currently only supports "ncmp-datastore:passthrough-running")
     * @param cmHandleReference  cm handle or alternate identifier
     * @param resourceIdentifier resource identifier
     * @param contentType        content type of the body
     * @param authorization      contents of Authorization header, or null if not present
     * @return response entity no content if request is successful
     */
    @Override
    public ResponseEntity<Void> deleteResourceDataRunningForCmHandle(final String datastoreName,
                                                                     final String cmHandleReference,
                                                                     final String resourceIdentifier,
                                                                     final String contentType,
                                                                     final String authorization) {

        validateDataStore(PASSTHROUGH_RUNNING, datastoreName);

        networkCmProxyFacade.writeResourceDataPassThroughRunningForCmHandle(cmHandleReference,
                resourceIdentifier, DELETE, NO_BODY, contentType, authorization);
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
        final Collection<NcmpServiceCmHandle> cmHandles = networkCmProxyInventoryFacade
                .executeCmHandleSearch(cmHandleQueryApiParameters);
        final List<RestOutputCmHandle> restOutputCmHandles =
                cmHandles.stream().map(this::toRestOutputCmHandle).collect(Collectors.toList());
        return ResponseEntity.ok(restOutputCmHandles);
    }

    /**
     * Query and return cm handle ids or alternate ids that match the given query parameters.
     *
     * @param cmHandleQueryParameters   the cm handle query parameters
     * @param outputAlternateId         Boolean for cm handle reference type either
     *                                  cm handle id (false or null) or alternate id (true)
     * @return                          collection of cm handle ids
     */
    @Override
    public ResponseEntity<List<String>> searchCmHandleIds(final CmHandleQueryParameters cmHandleQueryParameters,
                                                          final Boolean outputAlternateId) {
        final CmHandleQueryApiParameters cmHandleQueryApiParameters =
                jsonObjectMapper.convertToValueType(cmHandleQueryParameters, CmHandleQueryApiParameters.class);
        final Collection<String> cmHandleIds
            = networkCmProxyInventoryFacade.executeCmHandleIdSearch(cmHandleQueryApiParameters, outputAlternateId);
        return ResponseEntity.ok(List.copyOf(cmHandleIds));
    }

    /**
     * Search for Cm Handle and Properties by Name.
     *
     * @param cmHandleReference cm-handle or alternate identifier
     * @return cm handle and its properties
     */
    @Override
    public ResponseEntity<RestOutputCmHandle> retrieveCmHandleDetailsById(final String cmHandleReference) {
        final NcmpServiceCmHandle ncmpServiceCmHandle
            = networkCmProxyInventoryFacade.getNcmpServiceCmHandle(cmHandleReference);
        final RestOutputCmHandle restOutputCmHandle = toRestOutputCmHandle(ncmpServiceCmHandle);
        return ResponseEntity.ok(restOutputCmHandle);
    }

    /**
     * Get Cm Handle Properties by Cm Handle or alternate Identifier.
     *
     * @param cmHandleReference cm-handle or alternate identifier
     * @return cm handle properties
     */
    @Override
    public ResponseEntity<RestOutputCmHandlePublicProperties> getCmHandlePublicPropertiesByCmHandleId(
            final String cmHandleReference) {
        final CmHandlePublicProperties cmHandlePublicProperties = new CmHandlePublicProperties();
        cmHandlePublicProperties.add(networkCmProxyInventoryFacade.getCmHandlePublicProperties(cmHandleReference));
        final RestOutputCmHandlePublicProperties restOutputCmHandlePublicProperties =
                new RestOutputCmHandlePublicProperties();
        restOutputCmHandlePublicProperties.setPublicCmHandleProperties(cmHandlePublicProperties);
        return ResponseEntity.ok(restOutputCmHandlePublicProperties);
    }

    /**
     * Get Cm Handle State by Cm Handle Id.
     *
     * @param cmHandleReference cm-handle or alternate identifier
     * @return cm handle state
     */
    @Override
    public ResponseEntity<RestOutputCmHandleCompositeState> getCmHandleStateByCmHandleId(
            final String cmHandleReference) {
        final CompositeState cmHandleState = networkCmProxyInventoryFacade.getCmHandleCompositeState(cmHandleReference);
        final RestOutputCmHandleCompositeState restOutputCmHandleCompositeState =
                new RestOutputCmHandleCompositeState();
        restOutputCmHandleCompositeState.setState(
                cmHandleStateMapper.toCmHandleCompositeStateExternalLockReason(cmHandleState));
        return ResponseEntity.ok(restOutputCmHandleCompositeState);
    }

    /**
     * Return module definitions.
     *
     * @param cmHandleReference   cm handle or alternate id identifier
     * @param moduleName          module name
     * @param revision            the revision of the module
     * @return list of module definitions (module name, revision, yang resource content)
     */
    @Override
    public ResponseEntity<List<RestModuleDefinition>> getModuleDefinitions(final String cmHandleReference,
                                                                           final String moduleName,
                                                                           final String revision) {
        final Collection<ModuleDefinition> moduleDefinitions;
        if (StringUtils.hasText(moduleName)) {
            moduleDefinitions =
                networkCmProxyInventoryFacade.getModuleDefinitionsByCmHandleAndModule(cmHandleReference,
                    moduleName, revision);
        } else {
            moduleDefinitions =
                networkCmProxyInventoryFacade.getModuleDefinitionsByCmHandleReference(cmHandleReference);
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
     * @param cmHandleReference cm handle or alternate id identifier
     * @return module references for cm handle. Namespace will be always blank because restConf does not include this.
     */
    public ResponseEntity<List<RestModuleReference>> getModuleReferencesByCmHandle(final String cmHandleReference) {
        final List<RestModuleReference> restModuleReferences =
            networkCmProxyInventoryFacade.getYangResourcesModuleReferences(cmHandleReference).stream()
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
        networkCmProxyInventoryFacade.setDataSyncEnabled(cmHandleId, dataSyncEnabledFlag);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    private RestOutputCmHandle toRestOutputCmHandle(final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final RestOutputCmHandle restOutputCmHandle = new RestOutputCmHandle();
        final CmHandlePublicProperties cmHandlePublicProperties = new CmHandlePublicProperties();
        restOutputCmHandle.setCmHandle(ncmpServiceCmHandle.getCmHandleId());
        cmHandlePublicProperties.add(ncmpServiceCmHandle.getPublicProperties());
        restOutputCmHandle.setPublicCmHandleProperties(cmHandlePublicProperties);
        restOutputCmHandle.setState(cmHandleStateMapper.toCmHandleCompositeStateExternalLockReason(
                ncmpServiceCmHandle.getCompositeState()));
        if (ncmpServiceCmHandle.getCurrentTrustLevel() != null) {
            restOutputCmHandle.setTrustLevel(ncmpServiceCmHandle.getCurrentTrustLevel().toString());
        }
        restOutputCmHandle.setModuleSetTag(ncmpServiceCmHandle.getModuleSetTag());
        restOutputCmHandle.setAlternateId(ncmpServiceCmHandle.getAlternateId());
        restOutputCmHandle.setDataProducerIdentifier(ncmpServiceCmHandle.getDataProducerIdentifier());
        return restOutputCmHandle;
    }

    private void validateDataStore(final DatastoreType acceptableDataStoreType, final String requestedDatastoreName) {
        final DatastoreType datastoreType = DatastoreType.fromDatastoreName(requestedDatastoreName);

        if (acceptableDataStoreType != datastoreType) {
            throw new InvalidDatastoreException(requestedDatastoreName + " is not supported");
        }
    }

}

