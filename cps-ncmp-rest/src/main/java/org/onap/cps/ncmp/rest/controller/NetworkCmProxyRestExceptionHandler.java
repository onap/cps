/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2024 Nordix Foundation
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

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.data.exceptions.InvalidDatastoreException;
import org.onap.cps.ncmp.api.data.exceptions.InvalidOperationException;
import org.onap.cps.ncmp.api.data.exceptions.OperationNotSupportedException;
import org.onap.cps.ncmp.api.exceptions.CmHandleNotFoundException;
import org.onap.cps.ncmp.api.exceptions.DmiClientRequestException;
import org.onap.cps.ncmp.api.exceptions.DmiRequestException;
import org.onap.cps.ncmp.api.exceptions.InvalidTopicException;
import org.onap.cps.ncmp.api.exceptions.NcmpException;
import org.onap.cps.ncmp.api.exceptions.PayloadTooLargeException;
import org.onap.cps.ncmp.api.exceptions.PolicyExecutorException;
import org.onap.cps.ncmp.api.exceptions.ServerNcmpException;
import org.onap.cps.ncmp.rest.model.DmiErrorMessage;
import org.onap.cps.ncmp.rest.model.DmiErrorMessageDmiResponse;
import org.onap.cps.ncmp.rest.model.ErrorMessage;
import org.onap.cps.spi.api.exceptions.AlreadyDefinedException;
import org.onap.cps.spi.api.exceptions.CpsException;
import org.onap.cps.spi.api.exceptions.DataNodeNotFoundException;
import org.onap.cps.spi.api.exceptions.DataValidationException;
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
    public static ResponseEntity<Object> handleInternalServerErrorExceptions(final Exception exception) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception);
    }

    @ExceptionHandler({CpsException.class, ServerNcmpException.class})
    public static ResponseEntity<Object> handleAnyOtherCpsExceptions(final Exception exception) {
        return buildErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR, exception);
    }

    @ExceptionHandler({DmiClientRequestException.class})
    public static ResponseEntity<Object> handleClientRequestExceptions(
            final DmiClientRequestException dmiClientRequestException) {
        return wrapDmiErrorResponse(dmiClientRequestException);
    }

    @ExceptionHandler({DmiRequestException.class, DataValidationException.class, InvalidOperationException.class,
        OperationNotSupportedException.class, HttpMessageNotReadableException.class, InvalidTopicException.class,
        InvalidDatastoreException.class})
    public static ResponseEntity<Object> handleDmiRequestExceptions(final Exception exception) {
        return buildErrorResponse(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler({AlreadyDefinedException.class, PolicyExecutorException.class})
    public static ResponseEntity<Object> handleConflictExceptions(final Exception exception) {
        return buildErrorResponse(HttpStatus.CONFLICT, exception);
    }

    @ExceptionHandler({CmHandleNotFoundException.class, DataNodeNotFoundException.class})
    public static ResponseEntity<Object> cmHandleNotFoundExceptions(final Exception exception) {
        return buildErrorResponse(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler({PayloadTooLargeException.class})
    public static ResponseEntity<Object> handlePayloadTooLargeExceptions(final Exception exception) {
        return buildErrorResponse(HttpStatus.PAYLOAD_TOO_LARGE, exception);
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
        return new ResponseEntity<>(errorMessage, status);
    }

    private static ResponseEntity<Object> wrapDmiErrorResponse(final DmiClientRequestException
                                                                       dmiClientRequestException) {
        final var dmiErrorMessage = new DmiErrorMessage();
        final var dmiErrorResponse = new DmiErrorMessageDmiResponse();
        dmiErrorResponse.setHttpCode(dmiClientRequestException.getHttpStatusCode());
        dmiErrorResponse.setBody(dmiClientRequestException.getResponseBodyAsString());
        dmiErrorMessage.setMessage(dmiClientRequestException.getMessage());
        dmiErrorMessage.setDmiResponse(dmiErrorResponse);
        return new ResponseEntity<>(dmiErrorMessage, HttpStatus.BAD_GATEWAY);
    }
}
