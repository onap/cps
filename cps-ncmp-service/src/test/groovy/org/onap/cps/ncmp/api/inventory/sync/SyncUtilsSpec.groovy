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
import spock.lang.Specification

class SyncUtilsSpec extends Specification{

    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))
    def mockYangModelCmHandleRetriever = Mock(YangModelCmHandleRetriever)

    def dataNode = new DataNode(leaves: ['id': 'Some-Cm-Handle', 'dmi-service-name': 'testDmiService'])

    def expectedJsonData = '{"cm-handles":[{"id":"Some-Cm-Handle","dmi-service-name":"some service name","state":"READY"}]}'

    def objectUnderTest = new SyncUtils(mockCpsDataService, mockCpsDataPersistenceService, spiedJsonObjectMapper, mockYangModelCmHandleRetriever)

    def 'Schedule a Cm-Handle Sync for ADVISED Cm-Handles'() {
        given: 'a cm handle with an "ADVISED" state'
            def dmiServiceName = 'some service name'
            def yangModelCmHandle = new YangModelCmHandle(id:'Some-Cm-Handle', cmHandleState: 'ADVISED', dmiServiceName:  dmiServiceName)
        when: 'a sync is scheduled'
            objectUnderTest.scheduleCmHandleSync()
        then: 'cm handles with an "ADVISED" state are queried and retrieved'
            1 * mockCpsDataPersistenceService.queryDataNodes("NCMP-Admin",
                "ncmp-dmi-registry", "//cm-handles[@state=\"ADVISED\"]",
                FetchDescendantsOption.OMIT_DESCENDANTS) >> [ dataNode ]
        and: 'a single cm handle yang model is retrieved'
            1 * mockYangModelCmHandleRetriever.getYangModelCmHandle('Some-Cm-Handle') >> yangModelCmHandle
        and: 'yang model cm handle state is set to "READY"'
            yangModelCmHandle.cmHandleState == 'READY'
        and: 'the data node is then updated'
            1 * mockCpsDataService.updateNodeLeaves('NCMP-Admin', 'ncmp-dmi-registry', '/dmi-registry', expectedJsonData , null)
        then: 'no further cm handles are retrieved with an "ADVISED" state'
            1 * mockCpsDataPersistenceService.queryDataNodes("NCMP-Admin",
                "ncmp-dmi-registry", "//cm-handles[@state=\"ADVISED\"]",
                FetchDescendantsOption.OMIT_DESCENDANTS) >> []
        and: 'no further cm handle yang models are retrieved'
            0 * mockYangModelCmHandleRetriever.getYangModelCmHandle(_)
        and: 'no further updates are made to the data node'
            0 * mockCpsDataService.updateNodeLeaves(_, _, _, _ , _)
    }

}
