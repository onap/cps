/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
 *  Modifications Copyright (C) 2021 Pantheon.tech
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

package org.onap.cps.rest.controller;

import static org.onap.cps.rest.utils.MultipartFileUtil.extractYangResourcesMap;
import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_PROHIBITED;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.modelmapper.ModelMapper;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.rest.api.CpsAdminApi;
import org.onap.cps.rest.model.AnchorDetails;
import org.onap.cps.spi.model.Anchor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("${rest.api.cps-base-path}")
public class AdminRestController implements CpsAdminApi {

    @Autowired
    private CpsAdminService cpsAdminService;

    @Autowired
    private CpsModuleService cpsModuleService;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ResponseEntity<String> createDataspace(final String dataspaceName) {
        cpsAdminService.createDataspace(dataspaceName);
        return new ResponseEntity<>(dataspaceName, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Object> deleteDataspace(final String dataspaceName) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    @Override
    public ResponseEntity<String> createSchemaSet(final MultipartFile multipartFile,
        final String schemaSetName, final String dataspaceName) {
        cpsModuleService.createSchemaSet(dataspaceName, schemaSetName, extractYangResourcesMap(multipartFile));
        return new ResponseEntity<>(schemaSetName, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Object> getSchemaSet(final String dataspaceName, final String schemaSetName) {
        final var schemaSet = cpsModuleService.getSchemaSet(dataspaceName, schemaSetName);
        return new ResponseEntity<>(schemaSet, HttpStatus.OK);
    }

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
     * @return a ResponseEntity with the anchor name.
     */
    @Override
    public ResponseEntity<String> createAnchor(final String dataspaceName, final String schemaSetName,
        final String anchorName) {
        cpsAdminService.createAnchor(dataspaceName, schemaSetName, anchorName);
        return new ResponseEntity<>(anchorName, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Void> deleteAnchor(final String dataspaceName, final String anchorName) {
        cpsAdminService.deleteAnchor(dataspaceName, anchorName);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @Override
    public ResponseEntity<Object> getAnchor(final String dataspaceName, final String anchorName) {
        final var anchor = cpsAdminService.getAnchor(dataspaceName, anchorName);
        return new ResponseEntity<>(anchor, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<List<AnchorDetails>> getAnchors(final String dataspaceName) {
        final Collection<Anchor> anchors = cpsAdminService.getAnchors(dataspaceName);
        final List<AnchorDetails> anchorDetails = new ArrayList<>();

        anchors.stream().forEach(anchor -> {
            final AnchorDetails anchordetail = new AnchorDetails();
            anchordetail.setName(anchor.getName());
            anchordetail.setDataspaceName(anchor.getDataspaceName());
            anchordetail.setSchemaSetName(anchor.getSchemaSetName());
            anchorDetails.add(anchordetail);
        });

        return new ResponseEntity<>(anchorDetails, HttpStatus.OK);
    }
}
