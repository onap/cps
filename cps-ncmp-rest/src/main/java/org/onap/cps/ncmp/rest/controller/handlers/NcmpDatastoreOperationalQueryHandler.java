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

import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NetworkCmProxyQueryService;
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor;
import org.onap.cps.spi.FetchDescendantsOption;

@Slf4j
public class NcmpDatastoreOperationalQueryHandler extends NcmpDatastoreRequestHandler {

    private final NetworkCmProxyQueryService networkCmProxyQueryService;

    public NcmpDatastoreOperationalQueryHandler(final NetworkCmProxyQueryService networkCmProxyQueryService,
                                                final CpsNcmpTaskExecutor cpsNcmpTaskExecutor,
                                                final int timeOutInMilliSeconds,
                                                final boolean notificationFeatureEnabled) {
        super(null, cpsNcmpTaskExecutor, timeOutInMilliSeconds, notificationFeatureEnabled);
        this.networkCmProxyQueryService = networkCmProxyQueryService;
    }

    @Override
    public Supplier<Object> getTaskSupplier(final String cmHandle,
                                            final String resourceIdentifier,
                                            final String optionsParamInQuery,
                                            final String topicParamInQuery,
                                            final String requestId,
                                            final Boolean includeDescendant) {

        final FetchDescendantsOption fetchDescendantsOption = getFetchDescendantsOption(includeDescendant);

        return () -> networkCmProxyQueryService.queryResourceDataOperational(cmHandle, resourceIdentifier,
            fetchDescendantsOption);
    }

}
