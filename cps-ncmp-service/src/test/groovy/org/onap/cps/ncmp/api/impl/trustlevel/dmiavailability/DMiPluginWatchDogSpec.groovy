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
import spock.lang.Specification

class DMiPluginWatchDogSpec extends Specification {

    def mockDmiRestClient = Mock(DmiRestClient)
    def mockNetworkCmProxyDataService = Mock(NetworkCmProxyDataService)
    def trustLevelPerDmiPlugin = [:]
    def trustLevelPerCmHandle = [:]

    def objectUnderTest = new DMiPluginWatchDog(mockDmiRestClient, mockNetworkCmProxyDataService, trustLevelPerDmiPlugin, trustLevelPerCmHandle)

    def 'watch dmi plugin aliveness for #scenario'() {
        given: 'dmi-1 having trust level complete'
            trustLevelPerDmiPlugin.put('dmi-1', TrustLevel.COMPLETE)
        and: 'ch-1 having trust level complete'
            trustLevelPerCmHandle.put('ch-1', TrustLevel.COMPLETE)
        and: 'dmi client returns health status none'
            mockDmiRestClient.getDmiPluginTrustLevel('dmi-1') >> { newDmiTrustLevel }
        and: 'data service returns cm handles'
            mockNetworkCmProxyDataService.getAllCmHandleIdsByDmiPluginIdentifier('dmi-1') >> ['ch-1']
        when: 'watch dog started and updated the caches'
            objectUnderTest.watchDmiPluginTrustLevel()
        then: 'the result is as expected'
            trustLevelPerDmiPlugin.get('dmi-1') == expectedResult
            trustLevelPerCmHandle.get('ch-1') == expectedResult
        where: 'the given values is used'
            scenario                        | newDmiTrustLevel    || expectedResult
            'dmi-1 trust level changed'     | TrustLevel.NONE     || TrustLevel.NONE
            'dmi-1 trust level stayed same' | TrustLevel.COMPLETE || TrustLevel.COMPLETE
    }

}
