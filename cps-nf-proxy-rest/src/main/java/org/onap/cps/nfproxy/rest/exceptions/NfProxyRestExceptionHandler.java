/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
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

package org.onap.cps.nfproxy.rest.exceptions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.onap.cps.nfproxy.rest.controller.NfProxyController;
import org.onap.cps.nfproxy.rest.model.ErrorMessage;
import org.onap.cps.spi.exceptions.CpsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handler with error message return.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@RestControllerAdvice(assignableTypes = {NfProxyController.class})
public class NfProxyRestExceptionHandler {

    /**
     * Default exception handler.
     *
     * @param exception the exception to handle
     * @return response with response code 500.
     */
    @ExceptionHandler
    public static ResponseEntity<Object> handleInternalServerErrorExceptions(
        final Exception exception) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception);
    }

    @ExceptionHandler({CpsException.class})
    public static ResponseEntity<Object> handleAnyOtherCpsExceptions(final CpsException exception) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), extractDetails(exception));
    }

    private static ResponseEntity<Object> buildErrorResponse(final HttpStatus status, final Exception exception) {
        return buildErrorResponse(status, exception.getMessage(), ExceptionUtils.getStackTrace(exception));
    }

    private static ResponseEntity<Object> buildErrorResponse(final HttpStatus status, final String message,
        final String details) {
        log.error("An error has occurred : {} Status: {} Details: {}", message, status, details);
        final ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(status.toString());
        errorMessage.setMessage(message);
        return new ResponseEntity<>(errorMessage, status);
    }

    private static String extractDetails(final CpsException exception) {
        return exception.getCause() == null
            ? exception.getDetails()
            : ExceptionUtils.getStackTrace(exception.getCause());
    }
}
