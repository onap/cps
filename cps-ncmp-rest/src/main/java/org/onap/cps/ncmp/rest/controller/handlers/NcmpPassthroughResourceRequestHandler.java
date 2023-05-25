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

import java.util.function.Supplier;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.models.ResourceDataBatchRequest;
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class NcmpPassthroughResourceRequestHandler extends NcmpDatastoreRequestHandler {

    private final NetworkCmProxyDataService networkCmProxyDataService;

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

    @Override
    public Supplier<Object> getTaskSupplierForGetRequest(final String datastoreName,
                                                         final String cmHandleId,
                                                         final String resourceIdentifier,
                                                         final String optionsParamInQuery,
                                                         final String topicParamInQuery,
                                                         final String requestId,
                                                         final boolean includeDescendants) {

        return () -> networkCmProxyDataService.getResourceDataForCmHandle(
                datastoreName, cmHandleId, resourceIdentifier, optionsParamInQuery, topicParamInQuery, requestId);
    }

    @Async
    @Override
    public void sendResourceDataBatchRequestAsynchronously(final String topicParamInQuery,
                                                           final ResourceDataBatchRequest
                                                                  resourceDataBatchRequest,
                                                           final String requestId) {
        networkCmProxyDataService.requestResourceDataForCmHandleBatch(topicParamInQuery, resourceDataBatchRequest,
                requestId);

    }
}
