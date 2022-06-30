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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations
import org.onap.cps.ncmp.api.impl.operations.DmiOperations
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeState
import org.onap.cps.ncmp.api.inventory.CompositeStateBuilder
import org.onap.cps.ncmp.api.inventory.DataStoreSyncState
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.inventory.LockReasonCategory
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.DataNode
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Shared
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class SyncUtilsSpec extends Specification{

    def mockInventoryPersistence = Mock(InventoryPersistence)

    def mockDmiDataOperations = Mock(DmiDataOperations)

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    def objectUnderTest = new SyncUtils(mockInventoryPersistence, mockDmiDataOperations, jsonObjectMapper)

    @Shared
    def formattedDateAndTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(OffsetDateTime.now())

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
            mockInventoryPersistence.getCmHandleDataNodesByCpsPath(
                    '//lock-reason[@reason="LOCKED_MISBEHAVING"]/ancestor::cm-handles',
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> [dataNode ]
        when: 'get locked Misbehaving cm handle is called'
            def result = objectUnderTest.getLockedMisbehavingYangModelCmHandles()
        then: 'the returned cm handle collection is the correct size'
            result.size() == 1
        and: 'the correct cm handle is returned'
            result[0].id == 'cm-handle-123'
    }

    def 'Retry Locked Cm-Handle where the last update time is #scenario'() {
        when: 'retry locked cm handle is invoked'
            def result = objectUnderTest.isReadyForRetry(new CompositeStateBuilder()
                .withLockReason(LockReasonCategory.LOCKED_MISBEHAVING, details)
                .withLastUpdatedTime(lastUpdateTime).build())
        then: 'result returns #expectedResult'
            result == expectedResult
        where:
            scenario                        | lastUpdateTime                     | details                 || expectedResult
            'is the first attempt'          | '1900-01-01T00:00:00.000+0100'     | 'First Attempt'         || true
            'is greater than one minute'    | '1900-01-01T00:00:00.000+0100'     | 'Attempt #1 failed:'    || true
            'is less than eight minutes'    | formattedDateAndTime               | 'Attempt #3 failed:'    || false
    }


    def 'Get a Cm-Handle where Operational Sync state is UnSynchronized and Cm-handle state is READY and #scenario'() {
        given: 'the inventory persistence service returns a collection of data nodes'
            mockInventoryPersistence.getCmHandlesByOperationalSyncState(DataStoreSyncState.UNSYNCHRONIZED) >> unSynchronizedDataNodes
            mockInventoryPersistence.getCmHandlesByIdAndState("cm-handle-123", CmHandleState.READY) >> readyDataNodes
        when: 'get advised cm handle is called'
            objectUnderTest.getAnUnSynchronizedReadyCmHandle()
        then: 'the returned data node collection is the correct size'
            readyDataNodes.size() == expectedDataNodeSize
        and: 'get yang model cm handles is invoked the correct number of times'
            expectedCallsToGetYangModelCmHandle * mockInventoryPersistence.getYangModelCmHandle('cm-handle-123')
        where: 'the following scenarios are used'
            scenario                             | unSynchronizedDataNodes | readyDataNodes || expectedCallsToGetYangModelCmHandle | expectedDataNodeSize
            'exists'                             | [dataNode]              | [dataNode]     || 1                                   | 1
            'unsynchronized exist but not ready' | [dataNode]              | []             || 0                                   | 0
            'does not exist'                     | []                      | []             || 0                                   | 0
    }

    def 'Get resource data through DMI Operations #scenario'() {
        given: 'the inventory persistence service returns a collection of data nodes'
            def jsonString = '{"stores:bookstore":{"categories":[{"code":"01"}]}}'
            JsonNode jsonNode = jsonObjectMapper.convertToJsonNode(jsonString);
            def responseEntity = new ResponseEntity<>(jsonNode, HttpStatus.OK)
            mockDmiDataOperations.getResourceDataFromDmi('cm-handle-123', DmiOperations.DataStoreEnum.PASSTHROUGH_OPERATIONAL, _) >> responseEntity
        when: 'get resource data is called'
            def result = objectUnderTest.getResourceData('cm-handle-123')
        then: 'the returned data is correct'
            result == jsonString
    }
}
