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

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import javax.persistence.PersistenceException;
import org.hibernate.exception.ConstraintViolationException;
import org.onap.cps.api.CpService;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
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
public class CpsRestController {

    @Autowired
    private CpService cpService;

    /*
    Old rest endpoints before contract first approach (Need to be removed).
     */

    /**
     * Upload a yang model file.
     *
     * @param uploadedFile the yang model Multipart File.
     * @param dataspaceName the dataspace name linked to the model.
     * @return a ResponseEntity.
     */
    @PostMapping("/dataspaces/{dataspace_name}/modules")
    public final ResponseEntity<String> uploadYangModelFile(
        @RequestParam("file") MultipartFile uploadedFile,
        @PathVariable("dataspace_name") String dataspaceName) {
        try {
            final File fileToParse = saveToFile(uploadedFile);
            final SchemaContext schemaContext = cpService.parseAndValidateModel(fileToParse);
            cpService.storeSchemaContext(schemaContext, dataspaceName);
            return new ResponseEntity<String>("Resource successfully created", HttpStatus.CREATED);
        } catch (final YangParserException | ConstraintViolationException e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (final Exception e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Upload a JSON file.
     *
     * @param uploadedFile the JSON Multipart file.
     * @return a ResponseEntity.
     */
    @PostMapping("/upload-yang-json-data-file")
    public final ResponseEntity<String> uploadYangJsonDataFile(
        @RequestParam("file") MultipartFile uploadedFile) {
        try {
            validateJsonStructure(uploadedFile);
            final int persistenceObjectId = cpService.storeJsonStructure(new String(uploadedFile.getBytes()));
            return new ResponseEntity<String>(
                "Object stored in CPS with identity: " + persistenceObjectId, HttpStatus.OK);
        } catch (final JsonSyntaxException e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (final Exception e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
        try {
            return new ResponseEntity<String>(cpService.getJsonById(jsonObjectId), HttpStatus.OK);
        } catch (final PersistenceException e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.NOT_FOUND);
        } catch (final Exception e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
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
        try {
            cpService.deleteJsonById(jsonObjectId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (final EmptyResultDataAccessException e) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND.toString(), HttpStatus.NOT_FOUND);
        } catch (final Exception e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private static final void validateJsonStructure(final MultipartFile jsonFile)
        throws JsonSyntaxException, IOException {
        final Gson gson = new Gson();
        gson.fromJson(new String(jsonFile.getBytes()), Object.class);
    }

    private static final File saveToFile(final MultipartFile multipartFile)
        throws IOException {
        final File file = File.createTempFile("tempFile", ".yang");
        file.deleteOnExit();

        try (OutputStream outputStream = new FileOutputStream(file)) {
            outputStream.write(multipartFile.getBytes());
        }
        return file;
    }
}