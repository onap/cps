/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020-2022 Nordix Foundation
 *  Modifications Copyright (C) 2020-2021 Bell Canada.
 *  Modifications Copyright (C) 2021 Pantheon.tech
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

import static org.onap.cps.rest.utils.MultipartFileUtil.extractYangResourcesMap;
import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_PROHIBITED;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.rest.api.CpsAdminApi;
import org.onap.cps.rest.model.AnchorDetails;
import org.onap.cps.rest.model.SchemaSetDetails;
import org.onap.cps.spi.model.Anchor;
import org.onap.cps.spi.model.SchemaSet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("${rest.api.cps-base-path}")
@RequiredArgsConstructor
public class AdminRestController implements CpsAdminApi {

    @Autowired
    private CpsAdminService cpsAdminService;

    @Autowired
    private CpsModuleService cpsModuleService;

    private final RestControllerMapper restControllerMapper;

    /**
     * Create a dataspace.
     *
     * @param dataspaceName dataspace name
     * @return a {@Link ResponseEntity} of created dataspace name & {@link HttpStatus} CREATED
     */
    @Override
    public ResponseEntity<String> createDataspace(@NotNull @Valid final String dataspaceName) {
        cpsAdminService.createDataspace(dataspaceName);
        return new ResponseEntity<>(dataspaceName, HttpStatus.CREATED);
    }

    /**
     * Delete a dataspace.
     *
     * @param dataspaceName name of dataspace to be deleted
     * @return a {@Link ResponseEntity} of {@link HttpStatus} NO_CONTENT
     */
    @Override
    public ResponseEntity<Void> deleteDataspace(final String dataspaceName) {
        cpsAdminService.deleteDataspace(dataspaceName);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Create a {@link SchemaSet}.
     *
     * @param multipartFile multipart file
     * @param schemaSetName schemaset name
     * @param dataspaceName dataspace name
     * @return a {@Link ResponseEntity} of created schemaset name & {@link HttpStatus} CREATED
     */
    @Override
    public ResponseEntity<String> createSchemaSet(@NotNull @Valid final String schemaSetName,
        final String dataspaceName, @Valid final MultipartFile multipartFile) {
        cpsModuleService.createSchemaSet(dataspaceName, schemaSetName, extractYangResourcesMap(multipartFile));
        return new ResponseEntity<>(schemaSetName, HttpStatus.CREATED);
    }

    /**
     * Get {@link SchemaSetDetails} based on dataspace name & {@link SchemaSet} name.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schemaset name
     * @return a {@Link ResponseEntity} of {@Link SchemaSetDetails} & {@link HttpStatus} OK
     */
    @Override
    public ResponseEntity<SchemaSetDetails> getSchemaSet(final String dataspaceName, final String schemaSetName) {
        final var schemaSet = cpsModuleService.getSchemaSet(dataspaceName, schemaSetName);
        final var schemaSetDetails = restControllerMapper.toSchemaSetDetails(schemaSet);
        return new ResponseEntity<>(schemaSetDetails, HttpStatus.OK);
    }

    /**
     * Delete a {@link SchemaSet} based on given dataspace name & schemaset name.
     *
     * @param dataspaceName dataspace name
     * @param schemaSetName schemaset name
     * @return a {@Link ResponseEntity} of {@link HttpStatus} NO_CONTENT
     */
    @Override
    public ResponseEntity<Void> deleteSchemaSet(final String dataspaceName, final String schemaSetName) {
        cpsModuleService.deleteSchemaSet(dataspaceName, schemaSetName, CASCADE_DELETE_PROHIBITED);
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
        cpsAdminService.createAnchor(dataspaceName, schemaSetName, anchorName);
        return new ResponseEntity<>(anchorName, HttpStatus.CREATED);
    }

    /**
     * Delete an {@link Anchor} based on given dataspace name & anchor name.
     *
     * @param dataspaceName dataspace name
     * @param anchorName anchor name
     * @return a {@Link ResponseEntity} of {@link HttpStatus} NO_CONTENT
     */
    @Override
    public ResponseEntity<Void> deleteAnchor(final String dataspaceName, final String anchorName) {
        cpsAdminService.deleteAnchor(dataspaceName, anchorName);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    /**
     * Get an {@link Anchor} based on given dataspace name & anchor name.
     *
     * @param dataspaceName dataspace name
     * @param anchorName anchor name
     * @return a {@Link ResponseEntity} of an {@Link AnchorDetails} & {@link HttpStatus} OK
     */
    @Override
    public ResponseEntity<AnchorDetails> getAnchor(final String dataspaceName, final String anchorName) {
        final var anchor = cpsAdminService.getAnchor(dataspaceName, anchorName);
        final var anchorDetails = restControllerMapper.toAnchorDetails(anchor);
        return new ResponseEntity<>(anchorDetails, HttpStatus.OK);
    }

    /**
     *  Get all {@link Anchor} based on given dataspace name.
     *
     * @param dataspaceName dataspace name
     * @return a {@Link ResponseEntity} of all {@Link AnchorDetails} & {@link HttpStatus} OK
     */
    @Override
    public ResponseEntity<List<AnchorDetails>> getAnchors(final String dataspaceName) {
        final Collection<Anchor> anchors = cpsAdminService.getAnchors(dataspaceName);
        final List<AnchorDetails> anchorDetails = anchors.stream().map(restControllerMapper::toAnchorDetails)
            .collect(Collectors.toList());
        return new ResponseEntity<>(anchorDetails, HttpStatus.OK);
    }
}
