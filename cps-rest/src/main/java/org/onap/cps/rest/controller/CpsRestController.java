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

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Set;
import javax.validation.Valid;
import org.modelmapper.ModelMapper;
import org.onap.cps.api.CpService;
import org.onap.cps.api.CpsAdminService;
import org.onap.cps.api.CpsModuleService;
import org.onap.cps.exceptions.CpsException;
import org.onap.cps.exceptions.CpsValidationException;
import org.onap.cps.rest.api.CpsRestApi;
import org.onap.cps.spi.model.Anchor;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class CpsRestController implements CpsRestApi {

    @Autowired
    private CpService cpService;

    @Autowired
    private CpsModuleService cpsModuleService;

    @Autowired
    private CpsAdminService cpsAdminService;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public ResponseEntity<String> createSchemaSet(final String schemaSetName, final MultipartFile multipartFile,
                                                  final String dataspaceName) {

        final String filename = multipartFile.getOriginalFilename().toLowerCase();
        if (!filename.endsWith(".yang")) {
            throw new CpsValidationException("Invalid file type.", "Filename should end with .yang.");
        }
        try {
            final String content = new String(multipartFile.getBytes());
            if(content.isEmpty()){
                throw new CpsValidationException("Invalid file.", String.format("File %s is empty.", filename));
            }
            cpsModuleService.createSchemaSet(dataspaceName, schemaSetName,
                ImmutableMap.<String, String>builder().put(filename, content).build()
            );
        } catch (final IOException e) {
            throw new CpsException(e);
        }
        return new ResponseEntity<String>(schemaSetName, HttpStatus.CREATED);
    }

    /**
     * Create a new anchor.
     *
     * @param anchor        the anchor details object.
     * @param dataspaceName the dataspace name.
     * @return a ResponseEntity with the anchor name.
     */
    @Override
    public ResponseEntity<String> createAnchor(final org.onap.cps.rest.model.@Valid Anchor anchor,
                                               final String dataspaceName) {
        final Anchor anchorDetails = modelMapper.map(anchor, Anchor.class);
        anchorDetails.setDataspaceName(dataspaceName);
        final String anchorName = cpsAdminService.createAnchor(anchorDetails);
        return new ResponseEntity<String>(anchorName, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Object> createModules(@Valid final MultipartFile multipartFile, final String dataspaceName) {
        final File fileToParse = saveToFile(multipartFile);
        final SchemaContext schemaContext = cpsModuleService.parseAndValidateModel(fileToParse);
        cpsModuleService.storeSchemaContext(schemaContext, dataspaceName);
        return new ResponseEntity<>("Resource successfully created", HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<Object> createNode(@Valid final MultipartFile multipartFile, final String dataspaceName) {
        return null;
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
        return null;
    }

    @Override
    public ResponseEntity<Object> getModule(final String dataspaceName, @Valid final String namespaceName,
                                            @Valid final String revision) {
        return null;
    }

    @Override
    public ResponseEntity<Object> getNode(final String dataspaceName) {
        return null;
    }

    @Override
    public ResponseEntity<Object> getNodeByDataspaceAndAnchor(final String dataspaceName, final String anchorName) {
        return null;
    }

    /*
    Old rest endpoints before contract first approach (Need to be removed).
     */

    /**
     * Upload a JSON file.
     *
     * @param uploadedFile the JSON Multipart file.
     * @return a ResponseEntity.
     */
    @PostMapping("/upload-yang-json-data-file")
    public final ResponseEntity<String> uploadYangJsonDataFile(@RequestParam("file") final MultipartFile uploadedFile) {
        validateJsonStructure(uploadedFile);
        final int persistenceObjectId = cpService.storeJsonStructure(getJsonString(uploadedFile));
        return new ResponseEntity<>(
            "Object stored in CPS with identity: " + persistenceObjectId, HttpStatus.OK);
    }

    /**
     * Read a JSON Object using the object identifier.
     *
     * @param jsonObjectId the JSON object identifier.
     * @return a ResponseEntity.
     */
    @GetMapping("/json-object/{id}")
    public final ResponseEntity<String> getJsonObjectById(
        @PathVariable("id") final int jsonObjectId) {
        return new ResponseEntity<String>(cpService.getJsonById(jsonObjectId), HttpStatus.OK);
    }

    /**
     * Delete a JSON Object using the object identifier.
     *
     * @param jsonObjectId the JSON object identifier.
     * @return a ResponseEntity.
     */
    @DeleteMapping("json-object/{id}")
    public final ResponseEntity<Object> deleteJsonObjectById(
        @PathVariable("id") final int jsonObjectId) {
        cpService.deleteJsonById(jsonObjectId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private static void validateJsonStructure(final MultipartFile multipartFile) {
        try {
            final Gson gson = new Gson();
            gson.fromJson(getJsonString(multipartFile), Object.class);
        } catch (final JsonSyntaxException e) {
            throw new CpsValidationException("Not a valid JSON file.", e);
        }
    }

    private static File saveToFile(final MultipartFile multipartFile) {
        try {
            final File file = File.createTempFile("tempFile", ".yang");
            file.deleteOnExit();
            try (OutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(multipartFile.getBytes());
            }
            return file;

        } catch (final IOException e) {
            throw new CpsException(e);
        }
    }

    private static String getJsonString(final MultipartFile multipartFile) {
        try {
            return new String(multipartFile.getBytes());
        } catch (final IOException e) {
            throw new CpsException(e);
        }
    }
}