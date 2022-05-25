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

import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeState
import org.onap.cps.ncmp.api.inventory.CompositeStateBuilder
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class DataSyncSpec extends Specification {

    def mockInventoryPersistence = Mock(InventoryPersistence)

    def mockSyncUtils = Mock(SyncUtils)

    def cmHandleState = CmHandleState.READY

    def objectUnderTest = new DataSyncWatchdog(mockInventoryPersistence, mockSyncUtils)

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
        and: 'sync utilities return a cm handle twice'
            mockSyncUtils.getUnSynchronizedReadyCmHandle() >>> [yangModelCmHandle1, yangModelCmHandle2, null]
        when: 'data sync poll is executed'
            objectUnderTest.executeUnSynchronizedReadyCmHandlePoll()
        then: 'the inventory persistence cm handle returns a composite state for the first cm handle'
            1 * mockInventoryPersistence.getCmHandleState('some-cm-handle-1') >> compositeState1
        and: 'the first cm handle operational sync state is updated from "UNSYNCHRONIZED" to "SYNCHRONIZED"'
            1 * mockInventoryPersistence.saveCmHandleState('some-cm-handle-1', compositeState1)
        then: 'the inventory persistence cm handle returns a composite state for the first cm handle'
            1 * mockInventoryPersistence.getCmHandleState('some-cm-handle-2') >> compositeState2
        and: 'the second cm handle operational sync state is updated from "UNSYNCHRONIZED" to "SYNCHRONIZED"'
            1 * mockInventoryPersistence.saveCmHandleState('some-cm-handle-2', compositeState2)
    }
}
