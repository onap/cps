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
import org.onap.cps.ncmp.impl.provmns.model.ErrorResponseDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Tag(name = "provmns-extension", description = "the provmns-extension API")
public interface ProvMnSExtension {

    /**
     * POST /v1/{fdn}/{action} : Execute an action on a network resource
     * Executes a specified action on a network resource identified by its FDN (Fully Distinguished Name).
     * The action and input parameters are provided in the request.
     *
     * @param httpServletRequest (required)
     * @param body  (required)
     * @return Success case (\&quot;200 OK\&quot;). The action was executed successfully and
     *         the result is returned in the response body. (status code 200)
     *         or Success case (\&quot;204 No Content\&quot;).
     *         The action was executed successfully with no result to return. (status code 204)
     *         or Error case. The action execution failed. (status code 200)
     */
    @Operation(
        operationId = "provmnsExtensionAction",
        summary = "Execute an action on a network resource",
        description = "Executes a specified action on a network resource identified by its "
            + "FDN (Fully Distinguished Name). The action and input parameters are provided in the request.",
        tags = { "provmns-extension" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Success case (\"200 OK\"). "
                + "The action was executed successfully and the result is returned in the response body.",
                content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Object.class))
                }),
            @ApiResponse(responseCode = "204", description = "Success case (\"204 No Content\"). "
                + "The action was executed successfully with no result to return."),
            @ApiResponse(responseCode = "default", description = "Error case. The action execution failed.",
                content = {
                    @Content(mediaType = "application/json",
                        schema = @Schema(implementation = ErrorResponseDefault.class))
                })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/v1/**",
        produces = { "application/json" },
        consumes = { "application/json" }
    )

    ResponseEntity<Object> provmnsExtensionAction(
        HttpServletRequest httpServletRequest,
        @Parameter(name = "body", description = "", required = true) @Valid @RequestBody Object body
    );

}
