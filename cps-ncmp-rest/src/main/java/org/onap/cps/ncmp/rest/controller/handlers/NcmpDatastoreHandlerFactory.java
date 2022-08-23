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
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor;

@RequiredArgsConstructor
public class NcmpDatastoreHandlerFactory {

    private final NetworkCmProxyDataService networkCmProxyDataService;
    private final CpsNcmpTaskExecutor cpsNcmpTaskExecutor;
    private final int timeOutInMilliSeconds;
    private final boolean asyncEnabled;

    /**
     * Gets ncmp datastore handler.
     *
     * @param datastoreType the datastore type
     * @return the ncmp datastore handler
     */
    public NcmpDatastoreHandler getNcmpDatastoreHandler(final DatastoreType datastoreType) {

        switch (datastoreType) {
            case OPERATIONAL:
                return new NcmpDatastoreOperationalHandler(networkCmProxyDataService, cpsNcmpTaskExecutor,
                        timeOutInMilliSeconds, asyncEnabled);
            case PASSTHROUGH_RUNNING:
                return new NcmpDatastorePassthroughRunningHandler(networkCmProxyDataService, cpsNcmpTaskExecutor,
                        timeOutInMilliSeconds, asyncEnabled);
            case PASSTHROUGH_OPERATIONAL:
                return new NcmpDatastorePassthroughOperationalHandler(networkCmProxyDataService, cpsNcmpTaskExecutor,
                        timeOutInMilliSeconds, asyncEnabled);
            default:
                return null;
        }
    }
}
