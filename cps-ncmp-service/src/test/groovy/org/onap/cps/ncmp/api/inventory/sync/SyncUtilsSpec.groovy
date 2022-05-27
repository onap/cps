/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada
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
import org.onap.cps.ncmp.api.inventory.LockReasonCategory
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import spock.lang.Shared
import spock.lang.Specification

class SyncUtilsSpec extends Specification{

    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def mockInventoryPersistence = Mock(InventoryPersistence)

    def objectUnderTest = new SyncUtils(mockInventoryPersistence)

    def static cmHandleId = 'cm-handle-123'
    def static cmHandleXpath = "/dmi-registry/cm-handles[@id='${cmHandleId}/state']"
    def static stateDataNodes = [new DataNodeBuilder()
                                         .withXpath("/dmi-registry/cm-handles[@id='${cmHandleId}']/state/lock-reason")
                                         .withLeaves(['reason': 'LOCKED_MISBEHAVING', 'details': 'lock details']).build()
                                 ]

    def static cmHandleDataNode = new DataNode(xpath: cmHandleXpath, childDataNodes: stateDataNodes, leaves: ['cm-handle-state': 'LOCKED'])

    def static yangModelCmHandle = new YangModelCmHandle(compositeState: new CompositeStateBuilder().fromDataNode(cmHandleDataNode).build())

    @Shared
    def dataNode = new DataNode(leaves: ['id': 'cm-handle-123'])

    def 'Get an advised Cm-Handle where ADVISED cm handle #scenario'() {
        given: 'the inventory persistence service returns a collection of data nodes'
            mockInventoryPersistence.getCmHandlesByState(CmHandleState.ADVISED) >> dataNodeCollection
        when: 'get advised cm handle is called'
            objectUnderTest.getAnAdvisedCmHandle()
        then: 'the returned data node collection is the correct size'
            dataNodeCollection.size() == expectedDataNodeSize
        and: 'get yang model cm handles is invoked the correct number of times'
           expectedCallsToGetYangModelCmHandle * mockInventoryPersistence.getYangModelCmHandle('cm-handle-123')
        where: 'the following scenarios are used'
            scenario         | dataNodeCollection || expectedCallsToGetYangModelCmHandle | expectedDataNodeSize
            'exists'         | [ dataNode ]       || 1                                   | 1
            'does not exist' | [ ]                || 0                                   | 0

    }

    def 'Update Lock Reason, Details and Attempts where lock reason #scenario'() {
        given: 'A locked state'
           def compositeState = new CompositeState(lockReason: lockReason)
        when: 'update cm handle details and attempts is called'
            objectUnderTest.updateLockReasonDetailsAndAttempts(compositeState, LockReasonCategory.LOCKED_MISBEHAVING, 'new error message')
        then: 'the composite state lock reason and details are updated'
            assert compositeState.lockReason.lockReasonCategory == LockReasonCategory.LOCKED_MISBEHAVING
            assert compositeState.lockReason.details == expectedDetails
        where:
            scenario         | lockReason                                                                                   || expectedDetails
            'does not exist' | null                                                                                         || 'Attempt #1 failed: new error message'
            'exists'         | CompositeState.LockReason.builder().details("Attempt #2 failed: some error message").build() || 'Attempt #3 failed: new error message'
    }
    def 'Get all locked Cm-Handle where Lock Reason is LOCKED_MISBEHAVING cm handle #scenario'() {
        given: 'the cps (persistence service) returns a collection of data nodes'
            mockInventoryPersistence.getCmHandlesByCpsPath() >> dataNodeCollection
            mockInventoryPersistence.getYangModelCmHandle('cm-handle-123') >> yangModelCmHandle
        when: 'get locked Misbehaving cm handle is called'
            objectUnderTest.getLockedMisbehavingCmHandles()
        then: 'the returned cm handle collection is the correct size'
            dataNodeCollection.size() == expectedCmHandleListSize
        and: 'get yang model cm handles is invoked the correct number of times'
            expectedCallsToGetYangModelCmHandle * mockInventoryPersistence.getYangModelCmHandle('cm-handle-123')
        where: 'the following scenarios are used'
            scenario         | dataNodeCollection         || expectedCallsToGetYangModelCmHandle | expectedCmHandleListSize
            'exists'         | [dataNode ]                || 1                                   | 1
            'does not exist' | [ ]                        || 0                                   | 0

    }
}
