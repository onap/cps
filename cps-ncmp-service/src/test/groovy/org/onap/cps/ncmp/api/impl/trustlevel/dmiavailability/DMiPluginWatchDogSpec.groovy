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

import org.onap.cps.ncmp.api.impl.client.DmiRestClient
import spock.lang.Specification

class DMiPluginWatchDogSpec extends Specification {

    def healthStatusPerDmiPlugin = ['my dmi plugin': '', 'my other dmi plugin': '']
    def mockDmiRestClient = Mock(DmiRestClient)
    def objectUnderTest = new DMiPluginWatchDog(healthStatusPerDmiPlugin, mockDmiRestClient)

    def 'watch dmi plugin aliveness'() {
        given: 'the dmi client returns aliveness for #dmiPlugin'
            mockDmiRestClient.getDmiPluginStatus(dmiPlugin) >> dmiPluginStatus
        when: 'dmi watch dog started'
            objectUnderTest.watchDmiPluginAliveness()
        then: 'dmi healthiness cache has been populated with #dmiPluginStatus'
            healthStatusPerDmiPlugin.put(dmiPlugin, dmiPluginStatus)
        where: 'the following parameter are used'
            scenario      | dmiPlugin             || dmiPluginStatus
            'dmi is UP'   | 'my dmi plugin'       || DmiPluginStatus.UP
            'dmi is DOWN' | 'my other dmi plugin' || DmiPluginStatus.DOWN
    }

}
