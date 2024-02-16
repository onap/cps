/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the 'License');
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an 'AS IS' BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.integration.base

import org.onap.cps.ncmp.api.NetworkCmProxyCmHandleQueryService
import org.onap.cps.ncmp.api.NetworkCmProxyDataService
import org.onap.cps.ncmp.api.NetworkCmProxyQueryService
import org.onap.cps.ncmp.api.impl.inventory.sync.ModuleSyncWatchdog
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.web.client.RestTemplate

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus

abstract class NcmpIntegrationSpecBase extends CpsIntegrationSpecBase {

    @Autowired
    NetworkCmProxyDataService networkCmProxyDataService

    @Autowired
    NetworkCmProxyQueryService networkCmProxyQueryService

    @Autowired
    NetworkCmProxyCmHandleQueryService networkCmProxyCmHandleQueryService

    @Autowired
    RestTemplate restTemplate

    @Autowired
    ModuleSyncWatchdog moduleSyncWatchdog

    def DMI_URL = 'http://mock-dmi-server'

    def mockDmiServer

    def setup() {
        mockDmiServer = MockRestServiceServer.createServer(restTemplate)
    }

    def cleanup() {
        mockDmiServer.verify()
    }

    def mockDmiResponse(url, httpStatus, responseBody) {
        mockDmiServer.expect(requestTo("${DMI_URL}${url}"))
                .andRespond(withStatus(httpStatus).contentType(MediaType.APPLICATION_JSON).body(responseBody))
    }

}
