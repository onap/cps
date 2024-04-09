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

package org.onap.cps.ncmp.rest.controller.handlers;

import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.OPERATIONAL;
import static org.onap.cps.ncmp.api.impl.operations.OperationType.READ;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.impl.exception.InvalidDatastoreException;
import org.onap.cps.ncmp.api.impl.operations.DatastoreType;
import org.onap.cps.ncmp.api.impl.operations.OperationType;
import org.onap.cps.ncmp.api.models.CmResourceAddress;
import org.onap.cps.ncmp.api.models.DataOperationRequest;
import org.onap.cps.ncmp.rest.exceptions.OperationNotSupportedException;
import org.onap.cps.ncmp.rest.exceptions.OperationTooLargeException;
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor;
import org.onap.cps.ncmp.rest.util.TopicValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class NcmpPassthroughResourceRequestHandler extends NcmpDatastoreRequestHandler {

    private final NetworkCmProxyDataService networkCmProxyDataService;

    private static final Object noReturn = null;

    private static final int MAXIMUM_CM_HANDLES_PER_OPERATION = 50;

    /**
     * Constructor.
     *
     * @param cpsNcmpTaskExecutor        @see org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor
     * @param networkCmProxyDataService  @see org.onap.cps.ncmp.api.NetworkCmProxyDataService
     */
    public NcmpPassthroughResourceRequestHandler(final CpsNcmpTaskExecutor cpsNcmpTaskExecutor,
                                                 final NetworkCmProxyDataService networkCmProxyDataService) {
        super(cpsNcmpTaskExecutor);
        this.networkCmProxyDataService = networkCmProxyDataService;
    }

    /**
     * Executes asynchronous request for group of cm handles to resource data.
     *
     * @param topicParamInQuery        the topic param in query
     * @param dataOperationRequest     data operation request details for resource data
     * @param authorization            contents of Authorization header, or null if not present
     * @return the response entity
     */
    public ResponseEntity<Object> executeRequest(final String topicParamInQuery,
                                                 final DataOperationRequest dataOperationRequest,
                                                 final String authorization) {
        validateDataOperationRequest(topicParamInQuery, dataOperationRequest);
        if (!notificationFeatureEnabled) {
            return ResponseEntity.ok(Map.of("status",
                "Asynchronous request is unavailable as notification feature is currently disabled."));
        }
        return getRequestIdAndSendDataOperationRequestToDmiService(topicParamInQuery, dataOperationRequest,
                authorization);
    }

    @Override
    protected Supplier<Object> getTaskSupplierForGetRequest(final CmResourceAddress cmResourceAddress,
                                                            final String optionsParamInQuery,
                                                            final String topicParamInQuery,
                                                            final String requestId,
                                                            final boolean includeDescendants,
                                                            final String authorization) {

        return () -> networkCmProxyDataService.getResourceDataForCmHandle(cmResourceAddress, optionsParamInQuery,
            topicParamInQuery, requestId, authorization);
    }

    private ResponseEntity<Object> getRequestIdAndSendDataOperationRequestToDmiService(
            final String topicParamInQuery,
            final DataOperationRequest dataOperationRequest,
            final String authorization) {
        final String requestId = UUID.randomUUID().toString();
        cpsNcmpTaskExecutor.executeTask(
            getTaskSupplierForDataOperationRequest(topicParamInQuery, dataOperationRequest, requestId, authorization),
            timeOutInMilliSeconds);
        return ResponseEntity.ok(Map.of("requestId", requestId));
    }

    private void validateDataOperationRequest(final String topicParamInQuery,
                                              final DataOperationRequest dataOperationRequest) {
        TopicValidator.validateTopicName(topicParamInQuery);
        dataOperationRequest.getDataOperationDefinitions().forEach(dataOperationDetail -> {
            if (OperationType.fromOperationName(dataOperationDetail.getOperation()) != READ) {
                throw new OperationNotSupportedException(
                    dataOperationDetail.getOperation() + " operation not yet supported");
            }
            if (DatastoreType.fromDatastoreName(dataOperationDetail.getDatastore()) == OPERATIONAL) {
                throw new InvalidDatastoreException(dataOperationDetail.getDatastore()
                    + " datastore is not supported");
            }
            if (dataOperationDetail.getCmHandleIds().size() > MAXIMUM_CM_HANDLES_PER_OPERATION) {
                throw new OperationTooLargeException();
            }
        });
    }

    private Supplier<Object> getTaskSupplierForDataOperationRequest(final String topicParamInQuery,
                                                                    final DataOperationRequest dataOperationRequest,
                                                                    final String requestId,
                                                                    final String authorization) {
        return () -> {
            networkCmProxyDataService.executeDataOperationForCmHandles(topicParamInQuery,
                dataOperationRequest,
                requestId,
                authorization);
            return noReturn;
        };
    }

}
