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

import java.util.ArrayList;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.stream.Collectors;

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
     * @param cmHandleId  cmHandle identifier
     * @param exception exception
     * @return CmHandleRegistrationResponse
     */
    public static CmHandleRegistrationResponse createFailureResponse(final String cmHandleId,
                                                                     final Exception exception) {
        return CmHandleRegistrationResponse.builder()
            .cmHandle(cmHandleId)
            .status(Status.FAILURE)
            .registrationError(RegistrationError.UNKNOWN_ERROR)
            .errorText(exception.getMessage()).build();
    }

    /**
     * Creates a failure response based on registration error.
     *
     * @param cmHandleId          cmHandle identifier
     * @param registrationError registrationError
     * @return CmHandleRegistrationResponse
     */
    public static CmHandleRegistrationResponse createFailureResponse(final String cmHandleId,
        final RegistrationError registrationError) {
        return CmHandleRegistrationResponse.builder().cmHandle(cmHandleId)
            .status(Status.FAILURE)
            .registrationError(registrationError)
            .errorText(registrationError.errorText)
            .build();
    }

    public static List<CmHandleRegistrationResponse> createSuccessResponses(final String... cmHandleIds) {
        List<CmHandleRegistrationResponse> cmHandleRegistrationResponses = new ArrayList<>(cmHandleIds.length);
        for (final String cmHandleId : cmHandleIds) {
            cmHandleRegistrationResponses.add(CmHandleRegistrationResponse.builder().cmHandle(cmHandleId)
                .status(Status.SUCCESS).build());
        }
        return cmHandleRegistrationResponses;
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
