/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.rest.provmns;

import java.util.Map;
import org.onap.cps.ncmp.impl.provmns.model.ErrorResponseDefault;
import org.onap.cps.ncmp.impl.provmns.model.ErrorResponseGet;
import org.onap.cps.ncmp.impl.provmns.model.ErrorResponsePatch;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class ErrorResponseBuilder {

    private static final Map<HttpStatus, String> ERROR_MAP = Map.of(
        HttpStatus.NOT_FOUND, "IE_NOT_FOUND",
        HttpStatus.NOT_ACCEPTABLE, "APPLICATION_LAYER_ERROR",
        HttpStatus.UNPROCESSABLE_ENTITY, "SERVER_LIMITATION",
        HttpStatus.PAYLOAD_TOO_LARGE, "SERVER_LIMITATION"
    );

    /**
     * Create response entity for default error response.
     * Default is used by PUT and DELETE
     *
     * @param httpStatus   HTTP response
     * @param reason       reason for error response
     * @return response entity
     */
    public ResponseEntity<Object> buildErrorResponseDefault(final HttpStatus httpStatus, final String reason) {
        final ErrorResponseDefault errorResponseDefault = new ErrorResponseDefault(ERROR_MAP.get(httpStatus));
        errorResponseDefault.setStatus(httpStatus.toString());
        errorResponseDefault.setReason(reason);
        return new ResponseEntity<>(errorResponseDefault, httpStatus);
    }

    /**
     * Create response entity for get error response.
     *
     * @param httpStatus   HTTP response
     * @param reason       reason for error response
     * @return response entity
     */
    public ResponseEntity<Object> buildErrorResponseGet(final HttpStatus httpStatus, final String reason) {
        final ErrorResponseGet errorResponseGet = new ErrorResponseGet(ERROR_MAP.get(httpStatus));
        errorResponseGet.setStatus(httpStatus.toString());
        errorResponseGet.setReason(reason);
        return new ResponseEntity<>(errorResponseGet, httpStatus);
    }

    /**
     * Create response entity for patch error response.
     *
     * @param httpStatus   HTTP response
     * @param reason       reason for error response
     * @return response entity
     */
    public ResponseEntity<Object> buildErrorResponsePatch(final HttpStatus httpStatus, final String reason) {
        final ErrorResponsePatch errorResponsePatch = new ErrorResponsePatch(ERROR_MAP.get(httpStatus));
        errorResponsePatch.setStatus(httpStatus.toString());
        errorResponsePatch.setReason(reason);
        return new ResponseEntity<>(errorResponsePatch, httpStatus);
    }
}
