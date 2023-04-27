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
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.onap.cps.ncmp.api.NetworkCmProxyQueryService;
import org.onap.cps.spi.FetchDescendantsOption;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class NcmpCachedResourceRequestHandler extends NcmpDatastoreRequestHandler {

    @Setter
    private String dataStoreName;
    private final NetworkCmProxyQueryService networkCmProxyQueryService;

    @Override
    public Supplier<Object> getTaskSupplierForGetRequest(final String cmHandleId,
                                                         final String resourceIdentifier,
                                                         final String optionsParamInQuery,
                                                         final String topicParamInQuery,
                                                         final String requestId,
                                                         final boolean includeDescendants) {

        final FetchDescendantsOption fetchDescendantsOption =
                TaskManagementDefaultHandler.getFetchDescendantsOption(includeDescendants);

        return () -> networkCmProxyDataService.getResourceDataForCmHandle(dataStoreName, cmHandleId, resourceIdentifier,
                fetchDescendantsOption);
    }

    /**
     * Gets ncmp datastore query handler.
     * Note. Currently only ncmp-datastore:operational supports query operations
     * @return a ncmp datastore query handler.
     */
    @Override
    public Supplier<Object> getTaskSupplierForQueryRequest(final String cmHandleId,
                                                           final String resourceIdentifier,
                                                           final boolean includeDescendants) {

        final FetchDescendantsOption fetchDescendantsOption =
                TaskManagementDefaultHandler.getFetchDescendantsOption(includeDescendants);

        return () -> networkCmProxyQueryService.queryResourceDataOperational(cmHandleId, resourceIdentifier,
                fetchDescendantsOption);
    }

}
