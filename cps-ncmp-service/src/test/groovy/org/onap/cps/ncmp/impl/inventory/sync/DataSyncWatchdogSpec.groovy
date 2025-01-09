/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ncmp.impl.inventory.sync

import com.hazelcast.map.IMap
import org.onap.cps.api.CpsDataService
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.api.inventory.DataStoreSyncState
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.inventory.models.CmHandleState
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import spock.lang.Specification

import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME

class DataSyncWatchdogSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)

    def mockCpsDataService = Mock(CpsDataService)

    def mockSyncUtils = Mock(ModuleOperationsUtils)

    def mockDataSyncSemaphores = Mock(IMap<String,Boolean>)

    def jsonString = '{"stores:bookstore":{"categories":[{"code":"01"}]}}'

    def objectUnderTest = new DataSyncWatchdog(mockInventoryPersistence, mockCpsDataService, mockSyncUtils, mockDataSyncSemaphores)

    def compositeState = getCompositeState()

    def yangModelCmHandle1 = createSampleYangModelCmHandle('cm-handle-1')

    def yangModelCmHandle2 = createSampleYangModelCmHandle('cm-handle-2')

    def 'Data Sync for Cm Handle State in READY and Operational Sync State in UNSYNCHRONIZED.'() {
        given: 'sample resource data'
            def resourceData = jsonString
        and: 'sync utilities returns a cm handle twice'
            mockSyncUtils.getUnsynchronizedReadyCmHandles() >> [yangModelCmHandle1, yangModelCmHandle2]
        when: 'data sync poll is executed'
            objectUnderTest.executeUnSynchronizedReadyCmHandlePoll()
        then: 'the inventory persistence cm handle returns a composite state for the first cm handle'
            1 * mockInventoryPersistence.getCmHandleState('cm-handle-1') >> compositeState
        and: 'the sync util returns first resource data'
            1 * mockSyncUtils.getResourceData('cm-handle-1') >> resourceData
        and: 'the cm-handle data is saved'
            1 * mockCpsDataService.saveData(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, 'cm-handle-1', jsonString, _)
        and: 'the first cm handle operational sync state is updated'
            1 * mockInventoryPersistence.saveCmHandleState('cm-handle-1', compositeState)
        then: 'the inventory persistence cm handle returns a composite state for the second cm handle'
            1 * mockInventoryPersistence.getCmHandleState('cm-handle-2') >> compositeState
        and: 'the sync util returns first resource data'
            1 * mockSyncUtils.getResourceData('cm-handle-2') >> resourceData
        and: 'the cm-handle data is saved'
            1 * mockCpsDataService.saveData(NFP_OPERATIONAL_DATASTORE_DATASPACE_NAME, 'cm-handle-2', jsonString, _)
        and: 'the second cm handle operational sync state is updated from "UNSYNCHRONIZED" to "SYNCHRONIZED"'
            1 * mockInventoryPersistence.saveCmHandleState('cm-handle-2', compositeState)
    }

    def 'Data Sync for Cm Handle State in READY and Operational Sync State in UNSYNCHRONIZED without resource data.'() {
        given: 'sync utilities returns a cm handle'
            mockSyncUtils.getUnsynchronizedReadyCmHandles() >> [yangModelCmHandle1]
        when: 'data sync poll is executed'
            objectUnderTest.executeUnSynchronizedReadyCmHandlePoll()
        then: 'the inventory persistence cm handle returns a composite state for the first cm handle'
            1 * mockInventoryPersistence.getCmHandleState('cm-handle-1') >> compositeState
        and: 'the sync util returns no resource data'
            1 * mockSyncUtils.getResourceData('cm-handle-1') >> null
        and: 'the cm-handle data is not saved'
            0 * mockCpsDataService.saveData(*_)
    }

    def 'Data Sync for Cm Handle that is already being processed.'() {
        given: 'sync utilities returns a cm handle'
            mockSyncUtils.getUnsynchronizedReadyCmHandles() >> [yangModelCmHandle1]
        and: 'the shared data sync semaphore indicate it is already being processed'
            mockDataSyncSemaphores.putIfAbsent('cm-handle-1', _, _, _) >> 'something (not null)'
        when: 'data sync poll is executed'
            objectUnderTest.executeUnSynchronizedReadyCmHandlePoll()
        then: 'it is NOT processed e.g. state is not requested'
            0 * mockInventoryPersistence.getCmHandleState(*_)
    }

    def createSampleYangModelCmHandle(cmHandleId) {
        def compositeState = getCompositeState()
        return new YangModelCmHandle(id: cmHandleId, compositeState: compositeState)
    }

    def getCompositeState() {
        def cmHandleState = CmHandleState.READY
        def compositeState = new CompositeState(cmHandleState: cmHandleState)
        compositeState.setDataStores(CompositeState.DataStores.builder()
            .operationalDataStore(CompositeState.Operational.builder().dataStoreSyncState(DataStoreSyncState.SYNCHRONIZED)
                .build()).build())
        return compositeState
    }
}
