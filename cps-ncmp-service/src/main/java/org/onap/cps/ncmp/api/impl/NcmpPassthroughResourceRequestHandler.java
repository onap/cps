/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
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

import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.OPERATIONAL;
import static org.onap.cps.ncmp.api.impl.operations.OperationType.READ;

import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.impl.exception.InvalidDatastoreException;
import org.onap.cps.ncmp.api.impl.operations.DatastoreType;
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations;
import org.onap.cps.ncmp.api.impl.operations.OperationType;
import org.onap.cps.ncmp.api.models.CmResourceAddress;
import org.onap.cps.ncmp.api.models.DataOperationRequest;
import org.onap.cps.ncmp.exceptions.OperationNotSupportedException;
import org.onap.cps.ncmp.exceptions.PayloadTooLargeException;
import org.onap.cps.ncmp.utils.events.TopicValidator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class NcmpPassthroughResourceRequestHandler extends NcmpDatastoreRequestHandler {

    private final DmiDataOperations dmiDataOperations;

    private static final int MAXIMUM_CM_HANDLES_PER_OPERATION = 200;
    private static final String PAYLOAD_TOO_LARGE_TEMPLATE = "Operation '%s' affects too many (%d) cm handles";

    /**
     * Executes asynchronous request for group of cm handles to resource data.
     *
     * @param topic                 the topic param in query
     * @param dataOperationRequest  data operation request details for resource data
     * @param authorization         contents of Authorization header, or null if not present
     * @return a map with one entry of request Id for success or status and error when async feature is disabled
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
        TopicValidator.validateTopicName(topicParamInQuery);
        dataOperationRequest.getDataOperationDefinitions().forEach(dataOperationDefinition -> {
            if (OperationType.fromOperationName(dataOperationDefinition.getOperation()) != READ) {
                throw new OperationNotSupportedException(
                        dataOperationDefinition.getOperation() + " operation not yet supported");
            }
            if (DatastoreType.fromDatastoreName(dataOperationDefinition.getDatastore()) == OPERATIONAL) {
                throw new InvalidDatastoreException(dataOperationDefinition.getDatastore()
                        + " datastore is not supported");
            }
            if (dataOperationDefinition.getCmHandleIds().size() > MAXIMUM_CM_HANDLES_PER_OPERATION) {
                final String errorMessage = String.format(PAYLOAD_TOO_LARGE_TEMPLATE,
                        dataOperationDefinition.getOperationId(),
                        dataOperationDefinition.getCmHandleIds().size());
                throw new PayloadTooLargeException(errorMessage);
            }
        });
    }
}
