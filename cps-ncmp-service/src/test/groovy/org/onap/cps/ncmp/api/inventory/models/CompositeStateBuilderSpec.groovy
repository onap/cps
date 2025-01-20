/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Bell Canada
 * Modifications Copyright (C) 2022-2023 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.inventory.models


import org.onap.cps.ncmp.api.inventory.DataStoreSyncState
import org.onap.cps.api.model.DataNode
import org.onap.cps.impl.DataNodeBuilder
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class CompositeStateBuilderSpec extends Specification {

    def formattedDateAndTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        .format(OffsetDateTime.of(2022, 12, 31, 20, 30, 40, 1, ZoneOffset.UTC))

    def static cmHandleId = 'myHandle1'
    def static cmHandleXpath = "/dmi-registry/cm-handles[@id='${cmHandleId}/state']"
    def static stateDataNodes = [new DataNodeBuilder().withXpath("/dmi-registry/cm-handles[@id='${cmHandleId}']/state/lock-reason")
                                         .withLeaves(['reason': 'MODULE_SYNC_FAILED', 'details': 'lock details']).build(),
                                 new DataNodeBuilder().withXpath("/dmi-registry/cm-handles[@id='${cmHandleId}']/state/datastores")
                                            .withChildDataNodes(Arrays.asList(new DataNodeBuilder()
                                                    .withXpath("/dmi-registry/cm-handles[@id='${cmHandleId}']/state/datastores/operational")
                                                    .withLeaves(['sync-state': 'UNSYNCHRONIZED']).build())).build()]
    def static cmHandleDataNode = new DataNode(xpath: cmHandleXpath, childDataNodes: stateDataNodes, leaves: ['cm-handle-state': 'ADVISED'])

    def "Composite State Specification"() {
        when: 'using composite state builder '
            def compositeState = new CompositeStateBuilder().withCmHandleState(CmHandleState.ADVISED)
                    .withLockReason(LockReasonCategory.MODULE_SYNC_FAILED,"").withOperationalDataStores(DataStoreSyncState.UNSYNCHRONIZED,
                    formattedDateAndTime.toString()).withLastUpdatedTime(formattedDateAndTime).build()
        then: 'it matches expected cm handle state and data store sync state'
            assert compositeState.cmHandleState == CmHandleState.ADVISED
            assert compositeState.dataStores.operationalDataStore.dataStoreSyncState == DataStoreSyncState.UNSYNCHRONIZED
    }

    def "Build composite state from DataNode "() {
        given: "a Data Node "
            new DataNode(leaves: ['cm-handle-state': 'ADVISED'])
        when: 'build from data node function is invoked'
            def compositeState = new CompositeStateBuilder().fromDataNode(cmHandleDataNode).build()
        then: 'it matches expected state model as JSON'
            assert compositeState.cmHandleState == CmHandleState.ADVISED
    }

    def 'CompositeStateBuilder build'() {
        given: 'A CompositeStateBuilder with all private fields set'
            def finalCompositeStateBuilder = new CompositeStateBuilder()
                .withCmHandleState(CmHandleState.ADVISED)
                .withLastUpdatedTime(formattedDateAndTime.toString())
                .withLockReason(LockReasonCategory.MODULE_SYNC_FAILED, 'locked details')
                .withOperationalDataStores(DataStoreSyncState.SYNCHRONIZED, formattedDateAndTime)
        when: 'build is called'
            def result = finalCompositeStateBuilder.build()
        then: 'result is of the correct type'
            assert result.class == CompositeState.class
        and: 'built result should have correct values'
            assert !result.getDataSyncEnabled()
            assert result.getLastUpdateTime() == formattedDateAndTime
            assert result.getLockReason().getLockReasonCategory() == LockReasonCategory.MODULE_SYNC_FAILED
            assert result.getLockReason().getDetails() == 'locked details'
            assert result.getCmHandleState() == CmHandleState.ADVISED
            assert result.getDataStores().getOperationalDataStore().getDataStoreSyncState() == DataStoreSyncState.SYNCHRONIZED
            assert result.getDataStores().getOperationalDataStore().getLastSyncTime() == formattedDateAndTime
    }

}
