/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe
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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.onap.cps.ncmp.impl.provmns.model.ClassNameIdGetDataNodeSelectorParameter;
import org.onap.cps.ncmp.impl.provmns.model.ClassNameIdPatchDefaultResponse;
import org.onap.cps.ncmp.impl.provmns.model.ErrorResponseDefault;
import org.onap.cps.ncmp.impl.provmns.model.ErrorResponseGet;
import org.onap.cps.ncmp.impl.provmns.model.PatchItem;
import org.onap.cps.ncmp.impl.provmns.model.Resource;
import org.onap.cps.ncmp.impl.provmns.model.Scope;
import org.onap.cps.ncmp.rest.model.ErrorMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "ProvMnS", description = "Provisioning Management Service")
public interface ProvMnS {

    /**
     * GET /{URI-LDN-first-part}/{className}={id} : Reads one or multiple resources
     * With HTTP GET resources are read. The resources to be retrieved are identified with the target URI.
     * The attributes and fields parameter of the query components allow
     * to select the resource properties to be returned.
     *
     * @param httpServletRequest (required)
     * @param scope This parameter extends the set of targeted resources beyond the base resource identified
     *              with the path component of the URI.
     *              No scoping mechanism is specified in the present document. (optional)
     * @param filter This parameter reduces the targeted set of resources by applying a filter to the scoped set of
     *               resource representations. Only resource representations for which the filter construct evaluates
     *               to "true" are targeted. (optional)
     * @param attributes This parameter specifies the attributes of the scoped resources that are returned. (optional)
     * @param fields This parameter specifies the attribute field of the scoped resources that are returned. (optional)
     * @param dataNodeSelector This parameter contains an expression allowing
     *                         to conditionally select data nodes. (optional)
     * @return Success case "200 OK".
     *          The resources identified in the request for retrieval are returned in the response message body.
     *          In case the attributes or fields query parameters are used,
     *          only the selected attributes or sub-attributes are returned.
     *          The response message body is constructed according to the hierarchical response
     *          construction method (TS 32.158 [15]). (status code 200) or Error case. (status code 200)
     */
    @Operation(
        operationId = "getMoi",
        summary = "Reads one or multiple resources",
        description = "With HTTP GET resources are read. "
            + "The resources to be retrieved are identified with the target URI. "
            + "The attributes and fields parameter of the query components allow"
            + " to select the resource properties to be returned.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Success case (\"200 OK\"). "
                + "The resources identified in the request for retrieval are returned in the response message body. "
                + "In case the attributes or fields query parameters are used, "
                + "only the selected attributes or sub-attributes are returned. "
                + "The response message body is constructed according to the "
                + "hierarchical response construction method (TS 32.158 [15]).", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Resource.class))
                    }),
            @ApiResponse(responseCode = "422", description = "Invalid Path Exception", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))
            }),
            @ApiResponse(responseCode = "default", description = "Error case.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseGet.class))
            })
        }
    )
    @GetMapping(
        value = "v1/**",
        produces = { "application/json"}
    )

    ResponseEntity<Object> getMoi(
        HttpServletRequest httpServletRequest,
        @Parameter(name = "scope", description = "This parameter extends the set of targeted resources beyond the "
            + "base resource identified with the path component of the URI. "
            + "No scoping mechanism is specified in the present document.", in = ParameterIn.QUERY) @Valid Scope scope,
        @Parameter(name = "filter", description = "This parameter reduces the targeted set of resources by applying"
            + " a filter to the scoped set of resource representations. "
            + "Only resource representations for which the filter construct evaluates to \"true\" are targeted.",
            in = ParameterIn.QUERY) @Valid @RequestParam(value = "filter", required = false) String filter,
        @Parameter(name = "attributes", description = "This parameter specifies the attributes of the scoped "
            + "resources that are returned.", in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "attributes", required = false)
        List<String> attributes,
        @Parameter(name = "fields", description = "This parameter specifies the attribute field "
            + "of the scoped resources that are returned.", in = ParameterIn.QUERY)
        @Valid @RequestParam(value = "fields", required = false) List<String> fields,
        @Parameter(name = "dataNodeSelector", description = "This parameter contains an expression "
            + "allowing to conditionally select data nodes.", in = ParameterIn.QUERY) @Valid
        ClassNameIdGetDataNodeSelectorParameter dataNodeSelector
    );

    /**
     * DELETE /{URI-LDN-first-part}/{className}={id} : Deletes one resource
     * With HTTP DELETE one resource is deleted. The resources to be deleted is identified with the target URI.
     *
     * @param httpServletRequest (required)
     * @return Success case "200 OK". This status code is returned, when the resource has been successfully deleted.
     *         The response body is empty. (status code 200)
     */
    @Operation(
        operationId = "deleteMoi",
        summary = "Deletes one resource",
        description = "With HTTP DELETE one resource is deleted. "
            + "The resources to be deleted is identified with the target URI.",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Success case (\"200 OK\"). This status code is returned, "
                    + "when the resource has been successfully deleted. The response body is empty."),
            @ApiResponse(responseCode = "422", description = "Invalid Path Exception", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))
            }),
            @ApiResponse(responseCode = "default", description = "Error case.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDefault.class))
            })
        }
    )
    @DeleteMapping(
        value = "v1/**",
        produces = { "application/json" }
    )
    ResponseEntity<Object> deleteMoi(HttpServletRequest httpServletRequest);

    /**
     * PATCH /{URI-LDN-first-part}/{className}={id} : Patches one or multiple resources
     * With HTTP PATCH resources are created, updated or deleted.
     * The resources to be modified are identified with the target URI (base resource)
     * and the patch document included in the request message body.
     *
     * @param httpServletRequest (required)
     * @param patchItems The request body describes changes to be made to the target resources.
     *                 The following patch media types are available
     *                 - "application/json-patch+json" (RFC 6902)
     *                 - "application/3gpp-json-patch+json" (TS 32.158) (required)
     * @return Success case ("200 OK").
     *         This status code is returned when the updated resource representations
     *         shall be returned for some reason.
     *         The resource representations are returned in the response message body.
     *         The response message body is constructed according to the hierarchical
     *         response construction method (TS 32.158 [15]) (status code 200)
     *         or Success case ("204 No Content"). This status code is returned when there is no need to
     *         return the updated resource representations. The response message body is empty. (status code 204)
     *         or Error case. (status code 200)
     */
    @Operation(
        operationId = "patchMoi",
        summary = "Patches one or multiple resources",
        description = "With HTTP PATCH resources are created, updated or deleted. "
            + "The resources to be modified are identified with the target URI (base resource) "
            + "and the patch document included in the request message body.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Success case (\"200 OK\"). "
                + "This status code is returned when the updated the resource representations shall be returned "
                + "for some reason. The resource representations are returned in the response message body. The "
                + "response message body is constructed according to the hierarchical response construction method "
                + "(TS 32.158 [15])", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Resource.class))
                }),
            @ApiResponse(responseCode = "204", description = "Success case (\"204 No Content\"). "
                + "This status code is returned when there is no need to return the updated resource representations. "
                + "The response message body is empty."),
            @ApiResponse(responseCode = "422", description = "Invalid Path Exception", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))
                }),
            @ApiResponse(responseCode = "default", description = "Error case.", content = {
                @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ClassNameIdPatchDefaultResponse.class))
            })
        }
    )
    @PatchMapping(
        value = "v1/**",
        produces = { "application/json" },
        consumes = { "application/json-patch+json", "application/3gpp-json-patch+json" }
    )

    ResponseEntity<Object> patchMoi(
        HttpServletRequest httpServletRequest,
        @Parameter(name = "Resource", description = "The request body describes changes to be made to the target "
            + "resources. The following patch media types are available   "
            + "- \"application/json-patch+json\" (RFC 6902)   "
            + "- \"application/3gpp-json-patch+json\" (TS 32.158)", required = true) @Valid @RequestBody
        List<PatchItem> patchItems
    );


    /**
     * PUT /{URI-LDN-first-part}/{className}={id} : Replaces a complete single resource or
     * creates it if it does not exist
     * With HTTP PUT a complete resource is replaced or created if it does not exist.
     * The target resource is identified by the target URI.
     *
     * @param httpServletRequest (required)
     * @param resource  (required)
     * @return Success case ("200 OK"). This status code shall be returned when the resource is replaced,
     *         and when the replaced resource representation is not identical to the resource representation in
     *         the request. This status code may be returned when the resource is updated and when the updated
     *         resource representation is identical to the resource representation in the request.
     *         The representation of the updated resource is returned in the response message body. (status code 200)
     *         or Success case ("201 Created"). This status code shall be returned when the resource
     *         is created.
     *         The representation of the created resource is returned in the response message body. (status code 201)
     *         or Success case ("204 No Content"). This status code may be returned only when the replaced
     *         resource representation is identical to the representation in the request.
     *         The response has no message body. (status code 204)
     *         or Error case. (status code 200)
     */
    @Operation(
        operationId = "putMoi",
        summary = "Replaces a complete single resource or creates it if it does not exist",
        description = "With HTTP PUT a complete resource is replaced or created if it does not exist. "
            + "The target resource is identified by the target URI.",
        responses = {
            @ApiResponse(responseCode = "200", description = "Success case (\"200 OK\"). "
                + "This status code shall be returned when the resource is replaced, and when the replaced "
                + "resource representation is not identical to the resource representation in the request. "
                + "This status code may be returned when the resource is updated and when the updated resource "
                + "representation is identical to the resource representation in the request. "
                + "The representation of the updated resource is returned in the response message body.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Resource.class))
                }),
            @ApiResponse(responseCode = "201", description = "Success case (\"201 Created\"). "
                + "This status code shall be returned when the resource is created. The representation of"
                + " the created resource is returned in the response message body.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Resource.class))
                }),
            @ApiResponse(responseCode = "204", description = "Success case (\"204 No Content\"). "
                + "This status code may be returned only when the replaced resource representation is identical "
                + "to the representation in the request. The response has no message body."),
            @ApiResponse(responseCode = "422", description = "Invalid Path Exception", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorMessage.class))
                }),
            @ApiResponse(responseCode = "default", description = "Error case.", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponseDefault.class))
            })
        }
    )
    @PutMapping(
        value = "v1/**",
        produces = { "application/json" },
        consumes = { "application/json" }
    )

    ResponseEntity<Object> putMoi(
        HttpServletRequest httpServletRequest,
        @Parameter(name = "Resource",
            description = "The request body describes the resource that has been created or replaced", required = true)
        @Valid @RequestBody Resource resource
    );

}