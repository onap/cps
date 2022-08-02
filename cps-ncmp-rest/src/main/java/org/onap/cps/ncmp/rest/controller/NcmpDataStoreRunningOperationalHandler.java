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

package org.onap.cps.ncmp.rest.controller;

import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.rest.controller.utils.NcmpDataStoreHandlerUtils;
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor;
import org.springframework.http.ResponseEntity;

@Slf4j
public class NcmpDataStoreRunningOperationalHandler implements NcmpDataStoreHandler {
    private static final String NO_REQUEST_ID = null;
    private static final String NO_TOPIC = null;
    private NetworkCmProxyDataService networkCmProxyDataService;
    private CpsNcmpTaskExecutor cpsNcmpTaskExecutor;

    private int timeOutInMilliSeconds;

    private boolean asyncEnabled;

    private Boolean includeDescendants;

    /**
     * Instantiates a new Ncmp data store operational running handler.
     *
     * @param networkCmProxyDataService the network cm proxy data service
     * @param cpsNcmpTaskExecutor       the cps ncmp task executor
     * @param timeOutInMilliSeconds     the timeout in milliseconds
     * @param asyncEnabled              the async enabled
     */
    public NcmpDataStoreRunningOperationalHandler(final NetworkCmProxyDataService networkCmProxyDataService,
                                                  final CpsNcmpTaskExecutor cpsNcmpTaskExecutor,
                                                  final int timeOutInMilliSeconds,
                                                  final boolean asyncEnabled) {
        this.networkCmProxyDataService = networkCmProxyDataService;
        this.cpsNcmpTaskExecutor = cpsNcmpTaskExecutor;
        this.timeOutInMilliSeconds = timeOutInMilliSeconds;
        this.asyncEnabled = asyncEnabled;
    }

    /**
     * Get resource data from operational running datastore.
     *
     * @param cmHandle cm handle identifier
     * @param resourceIdentifier resource identifier
     * @param optionsParamInQuery options query parameter
     * @param topicParamInQuery topic query parameter
     * @param includeDescendants whether include descendants
     * @return {@code ResponseEntity} response from dmi plugin
     */
    @Override
    public ResponseEntity<Object> handle(final String cmHandle,
                                         final String resourceIdentifier,
                                         final String optionsParamInQuery,
                                         final String topicParamInQuery,
                                         final Boolean includeDescendants) {

        if (asyncEnabled && NcmpDataStoreHandlerUtils.isValidTopic(topicParamInQuery)) {
            final String requestId = UUID.randomUUID().toString();
            log.info("Received Async passthrough-running request with id {}", requestId);
            cpsNcmpTaskExecutor.executeTask(() ->
                    networkCmProxyDataService.getResourceDataPassThroughRunningForCmHandle(
                            cmHandle, resourceIdentifier, optionsParamInQuery, topicParamInQuery, requestId
                    ), timeOutInMilliSeconds
            );
            return ResponseEntity.ok(Map.of("requestId", requestId));
        } else {
            log.warn("Asynchronous messaging is currently disabled for passthrough-running."
                    + " Will use synchronous operation.");
        }

        final Object responseObject = networkCmProxyDataService.getResourceDataPassThroughRunningForCmHandle(
                cmHandle, resourceIdentifier, optionsParamInQuery, NO_TOPIC, NO_REQUEST_ID);

        return ResponseEntity.ok(responseObject);
    }
}
