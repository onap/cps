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

import com.hazelcast.map.IMap
import org.onap.cps.ncmp.api.impl.client.DmiRestClient
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevel
import spock.lang.Specification

class DMiPluginWatchDogSpec extends Specification {


    def mockTrustLevelPerDmiPlugin = Mock(IMap<String, TrustLevel>)
    def mockDmiRestClient = Mock(DmiRestClient)
    def objectUnderTest = new DMiPluginWatchDog(mockTrustLevelPerDmiPlugin, mockDmiRestClient)


    def 'watch dmi plugin aliveness'() {
        given: 'the dmi client returns aliveness for #dmi1Status'
            mockDmiRestClient.getDmiPluginStatus('dmi1') >> dmi1Status
        and: 'trust level cache returns dmi1'
            mockTrustLevelPerDmiPlugin.keySet() >> {['dmi1'] as Set}
        when: 'watch dog started'
            objectUnderTest.watchDmiPluginAliveness()
        then: 'trust level cache has been populated with #dmi1TrustLevel for dmi1'
            1 * mockTrustLevelPerDmiPlugin.put('dmi1', dmi1TrustLevel)
        where: 'the following parameter are used'
            scenario                  | dmi1Status              || dmi1TrustLevel
            'dmi1 is UP'              | DmiPluginStatus.UP      || TrustLevel.COMPLETE
            'dmi1 is DOWN'            | DmiPluginStatus.DOWN    || TrustLevel.NONE
    }

}
