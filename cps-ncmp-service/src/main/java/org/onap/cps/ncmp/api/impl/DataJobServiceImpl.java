/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.DataJobService;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.impl.operations.OperationType;
import org.onap.cps.ncmp.api.models.datajob.DataJobMetadata;
import org.onap.cps.ncmp.api.models.datajob.DataJobReadRequest;
import org.onap.cps.ncmp.api.models.datajob.DataJobWriteRequest;
import org.onap.cps.ncmp.api.models.datajob.ProducerKey;
import org.onap.cps.ncmp.api.models.datajob.WriteOperation;
import org.onap.cps.spi.model.DataNode;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataJobServiceImpl implements DataJobService {

    private final InventoryPersistence inventoryPersistence;
    private final DmiRestClient dmiRestClient;
    private final ObjectMapper objectMapper;
    private static final String SEPARATOR = "/";

    @Override
    public void readDataJob(final String dataJobId, final DataJobMetadata dataJobMetadata,
                            final DataJobReadRequest dataJobReadRequest) {
        log.info("data job id for read operation is: {}", dataJobId);
    }

    @Override
    public void writeDataJob(final String dataJobId, final DataJobMetadata dataJobMetadata,
                             final DataJobWriteRequest dataJobWriteRequest) {
        if (!dataJobWriteRequest.data().isEmpty()) {
            final WriteOperation writeOperation = dataJobWriteRequest.data().get(0);
            final DataNode dataNode = inventoryPersistence
                    .getCmHandleDataNodeByLongestMatchAlternateId(writeOperation.path(), SEPARATOR);
            final ProducerKey producerKey = new ProducerKey(dataNode.getLeaves().get("id") + "_"
                    + dataNode.getLeaves().get("data-producer-identifier"), writeOperation);
            try {
                dmiRestClient.postOperationWithJsonData("", objectMapper.writeValueAsString(producerKey),
                        OperationType.CREATE, null);
            } catch (final JsonProcessingException e) {
                log.error("Error reading the file.", e);
            }
        }
        log.info("data job id for write operation is: {}", dataJobId);
    }
}
