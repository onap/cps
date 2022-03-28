/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Nordix Foundation
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

package org.onap.cps.ncmp.rest.exceptions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.exception.DmiRequestException;
import org.onap.cps.ncmp.api.impl.exception.HttpClientRequestException;
import org.onap.cps.ncmp.api.impl.exception.InvalidTopicException;
import org.onap.cps.ncmp.api.impl.exception.NcmpException;
import org.onap.cps.ncmp.api.impl.exception.ServerNcmpException;
import org.onap.cps.ncmp.rest.controller.NetworkCmProxyController;
import org.onap.cps.ncmp.rest.controller.NetworkCmProxyInventoryController;
import org.onap.cps.ncmp.rest.model.DmiErrorMessage;
import org.onap.cps.ncmp.rest.model.DmiErrorMessageDmiresponse;
import org.onap.cps.ncmp.rest.model.ErrorMessage;
import org.onap.cps.spi.exceptions.CpsException;
import org.onap.cps.spi.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Exception handler with error message return.
 */
@Slf4j
@RestControllerAdvice(assignableTypes = {NetworkCmProxyController.class, NetworkCmProxyInventoryController.class})
@NoArgsConstructor(access = AccessLevel.PACKAGE)
public class NetworkCmProxyRestExceptionHandler {

    private static final String CHECK_LOGS_FOR_DETAILS = "Check logs for details.";

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

    @ExceptionHandler({CpsException.class, ServerNcmpException.class})
    public static ResponseEntity<Object> handleAnyOtherCpsExceptions(final Exception exception) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception);
    }

    @ExceptionHandler({HttpClientRequestException.class})
    public static ResponseEntity<Object> handleClientRequestExceptions(final HttpClientRequestException exception) {
        return wrapDmiErrorResponse(HttpStatus.BAD_GATEWAY, exception);
    }

    @ExceptionHandler({DmiRequestException.class, DataValidationException.class, HttpMessageNotReadableException.class,
            InvalidTopicException.class})
    public static ResponseEntity<Object> handleDmiRequestExceptions(final Exception exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler({DataNodeNotFoundException.class})
    public static ResponseEntity<Object> handleNotFoundExceptions(final CpsException exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception);
    }

    private static ResponseEntity<Object> buildErrorResponse(final HttpStatus status, final Exception exception) {
        if (exception.getCause() != null || !(exception instanceof CpsException)) {
            log.error("Exception occurred", exception);
        }
        final var errorMessage = new ErrorMessage();
        errorMessage.setStatus(status.toString());
        errorMessage.setMessage(exception.getMessage());
        if (exception instanceof CpsException) {
            errorMessage.setDetails(((CpsException) exception).getDetails());
        } else if (exception instanceof NcmpException) {
            errorMessage.setDetails(((NcmpException) exception).getDetails());
        } else {
            errorMessage.setDetails(CHECK_LOGS_FOR_DETAILS);
        }
        errorMessage.setDetails(
                exception instanceof CpsException ? ((CpsException) exception).getDetails() : CHECK_LOGS_FOR_DETAILS);
        return new ResponseEntity<>(errorMessage, status);
    }

    private static ResponseEntity<Object> wrapDmiErrorResponse(final HttpStatus httpStatus,
            final HttpClientRequestException exception) {
        final var dmiErrorMessage = new DmiErrorMessage();
        final var dmiErrorResponse = new DmiErrorMessageDmiresponse();
        dmiErrorResponse.setHttpCode(exception.getHttpStatus());
        dmiErrorResponse.setBody(exception.getDetails());
        dmiErrorMessage.setMessage(exception.getMessage());
        dmiErrorMessage.setDmiResponse(dmiErrorResponse);
        return new ResponseEntity<>(dmiErrorMessage, httpStatus);
    }
}
