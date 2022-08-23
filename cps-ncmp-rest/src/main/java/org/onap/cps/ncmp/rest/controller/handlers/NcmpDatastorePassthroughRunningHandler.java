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
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor;

@Slf4j
public class NcmpDatastorePassthroughRunningHandler extends NcmpDatastoreHandler {

    public NcmpDatastorePassthroughRunningHandler(final NetworkCmProxyDataService networkCmProxyDataService,
                                                  final CpsNcmpTaskExecutor cpsNcmpTaskExecutor,
                                                  final int timeOutInMilliSeconds,
                                                  final boolean asyncEnabled) {
        super(networkCmProxyDataService, cpsNcmpTaskExecutor, timeOutInMilliSeconds, asyncEnabled);
    }

    @Override
    public Supplier<Object> getTask(final String cmHandle,
                                    final String resourceIdentifier,
                                    final String optionsParamInQuery,
                                    final String topicParamInQuery,
                                    final String requestId,
                                    final Boolean includeDescendant) {

        if (isUserWantsAsyncResponse(topicParamInQuery)) {
            log.info("Received Async passthrough-running request with id {}", requestId);
        } else {
            log.warn("Asynchronous messaging is currently disabled for passthrough-running."
                    + " Will use synchronous operation.");
        }

        return () -> networkCmProxyDataService.getResourceDataPassThroughRunningForCmHandle(
                cmHandle, resourceIdentifier, optionsParamInQuery, topicParamInQuery, requestId);
    }
}
