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

import java.util.List;
import java.util.function.Supplier;
import lombok.Setter;
import org.springframework.stereotype.Component;

@Component
public class NcmpPassthroughResourceRequestHandler extends NcmpDatastoreRequestHandler {

    @Setter
    private String dataStoreName;

    @Override
    public Supplier<Object> getTaskSupplierForGetRequest(final String cmHandleId,
                                                         final String resourceIdentifier,
                                                         final String optionsParamInQuery,
                                                         final String topicParamInQuery,
                                                         final String requestId,
                                                         final boolean includeDescendants) {

        return () -> networkCmProxyDataService.getResourceDataForCmHandle(
                dataStoreName, cmHandleId, resourceIdentifier, optionsParamInQuery, topicParamInQuery, requestId);
    }

    @Override
    public Supplier<Object> getTaskSupplierForBulkRequest(final List<String> cmHandleIds,
                                                         final String resourceIdentifier,
                                                         final String optionsParamInQuery,
                                                         final String topicParamInQuery,
                                                         final String requestId,
                                                         final boolean includeDescendants) {

        return () -> networkCmProxyDataService.getResourceDataForCmHandleBatch(
                dataStoreName, cmHandleIds, resourceIdentifier, optionsParamInQuery, topicParamInQuery, requestId);
    }

}
