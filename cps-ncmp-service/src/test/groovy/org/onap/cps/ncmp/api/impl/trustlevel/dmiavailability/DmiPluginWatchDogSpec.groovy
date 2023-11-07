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
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevel
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevelManager
import org.onap.cps.spi.model.DataNode
import org.springframework.boot.context.event.ApplicationReadyEvent
import spock.lang.Specification

import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DMI_REGISTRY_PARENT

class DmiPluginWatchDogSpec extends Specification {

    def mockDmiRestClient = Mock(DmiRestClient)
    def mockNetworkCmProxyDataService = Mock(NetworkCmProxyDataService)
    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockTrustLevelManager = Mock(TrustLevelManager)
    def trustLevelPerDmiPlugin = [:]


    def objectUnderTest = new DmiPluginWatchDog(mockDmiRestClient,
        mockNetworkCmProxyDataService,
        mockInventoryPersistence,
        mockTrustLevelManager,
        trustLevelPerDmiPlugin)

    def 'watch dmi plugin health status for #dmiHealhStatus'() {
        given: 'the cache has been initialised and "knows" about dmi-1'
            trustLevelPerDmiPlugin.put('dmi-1', dmiOldTrustLevel)
        and: 'dmi client returns health status #dmiHealhStatus'
            mockDmiRestClient.getDmiHealthStatus('dmi-1') >> dmiHealhStatus
        and: 'network cm proxy data returns a list of all cm handle ids belonging to a dmi'
            mockNetworkCmProxyDataService.getAllCmHandleIdsByDmiPluginIdentifier('dmi-1') >> []
        when: 'dmi watch dog method runs'
            objectUnderTest.checkDmiAvailability()
        then: 'the result is as expected'
            assert trustLevelPerDmiPlugin.get('dmi-1') == newDmiTrustLevel
        where: 'the following health status is used'
            dmiHealhStatus  | dmiOldTrustLevel    || newDmiTrustLevel
            'UP'            | TrustLevel.NONE     || TrustLevel.COMPLETE
            'DOWN'          | TrustLevel.COMPLETE || TrustLevel.NONE
            ''              | TrustLevel.COMPLETE || TrustLevel.NONE
    }

    def 'initialise the caches upon restart'() {
        given: 'dmi-registry with my cm handles'
            def dmiRegistry = new DataNode(xpath: NCMP_DMI_REGISTRY_PARENT, childDataNodes: createDataNodeListForMyDmi(['ch-1', 'ch-2']))
        and: 'inventory persistence service returns my dmi registry'
            mockInventoryPersistence.getDataNode(*_) >> [dmiRegistry]
        and: 'dmi client returns "UP" status for "my-dmi"'
            mockDmiRestClient.getDmiHealthStatus('my-dmi') >> dmiResponse
        when: 'the application is re-started'
            objectUnderTest.onApplicationEvent(Mock(ApplicationReadyEvent))
        then: 'the restart being handled by trust level manager'
            times * mockTrustLevelManager.handleRestartCpsNcmpApplication(*_)
        and: 'trust level for "my-dmi" is complete'
            assert trustLevelPerDmiPlugin.get('my-dmi') == finalTrustLevel
        where: 'the below parameters used'
            scenario | dmiResponse | times || finalTrustLevel
            'UP'     | 'UP'        | 2     || TrustLevel.COMPLETE
            'other'  | 'other'     | 2     || TrustLevel.NONE
    }

    def static createDataNodeListForMyDmi(dataNodeIds) {
        def dataNodes =[]
        dataNodeIds.each{ dataNodes << new DataNode(xpath: "/dmi-registry/cm-handles[@id='${it}']", leaves: ['id':it,'dmi-service-name':'my-dmi']) }
        return dataNodes
    }

}
