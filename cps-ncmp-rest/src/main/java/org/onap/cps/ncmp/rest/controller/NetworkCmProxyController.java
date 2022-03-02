/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Nordix Foundation
 *  Modification Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications (C) 2021 Bell Canada
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

import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.CREATE;
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.DELETE;
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.PATCH;
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.UPDATE;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;
import org.onap.cps.ncmp.rest.api.NetworkCmProxyApi;
import org.onap.cps.ncmp.rest.model.CmHandleProperties;
import org.onap.cps.ncmp.rest.model.CmHandleProperty;
import org.onap.cps.ncmp.rest.model.CmHandlePublicProperties;
import org.onap.cps.ncmp.rest.model.CmHandles;
import org.onap.cps.ncmp.rest.model.ConditionProperties;
import org.onap.cps.ncmp.rest.model.Conditions;
import org.onap.cps.ncmp.rest.model.ModuleNameAsJsonObject;
import org.onap.cps.ncmp.rest.model.ModuleNamesAsJsonArray;
import org.onap.cps.ncmp.rest.model.RestModuleReference;
import org.onap.cps.ncmp.rest.model.RestOutputCmHandle;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final NcmpRestInputMapper ncmpRestInputMapper;

    /**
     * Get resource data from operational datastore.
     *
     * @param cmHandle cm handle identifier
     * @param resourceIdentifier resource identifier
     * @param acceptParamInHeader accept header parameter
     * @param optionsParamInQuery options query parameter
     * @return {@code ResponseEntity} response from dmi plugin
     */
    @Override
    public ResponseEntity<Object> getResourceDataOperationalForCmHandle(final String cmHandle,
                                                                        final @NotNull @Valid String resourceIdentifier,
                                                                        final String acceptParamInHeader,
                                                                        final @Valid String optionsParamInQuery) {
        final Object responseObject = networkCmProxyDataService.getResourceDataOperationalForCmHandle(cmHandle,
                resourceIdentifier,
                acceptParamInHeader,
                optionsParamInQuery);
        return ResponseEntity.ok(responseObject);
    }

    /**
     * Get resource data from pass-through running datastore.
     *
     * @param cmHandle cm handle identifier
     * @param resourceIdentifier resource identifier
     * @param acceptParamInHeader accept header parameter
     * @param optionsParamInQuery options query parameter
     * @return {@code ResponseEntity} response from dmi plugin
     */
    @Override
    public ResponseEntity<Object> getResourceDataRunningForCmHandle(final String cmHandle,
                                                                    final @NotNull @Valid String resourceIdentifier,
                                                                    final String acceptParamInHeader,
                                                                    final @Valid String optionsParamInQuery) {
        final Object responseObject = networkCmProxyDataService.getResourceDataPassThroughRunningForCmHandle(cmHandle,
                resourceIdentifier,
                acceptParamInHeader,
                optionsParamInQuery);
        return ResponseEntity.ok(responseObject);
    }

    @Override
    public ResponseEntity<Object> patchResourceDataRunningForCmHandle(final String resourceIdentifier,
        final String cmHandle,
        final Object requestBody, final String contentType) {
        final Object responseObject = networkCmProxyDataService.writeResourceDataPassThroughRunningForCmHandle(cmHandle,
            resourceIdentifier, PATCH, jsonObjectMapper.asJsonString(requestBody), contentType);
        return ResponseEntity.ok(responseObject);
    }

    /**
     * Create resource data in datastore pass-through running for given cm-handle.
     *
     * @param resourceIdentifier resource identifier
     * @param cmHandle cm handle identifier
     * @param requestBody the request body
     * @param contentType content type of body
     * @return {@code ResponseEntity} response from dmi plugin
     */
    @Override
    public ResponseEntity<Void> createResourceDataRunningForCmHandle(final String resourceIdentifier,
        final String cmHandle, final Object requestBody, final String contentType) {
        networkCmProxyDataService.writeResourceDataPassThroughRunningForCmHandle(cmHandle,
                resourceIdentifier, CREATE, jsonObjectMapper.asJsonString(requestBody), contentType);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Update resource data in datastore pass-through running for given cm-handle.
     *
     * @param resourceIdentifier resource identifier
     * @param cmHandle cm handle identifier
     * @param requestBody the request body
     * @param contentType content type of the body
     * @return response entity
     */
    @Override
    public ResponseEntity<Object> updateResourceDataRunningForCmHandle(final String resourceIdentifier,
                                                                       final String cmHandle,
                                                                       final Object requestBody,
                                                                       final String contentType) {
        networkCmProxyDataService.writeResourceDataPassThroughRunningForCmHandle(cmHandle,
                resourceIdentifier, UPDATE, jsonObjectMapper.asJsonString(requestBody), contentType);
        return new ResponseEntity<>(HttpStatus.OK);
    }


    /**
     *  Delete resource data in datastore pass-through running for a given cm-handle.
     *
     * @param resourceIdentifier resource identifier
     * @param cmHandle cm handle identifier
     * @param contentType content type of the body
     * @return response entity no content if request is successful
     */
    @Override
    public ResponseEntity<Void> deleteResourceDataRunningForCmHandle(final String cmHandle,
                                                                     final String resourceIdentifier,
                                                                     final String contentType) {
        networkCmProxyDataService.writeResourceDataPassThroughRunningForCmHandle(cmHandle,
            resourceIdentifier, DELETE, NO_BODY, contentType);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Execute cm handle search.
     *
     * @param conditions the conditions
     * @return cm handles returned from search.
     */
    @Override
    public ResponseEntity<CmHandles> executeCmHandleSearch(final Conditions conditions) {
        final List<ConditionProperties> conditionProperties =
            conditions.getConditions().stream().collect(Collectors.toList());
        final CmHandles cmHandles = new CmHandles();
        cmHandles.setCmHandles(toCmHandleProperties(processConditions(conditionProperties)));
        return ResponseEntity.ok(cmHandles);
    }

    /**
     * Search for Cm Handle and Properties by Name.
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

    private Collection<String> processConditions(final List<ConditionProperties> conditionProperties) {
        for (final ConditionProperties conditionProperty : conditionProperties) {
            if (conditionProperty.getName().equals("hasAllModules")) {
                return executeCmHandleSearchesForModuleNames(conditionProperty);
            } else {
                log.warn("Unrecognized condition name {}.", conditionProperty.getName());
            }
        }
        log.warn("No valid conditions found {}.", conditionProperties);
        return Collections.emptyList();
    }

    private Collection<String> executeCmHandleSearchesForModuleNames(final ConditionProperties conditionProperties) {
        return networkCmProxyDataService
            .executeCmHandleHasAllModulesSearch(getModuleNames(conditionProperties.getConditionParameters()));
    }

    private Collection<String> getModuleNames(final ModuleNamesAsJsonArray moduleNamesAsJsonArray) {
        final Collection<String> moduleNames = new ArrayList<>(moduleNamesAsJsonArray.size());
        for (final ModuleNameAsJsonObject moduleNameAsJsonObject : moduleNamesAsJsonArray) {
            moduleNames.add(moduleNameAsJsonObject.getModuleName());
        }
        return moduleNames;
    }

    private CmHandleProperties toCmHandleProperties(final Collection<String> cmHandleIdentifiers) {
        final CmHandleProperties cmHandleProperties = new CmHandleProperties();
        for (final String cmHandleIdentifier : cmHandleIdentifiers) {
            final CmHandleProperty cmHandleProperty = new CmHandleProperty();
            cmHandleProperty.setCmHandleId(cmHandleIdentifier);
            cmHandleProperties.add(cmHandleProperty);
        }
        return cmHandleProperties;
    }

    private RestOutputCmHandle toRestOutputCmHandle(final NcmpServiceCmHandle ncmpServiceCmHandle) {
        final RestOutputCmHandle restOutputCmHandle = new RestOutputCmHandle();
        final CmHandlePublicProperties cmHandlePublicProperties = new CmHandlePublicProperties();
        restOutputCmHandle.setCmHandle(ncmpServiceCmHandle.getCmHandleID());
        cmHandlePublicProperties.add(ncmpServiceCmHandle.getPublicProperties());
        restOutputCmHandle.setPublicCmHandleProperties(cmHandlePublicProperties);
        return restOutputCmHandle;
    }
}
