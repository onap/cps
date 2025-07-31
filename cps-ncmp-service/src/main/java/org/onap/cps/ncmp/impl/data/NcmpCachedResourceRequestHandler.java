/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2025 OpenInfra Foundation Europe. All rights reserved.
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
import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME;

import java.util.Collection;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.CpsFacade;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.ncmp.api.data.models.CmResourceAddress;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class NcmpCachedResourceRequestHandler extends NcmpDatastoreRequestHandler {

    private final CpsFacade cpsFacade;
    private final NetworkCmProxyQueryService networkCmProxyQueryService;

    /**
     * Executes a synchronous query request for given cm handle.
     * Note. Currently only ncmp-datastore:operational supports query operations.
     *
     * @param cmHandleId         the cm handle
     * @param resourceIdentifier the resource identifier
     * @param includeDescendants whether include descendants
     * @return a collection of data nodes
     */
    public Collection<DataNode> executeRequest(final String cmHandleId, final String resourceIdentifier,
                                                 final boolean includeDescendants) {
        final FetchDescendantsOption fetchDescendantsOption
            = FetchDescendantsOption.getFetchDescendantsOption(includeDescendants);
        return networkCmProxyQueryService.queryResourceDataOperational(cmHandleId, resourceIdentifier,
            fetchDescendantsOption);
    }

    @Override
    protected Mono<Object> getResourceDataForCmHandle(final CmResourceAddress cmResourceAddress,
                                                      final String optionsParamInQuery,
                                                      final String topicParamInQuery,
                                                      final String requestId,
                                                      final boolean includeDescendants,
                                                      final String authorization) {
        final FetchDescendantsOption fetchDescendantsOption =
                FetchDescendantsOption.getFetchDescendantsOption(includeDescendants);

        final Map<String, Object> dataNodes = cpsFacade.getDataNodesByAnchorV3(resolveDatastoreName(cmResourceAddress),
                cmResourceAddress.resolveCmHandleReferenceToId(), cmResourceAddress.getResourceIdentifier(),
                fetchDescendantsOption);
        return Mono.justOrEmpty(dataNodes);
    }

    private String resolveDatastoreName(final CmResourceAddress cmResourceAddress) {
        final String datastoreName = cmResourceAddress.getDatastoreName();
        if (datastoreName.equals(OPERATIONAL.getDatastoreName())) {
            return NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME;
        }
        throw new IllegalArgumentException(
                "Unsupported datastore name provided to fetch the cached data: " + datastoreName);
    }

}
