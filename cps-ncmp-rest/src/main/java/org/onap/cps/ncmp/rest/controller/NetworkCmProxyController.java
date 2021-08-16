/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications (C) 2021 Nordix Foundation
 *  Modification Copyright (C) 2021 highstreet technologies GmbH
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


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Collection;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.models.DmiPluginRegistration;
import org.onap.cps.ncmp.rest.api.NetworkCmProxyApi;
import org.onap.cps.ncmp.rest.model.RestDmiPluginRegistration;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
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

    private final ObjectMapper objectMapper;

    /**
     * Constructor Injection for Dependencies.
     * @param networkCmProxyDataService Data Service Interface
     * @param objectMapper Object Mapper
     */
    public NetworkCmProxyController(final NetworkCmProxyDataService networkCmProxyDataService,
        final ObjectMapper objectMapper) {
        this.networkCmProxyDataService = networkCmProxyDataService;
        this.objectMapper = objectMapper;
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
     * Update DMI Plugin Registration (used for first registration also).
     * @param restDmiPluginRegistration the registration data
     */
    @Override
    public ResponseEntity<Void> updateDmiPluginRegistration(
        final @Valid RestDmiPluginRegistration restDmiPluginRegistration) {
        final DmiPluginRegistration dmiPluginRegistration =
            convertRestObjectToJavaApiObject(restDmiPluginRegistration);
        networkCmProxyDataService.updateDmiPluginRegistration(dmiPluginRegistration);
        return new ResponseEntity<>(HttpStatus.CREATED);
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
     * Get resource data for for operational datastore.
     *
     * @param cmHandle cm handle identifier
     * @param resourceIdentifier resource identifier
     * @param accept accept header parameter
     * @param fields fields query parameter
     * @param depth depth query parameter
     * @return {@code ResponseEntity} response from dmi plugin
     */
    @Override
    public ResponseEntity<Object> getResourceDataOperationalForCmHandle(final String cmHandle,
                                                                        final String resourceIdentifier,
                                                                        final String accept,
                                                                        final @Valid String fields,
                                                                        final @Min(1) @Valid Integer depth) {
        final var responseObject = networkCmProxyDataService.getResourceDataOperationalFoCmHandle(cmHandle,
                resourceIdentifier,
                accept,
                fields,
                depth);
        return ResponseEntity.ok(responseObject);
    }

    private DmiPluginRegistration convertRestObjectToJavaApiObject(
        final RestDmiPluginRegistration restDmiPluginRegistration) {
        return objectMapper.convertValue(restDmiPluginRegistration, DmiPluginRegistration.class);
    }

}
