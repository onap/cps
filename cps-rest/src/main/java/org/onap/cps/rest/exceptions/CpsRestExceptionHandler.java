/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
 *  Modifications Copyright (C) 2021 Nordix Foundation
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

package org.onap.cps.rest.exceptions;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.rest.controller.AdminRestController;
import org.onap.cps.rest.controller.DataRestController;
import org.onap.cps.rest.controller.QueryRestController;
import org.onap.cps.rest.model.ErrorMessage;
import org.onap.cps.spi.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.exceptions.CpsAdminException;
import org.onap.cps.spi.exceptions.CpsException;
import org.onap.cps.spi.exceptions.CpsPathException;
import org.onap.cps.spi.exceptions.DataInUseException;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.exceptions.ModelValidationException;
import org.onap.cps.spi.exceptions.NotFoundInDataspaceException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice(assignableTypes = {AdminRestController.class, DataRestController.class,
    QueryRestController.class})
public class CpsRestExceptionHandler {

    private CpsRestExceptionHandler() {
    }

    /**
     * Default exception handler.
     *
     * @param exception the exception to handle
     * @return response with response code 500.
     */
    @ExceptionHandler
    public static ResponseEntity<Object> handleInternalServerErrorExceptions(final Exception exception) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception);
    }

    @ExceptionHandler({ModelValidationException.class, DataValidationException.class, CpsAdminException.class,
        CpsPathException.class})
    public static ResponseEntity<Object> handleBadRequestExceptions(final CpsException exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler({ValidationException.class})
    public static ResponseEntity<Object> handleBadRequestExceptions(final ValidationException validationException) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, validationException);
    }

    @ExceptionHandler({NotFoundInDataspaceException.class, DataNodeNotFoundException.class})
    public static ResponseEntity<Object> handleNotFoundExceptions(final CpsException exception,
        final HttpServletRequest request) {
        return buildErrorResponse(HttpMethod.GET.matches(request.getMethod())
            ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler({DataInUseException.class, AlreadyDefinedException.class})
    public static ResponseEntity<Object> handleDataInUseException(final CpsException exception) {
        return buildErrorResponse(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler({CpsException.class})
    public static ResponseEntity<Object> handleAnyOtherCpsExceptions(final CpsException exception) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception);
    }

    private static ResponseEntity<Object> buildErrorResponse(final HttpStatus status, final Exception exception) {
        if (exception.getCause() != null || !(exception instanceof CpsException)) {
            log.error("Exception occurred", exception);
        }
        final var errorMessage = new ErrorMessage();
        errorMessage.setStatus(status.toString());
        errorMessage.setMessage(exception.getMessage());
        errorMessage.setDetails(exception instanceof CpsException ? ((CpsException) exception).getDetails() :
            "Check logs for details.");
        return new ResponseEntity<>(errorMessage, status);
    }
}
