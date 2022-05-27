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

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsDataService
import org.onap.cps.ncmp.api.impl.operations.YangModelCmHandleRetriever
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.ncmp.api.inventory.CmHandleState
import org.onap.cps.ncmp.api.inventory.CompositeState
import org.onap.cps.ncmp.api.inventory.CompositeStateBuilder
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.DataNodeBuilder
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Shared
import spock.lang.Specification

import java.time.OffsetDateTime

class SyncUtilsSpec extends Specification{

    public static final String CM_HANDLE_DATASPACE = 'NCMP-Admin'
    public static final String CM_HANDLE_ANCHOR = 'ncmp-dmi-registry'
    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))
    def mockYangModelCmHandleRetriever = Mock(YangModelCmHandleRetriever)

    def objectUnderTest = new SyncUtils(mockCpsDataService, mockCpsDataPersistenceService, spiedJsonObjectMapper, mockYangModelCmHandleRetriever)

    def static cmHandleId = 'cm-handle-123'
    def static cmHandleXpath = "/dmi-registry/cm-handles[@id='${cmHandleId}/state']"
    def static stateDataNodes = [new DataNodeBuilder()
                                         .withXpath("/dmi-registry/cm-handles[@id='${cmHandleId}']/state/lock-reason")
                                         .withLeaves(['reason': 'LOCKED_MISBEHAVING', 'details': 'lock details']).build(),
                                 new DataNodeBuilder()
                                         .withXpath("/dmi-registry/cm-handles[@id='${cmHandleId}']/state/datastores")
                                         .withChildDataNodes(Arrays.asList(new DataNodeBuilder()
                                                 .withXpath("/dmi-registry/cm-handles[@id='${cmHandleId}']/state/datastores/operational")
                                                 .withLeaves(['sync-state': 'UNSYNCHRONIZED']).build())).build()]

    def static cmHandleDataNode = new DataNode(xpath: cmHandleXpath, childDataNodes: stateDataNodes, leaves: ['cm-handle-state': 'LOCKED'])

    def static yangModelCmHandle = new YangModelCmHandle(compositeState: new CompositeStateBuilder().fromDataNode(cmHandleDataNode).build())

    @Shared
    def dataNode = new DataNode(leaves: ['id': cmHandleId], childDataNodes: Arrays.asList(cmHandleDataNode))

    def 'Get an advised Cm-Handle where ADVISED cm handle #scenario'() {
        given: 'the cps (persistence service) returns a collection of data nodes'
            mockCpsDataPersistenceService.queryDataNodes(CM_HANDLE_DATASPACE,
                    CM_HANDLE_ANCHOR, '//cm-handles[@state=\"ADVISED\"]',
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

    def 'Get all locked Cm-Handle where Lock Reason is LOCKED_MISBEHAVING cm handle #scenario'() {
        given: 'the cps (persistence service) returns a collection of data nodes'
            mockCpsDataPersistenceService.queryDataNodes(CM_HANDLE_DATASPACE,
                    CM_HANDLE_ANCHOR, "//lock-reason[@reason=\"LOCKED_MISBEHAVING\"]/ancestor::cm-handles",
                    FetchDescendantsOption.OMIT_DESCENDANTS) >> dataNodeCollection
            mockYangModelCmHandleRetriever.getYangModelCmHandle('cm-handle-123') >> yangModelCmHandle
        when: 'get locked Misbehaving cm handle is called'
            objectUnderTest.getLockedMisbehavingCmHandles()
        then: 'the returned cm handle collection is the correct size'
            dataNodeCollection.size() == expectedCmHandleListSize
        and: 'get yang model cm handles is invoked the correct number of times'
            expectedCallsToGetYangModelCmHandle * mockYangModelCmHandleRetriever.getYangModelCmHandle('cm-handle-123')
        where: 'the following scenarios are used'
            scenario         | dataNodeCollection         || expectedCallsToGetYangModelCmHandle | expectedCmHandleListSize
            'exists'         | [dataNode ]                || 1                                   | 1
            'does not exist' | [ ]                        || 0                                   | 0

    }

    def 'Get unsynchronized  cm handle #scenario'() {
        when: 'get unsynchronized cm handle is called'
            def unsynchronizedCmHandles = objectUnderTest.getUnSynchronizedCmHandles(yangModelCmHandleList)
        then: 'the returned Cm Handles collection is the correct size'
            unsynchronizedCmHandles.size() == expectedCmHandlesListSize
        where: 'the following scenarios are used'
            scenario         | yangModelCmHandleList                           || expectedCmHandlesListSize
            'exists'         | [yangModelCmHandle, yangModelCmHandle ]         || 2
            'does not exist' | []                                              || 0
    }

    def 'Update cm handle state from Advised to Ready'() {
        given: 'a yang model cm handle and the expected json data'
            def compositeState = new CompositeState()
            compositeState.cmhandleState = CmHandleState.ADVISED
            def yangModelCmHandle = new YangModelCmHandle(id: 'Some-Cm-Handle', compositeState: compositeState )
            def expectedJsonData = '{"cm-handles":[{"id":"Some-Cm-Handle","state":{"cm-handle-state":"ADVISED"}}]}'
        when: 'update cm handle state is called'
            objectUnderTest.updateCmHandleState(yangModelCmHandle, compositeState)
        then: 'update data note leaves is invoked with the correct params'
            compositeState.setCmhandleState(CmHandleState.READY)
            1 * mockCpsDataService.updateNodeLeaves(CM_HANDLE_DATASPACE, CM_HANDLE_ANCHOR, '/dmi-registry', expectedJsonData, _ as OffsetDateTime)
    }

}
