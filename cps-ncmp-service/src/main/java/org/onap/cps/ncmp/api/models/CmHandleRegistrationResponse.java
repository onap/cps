/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada
 *  Modifications Copyright (C) 2022-2023 Nordix Foundation
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

import static org.onap.cps.ncmp.api.NcmpResponseStatus.UNKNOWN_ERROR;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NcmpResponseStatus;

@Data
@Builder
@Slf4j
public class CmHandleRegistrationResponse {

    private final String cmHandle;
    private final Status status;
    private NcmpResponseStatus ncmpResponseStatus;
    private String errorText;

    private static final Pattern cmHandleIdInXpathPattern = Pattern.compile("\\[@id='(.*?)']");

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
     * Creates a failure response based on registration error.
     *
     * @param failedXpaths       list of failed Xpaths
     * @param ncmpResponseStatus enum describing the type of registration error
     * @return CmHandleRegistrationResponse
     */
    public static List<CmHandleRegistrationResponse> createFailureResponses(final Collection<String> failedXpaths,
            final NcmpResponseStatus ncmpResponseStatus) {
        final List<CmHandleRegistrationResponse> cmHandleRegistrationResponses = new ArrayList<>(failedXpaths.size());
        for (final String xpath : failedXpaths) {
            final Matcher matcher = cmHandleIdInXpathPattern.matcher(xpath);
            if (matcher.find()) {
                cmHandleRegistrationResponses.add(
                    CmHandleRegistrationResponse.createFailureResponse(matcher.group(1), ncmpResponseStatus));
            } else {
                log.warn("Unexpected xpath {}", xpath);
            }
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

    public static List<CmHandleRegistrationResponse> createSuccessResponses(final List<String> cmHandleIds) {
        return cmHandleIds.stream().map(CmHandleRegistrationResponse::createSuccessResponse).toList();
    }

    public enum Status {
        SUCCESS, FAILURE;
    }
}
