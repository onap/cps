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
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class DataSyncSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)

    def mockCpsDataService =  Mock(CpsDataService)

    def mockSyncUtils = Mock(SyncUtils)

    def cmHandleState = CmHandleState.READY

    def jsonString = '{"stores:bookstore":{"categories":[{"code":"05","books":[{"title":"A Romance Book","price":"2000","pub_year":2002,"lang":"English","authors":["Lathish"]}],"name":"Romance"}]}}'

    def responseEntity = new ResponseEntity<>(jsonString, HttpStatus.OK)

    def objectUnderTest = new DataSyncWatchdog(mockInventoryPersistence, mockCpsDataService, mockSyncUtils)

    def 'Schedule Data Sync for Cm Handle State in READY and Operational Sync State in UNSYNCHRONIZED'() {
        given: 'cm handles in an ready state and operational sync state in unsynchronized'
            def compositeState1 = new CompositeState(cmHandleState: cmHandleState)
            def compositeState2 = new CompositeState(cmHandleState: cmHandleState)
            compositeState1.setDataStores(CompositeState.DataStores.builder()
                .operationalDataStore(CompositeState.Operational.builder().syncState("SYNCHRONIZED")
                    .build()).build())
            compositeState2.setDataStores(CompositeState.DataStores.builder()
                .operationalDataStore(CompositeState.Operational.builder().syncState("SYNCHRONIZED")
                    .build()).build())
            def yangModelCmHandle1 = new YangModelCmHandle(id: 'some-cm-handle-1', compositeState: compositeState1)
            def yangModelCmHandle2 = new YangModelCmHandle(id: 'some-cm-handle-2', compositeState: compositeState2)
            def resourceData = Optional.of(jsonString);
        and: 'sync utilities return a cm handle twice'
            mockSyncUtils.getUnSynchronizedReadyCmHandle() >>> [yangModelCmHandle1, yangModelCmHandle2, null]
        when: 'data sync poll is executed'
            objectUnderTest.executeUnSynchronizedReadyCmHandlePoll()
        then: 'the inventory persistence cm handle returns a composite state for the first cm handle'
            1 * mockInventoryPersistence.getCmHandleState('some-cm-handle-1') >> compositeState1
        and: 'the sync util returns first resource data'
            1 * mockSyncUtils.getResourceData('some-cm-handle-1') >> resourceData
        and: 'save the data'
            1 * mockCpsDataService.saveData('NFP-Operational', 'some-cm-handle-1', jsonString, _)
        and: 'the first cm handle operational sync state is updated from "UNSYNCHRONIZED" to "SYNCHRONIZED"'
            1 * mockInventoryPersistence.saveCmHandleState('some-cm-handle-1', compositeState1)
        then: 'the inventory persistence cm handle returns a composite state for the first cm handle'
            1 * mockInventoryPersistence.getCmHandleState('some-cm-handle-2') >> compositeState2
        and: 'the sync util returns first resource data'
            1 * mockSyncUtils.getResourceData('some-cm-handle-2') >> resourceData
        and: 'save the data'
            1 * mockCpsDataService.saveData('NFP-Operational', 'some-cm-handle-2', jsonString, _)
        and: 'the second cm handle operational sync state is updated from "UNSYNCHRONIZED" to "SYNCHRONIZED"'
            1 * mockInventoryPersistence.saveCmHandleState('some-cm-handle-2', compositeState2)
    }
}
