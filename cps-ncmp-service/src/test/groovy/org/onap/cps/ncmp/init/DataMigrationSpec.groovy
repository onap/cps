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
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import spock.lang.Specification
import spock.lang.Subject

import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.ADVISED
import static org.onap.cps.ncmp.api.inventory.models.CmHandleState.READY

class DataMigrationSpec extends Specification{

    def mockCmHandleQueryService = Mock(CmHandleQueryService)
    def mockNetworkCmProxyInventoryFacade = Mock(NetworkCmProxyInventoryFacade)
    def mockInventoryPersistence = Mock(InventoryPersistence)
    def cmHandle1 = new NcmpServiceCmHandle(cmHandleId: 'ch-1', dmiServiceName: 'dmi1', compositeState: new CompositeState(cmHandleState: READY))
    def cmHandle2 = new NcmpServiceCmHandle(cmHandleId: 'ch-2', dmiServiceName: 'dmi1', compositeState: new CompositeState(cmHandleState: ADVISED))
    def cmHandle3 = new NcmpServiceCmHandle(cmHandleId: 'ch-3', dmiServiceName: 'dmi2', compositeState: new CompositeState(cmHandleState: READY))

    def setup() {
        mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('ch-1') >> cmHandle1
        mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('ch-2') >> cmHandle2
        mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('ch-3') >> cmHandle3
    }

    @Subject
    def objectUnderTest = new DataMigration(mockInventoryPersistence, mockCmHandleQueryService, mockNetworkCmProxyInventoryFacade)

    def 'CM Handle migration.'() {
        given: 'a list of CM handle IDs'
            def cmHandleIds = ['ch-1', 'ch-2', 'ch-3']
            mockCmHandleQueryService.getAllCmHandleReferences(false) >> cmHandleIds
        and: 'CM handles with different DMI services'
        when: 'migration is performed'
            objectUnderTest.migrateInventoryToModelRelease20250722()
        then: 'handles are processed in bulk'
            1 * mockInventoryPersistence.bulkUpdateCmHandleStates({ cmHandleStateUpdates ->
                def actualData = cmHandleStateUpdates.collect { [id: it.cmHandleId, state: it.state] }
                assert actualData.size() == 3
                assert actualData.containsAll([
                    [id: 'ch-1', state: 'READY'],
                    [id: 'ch-2', state: 'ADVISED'],
                    [id: 'ch-3', state: 'READY']
                ])
            })
    }

    def 'CM Handle migration with exception during one batch.'() {
        given: 'a list of CM handle IDs that spans multiple batches'
            def cmHandleIds = ['ch-1', 'faultyCmHandle', 'ch-2']
            mockCmHandleQueryService.getAllCmHandleReferences(false) >> cmHandleIds
        and: 'a batch size of 1 so we get 3 batches'
            objectUnderTest.batchSize = 1
        and: 'networkCmProxyInventoryFacade throws an exception for one handle'
            mockNetworkCmProxyInventoryFacade.getNcmpServiceCmHandle('faultyCmHandle') >> { throw new RuntimeException('Simulated failure') }
        when: 'migration is performed'
            objectUnderTest.migrateInventoryToModelRelease20250722()
        then: 'migration processes 2 out of the 3 batches'
            2 * mockInventoryPersistence.bulkUpdateCmHandleStates({ cmHandleStateUpdates ->
                assert cmHandleStateUpdates.every { it.state in ['READY', 'ADVISED'] }
            })
    }

}
