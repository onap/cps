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
import org.onap.cps.ncmp.api.NetworkCmProxyQueryService;
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor;
import org.onap.cps.spi.FetchDescendantsOption;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class NcmpCachedResourceRequestHandler extends NcmpDatastoreRequestHandler {

    private final NetworkCmProxyDataService networkCmProxyDataService;
    private final NetworkCmProxyQueryService networkCmProxyQueryService;

    /**
     * Constructor.
     *
     * @param cpsNcmpTaskExecutor        @see org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor
     * @param networkCmProxyDataService  @see org.onap.cps.ncmp.api.NetworkCmProxyDataService
     * @param networkCmProxyQueryService @see org.onap.cps.ncmp.api.NetworkCmProxyQueryService
     */
    public NcmpCachedResourceRequestHandler(final CpsNcmpTaskExecutor cpsNcmpTaskExecutor,
                                            final NetworkCmProxyDataService networkCmProxyDataService,
                                            final NetworkCmProxyQueryService networkCmProxyQueryService) {
        super(cpsNcmpTaskExecutor);
        this.networkCmProxyDataService = networkCmProxyDataService;
        this.networkCmProxyQueryService = networkCmProxyQueryService;
    }

    /**
     * Executes a synchronous query request for given cm handle.
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

    @Override
    protected Supplier<Object> getTaskSupplierForGetRequest(final String datastoreName,
                                                  final String cmHandleId,
                                                  final String resourceIdentifier,
                                                  final String optionsParamInQuery,
                                                  final String topicParamInQuery,
                                                  final String requestId,
                                                  final boolean includeDescendants,
                                                  final String authorization) {

        final FetchDescendantsOption fetchDescendantsOption = getFetchDescendantsOption(includeDescendants);

        return () -> networkCmProxyDataService.getResourceDataForCmHandle(datastoreName, cmHandleId, resourceIdentifier,
            fetchDescendantsOption);
    }

    private Supplier<Object> getTaskSupplierForQueryRequest(final String cmHandleId,
                                                            final String resourceIdentifier,
                                                            final boolean includeDescendants) {

        final FetchDescendantsOption fetchDescendantsOption = getFetchDescendantsOption(includeDescendants);

        return () -> networkCmProxyQueryService.queryResourceDataOperational(cmHandleId, resourceIdentifier,
            fetchDescendantsOption);
    }

    private static FetchDescendantsOption getFetchDescendantsOption(final boolean includeDescendants) {
        return includeDescendants ? FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
            : FetchDescendantsOption.OMIT_DESCENDANTS;
    }


}
