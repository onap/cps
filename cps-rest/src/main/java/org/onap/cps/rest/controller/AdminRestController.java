/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2025 Nordix Foundation
 *  Modifications Copyright (C) 2020-2021 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2022-2025 TechMahindra Ltd.
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

package org.onap.cps.rest.controller;

import static org.onap.cps.api.parameters.CascadeDeleteAllowed.CASCADE_DELETE_PROHIBITED;
import static org.onap.cps.rest.utils.MultipartFileUtil.extractYangResourcesMap;

import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsAnchorService;
import org.onap.cps.api.CpsDataspaceService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.api.CpsNotificationService;
import org.onap.cps.api.model.Anchor;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.model.Dataspace;
import org.onap.cps.api.model.SchemaSet;
import org.onap.cps.rest.api.CpsAdminApi;
import org.onap.cps.rest.model.AnchorDetails;
import org.onap.cps.rest.model.DataspaceDetails;
import org.onap.cps.rest.model.SchemaSetDetails;
import org.onap.cps.utils.DataMapUtils;
import org.onap.cps.utils.JsonObjectMapper;
import org.onap.cps.utils.PrefixResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("${rest.api.cps-base-path}")
@RequiredArgsConstructor
public class AdminRestController implements CpsAdminApi {

    private final CpsDataspaceService cpsDataspaceService;
    private final CpsModuleService cpsModuleService;
    private final CpsRestInputMapper cpsRestInputMapper;
    private final CpsAnchorService cpsAnchorService;
    private final CpsNotificationService cpsNotificationService;
    private final JsonObjectMapper jsonObjectMapper;

    private final PrefixResolver prefixResolver;

    /**
     * Create a dataspace.
     *
     * @param dataspaceName dataspace name
     * @return a {@Link ResponseEntity} of created dataspace name & {@link HttpStatus} CREATED
     */
    @Override
    public ResponseEntity<String> createDataspace(@NotNull @Valid final String dataspaceName) {
        cpsDataspaceService.createDataspace(dataspaceName);
        return new ResponseEntity<>(dataspaceName, HttpStatus.CREATED);
    }

    /**
     * Create a dataspace without returning any response body.
     *
     * @param dataspaceName dataspace name
     * @return a {@Link ResponseEntity} of created dataspace name & {@link HttpStatus} CREATED
     */
    @Override
    public ResponseEntity<Void> createDataspaceV2(@NotNull @Valid final String dataspaceName) {
        cpsDataspaceService.createDataspace(dataspaceName);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Delete a dataspace.
     *
     * @param dataspaceName name of dataspace to be deleted
     * @return a {@Link ResponseEntity} of {@link HttpStatus} NO_CONTENT
     */
    @Override
    public ResponseEntity<Void> deleteDataspace(final String apiVersion, final String dataspaceName) {
        cpsDataspaceService.deleteDataspace(dataspaceName);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Create a {@link SchemaSet}.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schemaset name
     * @param multipartFile multipart file
     * @return a {@Link ResponseEntity} of created schemaset name & {@link HttpStatus} CREATED
     */
    @Override
    public ResponseEntity<String> createSchemaSet(final String dataspaceName,
                                                  @NotNull @Valid final String schemaSetName,
                                                  final MultipartFile multipartFile) {
        cpsModuleService.createSchemaSet(dataspaceName, schemaSetName, extractYangResourcesMap(multipartFile));
        return new ResponseEntity<>(schemaSetName, HttpStatus.CREATED);
    }

    /**
     * Create a {@link SchemaSet}.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schemaset name
     * @param multipartFile multipart file
     * @return a {@Link ResponseEntity} of created schema set without any response body & {@link HttpStatus} CREATED
     */
    @Override
    @Timed(value = "cps.rest.admin.controller.schemaset.create",
        description = "Time taken to create schemaset from controller")
    public ResponseEntity<Void> createSchemaSetV2(final String dataspaceName,
                                                  @NotNull @Valid final String schemaSetName,
                                                  final MultipartFile multipartFile) {
        cpsModuleService.createSchemaSet(dataspaceName, schemaSetName, extractYangResourcesMap(multipartFile));
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Get {@link SchemaSetDetails} based on dataspace name & {@link SchemaSet} name.
     *
     * @param apiVersion api version
     * @param dataspaceName dataspace name
     * @param schemaSetName schemaset name
     * @return a {@Link ResponseEntity} of {@Link SchemaSetDetails} & {@link HttpStatus} OK
     */
    @Override
    public ResponseEntity<SchemaSetDetails> getSchemaSet(final String apiVersion,
            final String dataspaceName, final String schemaSetName) {
        final var schemaSet = cpsModuleService.getSchemaSet(dataspaceName, schemaSetName);
        final var schemaSetDetails = cpsRestInputMapper.toSchemaSetDetails(schemaSet);
        return new ResponseEntity<>(schemaSetDetails, HttpStatus.OK);
    }

    /**
     * Get list of schema sets for a given dataspace name.
     *
     * @param apiVersion api version
     * @param dataspaceName dataspace name
     * @return a {@Link ResponseEntity} of schema sets & {@link HttpStatus} OK
     */
    @Override
    public ResponseEntity<List<SchemaSetDetails>> getSchemaSets(final String apiVersion, final String dataspaceName) {
        final Collection<SchemaSet> schemaSets = cpsModuleService.getSchemaSets(dataspaceName);
        final List<SchemaSetDetails> schemaSetDetails = schemaSets.stream().map(cpsRestInputMapper::toSchemaSetDetails)
                .collect(Collectors.toList());
        return new ResponseEntity<>(schemaSetDetails, HttpStatus.OK);
    }

    /**
     * Delete a {@link SchemaSet} based on given dataspace name & schemaset name.
     *
     * @param apiVersion api version
     * @param dataspaceName dataspace name
     * @param schemaSetName schemaset name
     * @return a {@Link ResponseEntity} of {@link HttpStatus} NO_CONTENT
     */
    @Override
    public ResponseEntity<Void> deleteSchemaSet(final String apiVersion,
            final String dataspaceName, final String schemaSetName) {
        cpsModuleService.deleteSchemaSet(dataspaceName, schemaSetName, CASCADE_DELETE_PROHIBITED);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Clean the given dataspace of any orphaned (module) data.
     *
     * @param apiVersion api version
     * @param dataspaceName dataspace name
     *
     * @return a {@Link ResponseEntity} of {@link HttpStatus} NO_CONTENT
     */
    @Override
    public ResponseEntity<Void> cleanDataspace(final String apiVersion, final String dataspaceName) {
        cpsModuleService.deleteAllUnusedYangModuleData(dataspaceName);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Create a new anchor.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schema set name
     * @param anchorName    anchorName
     * @return a ResponseEntity with the anchor name & {@link HttpStatus} CREATED
     */
    @Override
    public ResponseEntity<String> createAnchor(final String dataspaceName, @NotNull @Valid final String schemaSetName,
        @NotNull @Valid final String anchorName) {
        cpsAnchorService.createAnchor(dataspaceName, schemaSetName, anchorName);
        return new ResponseEntity<>(anchorName, HttpStatus.CREATED);
    }

    /**
     * Create an anchor.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schema set name
     * @param anchorName    anchorName
     * @return a ResponseEntity without response body & {@link HttpStatus} CREATED
     */
    @Override
    public ResponseEntity<Void> createAnchorV2(final String dataspaceName, @NotNull @Valid final String schemaSetName,
        @NotNull @Valid final String anchorName) {
        cpsAnchorService.createAnchor(dataspaceName, schemaSetName, anchorName);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    /**
     * Delete an {@link Anchor} based on given dataspace name & anchor name.
     *
     * @param apiVersion api version
     * @param dataspaceName dataspace name
     * @param anchorName anchor name
     * @return a {@Link ResponseEntity} of {@link HttpStatus} NO_CONTENT
     */
    @Override
    public ResponseEntity<Void> deleteAnchor(final String apiVersion,
            final String dataspaceName, final String anchorName) {
        cpsAnchorService.deleteAnchor(dataspaceName, anchorName);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Get an {@link Anchor} based on given dataspace name & anchor name.
     *
     * @param apiVersion api version
     * @param dataspaceName dataspace name
     * @param anchorName anchor name
     * @return a {@Link ResponseEntity} of an {@Link AnchorDetails} & {@link HttpStatus} OK
     */
    @Override
    public ResponseEntity<AnchorDetails> getAnchor(final String apiVersion,
            final String dataspaceName, final String anchorName) {
        final var anchor = cpsAnchorService.getAnchor(dataspaceName, anchorName);
        final var anchorDetails = cpsRestInputMapper.toAnchorDetails(anchor);
        return new ResponseEntity<>(anchorDetails, HttpStatus.OK);
    }

    /**
     *  Get all {@link Anchor} based on given dataspace name.
     *
     * @param apiVersion api version
     * @param dataspaceName dataspace name
     * @return a {@Link ResponseEntity} of all {@Link AnchorDetails} & {@link HttpStatus} OK
     */
    @Override
    public ResponseEntity<List<AnchorDetails>> getAnchors(final String apiVersion,
            final String dataspaceName) {
        final Collection<Anchor> anchors = cpsAnchorService.getAnchors(dataspaceName);
        final List<AnchorDetails> anchorDetails = anchors.stream().map(cpsRestInputMapper::toAnchorDetails)
            .collect(Collectors.toList());
        return new ResponseEntity<>(anchorDetails, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<DataspaceDetails>> getAllDataspaces(final String apiVersion) {
        final Collection<Dataspace> dataspaces = cpsDataspaceService.getAllDataspaces();
        final List<DataspaceDetails> dataspaceDetails = dataspaces.stream().map(cpsRestInputMapper::toDataspaceDetails)
                .collect(Collectors.toList());
        return new ResponseEntity<>(dataspaceDetails, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<DataspaceDetails> getDataspace(final String apiVersion, final String dataspaceName) {
        final Dataspace dataspace = cpsDataspaceService.getDataspace(dataspaceName);
        final DataspaceDetails dataspaceDetails = cpsRestInputMapper.toDataspaceDetails(dataspace);
        return new ResponseEntity<>(dataspaceDetails, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Void> createNotificationSubscription(final String xpath, final Object jsonData) {
        cpsNotificationService.createNotificationSubscription(jsonObjectMapper.asJsonString(jsonData), xpath);
        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> deleteNotificationSubscription(final String xpath) {
        cpsNotificationService.deleteNotificationSubscription(xpath);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<Object> getNotificationSubscription(final String xpath) {
        final Collection<DataNode> dataNodes = cpsNotificationService.getNotificationSubscription(xpath);
        final List<Map<String, Object>> dataMaps = new ArrayList<>(dataNodes.size());
        final Anchor anchor = cpsAnchorService.getAnchor("CPS-Admin",
                "cps-notification-subscriptions");
        for (final DataNode dataNode: dataNodes) {
            final String prefix = prefixResolver.getPrefix(anchor, dataNode.getXpath());
            final Map<String, Object> dataMap = DataMapUtils.toDataMapWithIdentifier(dataNode, prefix);
            dataMaps.add(dataMap);
        }
        return new ResponseEntity<>(jsonObjectMapper.asJsonString(dataMaps), HttpStatus.OK);
    }

}
