/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ncmp.rest.controller.handlers;

import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.OPERATIONAL;
import static org.onap.cps.ncmp.api.impl.operations.OperationType.READ;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.exception.InvalidDatastoreException;
import org.onap.cps.ncmp.api.impl.operations.DatastoreType;
import org.onap.cps.ncmp.api.impl.operations.OperationType;
import org.onap.cps.ncmp.api.models.DataOperationRequest;
import org.onap.cps.ncmp.rest.exceptions.OperationNotSupportedException;
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor;
import org.onap.cps.ncmp.rest.util.TopicValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NcmpDatastoreRequestHandler implements TaskManagementDefaultHandler {

    @Value("${notification.async.executor.time-out-value-in-ms:2000}")
    protected int timeOutInMilliSeconds;

    @Value("${notification.enabled:true}")
    protected boolean notificationFeatureEnabled;

    private final CpsNcmpTaskExecutor cpsNcmpTaskExecutor;

    /**
     * Executes synchronous/asynchronous request for given cm handle.
     *
     * @param datastoreName       the name of the datastore
     * @param cmHandleId          the cm handle
     * @param resourceIdentifier  the resource identifier
     * @param optionsParamInQuery the options param in query
     * @param topicParamInQuery   the topic param in query
     * @param includeDescendants  whether include descendants
     * @return the response entity
     */
    public ResponseEntity<Object> executeRequest(final String datastoreName,
                                                 final String cmHandleId,
                                                 final String resourceIdentifier,
                                                 final String optionsParamInQuery,
                                                 final String topicParamInQuery,
                                                 final boolean includeDescendants) {

        final boolean asyncResponseRequested = topicParamInQuery != null;
        if (asyncResponseRequested && notificationFeatureEnabled) {
            return executeAsyncTaskAndGetResponseEntity(datastoreName, cmHandleId, resourceIdentifier,
                optionsParamInQuery, topicParamInQuery, includeDescendants);
        }

        if (asyncResponseRequested) {
            log.warn("Asynchronous request is unavailable as notification feature is currently disabled, "
                    + "will use synchronous operation.");
        }
        final Supplier<Object> taskSupplier = getTaskSupplierForGetRequest(datastoreName, cmHandleId,
                resourceIdentifier, optionsParamInQuery, NO_TOPIC, NO_REQUEST_ID, includeDescendants);
        return executeTaskSync(taskSupplier);
    }

    /**
     * Executes a synchronous request for given cm handle.
     * Note. Currently only ncmp-datastore:operational supports query operations.
     *
     * @param cmHandleId         the cm handle
     * @param resourceIdentifier the resource identifier
     * @param includeDescendants whether include descendants
     * @return the response entity
     */
    public ResponseEntity<Object> executeRequest(final String cmHandleId,
                                                 final String resourceIdentifier,
                                                 final boolean includeDescendants) {

        final Supplier<Object> taskSupplier = getTaskSupplierForQueryRequest(cmHandleId, resourceIdentifier,
                includeDescendants);
        return executeTaskSync(taskSupplier);
    }

    /**
     * Executes asynchronous request for group of cm handles to resource data.
     *
     * @param topicParamInQuery        the topic param in query
     * @param dataOperationRequest     data operation request details for resource data
     * @return the response entity
     */
    public ResponseEntity<Object> executeRequest(final String topicParamInQuery,
                                                 final DataOperationRequest
                                                         dataOperationRequest) {
        validateDataOperationRequest(topicParamInQuery, dataOperationRequest);
        if (!notificationFeatureEnabled) {
            return ResponseEntity.ok(Map.of("status",
                    "Asynchronous request is unavailable as notification feature is currently disabled."));
        }
        return getRequestIdAndSendDataOperationRequestToDmiService(topicParamInQuery, dataOperationRequest);
    }

    protected ResponseEntity<Object> executeTaskAsync(final String topicParamInQuery,
                                                      final String requestId,
                                                      final Supplier<Object> taskSupplier) {

        TopicValidator.validateTopicName(topicParamInQuery);
        log.debug("Received Async request with id {}", requestId);
        cpsNcmpTaskExecutor.executeTask(taskSupplier, timeOutInMilliSeconds);
        return ResponseEntity.ok(Map.of("requestId", requestId));
    }

    protected ResponseEntity<Object> executeTaskSync(final Supplier<Object> taskSupplier) {
        return ResponseEntity.ok(taskSupplier.get());
    }

    private ResponseEntity<Object> executeAsyncTaskAndGetResponseEntity(final String datastoreName,
                                                                        final String cmHandleId,
                                                                        final String resourceIdentifier,
                                                                        final String optionsParamInQuery,
                                                                        final String topicParamInQuery,
                                                                        final boolean includeDescendants) {
        final String requestId = UUID.randomUUID().toString();
        final Supplier<Object> taskSupplier = getTaskSupplierForGetRequest(datastoreName, cmHandleId,
                resourceIdentifier, optionsParamInQuery, topicParamInQuery, requestId, includeDescendants);
        if (taskSupplier == NO_OBJECT_SUPPLIER) {
            return new ResponseEntity<>(Map.of("status", "Unable to execute request as "
                    + "datastore is not implemented."), HttpStatus.NOT_IMPLEMENTED);
        }
        return executeTaskAsync(topicParamInQuery, requestId, taskSupplier);
    }

    private ResponseEntity<Object> getRequestIdAndSendDataOperationRequestToDmiService(final String topicParamInQuery,
                                                                                       final DataOperationRequest
                                                                                       dataOperationRequest) {
        final String requestId = UUID.randomUUID().toString();
        sendDataOperationRequestAsynchronously(topicParamInQuery, dataOperationRequest, requestId);
        return ResponseEntity.ok(Map.of("requestId", requestId));
    }

    private void validateDataOperationRequest(final String topicParamInQuery,
                                              final DataOperationRequest
                                              dataOperationRequest) {
        TopicValidator.validateTopicName(topicParamInQuery);
        dataOperationRequest.getDataOperationDefinitions().forEach(dataOperationDetail -> {
            if (OperationType.fromOperationName(dataOperationDetail.getOperation()) != READ) {
                throw new OperationNotSupportedException(
                        dataOperationDetail.getOperation() + " operation not yet supported");
            } else if (DatastoreType.fromDatastoreName(dataOperationDetail.getDatastore()) == OPERATIONAL) {
                throw new InvalidDatastoreException(dataOperationDetail.getDatastore()
                        + " datastore is not supported");
            }
        });
    }
}
