/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe. All rights reserved.
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
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.rest.controller;

import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.datajobs.DataJobService;
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata;
import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest;
import org.onap.cps.ncmp.api.datajobs.models.SubJobWriteResponse;
import org.onap.cps.ncmp.rest.controller.models.DataJobRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller responsible for handling data job write operations.
 * This class exposes an API endpoint that accepts a write request for a data job and processes it.
 */
@Slf4j
@RestController
@RequestMapping("/do-not-use/dataJobs")
@RequiredArgsConstructor
public class DataJobControllerForTest {

    private final DataJobService dataJobService;

    /**
     * Handles POST requests to write a data job. This endpoint is unsupported and intended for testing purposes only.
     * This internal endpoint processes a data job write request by extracting necessary metadata and data
     * from the request body and delegating the operation to the {@link DataJobService}.
     * <p><b>Note:</b> The {@link DataJobRequest} parameter is created and used for testing purposes only.
     * In a production environment, data job write operations are not triggered through internal workflows.</p>
     *
     * @param authorization  The optional authorization token sent in the request header.
     * @param dataJobId      The unique identifier for the data job, extracted from the URL path.
     * @param dataJobRequest The request payload containing metadata and data for the data job write operation.
     * @return A {@link ResponseEntity} containing a list of {@link SubJobWriteResponse} objects representing the
     *     status of each sub-job within the data job, or an error response with an appropriate HTTP status code.
     */
    @PostMapping("/{dataJobId}/write")
    @Hidden
    public ResponseEntity<List<SubJobWriteResponse>> writeDataJob(@RequestHeader(value = "Authorization",
                                                                          required = false) final String authorization,
                                                                  @PathVariable("dataJobId") final String dataJobId,
                                                                  @RequestBody final DataJobRequest dataJobRequest) {
        log.info("Internal API: writeDataJob invoked for {}", dataJobId);
        final DataJobMetadata dataJobMetadata = dataJobRequest.dataJobMetadata();
        final DataJobWriteRequest dataJobWriteRequest = dataJobRequest.dataJobWriteRequest();
        final List<SubJobWriteResponse> subJobWriteResponses = dataJobService.writeDataJob(authorization, dataJobId,
                dataJobMetadata, dataJobWriteRequest);
        return ResponseEntity.ok(subJobWriteResponses);
    }
}
