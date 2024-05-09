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

package org.onap.cps.ncmp.impl.datajobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.datajobs.DataJobService;
import org.onap.cps.ncmp.api.datajobs.models.DataJobMetadata;
import org.onap.cps.ncmp.api.datajobs.models.DataJobReadRequest;
import org.onap.cps.ncmp.api.datajobs.models.DataJobWriteRequest;
import org.onap.cps.ncmp.api.datajobs.models.Dmi3ggpWriteOperation;
import org.onap.cps.ncmp.api.datajobs.models.ProducerKey;
import org.onap.cps.ncmp.api.datajobs.models.SubJobWriteRequest;
import org.onap.cps.ncmp.api.datajobs.models.WriteOperation;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.operations.OperationType;
import org.onap.cps.ncmp.api.impl.utils.DmiServiceUrlBuilder;
import org.onap.cps.ncmp.api.impl.utils.YangDataConverter;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.utils.AlternateIdMatcher;
import org.onap.cps.spi.model.DataNode;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataJobServiceImpl implements DataJobService {

    private final AlternateIdMatcher alternateIdMatcher;
    private final DmiRestClient dmiRestClient;
    private final DmiServiceUrlBuilder dmiServiceUrlBuilder;
    protected final JsonObjectMapper jsonObjectMapper;
    private static final String SEPARATOR = "/";
    private static final String NO_AUTH_HEADER = null;

    @Override
    public void readDataJob(final String dataJobId, final DataJobMetadata dataJobMetadata,
                            final DataJobReadRequest dataJobReadRequest) {
        log.info("data job id for read operation is: {}", dataJobId);
    }

    @Override
    public void writeDataJob(final String dataJobId, final DataJobMetadata dataJobMetadata,
                             final DataJobWriteRequest dataJobWriteRequest) {
        final Map<ProducerKey, Collection<SubJobWriteRequest>> producerKeyMap = new HashMap<>();

        for (final WriteOperation writeOperation: dataJobWriteRequest.data()) {

            log.info("data job id for write operation is: {}", dataJobId);
            final DataNode dataNode = alternateIdMatcher
                    .getCmHandleDataNodeByLongestMatchAlternateId(writeOperation.path(), SEPARATOR);

            final Dmi3ggpWriteOperation dmiWriteOperation = createWriteOperation(writeOperation, dataNode);
            final SubJobWriteRequest subJobWriteRequest = createSubJobWriteRequest(dataJobId,
                                                                                    dataJobMetadata, dmiWriteOperation);

            final ProducerKey producerKey = new ProducerKey((String) dataNode.getLeaves().get("dmi-service-name"),
                    (String) dataNode.getLeaves().get("data-producer-identifier"));

            if (producerKeyMap.containsKey(producerKey)) {
                final Collection<SubJobWriteRequest> subJobWriteRequests = producerKeyMap.get(producerKey);
                subJobWriteRequests.add(subJobWriteRequest);
            } else {
                final Collection<SubJobWriteRequest> subJobWriteRequestCollection = new ArrayList<>();
                subJobWriteRequestCollection.add(subJobWriteRequest);
                producerKeyMap.put(producerKey, subJobWriteRequestCollection);
            }
        }

        producerKeyMap.forEach((key, value) -> {
            final String dmiResourceUrl = dmiServiceUrlBuilder.getWriteJobUrl(key.dmiServiceName(), dataJobId);
            dmiRestClient.postOperationWithJsonData(dmiResourceUrl, jsonObjectMapper.asJsonString(value),
                    OperationType.CREATE, NO_AUTH_HEADER);
        });
    }

    private SubJobWriteRequest createSubJobWriteRequest(final String dataJobId,
                                                        final DataJobMetadata dataJobMetadata,
                                                        final Dmi3ggpWriteOperation writeOperation) {
        final ArrayList<Dmi3ggpWriteOperation> writeOperationList = new ArrayList<>();
        writeOperationList.add(writeOperation);
        return new SubJobWriteRequest(dataJobMetadata.dataAcceptType(),
                dataJobMetadata.dataContentType(), dataJobId, writeOperationList);
    }

    private Dmi3ggpWriteOperation createWriteOperation(final WriteOperation writeOperation, final DataNode dataNode) {
        return new Dmi3ggpWriteOperation(
        writeOperation.path(),
        writeOperation.op(),
        writeOperation.operationId(),
        writeOperation.value(),
        (String) dataNode.getLeaves().get("module-set-tag"),
        getPrivatePropertiesFromDataNode(dataNode));
    }

    private Map<String, String> getPrivatePropertiesFromDataNode(final DataNode dataNode) {
        final YangModelCmHandle yangModelCmHandle =
                YangDataConverter.convertCmHandleToYangModel(dataNode);
        final Map<String, String> cmHandleDmiProperties = new LinkedHashMap<>();
        yangModelCmHandle.getDmiProperties()
                .forEach(dmiProperty -> cmHandleDmiProperties.put(dmiProperty.getName(), dmiProperty.getValue()));
        return cmHandleDmiProperties;
    }
}
