/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.trustlevel.dmiavailability

import org.onap.cps.ncmp.api.NetworkCmProxyDataService
import org.onap.cps.ncmp.api.impl.client.DmiRestClient
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevel
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevelManager
import spock.lang.Specification

class DmiPluginWatchDogSpec extends Specification {

    def trustLevelPerDmiPlugin = [:]
    def mockDmiRestClient = Mock(DmiRestClient)
    def mockNetworkCmProxyDataService = Mock(NetworkCmProxyDataService)
    def mockTrustLevelManager = Mock(TrustLevelManager)

    def objectUnderTest = new DmiPluginWatchDog(trustLevelPerDmiPlugin,
        mockDmiRestClient,
        mockNetworkCmProxyDataService,
        mockTrustLevelManager)

    def static someListOfCmHandleIds = ["ch-1", "ch-2"]

    def 'watch dmi plugin health status for #dmiHealhStatus'() {
        given: 'the cache has been initialised and "knows" about dmi-1'
            trustLevelPerDmiPlugin.put('dmi-1', dmiOldTrustLevel)
        and: 'dmi client returns health status #dmiHealhStatus'
            mockDmiRestClient.getDmiHealthStatus('dmi-1') >> dmiHealhStatus
        and: 'network cm proxy data returns a list of all cm handle ids belonging to a dmi'
            mockNetworkCmProxyDataService.getAllCmHandleIdsByDmiPluginIdentifier('dmi-1') >> someListOfCmHandleIds
        when: 'dmi watch dog method runs'
            objectUnderTest.watchDmiPluginAliveness()
        then: 'the result is as expected'
            assert trustLevelPerDmiPlugin.get('dmi-1') == newDmiTrustLevel
        where: 'the following health status is used'
            dmiHealhStatus  | dmiOldTrustLevel    || newDmiTrustLevel
            'UP'            | TrustLevel.NONE     || TrustLevel.COMPLETE
            'DOWN'          | TrustLevel.COMPLETE || TrustLevel.NONE
            ''              | TrustLevel.COMPLETE || TrustLevel.NONE
    }

}
