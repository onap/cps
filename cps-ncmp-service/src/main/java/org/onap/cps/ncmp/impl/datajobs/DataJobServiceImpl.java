/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.datajobs;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.datajobs.DataJobService;
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata;
import org.onap.cps.ncmp.api.datajobs.models.DataJobReadRequest;
import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest;
import org.onap.cps.ncmp.api.datajobs.models.DmiWriteOperation;
import org.onap.cps.ncmp.api.datajobs.models.ProducerKey;
import org.onap.cps.ncmp.api.datajobs.models.SubJobWriteResponse;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataJobServiceImpl implements DataJobService {

    private final DmiSubJobRequestHandler dmiSubJobClient;
    private final WriteRequestExaminer writeRequestExaminer;
    private final JsonObjectMapper jsonObjectMapper;

    @Override
    public void readDataJob(final String authorization,
                            final String dataJobId,
                            final DataJobMetadata dataJobMetadata,
                            final DataJobReadRequest dataJobReadRequest) {
        logJobIdAndSize(dataJobId, dataJobReadRequest.data().size());
        log.info("Destination: {}", dataJobMetadata.destination());
        log.info("authorization: {}", authorization);
    }

    @Override
    public List<SubJobWriteResponse> writeDataJob(final String authorization,
                                                  final String dataJobId,
                                                  final DataJobMetadata dataJobMetadata,
                                                  final DataJobWriteRequest dataJobWriteRequest) {
        logJobIdAndSize(dataJobId, dataJobWriteRequest.data().size());
        logJsonRepresentation("Initiating WRITE operation for Data Job ID: " + dataJobId, dataJobWriteRequest);

        final Map<ProducerKey, List<DmiWriteOperation>> dmiWriteOperationsPerProducerKey =
                writeRequestExaminer.splitDmiWriteOperationsFromRequest(dataJobId, dataJobWriteRequest);

        final List<SubJobWriteResponse> subJobWriteResponses = dmiSubJobClient.sendRequestsToDmi(authorization,
                dataJobId, dataJobMetadata, dmiWriteOperationsPerProducerKey);

        log.info("Data Job ID: {} - Received {} sub-job(s) from DMI.", dataJobId, subJobWriteResponses.size());
        logJsonRepresentation("Finalized subJobWriteResponses for Data Job ID: " + dataJobId, subJobWriteResponses);
        return subJobWriteResponses;
    }

    private void logJobIdAndSize(final String dataJobId, final int numberOfOperations) {
        log.info("Data Job ID: {} - Total operations received: {}", dataJobId, numberOfOperations);
    }

    private void logJsonRepresentation(final String description, final Object object) {
        if (log.isDebugEnabled()) {
            final String objectAsJsonString = jsonObjectMapper.asJsonString(object);
            log.debug("{} (JSON): {}", description, objectAsJsonString);
        }
    }

}
