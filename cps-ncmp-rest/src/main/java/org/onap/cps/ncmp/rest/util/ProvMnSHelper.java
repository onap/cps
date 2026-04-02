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

package org.onap.cps.ncmp.rest.util;

import static org.onap.cps.ncmp.impl.provmns.ParameterHelper.NO_OP;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_ACCEPTABLE;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNPROCESSABLE_ENTITY;

import io.netty.handler.timeout.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.ncmp.api.exceptions.PolicyExecutorException;
import org.onap.cps.ncmp.api.exceptions.ProvMnSException;
import org.onap.cps.ncmp.api.inventory.models.CmHandleState;
import org.onap.cps.ncmp.exceptions.NoAlternateIdMatchFoundException;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.utils.AlternateIdMatcher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;


@Component
@Slf4j
@RequiredArgsConstructor
public class ProvMnSHelper {

    private static final String PROVMNS_NOT_SUPPORTED_ERROR_MESSAGE =
        "Registered DMI does not support the ProvMnS interface.";

    private final AlternateIdMatcher alternateIdMatcher;
    private final InventoryPersistence inventoryPersistence;

    /**
     * Retrieves and validates a YangModelCmHandle for the given FDN.
     *
     * @param fdn            the fully distinguished name to resolve
     * @param httpMethodName the HTTP method name for error reporting
     * @return a validated YangModelCmHandle in READY state with ProvMnS support
     * @throws ProvMnSException if the FDN is not found, the cm handle is not READY, or ProvMnS is not supported
     */
    public YangModelCmHandle getAndValidateYangModelCmHandle(final String fdn, final String httpMethodName)
        throws ProvMnSException {
        try {
            final String cmHandleId = alternateIdMatcher.getCmHandleIdByLongestMatchingAlternateId(fdn, "/");
            final YangModelCmHandle yangModelCmHandle = inventoryPersistence.getYangModelCmHandle(cmHandleId);
            if (!StringUtils.hasText(yangModelCmHandle.getDataProducerIdentifier())) {
                throw new ProvMnSException(httpMethodName, UNPROCESSABLE_ENTITY,
                    PROVMNS_NOT_SUPPORTED_ERROR_MESSAGE, NO_OP);
            }
            if (yangModelCmHandle.getCompositeState().getCmHandleState() != CmHandleState.READY) {
                final String title = yangModelCmHandle.getId() + " is not in READY state. Current state: "
                    + yangModelCmHandle.getCompositeState().getCmHandleState().name();
                throw new ProvMnSException(httpMethodName, NOT_ACCEPTABLE, title, NO_OP);
            }
            return yangModelCmHandle;
        } catch (final NoAlternateIdMatchFoundException noAlternateIdMatchFoundException) {
            throw new ProvMnSException(httpMethodName, NOT_FOUND, fdn + " not found", NO_OP);
        }
    }

    /**
     * Converts an exception to a ProvMnSException with an appropriate HTTP status.
     *
     * @param httpMethodName the HTTP method name for error reporting
     * @param exception      the original exception
     * @param badOp          the bad operation identifier, or null if not applicable
     * @return a ProvMnSException with the mapped HTTP status
     */
    public ProvMnSException toProvMnSException(final String httpMethodName, final Exception exception,
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
