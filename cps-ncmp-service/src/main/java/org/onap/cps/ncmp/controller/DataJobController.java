/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.ncmp.api.datajobs.DataJobService;
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata;
import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest;
import org.onap.cps.ncmp.api.datajobs.models.SubJobWriteResponse;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Collections;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/donotuse")
@RequiredArgsConstructor
public class DataJobController  {

    private final DataJobService dataJobService;
    private final JsonObjectMapper  jsonObjectMapper;

    @PostMapping("/{dataJobId}/write")
    public ResponseEntity<List<SubJobWriteResponse>> writeDataJob(@RequestHeader("Authorization") String authorization,
                                                                  @RequestHeader("DataJobMetadata") String dataJobMetadataJson,
                                                                  @PathVariable String dataJobId,
                                                                  @RequestBody DataJobWriteRequest dataJobWriteRequest) {
        log.info("Internal API: writeDataJob invoked for {}", dataJobId);

        try {
            DataJobMetadata dataJobMetadata = jsonObjectMapper.convertToValueType(dataJobMetadataJson, DataJobMetadata.class);
            List<SubJobWriteResponse> responseList = dataJobService.writeDataJob(authorization, dataJobId, dataJobMetadata, dataJobWriteRequest);
            return ResponseEntity.ok(responseList);
        } catch (DataValidationException e) {
            log.error("Error processing JSON for DataJobMetadata", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Collections.emptyList());
        } catch (Exception e) {
            log.error("Unexpected error occurred", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Collections.emptyList());
        }
    }
}

