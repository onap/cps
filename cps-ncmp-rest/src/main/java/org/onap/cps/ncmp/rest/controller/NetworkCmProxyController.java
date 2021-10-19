/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications (C) 2021 Nordix Foundation
 *  Modification Copyright (C) 2021 highstreet technologies GmbH
 *  Modifications (C) 2021 Bell Canada
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Collection;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.rest.api.NetworkCmProxyApi;
import org.onap.cps.ncmp.rest.model.CmHandles;
import org.onap.cps.ncmp.rest.model.Conditions;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.spi.model.ModuleReference;
import org.onap.cps.utils.DataMapUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.ncmp-base-path}")
public class NetworkCmProxyController implements NetworkCmProxyApi {

    private static final Gson GSON = new GsonBuilder().create();

    private final NetworkCmProxyDataService networkCmProxyDataService;

    /**
     * Constructor Injection for Dependencies.
     * @param networkCmProxyDataService Data Service Interface
     */
    public NetworkCmProxyController(final NetworkCmProxyDataService networkCmProxyDataService) {
        this.networkCmProxyDataService = networkCmProxyDataService;
    }

    /**
     * Create Node.
     * @deprecated This Method is no longer used as part of NCMP.
     */
    @Override
    @Deprecated(forRemoval = false)
    public ResponseEntity<Void> createNode(final String cmHandle, @Valid final String jsonData,
        @Valid final String parentNodeXpath) {
        networkCmProxyDataService.createDataNode(cmHandle, parentNodeXpath, jsonData);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Add List-node Child Element.
     * @deprecated This Method is no longer used as part of NCMP.
     */
    @Override
    @Deprecated(forRemoval = false)
    public ResponseEntity<Void> addListNodeElements(@NotNull @Valid final String parentNodeXpath,
        final String cmHandle, @Valid final String jsonData) {
        networkCmProxyDataService.addListNodeElements(cmHandle, parentNodeXpath, jsonData);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Get Node By CM Handle and X-Path.
     * @deprecated This Method is no longer used as part of NCMP.
     */
    @Override
    @Deprecated(forRemoval = false)
    public ResponseEntity<Object> getNodeByCmHandleAndXpath(final String cmHandle, @Valid final String xpath,
        @Valid final Boolean includeDescendants) {
        final FetchDescendantsOption fetchDescendantsOption = Boolean.TRUE.equals(includeDescendants)
            ? FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS : FetchDescendantsOption.OMIT_DESCENDANTS;
        final var dataNode = networkCmProxyDataService.getDataNode(cmHandle, xpath, fetchDescendantsOption);
        return new ResponseEntity<>(DataMapUtils.toDataMap(dataNode), HttpStatus.OK);
    }

    /**
     * Query Data Nodes.
     * @deprecated This Method is no longer used as part of NCMP.
     */
    @Override
    @Deprecated(forRemoval = false)
    public ResponseEntity<Object> queryNodesByCmHandleAndCpsPath(final String cmHandle, @Valid final String cpsPath,
        @Valid final Boolean includeDescendants) {
        final FetchDescendantsOption fetchDescendantsOption = Boolean.TRUE.equals(includeDescendants)
            ? FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS : FetchDescendantsOption.OMIT_DESCENDANTS;
        final Collection<DataNode> dataNodes =
            networkCmProxyDataService.queryDataNodes(cmHandle, cpsPath, fetchDescendantsOption);
        return new ResponseEntity<>(GSON.toJson(dataNodes), HttpStatus.OK);
    }

    /**
     * Replace Node With Descendants.
     * @deprecated This Method is no longer used as part of NCMP.
     */
    @Override
    @Deprecated(forRemoval = false)
    public ResponseEntity<Object> replaceNode(final String cmHandle, @Valid final String jsonData,
        @Valid final String parentNodeXpath) {
        networkCmProxyDataService.replaceNodeTree(cmHandle, parentNodeXpath, jsonData);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    /**
     * Update Node Leaves.
     * @deprecated This Method is no longer used as part of NCMP.
     */
    @Override
    @Deprecated(forRemoval = false)
    public ResponseEntity<Object> updateNodeLeaves(final String cmHandle, @Valid final String jsonData,
        @Valid final String parentNodeXpath) {
        networkCmProxyDataService.updateNodeLeaves(cmHandle, parentNodeXpath, jsonData);
        return new ResponseEntity<>(HttpStatus.OK);
    }

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

    /**
     * Create resource data in datastore pass through running
     * for given cm-handle.
     *
     * @param resourceIdentifier resource identifier
     * @param cmHandle cm handle identifier
     * @param requestBody requestBody
     * @param contentType content type of body
     * @return {@code ResponseEntity} response from dmi plugi
     */
    @Override
    public ResponseEntity<Void> createResourceDataRunningForCmHandle(final String resourceIdentifier,
                                                                     final String cmHandle,
                                                                     final String requestBody,
                                                                     final String contentType) {
        networkCmProxyDataService.createResourceDataPassThroughRunningForCmHandle(cmHandle,
                resourceIdentifier, requestBody, contentType);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<CmHandles> executeCmHandleSearches(final Conditions conditions) {
        return null;
    }

    @Override
    public ResponseEntity<Object> getModuleReferencesByCmHandle(final String cmHandle) {
        final Collection<ModuleReference>
            moduleReferences = networkCmProxyDataService.getYangResourcesModuleReferences(cmHandle);
        return new ResponseEntity<>(new Gson().toJson(moduleReferences), HttpStatus.OK);
    }

}
