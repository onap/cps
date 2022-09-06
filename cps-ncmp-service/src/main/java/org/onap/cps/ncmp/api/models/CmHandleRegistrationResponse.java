/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada
 *  Modifications Copyright (C) 2022 Nordix Foundation
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

import java.util.List;
import java.util.stream.Collectors;
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

    /**
     * Creates a failure response based on registration error.
     *
     * @param cmHandle          cmHandle
     * @param registrationError registrationError
     * @return CmHandleRegistrationResponse
     */
    public static CmHandleRegistrationResponse createFailureResponse(final String cmHandle,
        final RegistrationError registrationError) {
        return CmHandleRegistrationResponse.builder().cmHandle(cmHandle)
            .status(Status.FAILURE)
            .registrationError(registrationError)
            .errorText(registrationError.errorText)
            .build();
    }

    /**
     * Creates a failure response based on registration error.
     *
     * @param cmHandleIds       list of failed cmHandleIds
     * @param registrationError registrationError
     * @return CmHandleRegistrationResponse
     */
    public static List<CmHandleRegistrationResponse> createFailureResponses(final List<String> cmHandleIds,
            final RegistrationError registrationError) {
        return cmHandleIds.stream()
                .map(cmHandleId -> CmHandleRegistrationResponse.createFailureResponse(cmHandleId, registrationError))
                .collect(Collectors.toList());
    }

    /**
     * Creates a failure response based on exception.
     *
     * @param cmHandleIds list of failed cmHandleIds
     * @param exception   exception
     * @return CmHandleRegistrationResponse
     */
    public static List<CmHandleRegistrationResponse> createFailureResponses(final List<String> cmHandleIds,
            final Exception exception) {
        return cmHandleIds.stream()
                .map(cmHandleId -> CmHandleRegistrationResponse.createFailureResponse(cmHandleId, exception))
                .collect(Collectors.toList());
    }

    public static CmHandleRegistrationResponse createSuccessResponse(final String cmHandle) {
        return CmHandleRegistrationResponse.builder().cmHandle(cmHandle)
            .status(Status.SUCCESS).build();
    }

    public static List<CmHandleRegistrationResponse> createSuccessResponses(final List<String> cmHandleIds) {
        return cmHandleIds.stream().map(CmHandleRegistrationResponse::createSuccessResponse)
                .collect(Collectors.toList());
    }

    public enum Status {
        SUCCESS, FAILURE;
    }

    @RequiredArgsConstructor
    public enum RegistrationError {
        UNKNOWN_ERROR("00", "Unknown error"),
        CM_HANDLE_ALREADY_EXIST("01", "cm-handle already exists"),
        CM_HANDLE_DOES_NOT_EXIST("02", "cm-handle does not exist"),
        CM_HANDLE_INVALID_ID("03", "cm-handle has an invalid character(s) in id");

        public final String errorCode;
        public final String errorText;

    }
}