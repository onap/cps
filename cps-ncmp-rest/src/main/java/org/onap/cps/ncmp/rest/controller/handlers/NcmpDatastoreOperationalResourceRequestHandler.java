/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

import java.util.UUID;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.NetworkCmProxyQueryService;
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor;
import org.onap.cps.spi.FetchDescendantsOption;
import org.springframework.http.ResponseEntity;

@Slf4j
public class NcmpDatastoreOperationalResourceRequestHandler extends NcmpDatastoreResourceRequestHandler {

    private final NetworkCmProxyQueryService networkCmProxyQueryService;

    public NcmpDatastoreOperationalResourceRequestHandler(final NetworkCmProxyDataService networkCmProxyDataService,
                                                          final NetworkCmProxyQueryService networkCmProxyQueryService,
                                                          final CpsNcmpTaskExecutor cpsNcmpTaskExecutor,
                                                          final int timeOutInMilliSeconds,
                                                          final boolean notificationFeatureEnabled) {
        super(networkCmProxyDataService, cpsNcmpTaskExecutor, timeOutInMilliSeconds, notificationFeatureEnabled);
        this.networkCmProxyQueryService = networkCmProxyQueryService;
    }

    @Override
    public Supplier<Object> getTask(final String cmHandle,
                                    final String resourceIdentifier,
                                    final String optionsParamInQuery,
                                    final String topicParamInQuery,
                                    final String requestId,
                                    final Boolean includeDescendant) {

        final FetchDescendantsOption fetchDescendantsOption =
                Boolean.TRUE.equals(includeDescendant) ? FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
                        : FetchDescendantsOption.OMIT_DESCENDANTS;

        return () -> networkCmProxyDataService.getResourceDataOperational(cmHandle, resourceIdentifier,
                fetchDescendantsOption);
    }

    /**
     * Get resource data from datastore.
     *
     * @param cmHandleId          the cm handle
     * @param topicParamInQuery   the topic param in query
     * @param includeDescendants  whether include descendants
     * @return the response entity
     */
    public ResponseEntity<Object> queryResourceData(final String cmHandleId,
                                                    final String cpsPath,
                                                    final String topicParamInQuery,
                                                    final Boolean includeDescendants) {

        final Supplier<Object> queryTask = queryTask(cmHandleId, cpsPath, includeDescendants);

        final boolean asyncResponseRequested = topicParamInQuery != null;
        if (asyncResponseRequested && notificationFeatureEnabled) {
            final String requestId = UUID.randomUUID().toString();
            return executeTaskAsync(topicParamInQuery, requestId, queryTask);
        }
        if (asyncResponseRequested) {
            log.warn("Asynchronous messaging is currently disabled, will use synchronous operation.");
        }
        return executeTaskSync(queryTask);
    }

    private Supplier<Object> queryTask(final String cmHandle,
                                       final String cpsPath,
                                       final Boolean includeDescendant) {

        final FetchDescendantsOption fetchDescendantsOption =
            Boolean.TRUE.equals(includeDescendant) ? FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
                : FetchDescendantsOption.OMIT_DESCENDANTS;

        return () -> networkCmProxyQueryService.queryResourceDataOperational(cmHandle, cpsPath,
            fetchDescendantsOption);
    }

}
