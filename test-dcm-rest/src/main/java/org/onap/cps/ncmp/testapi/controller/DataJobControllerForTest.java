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

package org.onap.cps.ncmp.testapi.controller;

import io.swagger.v3.oas.annotations.Hidden;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.datajobs.DataJobService;
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata;
import org.onap.cps.ncmp.api.datajobs.models.DataJobReadRequest;
import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest;
import org.onap.cps.ncmp.api.datajobs.models.SubJobWriteResponse;
import org.onap.cps.ncmp.impl.datajobs.ReadRequestExaminer.ClassifiedSelectors;
import org.onap.cps.ncmp.testapi.controller.models.DataJobRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller responsible for handling data job operations for testing purposes.
 * This class exposes API endpoints that accept read and write requests for data jobs.
 */
@Slf4j
@RestController
@RequestMapping("/do-not-use/dataJobs")
@RequiredArgsConstructor
public class DataJobControllerForTest {

    private final DataJobService dataJobService;

    /**
     * Handles POST requests to read a data job. Intended for testing purposes only.
     *
     * @param dataJobReadRequest the read request payload
     * @return classified selectors
     */
    @PostMapping("/read")
    @Hidden
    public ResponseEntity<ClassifiedSelectors> readDataJob(
            @RequestBody final DataJobReadRequest dataJobReadRequest) {
        log.info("Internal API: readDataJob invoked for {}", dataJobReadRequest.jobId());
        final ClassifiedSelectors classifiedSelectors = dataJobService.readDataJob(dataJobReadRequest);
        return ResponseEntity.ok(classifiedSelectors);
    }

    /**
     * Handles POST requests to write a data job. Intended for testing purposes only.
     *
     * @param authorization  the optional authorization token
     * @param dataJobId      the unique identifier for the data job
     * @param dataJobRequest the request payload containing metadata and write data
     * @return a list of sub-job write responses
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
