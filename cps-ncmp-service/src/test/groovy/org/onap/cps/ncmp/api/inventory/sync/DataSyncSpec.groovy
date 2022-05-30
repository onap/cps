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

import org.onap.cps.ncmp.api.NetworkCmProxyDataService
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeState
import spock.lang.Specification
import java.time.format.DateTimeFormatter

class DataSyncSpec extends Specification {

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    def mockSyncUtils = Mock(SyncUtils)

    def networkCmProxyDataService = Mock(NetworkCmProxyDataService)

    def cmHandleState = CmHandleState.READY

    def objectUnderTest = new DataSyncWatchdog(mockSyncUtils, networkCmProxyDataService)

    def 'Schedule Data Sync for Cm Handle State in READY and Operational Sync State in UNSYNCHRONIZED'() {
        given: 'cm handles in an ready state and operational sync state in unsynchronized'
            def compositeState = new CompositeState()
            compositeState.cmhandleState = cmHandleState
            compositeState.setDataStores(CompositeState.DataStores.builder()
                .operationalDataStore(CompositeState.Operational.builder().syncState("UNSYNCHRONIZED")
                    .build()).build())
            def yangModelCmHandle = new YangModelCmHandle(id:'some-cm-handle', compositeState: compositeState)
        and: 'sync utilities return a random cm handle'
            mockSyncUtils.getUnSynchronizedReadyCmHandle() >>> [yangModelCmHandle, null]
        when: 'data sync poll is executed'
            objectUnderTest.executeUnSynchronizedReadyCmHandlePoll()
        then: 'the resource data is read from the node'
            1 * networkCmProxyDataService.getResourceDataPassThroughRunningForCmHandle('some-cm-handle','/',null,null,_)
        then: 'the cm handle with operational sync state is updated to "SYNCHRONIZED" from "UNSYNCHRONIZED"'
            1 * mockSyncUtils.updateCmHandleStateWithNodeLeaves(yangModelCmHandle)
    }
}
