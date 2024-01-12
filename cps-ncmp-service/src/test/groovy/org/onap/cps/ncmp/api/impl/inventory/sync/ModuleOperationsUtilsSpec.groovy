/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.inventory.sync

import static org.onap.cps.ncmp.api.impl.inventory.LockReasonCategory.LOCKED_MISBEHAVING
import static org.onap.cps.ncmp.api.impl.inventory.LockReasonCategory.MODULE_UPGRADE
import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_OPERATIONAL
import static org.onap.cps.ncmp.api.impl.inventory.LockReasonCategory.MODULE_SYNC_FAILED
import static org.onap.cps.ncmp.api.impl.inventory.LockReasonCategory.MODULE_UPGRADE_FAILED

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.core.read.ListAppender
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations
import org.onap.cps.ncmp.api.impl.inventory.CmHandleQueries
import org.onap.cps.ncmp.api.impl.inventory.CmHandleState
import org.onap.cps.ncmp.api.impl.inventory.CompositeState
import org.onap.cps.ncmp.api.impl.inventory.CompositeStateBuilder
import org.onap.cps.ncmp.api.impl.inventory.DataStoreSyncState
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.DataNode
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.stream.Collectors

class ModuleOperationsUtilsSpec extends Specification{

    def mockCmHandleQueries = Mock(CmHandleQueries)

    def mockDmiDataOperations = Mock(DmiDataOperations)

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    def objectUnderTest = new ModuleOperationsUtils(mockCmHandleQueries, mockDmiDataOperations, jsonObjectMapper)

    def static neverUpdatedBefore = '1900-01-01T00:00:00.000+0100'

    def static now = OffsetDateTime.now()

    def static nowAsString = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(now)

    def static dataNode = new DataNode(leaves: ['id': 'cm-handle-123'])

    def applicationContext = new AnnotationConfigApplicationContext()

    def logger = (Logger) LoggerFactory.getLogger(ModuleOperationsUtils)
    def loggingListAppender

    void setup() {
        logger.setLevel(Level.DEBUG)
        loggingListAppender = new ListAppender()
        logger.addAppender(loggingListAppender)
        loggingListAppender.start()
        applicationContext.refresh()
    }

    void cleanup() {
        ((Logger) LoggerFactory.getLogger(ModuleOperationsUtils.class)).detachAndStopAllAppenders()
        applicationContext.close()
    }

    def 'Get an advised Cm-Handle where ADVISED cm handle #scenario'() {
        given: 'the inventory persistence service returns a collection of data nodes'
            mockCmHandleQueries.queryCmHandlesByState(CmHandleState.ADVISED) >> dataNodeCollection
        when: 'get advised cm handles are fetched'
            def yangModelCmHandles = objectUnderTest.getAdvisedCmHandles()
        then: 'the returned data node collection is the correct size'
            yangModelCmHandles.size() == expectedDataNodeSize
        where: 'the following scenarios are used'
            scenario         | dataNodeCollection || expectedCallsToGetYangModelCmHandle | expectedDataNodeSize
            'exists'         | [dataNode]         || 1                                   | 1
            'does not exist' | []                 || 0                                   | 0
    }

    def 'Update Lock Reason, Details and Attempts where lock reason #scenario'() {
        given: 'A locked state'
            def compositeState = new CompositeState(lockReason: lockReason)
        when: 'update cm handle details and attempts is called'
            objectUnderTest.updateLockReasonDetailsAndAttempts(compositeState, MODULE_SYNC_FAILED, 'new error message')
        then: 'the composite state lock reason and details are updated'
            assert compositeState.lockReason.lockReasonCategory == MODULE_SYNC_FAILED
            assert compositeState.lockReason.details.contains(expectedDetails)
        where:
            scenario                           | lockReason                                                                                       || expectedDetails
            'does not exist'                   | null                                                                                             || 'Attempt #1 failed: new error message'
            'exists'                           | CompositeState.LockReason.builder().details("Attempt #2 failed: some error message").build()     || 'Attempt #3 failed: new error message'
    }

    def 'Update lock reason details that contains #scenario'() {
        given: 'A locked state'
            def compositeState = new CompositeStateBuilder().withCmHandleState(CmHandleState.LOCKED)
                .withLockReason(MODULE_UPGRADE, "Upgrade to ModuleSetTag: " + moduleSetTag).build()
        when: 'update cm handle details'
            objectUnderTest.updateLockReasonDetailsAndAttempts(compositeState, MODULE_UPGRADE_FAILED, 'new error message')
        then: 'the composite state lock reason and details are updated'
            assert compositeState.lockReason.lockReasonCategory == MODULE_UPGRADE_FAILED
            assert compositeState.lockReason.details.contains("Upgrade to ModuleSetTag: " + expectedDetails)
        where:
            scenario               | moduleSetTag       || expectedDetails
            'a module set tag'     | 'someModuleSetTag' || 'someModuleSetTag'
            'empty module set tag' | ''                 || 'not-specified'
    }

    def 'Get all locked cm-Handles where lock reasons are model sync failed or upgrade'() {
        given: 'the cps (persistence service) returns a collection of data nodes'
            mockCmHandleQueries.queryCmHandleAncestorsByCpsPath(ModuleOperationsUtils.CPS_PATH_CM_HANDLES_MODEL_SYNC_FAILED_OR_UPGRADE,
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> [dataNode]
        when: 'get locked Misbehaving cm handle is called'
            def result = objectUnderTest.getCmHandlesThatFailedModelSyncOrUpgrade()
        then: 'the returned cm handle collection is the correct size'
            result.size() == 1
        and: 'the correct cm handle is returned'
            result[0].id == 'cm-handle-123'
    }

    def 'Retry Locked Cm-Handle where the last update time is #scenario'() {
        given: 'Last update was #lastUpdateMinutesAgo minutes ago (-1 means never)'
            def lastUpdatedTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ").format(now.minusMinutes(lastUpdateMinutesAgo))
            if (lastUpdateMinutesAgo < 0 ) {
                lastUpdatedTime = neverUpdatedBefore
            }
        when: 'checking to see if cm handle is ready for retry'
         def result = objectUnderTest.needsModuleSyncRetryOrUpgrade(new CompositeStateBuilder()
                .withLockReason(MODULE_SYNC_FAILED, lockDetails)
                .withLastUpdatedTime(lastUpdatedTime).build())
        then: 'retry is only attempted when expected'
            assert result == retryExpected
        and: 'logs contain related information'
            def logs = loggingListAppender.list.toString()
            assert logs.contains(logReason)
        where: 'the following parameters are used'
            scenario                                    | lastUpdateMinutesAgo | lockDetails                     | logReason                               || retryExpected
            'never attempted before'                    | -1                   | 'Fist attempt:'                 | 'First Attempt:'                        || true
            '1st attempt, last attempt > 2 minute ago'  | 3                    | 'Attempt #1 failed: some error' | 'Retry due now'                         || true
            '2nd attempt, last attempt < 4 minutes ago' | 1                    | 'Attempt #2 failed: some error' | 'Time until next attempt is 3 minutes:' || false
            '2nd attempt, last attempt > 4 minutes ago' | 5                    | 'Attempt #2 failed: some error' | 'Retry due now'                         || true
    }

    def 'Retry Locked Cm-Handle with lock reasons (category) #lockReasonCategory'() {
        when: 'checking to see if cm handle is ready for retry'
            def result = objectUnderTest.needsModuleSyncRetryOrUpgrade(new CompositeStateBuilder()
                .withLockReason(lockReasonCategory, 'some details')
                .withLastUpdatedTime(nowAsString).build())
        then: 'verify retry attempts'
            assert !result
        and: 'logs contain related information'
            def logs = loggingListAppender.list.toString()
            assert logs.contains(logReason)
        where: 'the following lock reasons occurred'
            scenario             | lockReasonCategory    || logReason
            'module upgrade'     | MODULE_UPGRADE_FAILED || 'First Attempt:'
            'module sync failed' | MODULE_SYNC_FAILED    || 'First Attempt:'
            'lock misbehaving'   | LOCKED_MISBEHAVING    || 'Locked for other reason'
    }

    def 'Get a Cm-Handle where #scenario'() {
        given: 'the inventory persistence service returns a collection of data nodes'
            mockCmHandleQueries.queryCmHandlesByOperationalSyncState(DataStoreSyncState.UNSYNCHRONIZED) >> unSynchronizedDataNodes
            mockCmHandleQueries.cmHandleHasState('cm-handle-123', CmHandleState.READY) >> cmHandleHasState
        when: 'get advised cm handles are fetched'
            def yangModelCollection = objectUnderTest.getUnsynchronizedReadyCmHandles()
        then: 'the returned data node collection is the correct size'
            yangModelCollection.size() == expectedDataNodeSize
        and: 'the result contains the correct data'
            yangModelCollection.stream().map(yangModel -> yangModel.id).collect(Collectors.toSet()) == expectedYangModelCollectionIds
        where: 'the following scenarios are used'
            scenario                                   | unSynchronizedDataNodes | cmHandleHasState || expectedDataNodeSize | expectedYangModelCollectionIds
            'a Cm-Handle unsynchronized and ready'     | [dataNode]              | true             || 1                    | ['cm-handle-123'] as Set
            'a Cm-Handle unsynchronized but not ready' | [dataNode]              | false            || 0                    | [] as Set
            'all Cm-Handle synchronized'               | []                      | false            || 0                    | [] as Set
    }

    def 'Get resource data through DMI Operations #scenario'() {
        given: 'the inventory persistence service returns a collection of data nodes'
            def jsonString = '{"stores:bookstore":{"categories":[{"code":"01"}]}}'
            JsonNode jsonNode = jsonObjectMapper.convertToJsonNode(jsonString);
            def responseEntity = new ResponseEntity<>(jsonNode, HttpStatus.OK)
            mockDmiDataOperations.getResourceDataFromDmi(PASSTHROUGH_OPERATIONAL.datastoreName, 'cm-handle-123', _) >> responseEntity
        when: 'get resource data is called'
            def result = objectUnderTest.getResourceData('cm-handle-123')
        then: 'the returned data is correct'
            result == jsonString
    }

    def 'Extract module set tag and number of attempt when lock reason contains #scenario'() {
        expect: 'lock reason details are extracted correctly'
            def result = objectUnderTest.getLockedCompositeStateDetails(new CompositeStateBuilder().withLockReason(MODULE_UPGRADE, lockReasonDetails).build().lockReason)
        and: 'the result contains the correct moduleSetTag'
            assert result['moduleSetTag'] == expectedModuleSetTag
        and: 'the result contains the correct number of attempts'
            assert result['attempt'] == expectedNumberOfAttempts
        where: 'the following scenarios are used'
            scenario                                     | lockReasonDetails                                                           || expectedModuleSetTag | expectedNumberOfAttempts
            'module set tag only'                        | 'Upgrade to ModuleSetTag: targetModuleSetTag'                               || 'targetModuleSetTag' | null
            'number of attempts only'                    | 'Attempt #1 failed: some error'                                             || null                 | '1'
            'number of attempts and module set tag both' | 'Upgrade to ModuleSetTag: targetModuleSetTag Attempt #1 failed: some error' || 'targetModuleSetTag' | '1'
    }
}
