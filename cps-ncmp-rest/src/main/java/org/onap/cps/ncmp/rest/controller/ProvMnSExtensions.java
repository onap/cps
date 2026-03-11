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

@Tag(name = "provmns-extensions", description = "the provmns-extensions API")
public interface ProvMnSExtensions {

    /**
     * POST /actions/{distinguishedName}/{action} : Execute a Managed Object action
     * Execute a Managed Object action identified by the URI path with supplied input parameters if there are any.
     *
     * @param httpServletRequest httpServletRequest Data
     * @param actionRequest  (required)
     * @return Created - This status code is returned when the action has been successfully executed.
     *         The action execution result is returned in the response message body. (status code 201)
     *         or Accepted - This status code may be returned only when the action response has no message body.
     *         The action execution result is not yet available, the action is executed asynchronously.
     *         (status code 202) or Internal Server Error (status code 200)
     */
    @Operation(
        operationId = "executeAction",
        summary = "Execute a Managed Object action",
        description = "Execute a Managed Object action identified by the URI path with supplied "
            + "input parameters if there are any.",
        tags = { "action" },
        responses = {
            @ApiResponse(responseCode = "201", description = "Created - This status code is returned "
                + "when the action has been successfully executed. "
                + "The action execution result is returned in the response message body.",
                content = {
                    @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ActionResponse.class)),
                    @Content(mediaType = "application/problem+json",
                        schema = @Schema(implementation = ActionResponse.class))
                }),
            @ApiResponse(responseCode = "202", description = "Accepted - This status code may be returned "
                + "only when the action response has no message body. "
                + "The action execution result is not yet available, the action is executed asynchronously."),
            @ApiResponse(responseCode = "default", description = "Internal Server Error",
                content = {
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
