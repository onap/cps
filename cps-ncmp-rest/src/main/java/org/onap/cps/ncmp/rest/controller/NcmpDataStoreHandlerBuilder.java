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

public class NcmpDataStoreHandlerBuilder {

    private String datastoreName;
    private NetworkCmProxyDataService networkCmProxyDataService;
    private CpsNcmpTaskExecutor cpsNcmpTaskExecutor;
    private int timeOutInMilliSeconds;
    private boolean asyncEnabled;


    /**
     * Instantiates a new Ncmp data store handler builder.
     *
     * @param datastoreName the name of the datastore to use
     */
    public NcmpDataStoreHandlerBuilder(final String datastoreName) {
        this.datastoreName = datastoreName;
    }

    /**
     * Network cm proxy data service for ncmp data store handler builder.
     *
     * @param networkCmProxyDataService the network cm proxy data service
     * @return the ncmp data store handler builder
     */
    public NcmpDataStoreHandlerBuilder networkCmProxyDataService(
            final NetworkCmProxyDataService networkCmProxyDataService) {
        this.networkCmProxyDataService = networkCmProxyDataService;
        return this;
    }

    /**
     * Cps ncmp task executor for ncmp data store handler builder.
     *
     * @param cpsNcmpTaskExecutor the cps ncmp task executor
     * @return the ncmp data store handler builder
     */
    public NcmpDataStoreHandlerBuilder cpsNcmpTaskExecutor(final CpsNcmpTaskExecutor cpsNcmpTaskExecutor) {
        this.cpsNcmpTaskExecutor = cpsNcmpTaskExecutor;
        return this;
    }

    /**
     * Time out in milliseconds for ncmp data store handler builder.
     *
     * @param timeOutInMilliSeconds the timeout in milliseconds
     * @return the ncmp data store handler builder
     */
    public NcmpDataStoreHandlerBuilder timeOutInMilliSeconds(final int timeOutInMilliSeconds) {
        this.timeOutInMilliSeconds = timeOutInMilliSeconds;
        return this;
    }

    /**
     * Async enabled for ncmp data store handler builder.
     *
     * @param asyncEnabled is async enabled
     * @return the ncmp data store handler builder
     */
    public NcmpDataStoreHandlerBuilder asyncEnabled(final boolean asyncEnabled) {
        this.asyncEnabled = asyncEnabled;
        return this;
    }

    /**
     * Get the datastore enum from the string param.
     *
     * @param strDataStoreName the datastore param in the url
     * @return the Datastore name enum
     */

    private DatastoreName getDataStoreNameEnumFromParam(final String strDataStoreName) {

        final String dataStoreNameWithoutPrefix = strDataStoreName.split(":", 2)[1];

        return DatastoreName.valueOf(dataStoreNameWithoutPrefix.replace("-", "_")
                .toUpperCase(Locale.getDefault()));
    }

    /**
     * Build ncmp data store handler.
     *
     * @return the ncmp data store handler
     */
    public NcmpDataStoreHandler build() {

        final DatastoreName enumDatastoreName = getDataStoreNameEnumFromParam(this.datastoreName);

        switch (enumDatastoreName) {
            case OPERATIONAL:
                return new NcmpDataStoreOperationalHandler(this.networkCmProxyDataService,
                        this.cpsNcmpTaskExecutor, this.timeOutInMilliSeconds, this.asyncEnabled);
            case PASSTHROUGH_RUNNING:
                return new NcmpDataStorePassthroughRunningHandler(this.networkCmProxyDataService,
                        this.cpsNcmpTaskExecutor, this.timeOutInMilliSeconds, this.asyncEnabled);
            case PASSTHROUGH_OPERATIONAL:
                return new NcmpDataStorePassthroughOperationalHandler(this.networkCmProxyDataService,
                        this.cpsNcmpTaskExecutor, this.timeOutInMilliSeconds, this.asyncEnabled);
            default:
                throw new RuntimeException("NOT VALID DATA_STORE_NAME");
        }
    }

}
