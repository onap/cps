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
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevel
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.spi.model.DataNode
import org.springframework.boot.context.event.ApplicationReadyEvent
import spock.lang.Specification

import static org.onap.cps.ncmp.api.impl.ncmppersistence.NcmpPersistence.NCMP_DMI_REGISTRY_PARENT

class DMiPluginWatchDogSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)
    def mockDmiRestClient = Mock(DmiRestClient)
    def trustLevelPerDmiPlugin = [:]

    def objectUnderTest = new DmiPluginWatchDog(mockInventoryPersistence, mockDmiRestClient, trustLevelPerDmiPlugin)

    def dmiRegistry = new DataNode(xpath: NCMP_DMI_REGISTRY_PARENT, childDataNodes: createDataNodeListForMyDmi(['ch-1', 'ch-2']))

    def 'watch dmi plugin health status for #dmiHealhStatus'() {
        given: 'the cache has been initialised and "knows" about dmi-1'
            trustLevelPerDmiPlugin.put('dmi-1',null)
        and: 'dmi client returns health status #dmiHealhStatus'
            mockDmiRestClient.getDmiHealthStatus('dmi-1') >> dmiHealhStatus
        when: 'dmi watch dog method runs'
            objectUnderTest.watchDmiPluginTrustLevel()
        then: 'the result is as expected'
            assert trustLevelPerDmiPlugin.get('dmi-1') == expectedResult
        where: 'the following health status is used'
            dmiHealhStatus || expectedResult
            'UP'           || TrustLevel.COMPLETE
            'Other'        || TrustLevel.NONE
            null           || TrustLevel.NONE
    }

    def 'initialise the dmi keys upon restart'() {
        given: 'inventory persistence service returns dmi registry'
            mockInventoryPersistence.getDataNode(*_) >> [dmiRegistry]
        and: 'inventory persistence service returns yang model cm handles for "my-dmi"'
            mockInventoryPersistence.getYangModelCmHandles(*_) >> [
                createYangModelCmHandleForMyDmi('ch-2'),
                createYangModelCmHandleForMyDmi('ch-1' )
            ]
        and: 'dmi client returns "UP" health status for "my-dmi"'
            mockDmiRestClient.getDmiHealthStatus('my-dmi') >> 'UP'
        when: 'the application is ready to handle restart'
            objectUnderTest.onApplicationEvent(Mock(ApplicationReadyEvent))
        then: 'trust level for "my-dmi" is complete'
            assert trustLevelPerDmiPlugin.get('my-dmi') == TrustLevel.COMPLETE
    }

    def static createDataNodeListForMyDmi(dataNodeIds) {
        def dataNodes =[]
        dataNodeIds.each{ dataNodes << new DataNode(xpath: "/dmi-registry/cm-handles[@id='${it}']", leaves: ['id':it,'dmi-service-name':'my-dmi']) }
        return dataNodes
    }

    def static createYangModelCmHandleForMyDmi(cmHandleId) {
        return new YangModelCmHandle(id: cmHandleId, dmiServiceName: 'my-dmi')
    }
}
