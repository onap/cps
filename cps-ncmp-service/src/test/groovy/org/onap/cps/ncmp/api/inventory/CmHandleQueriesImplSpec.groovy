/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
 *  Modifications Copyright (C) 2023 TechMahindra Ltd.
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

import com.hazelcast.map.IMap
import org.onap.cps.ncmp.api.impl.trustlevel.TrustLevel
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.model.DataNode
import spock.lang.Shared
import spock.lang.Specification
import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class CmHandleQueriesImplSpec extends Specification {
    def cpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def mockTrustLevelPerCmHandle = Mock(IMap<String, TrustLevel>)

    def objectUnderTest = new CmHandleQueriesImpl(cpsDataPersistenceService, mockTrustLevelPerCmHandle)

    @Shared
    def static sampleDataNodes = [new DataNode()]

    def dataNodeWithPrivateField = '//additional-properties[@name=\"Contact3\" and @value=\"newemailforstore3@bookstore.com\"]/ancestor::cm-handles'

    def static pnfDemo = createDataNode('PNFDemo')
    def static pnfDemo2 = createDataNode('PNFDemo2')
    def static pnfDemo3 = createDataNode('PNFDemo3')
    def static pnfDemo4 = createDataNode('PNFDemo4')
    def static pnfDemo5 = createDataNode('PNFDemo5')

    def 'Query CmHandles with public properties query pair.'() {
        given: 'the DataNodes queried for a given cpsPath are returned from the persistence service.'
            mockResponses()
        when: 'a query on cmhandle public properties is performed with a public property pair'
            def result = objectUnderTest.queryCmHandlePublicProperties(publicPropertyPairs)
        then: 'the correct cm handle data objects are returned'
            result.containsAll(expectedCmHandleIds)
            result.size() == expectedCmHandleIds.size()
        where: 'the following data is used'
            scenario                         | publicPropertyPairs                                                                           || expectedCmHandleIds
            'single property matches'        | ['Contact' : 'newemailforstore@bookstore.com']                                                || ['PNFDemo', 'PNFDemo2', 'PNFDemo4']
            'public property does not match' | ['wont_match' : 'wont_match']                                                                 || []
            '2 properties, only one match'   | ['Contact' : 'newemailforstore@bookstore.com', 'Contact2': 'newemailforstore2@bookstore.com'] || ['PNFDemo4']
            '2 properties, no matches'       | ['Contact' : 'newemailforstore@bookstore.com', 'Contact2': '']                                || []
    }

    def 'Query cm handles with trust level complete'() {
        given: 'The hazelcast cache return a cm handle with some trust levels'
            def entry = Map.entry('cmhandle1', TrustLevel.COMPLETE)
            def entry2= Map.entry('cmhandle2', TrustLevel.NONE)
            mockTrustLevelPerCmHandle.entrySet() >> [entry, entry2]
        and: 'the query property is trust level complete'
            def trustLevelPropertyQueryPairs = ['trustLevel' : trustLevel.toString()] as Map
        when: 'the query is being executed'
            def result = objectUnderTest.queryCmHandlesByTrustLevel(trustLevelPropertyQueryPairs)
        then: 'the expected cm handles are returned successfully'
            result == expectedResult
        where: 'the following values are used'
            scenario                                   |   trustLevel                 || expectedResult
            'some-dmi have trust level complete'       |   TrustLevel.COMPLETE        || ['cmhandle1'] as Set
            'some-dmi have trust level none'           |   TrustLevel.NONE            || ['cmhandle2'] as Set
    }

    def 'Query CmHandles using empty public properties query pair.'() {
        when: 'a query on CmHandle public properties is executed using an empty map'
            def result = objectUnderTest.queryCmHandlePublicProperties([:])
        then: 'no cm handles are returned'
            result.size() == 0
    }

    def 'Query CmHandles using empty private properties query pair.'() {
        when: 'a query on CmHandle private properties is executed using an empty map'
            def result = objectUnderTest.queryCmHandleAdditionalProperties([:])
        then: 'no cm handles are returned'
            result.size() == 0
    }

    def 'Query CmHandles by a private field\'s value.'() {
        given: 'a data node exists with a certain additional-property'
            cpsDataPersistenceService.queryDataNodes(_, _, dataNodeWithPrivateField, _) >> [pnfDemo5]
        when: 'a query on CmHandle private properties is executed using a map'
            def result = objectUnderTest.queryCmHandleAdditionalProperties(['Contact3': 'newemailforstore3@bookstore.com'])
        then: 'one cm handle is returned'
            result.size() == 1
    }

    def 'Get CmHandles by it\'s state.'() {
        given: 'a cm handle state to query'
            def cmHandleState = CmHandleState.ADVISED
        and: 'the persistence service returns a list of data nodes'
            cpsDataPersistenceService.queryDataNodes('NCMP-Admin', 'ncmp-dmi-registry',
                '//state[@cm-handle-state="ADVISED"]/ancestor::cm-handles', INCLUDE_ALL_DESCENDANTS) >> sampleDataNodes
        when: 'cm handles are fetched by state'
            def result = objectUnderTest.queryCmHandlesByState(cmHandleState)
        then: 'the returned result matches the result from the persistence service'
            assert result == sampleDataNodes
    }

    def 'Check the state of a cmHandle when #scenario.'() {
        given: 'a cm handle state to compare'
            def cmHandleState = state
        and: 'the persistence service returns a list of data nodes'
            cpsDataPersistenceService.getDataNodes('NCMP-Admin', 'ncmp-dmi-registry',
                    '/dmi-registry/cm-handles[@id=\'some-cm-handle\']/state', OMIT_DESCENDANTS) >> [new DataNode(leaves: ['cm-handle-state': 'READY'])]
        when: 'cm handles are compared by state'
            def result = objectUnderTest.cmHandleHasState('some-cm-handle', cmHandleState)
        then: 'the returned result matches the expected result from the persistence service'
            result == expectedResult
        where:
            scenario                           | state                 || expectedResult
            'the provided state matches'       | CmHandleState.READY   || true
            'the provided state does not match'| CmHandleState.DELETED || false
    }

    def 'Get Cm Handles state by Cm-Handle Id'() {
        given: 'a cm handle state to query'
            def cmHandleState = CmHandleState.READY
        and: 'cps data service returns a list of data nodes'
            cpsDataPersistenceService.getDataNodes('NCMP-Admin', 'ncmp-dmi-registry',
                '/dmi-registry/cm-handles[@id=\'some-cm-handle\']/state', OMIT_DESCENDANTS) >> [new DataNode(leaves: ['cm-handle-state': 'READY'])]
        when: 'cm handles are fetched by state and id'
            def result = objectUnderTest.getCmHandleState('some-cm-handle')
        then: 'the returned result is a list of data nodes returned by cps data service'
            assert result == new DataNode(leaves: ['cm-handle-state': 'READY'])
    }

    def 'Retrieve Cm Handles By Operational Sync State : UNSYNCHRONIZED'() {
        given: 'a cm handle state to query'
            def cmHandleState = CmHandleState.READY
        and: 'cps data service returns a list of data nodes'
            cpsDataPersistenceService.queryDataNodes('NCMP-Admin', 'ncmp-dmi-registry',
                '//state/datastores/operational[@sync-state="'+'UNSYNCHRONIZED'+'"]/ancestor::cm-handles', OMIT_DESCENDANTS) >> sampleDataNodes
        when: 'cm handles are fetched by the UNSYNCHRONIZED operational sync state'
            def result = objectUnderTest.queryCmHandlesByOperationalSyncState(DataStoreSyncState.UNSYNCHRONIZED)
        then: 'the returned result is a list of data nodes returned by cps data service'
            assert result == sampleDataNodes
    }

    def 'Retrieve cm handle by cps path '() {
        given: 'a cm handle state to query based on the cps path'
            def cmHandleDataNode = new DataNode(xpath: 'xpath', leaves: ['cm-handle-state': 'LOCKED'])
            def cpsPath = '//cps-path'
        and: 'cps data service returns a valid data node'
            cpsDataPersistenceService.queryDataNodes('NCMP-Admin', 'ncmp-dmi-registry',
                cpsPath + '/ancestor::cm-handles', INCLUDE_ALL_DESCENDANTS)
                >> Arrays.asList(cmHandleDataNode)
        when: 'get cm handles by cps path is invoked'
            def result = objectUnderTest.queryCmHandleDataNodesByCpsPath(cpsPath, INCLUDE_ALL_DESCENDANTS)
        then: 'the returned result is a list of data nodes returned by cps data service'
            assert result.contains(cmHandleDataNode)
    }

    def 'Get all cm handles by dmi plugin identifier'() {
        given: 'the DataNodes queried for a given cpsPath are returned from the persistence service.'
            mockResponses()
        when: 'cm Handles are fetched for a given dmi plugin identifier'
            def result = objectUnderTest.getCmHandleIdsByDmiPluginIdentifier('my-dmi-plugin-identifier')
        then: 'result is the correct size'
            assert result.size() == 3
        and: 'result contains the correct cm handles'
            assert result.containsAll('PNFDemo', 'PNFDemo2', 'PNFDemo4')
    }

    void mockResponses() {
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\"Contact\" and @value=\"newemailforstore@bookstore.com\"]/ancestor::cm-handles', _) >> [pnfDemo, pnfDemo2, pnfDemo4]
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\"wont_match\" and @value=\"wont_match\"]/ancestor::cm-handles', _) >> []
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\"Contact2\" and @value=\"newemailforstore2@bookstore.com\"]/ancestor::cm-handles', _) >> [pnfDemo4]
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\"Contact2\" and @value=\"\"]/ancestor::cm-handles', _) >> []
        cpsDataPersistenceService.queryDataNodes(_, _, '//state[@cm-handle-state=\"READY\"]/ancestor::cm-handles', _) >> [pnfDemo, pnfDemo3]
        cpsDataPersistenceService.queryDataNodes(_, _, '//state[@cm-handle-state=\"LOCKED\"]/ancestor::cm-handles', _) >> [pnfDemo2, pnfDemo4]
        cpsDataPersistenceService.queryDataNodes('NCMP-Admin','ncmp-dmi-registry','/dmi-registry/cm-handles[@dmi-service-name=\'my-dmi-plugin-identifier\']',OMIT_DESCENDANTS) >> [pnfDemo, pnfDemo2]
        cpsDataPersistenceService.queryDataNodes('NCMP-Admin','ncmp-dmi-registry','/dmi-registry/cm-handles[@dmi-data-service-name=\'my-dmi-plugin-identifier\']',OMIT_DESCENDANTS) >> [pnfDemo,pnfDemo4]
        cpsDataPersistenceService.queryDataNodes('NCMP-Admin','ncmp-dmi-registry','/dmi-registry/cm-handles[@dmi-model-service-name=\'my-dmi-plugin-identifier\']',OMIT_DESCENDANTS) >> [pnfDemo2,pnfDemo4]
    }

    def static createDataNode(dataNodeId) {
        return new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'' + dataNodeId + '\']', leaves: ['id':dataNodeId])
    }

}
