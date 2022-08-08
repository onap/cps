
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

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor;

public class NcmpDatastorePassthroughOperationalHandler extends NcmpDatastoreHandler {

    private static final String NO_REQUEST_ID = null;
    private static final String NO_TOPIC = null;

    public NcmpDatastorePassthroughOperationalHandler(final NetworkCmProxyDataService networkCmProxyDataService,
                                                      final CpsNcmpTaskExecutor cpsNcmpTaskExecutor,
                                                      final int timeOutInMilliSeconds,
                                                      final boolean asyncEnabled) {
        super(networkCmProxyDataService, cpsNcmpTaskExecutor, timeOutInMilliSeconds, asyncEnabled);
    }

    @Override
    public List<Supplier<Object>> generateSuppliers(final String cmHandle,
                                                    final String resourceIdentifier,
                                                    final String optionsParamInQuery,
                                                    final String topicParamInQuery,
                                                    final String requestId) {

        final Supplier<Object> first = () ->
                networkCmProxyDataService.getResourceDataOperationalForCmHandle(
                        cmHandle, resourceIdentifier, optionsParamInQuery, topicParamInQuery, requestId);

        final Supplier<Object> second = () ->
                networkCmProxyDataService.getResourceDataOperationalForCmHandle(
                        cmHandle, resourceIdentifier, optionsParamInQuery, NO_TOPIC, NO_REQUEST_ID);

        return Arrays.asList(first, second);
    }

}
