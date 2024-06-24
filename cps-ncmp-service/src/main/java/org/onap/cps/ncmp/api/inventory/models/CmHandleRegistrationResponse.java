/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada
 *  Modifications Copyright (C) 2022-2024 Nordix Foundation
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

package org.onap.cps.ncmp.api.inventory.models;

import static org.onap.cps.ncmp.api.NcmpResponseStatus.UNKNOWN_ERROR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NcmpResponseStatus;
import org.onap.cps.ncmp.impl.utils.YangDataConverter;

@Data
@Builder
@Slf4j
public class CmHandleRegistrationResponse {

    private final String cmHandle;
    private final Status status;
    private NcmpResponseStatus ncmpResponseStatus;
    private String errorText;

    /**
     * Creates a failure response based on exception.
     *
     * @param cmHandleId  cmHandleId
     * @param exception exception
     * @return CmHandleRegistrationResponse
     */
    public static CmHandleRegistrationResponse createFailureResponse(final String cmHandleId,
                                                                     final Exception exception) {
        return CmHandleRegistrationResponse.builder()
            .cmHandle(cmHandleId)
            .status(Status.FAILURE)
            .ncmpResponseStatus(UNKNOWN_ERROR)
            .errorText(exception.getMessage()).build();
    }

    /**
     * Creates a failure response based on registration error.
     *
     * @param cmHandleId          cmHandleId
     * @param ncmpResponseStatus registration error code and status
     * @return CmHandleRegistrationResponse
     */
    public static CmHandleRegistrationResponse createFailureResponse(final String cmHandleId,
        final NcmpResponseStatus ncmpResponseStatus) {
        return CmHandleRegistrationResponse.builder().cmHandle(cmHandleId)
            .status(Status.FAILURE)
            .ncmpResponseStatus(ncmpResponseStatus)
            .errorText(ncmpResponseStatus.getMessage())
            .build();
    }

    /**
     * Create a failure response of cm handle registration based on xpath and registration error.
     * Conditions:
     * - the xpath should be valid according to the cps path, otherwise xpath is not included in the response.
     *
     * @param failedXpaths the failed xpaths
     * @param ncmpResponseStatus type of the registration error
     * @return collection of cm handle registration response
     */
    public static List<CmHandleRegistrationResponse> createFailureResponsesFromXpaths(
        final Collection<String> failedXpaths, final NcmpResponseStatus ncmpResponseStatus) {
        final List<CmHandleRegistrationResponse> cmHandleRegistrationResponses = new ArrayList<>(failedXpaths.size());
        for (final String xpath : failedXpaths) {
            try {
                final String cmHandleId = YangDataConverter.extractCmHandleIdFromXpath(xpath);
                cmHandleRegistrationResponses
                    .add(CmHandleRegistrationResponse.createFailureResponse(cmHandleId, ncmpResponseStatus));
            } catch (IllegalArgumentException | IllegalStateException e) {
                log.warn("Unexpected xpath {}", xpath);
            }
        }
        return cmHandleRegistrationResponses;
    }

    /**
     * Create a failure response of cm handle registration based on cm handle id and registration error.
     *
     * @param failedCmHandleIds the failed cm handle ids
     * @param ncmpResponseStatus type of the registration error
     * @return collection of cm handle registration response
     */
    public static List<CmHandleRegistrationResponse> createFailureResponses(
            final Collection<String> failedCmHandleIds, final NcmpResponseStatus ncmpResponseStatus) {
        final List<CmHandleRegistrationResponse> cmHandleRegistrationResponses =
            new ArrayList<>(failedCmHandleIds.size());
        for (final String failedCmHandleId : failedCmHandleIds) {
            cmHandleRegistrationResponses.add(
                CmHandleRegistrationResponse.createFailureResponse(failedCmHandleId, ncmpResponseStatus));
        }
        return cmHandleRegistrationResponses;
    }

    /**
     * Creates a failure response based on other exception.
     *
     * @param cmHandleIds list of failed cmHandleIds
     * @param exception   exception caught during the registration
     * @return CmHandleRegistrationResponse
     */
    public static List<CmHandleRegistrationResponse> createFailureResponses(final Collection<String> cmHandleIds,
            final Exception exception) {
        return cmHandleIds.stream()
                .map(cmHandleId -> CmHandleRegistrationResponse.createFailureResponse(cmHandleId, exception))
                .toList();
    }

    public static CmHandleRegistrationResponse createSuccessResponse(final String cmHandle) {
        return CmHandleRegistrationResponse.builder().cmHandle(cmHandle)
            .status(Status.SUCCESS).build();
    }

    public static List<CmHandleRegistrationResponse> createSuccessResponses(final Collection<String> cmHandleIds) {
        return cmHandleIds.stream().map(CmHandleRegistrationResponse::createSuccessResponse)
                .collect(Collectors.toList());
    }

    public enum Status {
        SUCCESS, FAILURE;
    }
}
