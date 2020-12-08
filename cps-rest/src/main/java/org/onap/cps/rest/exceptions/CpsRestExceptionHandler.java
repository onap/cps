/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
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

package org.onap.cps.rest.exceptions;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.onap.cps.rest.controller.CpsRestController;
import org.onap.cps.rest.model.ErrorMessage;
import org.onap.cps.spi.exceptions.CpsException;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.exceptions.ModelValidationException;
import org.onap.cps.spi.exceptions.NotFoundInDataspaceException;
import org.onap.cps.spi.exceptions.SchemaSetAlreadyDefinedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {CpsRestController.class})
public class CpsRestExceptionHandler {

    /**
     * Default exception handler.
     *
     * @param exception the exception to handle
     * @return response with response code 500.
     */
    @ExceptionHandler public static ResponseEntity<Object> handleInternalServerErrorExceptions(
        final Exception exception) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception);
    }

    @ExceptionHandler({ModelValidationException.class, DataValidationException.class,
        SchemaSetAlreadyDefinedException.class})
    public static ResponseEntity<Object> handleBadRequestExceptions(final CpsException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), extractDetails(exception));
    }

    @ExceptionHandler({NotFoundInDataspaceException.class})
    public static ResponseEntity<Object> handleNotFoundExceptions(final CpsException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception.getMessage(), extractDetails(exception));
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
        final ErrorMessage errorMessage = new ErrorMessage();
        errorMessage.setStatus(status.toString());
        errorMessage.setMessage(message);
        errorMessage.setDetails(details);
        return new ResponseEntity<>(errorMessage, status);
    }

    private static String extractDetails(final CpsException exception) {
        return exception.getCause() == null
            ? exception.getDetails()
            : ExceptionUtils.getStackTrace(exception.getCause());
    }
}
