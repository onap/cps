/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.onap.cps.ncmp.rest.model.ActionRequest;
import org.onap.cps.ncmp.rest.model.ActionResponse;
import org.onap.cps.ncmp.rest.model.ErrorResponseDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Tag(name = "action", description = "APIs for executing Managed Object actions")
public interface ProvMnSExtensions {

    /**
     * POST /actions/{distinguishedName}/{actionName} : Execute a Managed Object action
     * Execute a Managed Object action identified by the URI path with supplied input parameters if there are any.
     *
     * @param httpServletRequest httpServletRequest Data
     * @param actionRequest  (required)
     * @return Ok - This status code is returned when the action has been successfully executed. (status code 200)
     *         or No content - This status code may be returned only when the action response has no message body.
     *         (status code 204) or Bad Request (status code 400)
     *         or Not Found - The Managed Object does not exist in the path or there is no action with
     *         given Action Name. (status code 404) or Not Acceptable - Server is not yet ready to accept requests
     *         towards given Managed Object. (status code 406) or Internal Server Error (status code 500)
     *         or Gateway Timeout - Southbound System is unavailable. (status code 504)
     */
    @Operation(
        operationId = "executeAction",
        summary = "Execute a Managed Object action",
        description = "Execute a Managed Object action identified by the URI path with supplied input parameters "
            + "if there are any.",
        tags = { "action" },
        responses = {
            @ApiResponse(responseCode = "200", description = "OK - This status code is returned when the action has "
                + "been successfully executed.", content = {
                    @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ActionResponse.class)),
                    @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ActionResponse.class))
                }),
            @ApiResponse(responseCode = "204", description = "No Content - This status code may be returned only when "
                + "the action response has no message body."),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = {
                @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponseDefault.class)),
                @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ErrorResponseDefault.class))
            }),
            @ApiResponse(responseCode = "404", description = "Not Found - The Managed Object does not exist in the "
                + "path or there is no action with given Action Name.", content = {
                    @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponseDefault.class)),
                    @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ErrorResponseDefault.class))
                }),
            @ApiResponse(responseCode = "406", description = "Not Acceptable - Server is not yet ready to accept "
                + "requests towards given Managed Object.", content = {
                    @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponseDefault.class)),
                    @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ErrorResponseDefault.class))
                }),
            @ApiResponse(responseCode = "500", description = "Internal Server Error", content = {
                @Content(mediaType = "application/json",
                    schema = @Schema(implementation = ErrorResponseDefault.class)),
                @Content(mediaType = "application/problem+json",
                    schema = @Schema(implementation = ErrorResponseDefault.class))
            }),
            @ApiResponse(responseCode = "504", description = "Gateway Timeout - "
                + "Southbound System is unavailable.", content = {
                    @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponseDefault.class)),
                    @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ErrorResponseDefault.class))
                })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/v1alpha1/actions/**",
        produces = { "application/json", "application/problem+json" },
        consumes = { "application/json" }
    )

    ResponseEntity<Object> executeAction(HttpServletRequest httpServletRequest,
                                         @Parameter(name = "ActionRequest", description = "", required = true)
                                         @Valid @RequestBody ActionRequest actionRequest
    );

}
