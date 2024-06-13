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

import java.util.Collection;
import lombok.AllArgsConstructor;
import org.onap.cps.ncmp.api.NetworkCmProxyQueryService;
import org.onap.cps.ncmp.api.impl.NetworkCmProxyFacade;
import org.onap.cps.ncmp.api.models.CmResourceAddress;
import org.onap.cps.spi.FetchDescendantsOption;
import org.onap.cps.spi.model.DataNode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@AllArgsConstructor
public class NcmpCachedResourceRequestHandler extends NcmpDatastoreRequestHandler {

    /**
     * TODO The old 'networkCmProxyDataService' has over time been 'polluted' with non-data methods
     * It now effectively has become (and been renamed) to a facade. The controller should use (only) the
     * Facade. In turn the facade should choose handlers etc. and pass on the call
     */

    private final NetworkCmProxyFacade networkCmProxyFacade;
    private final NetworkCmProxyQueryService networkCmProxyQueryService;

    /**
     * Executes a synchronous query request for given cm handle.
     * Note. Currently only ncmp-datastore:operational supports query operations.
     *
     * @param cmHandleId         the cm handle
     * @param resourceIdentifier the resource identifier
     * @param includeDescendants whether include descendants
     * @return the response entity
     */
    public ResponseEntity<Object> executeRequest(final String cmHandleId, final String resourceIdentifier,
                                                 final boolean includeDescendants) {
        final Collection<DataNode> dataNodes = getTaskSupplierForQueryRequest(cmHandleId, resourceIdentifier,
                includeDescendants);
        return ResponseEntity.ok(dataNodes);
    }

    @Override
    protected Mono<Object> getResourceDataForCmHandle(final CmResourceAddress cmResourceAddress,
                                                      final String optionsParamInQuery,
                                                      final String topicParamInQuery,
                                                      final String requestId,
                                                      final boolean includeDescendants,
                                                      final String authorization) {
        final FetchDescendantsOption fetchDescendantsOption = getFetchDescendantsOption(includeDescendants);

        return Mono.fromSupplier(
                () -> networkCmProxyFacade.getResourceDataForCmHandle(cmResourceAddress, fetchDescendantsOption));
    }

    private Collection<DataNode> getTaskSupplierForQueryRequest(final String cmHandleId,
                                                                final String resourceIdentifier,
                                                                final boolean includeDescendants) {
        final FetchDescendantsOption fetchDescendantsOption = getFetchDescendantsOption(includeDescendants);
        return networkCmProxyQueryService.queryResourceDataOperational(cmHandleId, resourceIdentifier,
            fetchDescendantsOption);
    }

    private static FetchDescendantsOption getFetchDescendantsOption(final boolean includeDescendants) {
        return includeDescendants ? FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
            : FetchDescendantsOption.OMIT_DESCENDANTS;
    }
}
