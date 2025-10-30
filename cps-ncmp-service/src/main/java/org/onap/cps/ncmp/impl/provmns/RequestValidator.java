/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe
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

package org.onap.cps.ncmp.impl.provmns;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.inventory.models.CmHandleState;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RequestValidator {

    private static final String PROVMNS_NOT_SUPPORTED_ERROR_MESSAGE =
        "Registered DMI does not support the ProvMnS interface.";

    private final ErrorResponseBuilder errorResponseBuilder;

    /**
     * Check if dataProducerIdentifier is empty or null
     * and yangModelCmHandle is not in a ready state, if so return error response else return null.
     *
     * @param yangModelCmHandle given yangModelCmHandle.
     */
    public ResponseEntity<Object> checkValidElseReturnNull(final YangModelCmHandle yangModelCmHandle,
                                                           final String type) {
        HttpStatus errorResponseHttpStatus = HttpStatus.OK;
        String errorResponseMessage = "";

        if (yangModelCmHandle.getDataProducerIdentifier() == null
            || yangModelCmHandle.getDataProducerIdentifier().isEmpty()) {
            errorResponseHttpStatus = HttpStatus.UNPROCESSABLE_ENTITY;
            errorResponseMessage = PROVMNS_NOT_SUPPORTED_ERROR_MESSAGE;
        } else if (yangModelCmHandle.getCompositeState().getCmHandleState() != CmHandleState.READY) {
            errorResponseHttpStatus = HttpStatus.NOT_ACCEPTABLE;
            errorResponseMessage = errorResponseBuilder.buildNotReadyStateMessage(yangModelCmHandle);
        }

        if (errorResponseHttpStatus != HttpStatus.OK) {
            return switch (type) {
                case "GET" -> errorResponseBuilder.buildErrorResponseGet(errorResponseHttpStatus,
                    errorResponseMessage);
                case "PATCH" -> errorResponseBuilder.buildErrorResponsePatch(errorResponseHttpStatus,
                    errorResponseMessage);
                default -> errorResponseBuilder.buildErrorResponseDefault(errorResponseHttpStatus,
                    errorResponseMessage);
            };
        }
        return null;
    }
}
