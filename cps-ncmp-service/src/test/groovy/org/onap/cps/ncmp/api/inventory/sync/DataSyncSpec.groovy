/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.inventory.sync

import org.onap.cps.api.CpsDataService
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeState
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.inventory.DataStoreSyncState
import spock.lang.Shared
import spock.lang.Specification

class DataSyncSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)

    def mockCpsDataService = Mock(CpsDataService)

    def mockSyncUtils = Mock(SyncUtils)

    @Shared
    def jsonString = '{"stores:bookstore":{"categories":[{"code":"01"}]}}'

    def objectUnderTest = new DataSyncWatchdog(mockInventoryPersistence, mockCpsDataService, mockSyncUtils)

    def compositeState = getCompositeState()

    def yangModelCmHandle1 = createSampleYangModelCmHandle('some-cm-handle-1')

    def yangModelCmHandle2 = createSampleYangModelCmHandle('some-cm-handle-2')

    def 'Schedule Data Sync for Cm Handle State in READY and Operational Sync State in UNSYNCHRONIZED'() {
        given: 'sample resource data'
            def resourceData = jsonString
        and: 'sync utilities return a cm handle twice'
            mockSyncUtils.getAnUnSynchronizedReadyCmHandle() >>> [yangModelCmHandle1, yangModelCmHandle2, null]
        when: 'data sync poll is executed'
            objectUnderTest.executeUnSynchronizedReadyCmHandlePoll()
        then: 'the inventory persistence cm handle returns a composite state for the first cm handle'
            1 * mockInventoryPersistence.getCmHandleState('some-cm-handle-1') >> compositeState
        and: 'the sync util returns first resource data'
            1 * mockSyncUtils.getResourceData('some-cm-handle-1') >> resourceData
        and: 'the cm-handle data is saved'
            1 * mockCpsDataService.saveData('NFP-Operational', 'some-cm-handle-1', jsonString, _)
        and: 'the first cm handle operational sync state is updated'
            1 * mockInventoryPersistence.saveCmHandleState('some-cm-handle-1', compositeState)
        then: 'the inventory persistence cm handle returns a composite state for the second cm handle'
            1 * mockInventoryPersistence.getCmHandleState('some-cm-handle-2') >> compositeState
        and: 'the sync util returns first resource data'
            1 * mockSyncUtils.getResourceData('some-cm-handle-2') >> resourceData
        and: 'the cm-handle data is saved'
            1 * mockCpsDataService.saveData('NFP-Operational', 'some-cm-handle-2', jsonString, _)
        and: 'the second cm handle operational sync state is updated from "UNSYNCHRONIZED" to "SYNCHRONIZED"'
            1 * mockInventoryPersistence.saveCmHandleState('some-cm-handle-2', compositeState)
    }

    def 'Schedule Data Sync for Cm Handle State in READY and Operational Sync State in UNSYNCHRONIZED which return empty data from Node because #scenario'() {
        given: 'a yang model cm handle'
            def yangModelCmHandle = new YangModelCmHandle(id: 'some-cm-handle', compositeState: new CompositeState(dataSyncEnabled: dataSyncEnabled))
        and: 'sync utilities returns a single cm handle'
            mockSyncUtils.getAnUnSynchronizedReadyCmHandle() >>> [yangModelCmHandle, null]
        when: 'data sync poll is executed'
            objectUnderTest.executeUnSynchronizedReadyCmHandlePoll()
        then: 'the inventory persistence cm handle returns a composite state for the first cm handle'
            1 * mockInventoryPersistence.getCmHandleState('some-cm-handle') >> compositeState
        and: 'the sync util returns first resource data'
            1 * mockSyncUtils.getResourceData('some-cm-handle') >> resourceData
        and: 'the cm-handle data is not saved'
            0 * mockCpsDataService.saveData('NFP-Operational', 'some-cm-handle-1', jsonString, _)
        where:
            scenario                                             | dataSyncEnabled | resourceData
            'data sync is not enabled'                           | false           | jsonString
            'resource data is null'                              | true            | null
            'data sync is not enabled and resource data is null' | false           | null
    }

    def createSampleYangModelCmHandle(cmHandleId) {
        def compositeState = getCompositeState()
        return new YangModelCmHandle(id: cmHandleId, compositeState: compositeState)
    }

    def getCompositeState() {
        def cmHandleState = CmHandleState.READY
        def compositeState = new CompositeState(cmHandleState: cmHandleState, dataSyncEnabled: true)
        compositeState.setDataStores(CompositeState.DataStores.builder()
            .operationalDataStore(CompositeState.Operational.builder().dataStoreSyncState(DataStoreSyncState.SYNCHRONIZED)
                .build()).build())
        return compositeState
    }
}
