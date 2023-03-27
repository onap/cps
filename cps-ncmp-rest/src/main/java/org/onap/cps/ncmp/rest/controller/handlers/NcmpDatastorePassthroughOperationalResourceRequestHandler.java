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

import static org.onap.cps.ncmp.api.impl.operations.DmiOperations.DataStoreEnum.PASSTHROUGH_OPERATIONAL;

import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor;

@Slf4j
public class NcmpDatastorePassthroughOperationalResourceRequestHandler extends NcmpDatastoreRequestHandler {

    public NcmpDatastorePassthroughOperationalResourceRequestHandler(
            final NetworkCmProxyDataService networkCmProxyDataService,
            final CpsNcmpTaskExecutor cpsNcmpTaskExecutor,
            final int timeOutInMilliSeconds,
            final boolean notificationFeatureEnabled) {
        super(networkCmProxyDataService, cpsNcmpTaskExecutor, timeOutInMilliSeconds, notificationFeatureEnabled);
    }

    @Override
    public Supplier<Object> getTaskSupplier(final String cmHandleId,
                                            final String resourceIdentifier,
                                            final String optionsParamInQuery,
                                            final String topicParamInQuery,
                                            final String requestId,
                                            final Boolean includeDescendant) {

        return () -> networkCmProxyDataService.getResourceDataForCmHandle(
                PASSTHROUGH_OPERATIONAL.getValue(),
                cmHandleId, resourceIdentifier, optionsParamInQuery, topicParamInQuery, requestId);
    }

    @Override
    public Supplier<Object> getTaskSupplier(final List<String> cmHandleIds,
                                            final String resourceIdentifier,
                                            final String optionsParamInQuery,
                                            final String topicParamInQuery,
                                            final String requestId,
                                            final Boolean includeDescendant) {

        return () -> networkCmProxyDataService.getResourceDataForCmHandleBatch(
                PASSTHROUGH_OPERATIONAL.getValue(), cmHandleIds, resourceIdentifier,
                optionsParamInQuery, topicParamInQuery, requestId);
    }

}
