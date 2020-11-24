/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
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

import java.util.Collection;
import javax.validation.Valid;
import org.modelmapper.ModelMapper;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.rest.api.CpsAdminApi;
import org.onap.cps.spi.model.Anchor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminRestController implements CpsAdminApi {

    @Autowired
    private CpsAdminService cpsAdminService;

    @Autowired
    private ModelMapper modelMapper;

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
    public ResponseEntity<Object> deleteAnchor(final String dataspaceName, final String anchorName) {
        return null;
    }

    @Override
    public ResponseEntity<Object> deleteDataspace(final String dataspaceName) {
        return null;
    }

    @Override
    public ResponseEntity<Object> getAnchor(final String dataspaceName, final String anchorName) {
        return null;
    }

    @Override
    public ResponseEntity<Object> getAnchors(final String dataspaceName) {
        final Collection<Anchor> anchorDetails = cpsAdminService.getAnchors(dataspaceName);
        return new ResponseEntity<>(anchorDetails, HttpStatus.OK);
    }
}
