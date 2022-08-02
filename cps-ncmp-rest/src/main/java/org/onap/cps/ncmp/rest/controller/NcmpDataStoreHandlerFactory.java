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

import java.util.Locale;
import org.onap.cps.ncmp.api.NetworkCmProxyDataService;
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor;


public class NcmpDataStoreHandlerFactory {

    private NetworkCmProxyDataService networkCmProxyDataService;
    private CpsNcmpTaskExecutor cpsNcmpTaskExecutor;

    private int timeOutInMilliSeconds;

    private boolean asyncEnabled;

    /**
     * Instantiates a new Ncmp data store handler factory.
     *
     * @param networkCmProxyDataService the network cm proxy data service
     * @param cpsNcmpTaskExecutor       the cps ncmp task executor
     * @param timeOutInMilliSeconds     the timeout in milliseconds
     * @param asyncEnabled              the async enabled
     */
    public NcmpDataStoreHandlerFactory(final NetworkCmProxyDataService networkCmProxyDataService,
                                       final CpsNcmpTaskExecutor cpsNcmpTaskExecutor,
                                       final int timeOutInMilliSeconds,
                                       final boolean asyncEnabled) {
        this.networkCmProxyDataService = networkCmProxyDataService;
        this.cpsNcmpTaskExecutor = cpsNcmpTaskExecutor;
        this.timeOutInMilliSeconds = timeOutInMilliSeconds;
        this.asyncEnabled = asyncEnabled;
    }

    /**
     * Gets handler.
     *
     * @param strDataStoreType the str data store type
     * @return the handler
     */
    public NcmpDataStoreHandler getHandler(final String strDataStoreType) {

        final DataStoreName dataStoreName = DataStoreName.valueOf(
                strDataStoreType.replace("-", "_").toUpperCase(Locale.getDefault()));

        NcmpDataStoreHandler dataStoreHandler = null;

        switch (dataStoreName) {
            case OPERATIONAL:
                dataStoreHandler = new NcmpDataStoreOperationalHandler(this.networkCmProxyDataService,
                        this.cpsNcmpTaskExecutor, this.timeOutInMilliSeconds, this.asyncEnabled);
                break;
            case RUNNING_OPERATIONAL:
                dataStoreHandler = new NcmpDataStoreRunningOperationalHandler(this.networkCmProxyDataService,
                        this.cpsNcmpTaskExecutor, this.timeOutInMilliSeconds, this.asyncEnabled);
                break;
            case PASSTHROUGH_OPERATIONAL:
                dataStoreHandler = new NcmpDataStorePassthroughOperationalHandler(this.networkCmProxyDataService,
                        this.cpsNcmpTaskExecutor, this.timeOutInMilliSeconds, this.asyncEnabled);
                break;
            default:
                throw new RuntimeException("NOT VALID DATA_STORE_NAME");
        }

        return dataStoreHandler;
    }
}
