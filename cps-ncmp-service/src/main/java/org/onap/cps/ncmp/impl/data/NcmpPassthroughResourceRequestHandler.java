/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.data;

import static org.onap.cps.ncmp.api.data.models.DatastoreType.OPERATIONAL;
import static org.onap.cps.ncmp.api.data.models.OperationType.READ;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.data.exceptions.InvalidDatastoreException;
import org.onap.cps.ncmp.api.data.exceptions.OperationNotSupportedException;
import org.onap.cps.ncmp.api.data.models.CmResourceAddress;
import org.onap.cps.ncmp.api.data.models.DataOperationRequest;
import org.onap.cps.ncmp.api.data.models.DatastoreType;
import org.onap.cps.ncmp.api.data.models.OperationType;
import org.onap.cps.ncmp.api.exceptions.PayloadTooLargeException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class NcmpPassthroughResourceRequestHandler extends NcmpDatastoreRequestHandler {

    private final DmiDataOperations dmiDataOperations;

    @Value("${ncmp.cm-handle-query-max:120000}")
    private int cmHandleQueryMax;

    @Value("${app.ncmp.data.max-operations-per-request:200}")
    private int maxNumberOfOperationsPerRequest;

    private static final int MAXIMUM_CM_HANDLES_PER_OPERATION = 200;

    /**
     * Executes asynchronous request for group of cm handles to resource data.
     *
     * @param topic                 the topic param in query
     * @param dataOperationRequest  data operation request details for resource data
     * @param authorization         contents of Authorization header, or null if not present
     * @return a map with one entry of request id for success or status and error when async feature is disabled
     */
    public Map<String, String> executeAsynchronousRequest(final String topic,
                                                          final DataOperationRequest dataOperationRequest,
                                                          final String authorization) {
        validateDataOperationRequest(topic, dataOperationRequest);
        if (!notificationFeatureEnabled) {
            return Map.of("status",
                "Asynchronous request is unavailable as notification feature is currently disabled.");
        }
        final String requestId = UUID.randomUUID().toString();
        dmiDataOperations.requestResourceDataFromDmi(topic, dataOperationRequest, requestId, authorization);
        return Map.of("requestId", requestId);
    }

    @Override
    protected Mono<Object> getResourceDataForCmHandle(final CmResourceAddress cmResourceAddress,
                                                      final String options,
                                                      final String topic,
                                                      final String requestId,
                                                      final boolean includeDescendants,
                                                      final String authorization) {

        return dmiDataOperations.getResourceDataFromDmi(cmResourceAddress, options, topic, requestId, authorization)
            .flatMap(responseEntity -> Mono.justOrEmpty(responseEntity.getBody()));
    }

    private void validateDataOperationRequest(final String topicParamInQuery,
                                              final DataOperationRequest dataOperationRequest) {
        topicValidator.validateTopicName(topicParamInQuery);
        final int numberOfOperations = dataOperationRequest.getDataOperationDefinitions().size();
        if (numberOfOperations > maxNumberOfOperationsPerRequest) {
            throw new PayloadTooLargeException("Data operation request contains too many (" + numberOfOperations
                    + ") operations. Maximum allowed is " + maxNumberOfOperationsPerRequest + ".");
        }
        int totalCmHandleReferences = 0;
        for (final var dataOperationDefinition : dataOperationRequest.getDataOperationDefinitions()) {
            if (OperationType.fromOperationName(dataOperationDefinition.getOperation()) != READ) {
                throw new OperationNotSupportedException(
                        dataOperationDefinition.getOperation() + " operation not yet supported");
            }
            if (DatastoreType.fromDatastoreName(dataOperationDefinition.getDatastore()) == OPERATIONAL) {
                throw new InvalidDatastoreException(dataOperationDefinition.getDatastore()
                        + " datastore is not supported");
            }
            if (dataOperationDefinition.getCmHandleReferences().size() > MAXIMUM_CM_HANDLES_PER_OPERATION) {
                throw new PayloadTooLargeException("Operation '" + dataOperationDefinition.getOperationId()
                        + "' affects too many (" + dataOperationDefinition.getCmHandleReferences().size()
                        + ") cm handles");
            }
            totalCmHandleReferences += dataOperationDefinition.getCmHandleReferences().size();
        }
        if (totalCmHandleReferences > cmHandleQueryMax) {
            throw new PayloadTooLargeException("Data operation request affects too many (" + totalCmHandleReferences
                    + ") cm handles. Maximum allowed is " + cmHandleQueryMax + ".");
        }
    }
}
