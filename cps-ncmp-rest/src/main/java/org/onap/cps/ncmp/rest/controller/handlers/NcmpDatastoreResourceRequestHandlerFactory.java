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

import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.api.NetworkCmProxyQueryService;
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NcmpDatastoreResourceRequestHandlerFactory {

    private final NetworkCmProxyDataService networkCmProxyDataService;
    private final NetworkCmProxyQueryService networkCmProxyQueryService;
    private final CpsNcmpTaskExecutor cpsNcmpTaskExecutor;

    @Value("${notification.async.executor.time-out-value-in-ms:2000}")
    private int timeOutInMilliSeconds;
    @Value("${notification.enabled:true}")
    private boolean notificationFeatureEnabled;

    /**
     * Gets ncmp datastore handler.
     *
     * @param datastoreType the datastore type
     * @return the ncmp datastore handler
     */
    public NcmpDatastoreRequestHandler getNcmpDatastoreResourceRequestHandler(
            final DatastoreType datastoreType) {

        switch (datastoreType) {
            case OPERATIONAL:
                return new NcmpDatastoreOperationalResourceRequestHandler(networkCmProxyDataService,
                        cpsNcmpTaskExecutor, timeOutInMilliSeconds, notificationFeatureEnabled);
            case PASSTHROUGH_RUNNING:
                return new NcmpDatastorePassthroughRunningResourceRequestHandler(networkCmProxyDataService,
                        cpsNcmpTaskExecutor, timeOutInMilliSeconds, notificationFeatureEnabled);
            case PASSTHROUGH_OPERATIONAL:
            default:
                return new NcmpDatastorePassthroughOperationalResourceRequestHandler(networkCmProxyDataService,
                        cpsNcmpTaskExecutor, timeOutInMilliSeconds, notificationFeatureEnabled);
        }
    }

    /**
     * Gets ncmp datastore query handler.
     * Note. Currently only ncmp-datastore:operational supports query operations
     * @return a ncmp datastore query handler.
     */
    public NcmpDatastoreRequestHandler getNcmpDatastoreResourceQueryHandler() {
        return new NcmpDatastoreOperationalQueryHandler(networkCmProxyQueryService, cpsNcmpTaskExecutor,
            timeOutInMilliSeconds, notificationFeatureEnabled);
    }

}
