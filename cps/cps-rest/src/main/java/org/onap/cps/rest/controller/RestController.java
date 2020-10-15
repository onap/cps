/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
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
import java.io.IOException;
import javax.persistence.PersistenceException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.onap.cps.api.CpService;
import org.opendaylight.yangtools.yang.model.api.SchemaContext;
import org.opendaylight.yangtools.yang.model.parser.api.YangParserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;



@Path("v1")
public class RestController {

    @Autowired
    private CpService cpService;

    /**
     * Upload a yang model file.
     *
     * @param uploadedFile the yang model file.
     * @return a http response code.
     */
    @POST
    @Path("/upload-yang-model-file")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response uploadYangModelFile(@FormDataParam("file") File uploadedFile) throws IOException {
        try {
            final File fileToParse = renameFileIfNeeded(uploadedFile);
            final SchemaContext schemaContext = cpService.parseAndValidateModel(fileToParse);
            cpService.storeSchemaContext(schemaContext);
            return Response.status(Status.OK).entity("Yang File Parsed").build();
        } catch (final YangParserException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    /**
     * Upload a JSON file.
     *
     * @param uploadedFile the JSON file.
     * @return a http response code.
     */
    @POST
    @Path("/upload-yang-json-data-file")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public final Response uploadYangJsonDataFile(@FormDataParam("file") String uploadedFile) {
        try {
            validateJsonStructure(uploadedFile);
            final int persistenceObjectId = cpService.storeJsonStructure(uploadedFile);
            return Response.status(Status.OK).entity("Object stored in CPS with identity: " + persistenceObjectId)
                .build();
        } catch (final JsonSyntaxException e) {
            return Response.status(Status.BAD_REQUEST).entity(e.getMessage()).build();
        } catch (final Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    /**
     * Read a JSON Object using the object identifier.
     *
     * @param jsonObjectId the JSON object identifier.
     * @return a HTTP response.
     */
    @GET
    @Path("/json-object/{id}")
    public final Response getJsonObjectById(@PathParam("id") int jsonObjectId) {
        try {
            return Response.status(Status.OK).entity(cpService.getJsonById(jsonObjectId)).build();
        } catch (final PersistenceException e) {
            return Response.status(Status.NOT_FOUND).entity(e.getMessage()).build();
        } catch (final Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    /**
     * Delete a JSON Object using the object identifier.
     *
     * @param jsonObjectId the JSON object identifier.
     * @return a HTTP response.
     */
    @DELETE
    @Path("json-object/{id}")
    public final Response deleteJsonObjectById(@PathParam("id") int jsonObjectId) {
        try {
            cpService.deleteJsonById(jsonObjectId);
            return Response.status(Status.OK).entity(Status.OK.toString()).build();
        } catch (final EmptyResultDataAccessException e) {
            return Response.status(Status.NOT_FOUND).entity(Status.NOT_FOUND.toString()).build();
        } catch (final Exception e) {
            return Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage()).build();
        }
    }

    private static final void validateJsonStructure(final String jsonFile) {
        final Gson gson = new Gson();
        gson.fromJson(jsonFile, Object.class);
    }

    private static final File renameFileIfNeeded(File originalFile) {
        if (originalFile.getName().endsWith(".yang")) {
            return originalFile;
        }
        final File renamedFile = new File(originalFile.getName() + ".yang");
        originalFile.renameTo(renamedFile);
        return renamedFile;
    }
}