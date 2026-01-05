/*
 *  ============LICENSE_START=======================================================
 *  Modifications Copyright (C) 2025-2026 OpenInfra Foundation Europe
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

package org.onap.cps.ncmp.rest.controller;

import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.exceptions.ProvMnSException;
import org.onap.cps.ncmp.impl.provmns.model.ErrorResponseDefault;
import org.onap.cps.ncmp.impl.provmns.model.ErrorResponseGet;
import org.onap.cps.ncmp.impl.provmns.model.ErrorResponsePatch;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handler for ProvMns Controller only.
 */
@Slf4j
@RestControllerAdvice(assignableTypes = { ProvMnSController.class})
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class ProvMnSRestExceptionHandler {

    private static final String NO_OP = null;
    private static final Map<HttpStatus, String> PROVMNS_ERROR_TYPE_PER_ERROR_CODE = Map.of(
        HttpStatus.NOT_FOUND, "IE_NOT_FOUND",
        HttpStatus.NOT_ACCEPTABLE, "APPLICATION_LAYER_ERROR",
        HttpStatus.UNPROCESSABLE_ENTITY, "SERVER_LIMITATION",
        HttpStatus.PAYLOAD_TOO_LARGE, "SERVER_LIMITATION"
    );

    /**
     * Exception handler for ProvMnS exceptions with method-specific error responses.
     *
     * @param provMnSException the ProvMnS exception to handle
     * @return response with appropriate HTTP status and method-specific error format
     */
    @ExceptionHandler({ProvMnSException.class})
    public static ResponseEntity<Object> handleProvMnsExceptions(final ProvMnSException provMnSException) {
        switch (provMnSException.getHttpMethodName()) {
            case "PATCH":
                if (provMnSException.getBadOp() == NO_OP) {
                    return provMnSErrorResponseDefault(provMnSException.getHttpStatus(), provMnSException.getTitle());
                }
                return provMnSErrorResponsePatch(provMnSException.getHttpStatus(), provMnSException.getTitle(),
                        provMnSException.getBadOp());
            case "GET":
                return provMnSErrorResponseGet(provMnSException.getHttpStatus(), provMnSException.getTitle());
            default:
                return provMnSErrorResponseDefault(provMnSException.getHttpStatus(), provMnSException.getTitle());
        }
    }

    private static ResponseEntity<Object> provMnSErrorResponsePatch(final HttpStatus httpStatus,
                                                                    final String title,
                                                                    final String badOp) {
        final String type = PROVMNS_ERROR_TYPE_PER_ERROR_CODE.get(httpStatus);
        final ErrorResponsePatch errorResponsePatch = new ErrorResponsePatch(type, badOp);
        errorResponsePatch.setStatus(String.valueOf(httpStatus.value()));
        errorResponsePatch.setTitle(title);
        return new ResponseEntity<>(errorResponsePatch, httpStatus);
    }

    private static ResponseEntity<Object> provMnSErrorResponseGet(final HttpStatus httpStatus, final String title) {
        final String type = PROVMNS_ERROR_TYPE_PER_ERROR_CODE.get(httpStatus);
        final ErrorResponseGet errorResponseGet = new ErrorResponseGet(type);
        errorResponseGet.setStatus(String.valueOf(httpStatus.value()));
        errorResponseGet.setTitle(title);
        return new ResponseEntity<>(errorResponseGet, httpStatus);
    }

    private static ResponseEntity<Object> provMnSErrorResponseDefault(final HttpStatus httpStatus, final String title) {
        final String type = PROVMNS_ERROR_TYPE_PER_ERROR_CODE.get(httpStatus);
        final ErrorResponseDefault errorResponseDefault = new ErrorResponseDefault(type);
        errorResponseDefault.setStatus(String.valueOf(httpStatus.value()));
        errorResponseDefault.setTitle(title);
        return new ResponseEntity<>(errorResponseDefault, httpStatus);
    }

}
