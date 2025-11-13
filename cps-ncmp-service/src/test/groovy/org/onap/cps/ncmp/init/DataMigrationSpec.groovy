/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.init

import org.onap.cps.ncmp.api.inventory.NetworkCmProxyInventoryFacade
import org.onap.cps.ncmp.api.inventory.models.CmHandleState
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import spock.lang.Specification
import spock.lang.Subject

class DataMigrationSpec extends Specification{

    def mockCmHandleQueryService = Mock(CmHandleQueryService)
    def mockNetworkCmProxyInventoryFacade = Mock(NetworkCmProxyInventoryFacade)
    def mockInventoryPersistence = Mock(InventoryPersistence)

    @Subject
    def objectUnderTest = new DataMigration(mockInventoryPersistence, mockCmHandleQueryService, mockNetworkCmProxyInventoryFacade)

    def 'CM Handle migration.'() {
        given: 'a list of CM handle IDs'
            def cmHandleIds = ['cmhandle1', 'cmhandle2', 'cmhandle3']
            mockCmHandleQueryService.getAllCmHandleReferences(false) >> cmHandleIds
        and: 'CM handles with different DMI services'
            def cmHandle1= createCmHandle('cmhandle1', 'dmi1', CmHandleState.READY)
            def cmHandle2 = createCmHandle('cmhandle2', 'dmi1', CmHandleState.ADVISED)
            def cmHandle3 = createCmHandle('cmhandle3', 'dmi2', CmHandleState.READY)
            mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('cmhandle1') >> cmHandle1
            mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('cmhandle2') >> cmHandle2
            mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('cmhandle3') >> cmHandle3
        when: 'migration is performed'
            objectUnderTest.migrateInventoryToR20250722()
        then: 'handles are processed in bulk'
            1 * mockInventoryPersistence.bulkUpdateCmHandleStates(_) >> { args ->
                def updates = args[0]
                assert updates.size() == 3
                assert updates.any { it.cmHandleId == 'cmhandle1' && it.state == 'READY' }
                assert updates.any { it.cmHandleId == 'cmhandle2' && it.state == 'ADVISED' }
                assert updates.any { it.cmHandleId == 'cmhandle3' && it.state == 'READY' }
        }
    }

    def 'CM Handle migration with exception for one cm handle.'() {
        given: 'a list of CM handle IDs that spans multiple batches'
            objectUnderTest.batchSize = 1
            def cmHandleIds = ['handle1', 'handle2', 'handle3']
            mockCmHandleQueryService.getAllCmHandleReferences(false) >> cmHandleIds
        and: 'mock CM handles'
            def handle1 = createCmHandle('handle1', 'dmi1', CmHandleState.READY)
            def handle3 = createCmHandle('handle3', 'dmi1', CmHandleState.ADVISED)
        and: 'networkCmProxyInventoryFacade throws for one handle'
            mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('handle1') >> handle1
            mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('handle2') >> { throw new RuntimeException("Simulated failure") }
            mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('handle3') >> handle3
        when: 'migration is performed'
            objectUnderTest.migrateInventoryToR20250722()
        then: 'migration continues processing batches'
            2 * mockInventoryPersistence.bulkUpdateCmHandleStates(_) >> { args ->
                def updates = args[0]
                assert updates.every { it.state in ['READY', 'ADVISED'] }
            }
    }

    NcmpServiceCmHandle createCmHandle(String id, String dmiService, CmHandleState state) {
        new NcmpServiceCmHandle(
                cmHandleId: id,
                dmiServiceName: dmiService,
                compositeState: new CompositeState(
                        cmHandleState: state
                )
        )
    }


}
