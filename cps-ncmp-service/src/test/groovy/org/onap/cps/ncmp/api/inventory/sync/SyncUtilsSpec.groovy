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
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.DataNode
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Shared
import spock.lang.Specification

import java.time.OffsetDateTime

class SyncUtilsSpec extends Specification{

    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))
    def mockYangModelCmHandleRetriever = Mock(YangModelCmHandleRetriever)

    def objectUnderTest = new SyncUtils(mockCpsDataService, mockCpsDataPersistenceService, spiedJsonObjectMapper, mockYangModelCmHandleRetriever)

    @Shared
    def dataNode = new DataNode(leaves: ['id': 'cm-handle-123'])



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
            'exists'         | [ dataNode ]       || 1                                   | 1
            'does not exist' | [ ]                || 0                                   | 0

    }

    def 'Update cm handle state from Advised to Ready'() {
        given: 'a yang model cm handle and the expected json data'
            def yangModelCmHandle = new YangModelCmHandle('id': 'Some-Cm-Handle', 'cmHandleState': 'ADVISED')
            def expectedJsonData = '{"cm-handles":[{"id":"Some-Cm-Handle","state":"READY"}]}'
        when: 'update cm handle state is called'
            objectUnderTest.updateCmHandleState(yangModelCmHandle, 'READY')
        then: 'update data note leaves is invoked with the correct params'
            1 * mockCpsDataService.updateNodeLeaves('NCMP-Admin', 'ncmp-dmi-registry', '/dmi-registry', expectedJsonData, _ as OffsetDateTime)
    }

}
