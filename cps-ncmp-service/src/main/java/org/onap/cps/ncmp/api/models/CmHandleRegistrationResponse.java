/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada
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

package org.onap.cps.ncmp.api.models;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
public class CmHandleRegistrationResponse {

    private final String cmHandle;
    private final Status status;
    private RegistrationError registrationError;
    private String errorText;

    /**
     * Creates a failure response based on exception.
     *
     * @param cmHandle  cmHandle
     * @param exception exception
     * @return CmHandleRegistrationResponse
     */
    public static CmHandleRegistrationResponse createFailureResponse(final String cmHandle, final Exception exception) {
        return CmHandleRegistrationResponse.builder()
            .cmHandle(cmHandle)
            .status(Status.FAILURE)
            .registrationError(RegistrationError.UNKNOWN_ERROR)
            .errorText(exception.getMessage()).build();
    }

    public static CmHandleRegistrationResponse createFailureResponse(final String cmHandle, final RegistrationError registrationError) {
        return CmHandleRegistrationResponse.builder().cmHandle(cmHandle)
            .status(Status.FAILURE)
            .registrationError(registrationError)
            .errorText(registrationError.errorText)
            .build();
    }

    public static CmHandleRegistrationResponse createSuccessResponse(final String cmHandle) {
        return CmHandleRegistrationResponse.builder().cmHandle(cmHandle)
            .status(CmHandleRegistrationResponse.Status.SUCCESS).build();
    }

    public enum Status {
        SUCCESS, FAILURE;
    }

    @RequiredArgsConstructor
    public enum RegistrationError {
        UNKNOWN_ERROR("00", "Unknown error"),
        CM_HANDLE_ALREADY_EXIST("01", "cm-handle already exists"),
        CM_HANDLE_DOES_NOT_EXIST("02", "cm-handle does not exist");

        public final String errorCode;
        public final String errorText;

    }
}