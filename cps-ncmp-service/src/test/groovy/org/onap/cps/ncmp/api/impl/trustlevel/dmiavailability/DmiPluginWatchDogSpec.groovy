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

    def mockDmiRestClient = Mock(DmiRestClient)
    def mockNetworkCmProxyDataService = Mock(NetworkCmProxyDataService)
    def mockTrustLevelManager = Mock(TrustLevelManager)
    def trustLevelPerDmiPlugin = [:]


    def objectUnderTest = new DmiPluginWatchDog(mockDmiRestClient,
        mockNetworkCmProxyDataService,
        mockTrustLevelManager,
        trustLevelPerDmiPlugin)

    def 'watch dmi plugin health status for #dmiHealhStatus'() {
        given: 'the cache has been initialised and "knows" about dmi-1'
            trustLevelPerDmiPlugin.put('dmi-1', dmiOldTrustLevel)
        and: 'dmi client returns health status #dmiHealhStatus'
            mockDmiRestClient.getDmiHealthStatus('dmi-1') >> dmiHealhStatus
        when: 'dmi watch dog method runs'
            objectUnderTest.checkDmiAvailability()
        then: 'the update delegated to manager'
            times * mockTrustLevelManager.handleUpdateOfDmiTrustLevel(*_)
        where: 'the following parameters are used'
            dmiHealhStatus | dmiOldTrustLevel    || times
            'UP'           | TrustLevel.COMPLETE || 0
            'DOWN'         | TrustLevel.COMPLETE || 1
            'DOWN'         | TrustLevel.NONE     || 0
            'UP'           | TrustLevel.NONE     || 1
            ''             | TrustLevel.COMPLETE || 1
    }

}
