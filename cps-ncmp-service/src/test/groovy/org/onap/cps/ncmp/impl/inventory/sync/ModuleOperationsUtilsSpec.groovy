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

package org.onap.cps.ncmp.impl.inventory.sync

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.inventory.models.CompositeState
import org.onap.cps.ncmp.api.inventory.models.CompositeStateBuilder
import org.onap.cps.ncmp.impl.data.DmiDataOperations
import org.onap.cps.ncmp.impl.inventory.CmHandleQueryService
import org.onap.cps.ncmp.impl.inventory.DataStoreSyncState
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState
import org.onap.cps.api.parameters.FetchDescendantsOption
import org.onap.cps.api.model.DataNode
import org.onap.cps.utils.JsonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification
import java.util.stream.Collectors

import static org.onap.cps.ncmp.impl.inventory.models.LockReasonCategory.MODULE_SYNC_FAILED
import static org.onap.cps.ncmp.impl.inventory.models.LockReasonCategory.MODULE_UPGRADE
import static org.onap.cps.ncmp.impl.inventory.models.LockReasonCategory.MODULE_UPGRADE_FAILED

class ModuleOperationsUtilsSpec extends Specification{

    def mockCmHandleQueries = Mock(CmHandleQueryService)

    def mockDmiDataOperations = Mock(DmiDataOperations)

    def jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    def objectUnderTest = new ModuleOperationsUtils(mockCmHandleQueries, mockDmiDataOperations, jsonObjectMapper)

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
            mockCmHandleQueries.queryCmHandleIdsByState(CmHandleState.ADVISED) >> cmHandleIds
        when: 'advised cm handle ids are fetched'
            def advisedCmHandleIds = objectUnderTest.getAdvisedCmHandleIds()
        then: 'the expected cm handle ids are returned'
            advisedCmHandleIds == cmHandleIds
        where: 'the following scenarios are used'
            scenario         | cmHandleIds
            'exists'         | ['cm-handle-123']
            'does not exist' | []
    }

    def 'Update Lock Reason, Details and Attempts where lock reason #scenario'() {
        given: 'A locked state'
            def compositeState = new CompositeState(lockReason: lockReason)
        when: 'update cm handle details and attempts is called'
            objectUnderTest.updateLockReasonWithAttempts(compositeState, MODULE_SYNC_FAILED, 'new error message')
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
            objectUnderTest.updateLockReasonWithAttempts(compositeState, MODULE_UPGRADE_FAILED, 'new error message')
        then: 'the composite state lock reason and details are updated'
            assert compositeState.lockReason.lockReasonCategory == MODULE_UPGRADE_FAILED
            assert compositeState.lockReason.details.contains(expectedDetails)
        where:
            scenario               | moduleSetTag       || expectedDetails
            'a module set tag'     | 'someModuleSetTag' || 'Upgrade to ModuleSetTag: someModuleSetTag'
            'empty module set tag' | ''                 || 'Attempt'
    }

    def 'Get all locked cm-Handles where lock reasons are model sync failed or upgrade'() {
        given: 'the cps (persistence service) returns a collection of data nodes'
            mockCmHandleQueries.queryCmHandleAncestorsByCpsPath(ModuleOperationsUtils.CPS_PATH_CM_HANDLES_MODEL_SYNC_FAILED_OR_UPGRADE,
                FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> [dataNode]
        when: 'get locked Misbehaving cm handle is called'
            def result = objectUnderTest.getCmHandlesThatFailedModelSyncOrUpgrade()
        then: 'the returned cm handle collection is the correct size'
            assert result.size() == 1
        and: 'the correct cm handle is returned'
            assert result[0].id == 'cm-handle-123'
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

    def 'Retrieve resource data from DMI operations for #scenario'() {
        given: 'a JSON string representing the resource data'
            def jsonString = '{"stores:bookstore":{"categories":[{"code":"01"}]}}'
            JsonNode jsonNode = jsonObjectMapper.convertToJsonNode(jsonString)
        and: 'DMI operations are mocked to return a response based on the scenario'
            def responseEntity = new ResponseEntity<>(statusCode == HttpStatus.OK ? jsonNode : null, statusCode)
            mockDmiDataOperations.getAllResourceDataFromDmi('cm-handle-123', _) >> responseEntity
        when: 'get resource data is called'
            def result = objectUnderTest.getResourceData('cm-handle-123')
        then: 'the returned data matches the expected result'
            assert result == expectedResult
        where:
            scenario                              | statusCode                       | expectedResult
            'successful response'                 | HttpStatus.OK                    | '{"stores:bookstore":{"categories":[{"code":"01"}]}}'
            'response with not found status'      | HttpStatus.NOT_FOUND             | null
            'response with internal server error' | HttpStatus.INTERNAL_SERVER_ERROR | null
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
