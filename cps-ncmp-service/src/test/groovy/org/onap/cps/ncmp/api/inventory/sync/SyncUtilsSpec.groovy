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

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsDataService
import org.onap.cps.ncmp.api.impl.operations.YangModelCmHandleRetriever
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeState
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.DataNode
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Shared
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

class SyncUtilsSpec extends Specification {

    def DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))
    def mockYangModelCmHandleRetriever = Mock(YangModelCmHandleRetriever)

    def objectUnderTest = new SyncUtils(mockCpsDataService, mockCpsDataPersistenceService, spiedJsonObjectMapper, mockYangModelCmHandleRetriever)

    @Shared
    def dataNode = new DataNode(leaves: ['id': 'cm-handle-123'])

    def dataspaceName = 'NCMP-Admin'
    def anchorName = 'ncmp-dmi-registry'
    def lastSyncTime = DATE_TIME_FORMATTER.format(OffsetDateTime.now())

    def 'Get an advised Cm-Handle where ADVISED cm handle #scenario'() {
        given: 'the cps (persistence service) returns a collection of data nodes'
            mockCpsDataPersistenceService.queryDataNodes('NCMP-Admin',
                'ncmp-dmi-registry', '//cm-handles[@state=\"ADVISED\"]',
                FetchDescendantsOption.OMIT_DESCENDANTS) >> dataNodeCollection
        when: 'get advised cm handle is called'
            objectUnderTest.getAnAdvisedCmHandle()
        then: 'the returned data node collection is the correct size'
            dataNodeCollection.size() == expectedDataNodeSize
        and: 'get yang model cm handles is invoked the correct number of times'
            expectedCallsToGetYangModelCmHandle * mockYangModelCmHandleRetriever.getYangModelCmHandle('cm-handle-123')
        where: 'the following scenarios are used'
            scenario         | dataNodeCollection || expectedCallsToGetYangModelCmHandle | expectedDataNodeSize
            'exists'         | [dataNode]         || 1                                   | 1
            'does not exist' | []                 || 0                                   | 0

    }

    def 'Update cm handle state from Advised to Ready'() {
        given: 'a yang model cm handle and the expected json data'
            def compositeState = new CompositeState()
            compositeState.cmhandleState = CmHandleState.ADVISED
            def yangModelCmHandle = new YangModelCmHandle(id: 'Some-Cm-Handle', compositeState: compositeState)
            def expectedJsonData = '{"cm-handles":[{"id":"Some-Cm-Handle","state":{"cm-handle-state":"READY"}}]}'
        when: 'update cm handle state is called'
            objectUnderTest.updateCmHandleState(yangModelCmHandle, CmHandleState.READY)
        then: 'update data note leaves is invoked with the correct params'
            1 * mockCpsDataService.updateNodeLeaves('NCMP-Admin', 'ncmp-dmi-registry', '/dmi-registry', expectedJsonData, _ as OffsetDateTime)
    }

    def 'Get a Cm-Handle state in READY and Operation Sync State in UNSYNCHRONIZED #scenario'() {
        given: 'the cps (persistence service) returns a collection of data nodes'
            mockCpsDataPersistenceService.queryDataNodes(dataspaceName,
                anchorName, '//state/datastores/operational[@sync-state=\"UNSYNCHRONIZED\"]/ancestor::cm-handles',
                FetchDescendantsOption.OMIT_DESCENDANTS) >> dataNodeCollection
        when: 'get unsynchronized and ready cm handle is called'
            objectUnderTest.getUnSynchronizedReadyCmHandle()
        then: 'the returned data node collection is the correct size'
            dataNodeCollection.size() == expectedDataNodeSize
        and: 'get yang model cm handles is invoked the correct number of times'
            expectedCallsToGetYangModelCmHandle * mockYangModelCmHandleRetriever.getYangModelCmHandle('cm-handle-123')
        where: 'the following scenarios are used'
            scenario         | dataNodeCollection || expectedCallsToGetYangModelCmHandle | expectedDataNodeSize
            'exists'         | [dataNode]         || 1                                   | 1
            'does not exist' | []                 || 0                                   | 0

    }

    def 'Update cm handle operational sync state from UNSYNCHRONIZED to SYNCHRONIZED'() {
        given: 'a yang model cm handle and the expected json data'
            def compositeState = new CompositeState()
            compositeState.cmhandleState = CmHandleState.READY
            compositeState.setDataStores(CompositeState.DataStores.builder()
                .operationalDataStore(CompositeState.Operational.builder().syncState("SYNCHRONIZED")
                    .lastSyncTime(lastSyncTime).build()).build())
            def yangModelCmHandle = new YangModelCmHandle(id: 'cm-handle-1', compositeState: compositeState)
            def expectedJsonData = '{"cm-handles":[{"id":"cm-handle-1","state":{"cm-handle-state":"READY","datastores":{"operational":{"sync-state":"SYNCHRONIZED","last-sync-time":"' + lastSyncTime + '"}}}}]}'
        when: 'update cm handle state is called'
            objectUnderTest.updateCmHandleStateWithNodeLeaves(yangModelCmHandle)
        then: 'update data note leaves is invoked with the correct params'
            1 * mockCpsDataService.updateNodeLeavesAndExistingDescendantLeaves('NCMP-Admin', 'ncmp-dmi-registry', '/dmi-registry', expectedJsonData, _ as OffsetDateTime)
    }

}
