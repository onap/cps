/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.rest.util;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import io.netty.handler.timeout.TimeoutException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.ncmp.api.exceptions.PolicyExecutorException;
import org.onap.cps.ncmp.api.exceptions.ProvMnSException;
import org.springframework.http.HttpStatus;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProvMnSExceptionMapper {
    /**
     * Converts an exception to a ProvMnSException with an appropriate HTTP status.
     *
     * @param exception      the original exception
     * @param httpMethodName the HTTP method name for error reporting
     * @param badOp          the bad operation identifier, or null if not applicable
     * @return a ProvMnSException with the mapped HTTP status
     */
    public static ProvMnSException toProvMnSException(final Exception exception, final String httpMethodName,
                                               final String badOp) {
        if (exception instanceof ProvMnSException) {
            return (ProvMnSException) exception;
        }
        final HttpStatus httpStatus;
        if (exception instanceof PolicyExecutorException) {
            httpStatus = CONFLICT;
        } else if (exception instanceof DataValidationException) {
            httpStatus = BAD_REQUEST;
        } else if (exception.getCause() instanceof TimeoutException) {
            httpStatus = GATEWAY_TIMEOUT;
        } else {
            httpStatus = INTERNAL_SERVER_ERROR;
        }
        log.warn("ProvMns Exception: {}", exception.getMessage());
        return new ProvMnSException(httpMethodName, httpStatus, exception.getMessage(), badOp);
    }
}
