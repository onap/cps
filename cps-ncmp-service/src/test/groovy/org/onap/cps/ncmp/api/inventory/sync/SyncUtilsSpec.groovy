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


import org.onap.cps.ncmp.api.impl.operations.YangModelCmHandleRetriever
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeState
import org.onap.cps.ncmp.api.inventory.LockReasonCategory
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.DataNode
import spock.lang.Shared
import spock.lang.Specification

class SyncUtilsSpec extends Specification{

    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def mockYangModelCmHandleRetriever = Mock(YangModelCmHandleRetriever)
    def mockRegistryPersistence = Mock(InventoryPersistence)

    def objectUnderTest = new SyncUtils(mockCpsDataPersistenceService, mockYangModelCmHandleRetriever)

    @Shared
    def dataNode = new DataNode(leaves: ['id': 'cm-handle-123'])



    def 'Get an advised Cm-Handle where ADVISED cm handle #scenario'() {
        given: 'the cps (persistence service) returns a collection of data nodes'
            mockCpsDataPersistenceService.queryDataNodes('NCMP-Admin',
                'ncmp-dmi-registry', '//state[@cm-handle-state=\"ADVISED\"]/ancestor::cm-handles',
                FetchDescendantsOption.OMIT_DESCENDANTS) >> dataNodeCollection
        when: 'get advised cm handle is called'
            objectUnderTest.getAnAdvisedCmHandle()
        then: 'the returned data node collection is the correct size'
            dataNodeCollection.size() == expectedDataNodeSize
        and: 'get yang model cm handles is invoked the correct number of times'
           expectedCallsToGetYangModelCmHandle * mockYangModelCmHandleRetriever.getYangModelCmHandle('cm-handle-123')
        where: 'the following scenarios are used'
            scenario         | dataNodeCollection || expectedCallsToGetYangModelCmHandle | expectedDataNodeSize
            'exists'         | [ dataNode ]       || 1                                   | 1
            'does not exist' | [ ]                || 0                                   | 0

    }

    def 'Update Lock Reason, Details and Attempts'() {
        given: 'a composite state with a valid lock reason, along with details and the number of attempts attempts'
           def compositeState = new CompositeState(lockReason:
               CompositeState.LockReason.builder()
                   .details("Attempt #2 failed: some error message").build())
        when: 'update cm handle details and attempts is called'
            objectUnderTest.updateLockReasonDetailsAndAttempts(compositeState, LockReasonCategory.LOCKED_MISBEHAVING, 'new error message')
        then: 'assert that the composite state is updated with the lock reason, the new number of attempts and the new error message'
            assert compositeState.lockReason.lockReasonCategory == LockReasonCategory.LOCKED_MISBEHAVING
            assert compositeState.lockReason.details == "Attempt #3 failed: new error message"

    }

}
