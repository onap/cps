/*
 *  ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.api.inventory

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsDataService
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.utils.JsonObjectMapper
import org.onap.ncmp.cmhandle.lcm.event.Event
import spock.lang.Shared
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class InventoryPersistenceSpec extends Specification {

    def spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))

    def mockCpsDataService = Mock(CpsDataService)

    def mockCpsDataPersistenceService = Mock(CpsDataPersistenceService)


    def objectUnderTest = new InventoryPersistence(spiedJsonObjectMapper, mockCpsDataService, mockCpsDataPersistenceService)

    def formattedDateAndTime = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
            .format(OffsetDateTime.of(2022, 12, 31, 20, 30, 40, 1, ZoneOffset.UTC))

    def cmHandleId = 'some-cm-handle'
    def leaves = ["dmi-service-name":"common service name","dmi-data-service-name":"data service name","dmi-model-service-name":"model service name"]
    def xpath = "/dmi-registry/cm-handles[@id='some-cm-handle']"

    @Shared
    def childDataNodesForCmHandleWithAllProperties = [new DataNode(xpath: "/dmi-registry/cm-handles[@id='some cm handle']/additional-properties[@name='name1']", leaves: ["name":"name1", "value":"value1"]),
                                                      new DataNode(xpath: "/dmi-registry/cm-handles[@id='some cm handle']/public-properties[@name='name2']", leaves: ["name":"name2","value":"value2"])]

    @Shared
    def childDataNodesForCmHandleWithDMIProperties = [new DataNode(xpath: "/dmi-registry/cm-handles[@id='some-cm-handle']/additional-properties[@name='name1']", leaves: ["name":"name1", "value":"value1"])]

    @Shared
    def childDataNodesForCmHandleWithPublicProperties = [new DataNode(xpath: "/dmi-registry/cm-handles[@id='some-cm-handle']/public-properties[@name='name2']", leaves: ["name":"name2","value":"value2"])]

    @Shared
    def childDataNodesForCmHandleWithState = [new DataNode(xpath: "/dmi-registry/cm-handles[@id='some-cm-handle']/state", leaves: ['cm-handle-state': 'ADVISED'])]

    @Shared
    def static sampleDataNodes = [new DataNode()]

    def "Retrieve CmHandle using datanode with #scenario."() {
        given: 'the cps data service returns a data node from the DMI registry'
            def dataNode = new DataNode(childDataNodes:childDataNodes, leaves: leaves)
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry', xpath, INCLUDE_ALL_DESCENDANTS) >> dataNode
        when: 'retrieving the yang modelled cm handle'
            def result = objectUnderTest.getYangModelCmHandle(cmHandleId)
        then: 'the result has the correct id and service names'
            result.id == cmHandleId
            result.dmiServiceName == 'common service name'
            result.dmiDataServiceName == 'data service name'
            result.dmiModelServiceName == 'model service name'
        and: 'the expected DMI properties'
            result.dmiProperties == expectedDmiProperties
            result.publicProperties == expectedPublicProperties
        and: 'the state details are returned'
            result.compositeState.cmHandleState == expectedCompositeState
        where: 'the following parameters are used'
            scenario                    | childDataNodes                                || expectedDmiProperties                               || expectedPublicProperties                              || expectedCompositeState
            'no properties'             | []                                            || []                                                  || []                                                    || null
            'DMI and public properties' | childDataNodesForCmHandleWithAllProperties    || [new YangModelCmHandle.Property("name1", "value1")] || [new YangModelCmHandle.Property("name2", "value2")] || null
            'just DMI properties'       | childDataNodesForCmHandleWithDMIProperties    || [new YangModelCmHandle.Property("name1", "value1")] || []                                                    || null
            'just public properties'    | childDataNodesForCmHandleWithPublicProperties || []                                                  || [new YangModelCmHandle.Property("name2", "value2")]   || null
            'with state details'        | childDataNodesForCmHandleWithState            || []                                                  || []                                                    || CmHandleState.ADVISED
    }

    def "Retrieve CmHandle using datanode with invalid CmHandle id."() {
        when: 'retrieving the yang modelled cm handle with an invalid id'
            def result = objectUnderTest.getYangModelCmHandle('cm handle id with spaces')
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the result is not returned'
            result == null
    }

    def "Handling missing service names as null CPS-1043."() {
        given: 'the cps data service returns a data node from the DMI registry with empty child and leaf attributes'
            def dataNode = new DataNode(childDataNodes:[], leaves: [:])
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry', xpath, INCLUDE_ALL_DESCENDANTS) >> dataNode
        when: 'retrieving the yang modelled cm handle'
            def result = objectUnderTest.getYangModelCmHandle(cmHandleId)
        then: 'the service names ae returned as null'
            result.dmiServiceName == null
            result.dmiDataServiceName == null
            result.dmiModelServiceName == null
    }

    def 'Get a Cm Handle Composite State'() {
        given: 'a valid cm handle id'
            def cmHandleId = 'Some-Cm-Handle'
            def dataNode = new DataNode(leaves: ['cm-handle-state': CmHandleState.ADVISED.name()])
        and: 'cps data service returns a valid data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    '/dmi-registry/cm-handles[@id=\'Some-Cm-Handle\']/state', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        when: 'get cm handle state is invoked'
            def result = objectUnderTest.getCmHandleState(cmHandleId)
        then: 'result has returned the correct cm handle state'
            result.cmHandleState == CmHandleState.ADVISED
    }

    def 'Update Cm Handle with #scenario State'() {
        given: 'a cm handle and a composite state'
            def cmHandleId = 'Some-Cm-Handle'
            def compositeState = new CompositeState(cmHandleState: cmHandleState, lastUpdateTime: formattedDateAndTime)
        when: 'update cm handle state is invoked with the #scenario state'
            objectUnderTest.saveCmHandleState(cmHandleId, compositeState)
        then: 'update node leaves is invoked with the correct params'
            1 * mockCpsDataService.replaceNodeTree('NCMP-Admin', 'ncmp-dmi-registry', '/dmi-registry/cm-handles[@id=\'Some-Cm-Handle\']', expectedJsonData, _ as OffsetDateTime)
        where: 'the following states are used'
            scenario | cmHandleState        || expectedJsonData
            'READY'   | CmHandleState.READY  || '{"state":{"cm-handle-state":"READY","last-update-time":"2022-12-31T20:30:40.000+0000"}}'
            'LOCKED'  | CmHandleState.LOCKED || '{"state":{"cm-handle-state":"LOCKED","last-update-time":"2022-12-31T20:30:40.000+0000"}}'
    }

    def 'Get Cm Handles By State'() {
        given: 'a cm handle state to query'
            def cmHandleState = CmHandleState.ADVISED
        and: 'cps data service returns a list of data nodes'
            mockCpsDataPersistenceService.queryDataNodes('NCMP-Admin', 'ncmp-dmi-registry',
                '//state[@cm-handle-state="ADVISED"]/ancestor::cm-handles', OMIT_DESCENDANTS) >> sampleDataNodes
        when: 'get cm handles by state is invoked'
            def result = objectUnderTest.getCmHandlesByState(cmHandleState)
        then: 'the returned result is a list of data nodes returned by cps data service'
            assert result == sampleDataNodes
    }

    def 'Get Cm Handles By State and Cm-Handle Id'() {
        given: 'a cm handle state to query'
            def cmHandleState = CmHandleState.READY
        and: 'cps data service returns a list of data nodes'
            mockCpsDataPersistenceService.queryDataNodes('NCMP-Admin', 'ncmp-dmi-registry',
                '//cm-handles[@id=\'some-cm-handle\']/state[@cm-handle-state="'+ CmHandleState.READY.name()+'"]/ancestor::cm-handles', OMIT_DESCENDANTS) >> sampleDataNodes
        when: 'get cm handles by state and id is invoked'
            def result = objectUnderTest.getCmHandlesByIdAndState(cmHandleId, cmHandleState)
        then: 'the returned result is a list of data nodes returned by cps data service'
            assert result == sampleDataNodes
    }

    def 'Get Cm Handles By Operational Sync State : UNSYNCHRONIZED'() {
        given: 'a cm handle state to query'
            def cmHandleState = CmHandleState.READY
        and: 'cps data service returns a list of data nodes'
            mockCpsDataPersistenceService.queryDataNodes('NCMP-Admin', 'ncmp-dmi-registry',
                '//state/datastores/operational[@sync-state="'+SyncState.UNSYNCHRONIZED.name()+'"]/ancestor::cm-handles', OMIT_DESCENDANTS) >> sampleDataNodes
        when: 'get cm handles by operational sync state as UNSYNCHRONIZED is invoked'
            def result = objectUnderTest.getCmHandlesByOperationalSyncState(SyncState.UNSYNCHRONIZED)
        then: 'the returned result is a list of data nodes returned by cps data service'
            assert result == sampleDataNodes
    }

    def 'Retrieve cm handle by cps path '() {
        given: 'a cm handle state to query based on the cps path'
            def cmHandleDataNode = new DataNode(xpath: 'xpath', leaves: ['cm-handle-state': CmHandleState.LOCKED.name()])
            def cpsPath = '//cps-path'
        and: 'cps data service returns a valid data node'
            mockCpsDataPersistenceService.queryDataNodes('NCMP-Admin', 'ncmp-dmi-registry',
                    cpsPath, OMIT_DESCENDANTS)
                    >> Arrays.asList(cmHandleDataNode)
        when: 'get cm handles by cps path is invoked'
            def result = objectUnderTest.getCmHandleDataNodesByCpsPath(cpsPath)
        then: 'the returned result is a list of data nodes returned by cps data service'
            assert result.contains(cmHandleDataNode)
    }

}
