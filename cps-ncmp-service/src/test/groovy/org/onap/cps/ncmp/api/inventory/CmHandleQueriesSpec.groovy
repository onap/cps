/*
 *  ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.api.inventory

import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.model.DataNode
import spock.lang.Shared
import spock.lang.Specification

import static org.onap.cps.spi.FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS
import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS

class CmHandleQueriesSpec extends Specification {
    def cpsDataPersistenceService = Mock(CpsDataPersistenceService)

    def objectUnderTest = new CmHandleQueries(cpsDataPersistenceService)

    @Shared
    def static sampleDataNodes = [new DataNode()]

    def static pnfDemo = createDataNode('PNFDemo')
    def static pnfDemo2 = createDataNode('PNFDemo2')
    def static pnfDemo3 = createDataNode('PNFDemo3')
    def static pnfDemo4 = createDataNode('PNFDemo4')

    def static pnfDemoCmHandle = new NcmpServiceCmHandle(cmHandleId: 'PNFDemo')
    def static pnfDemo2CmHandle = new NcmpServiceCmHandle(cmHandleId: 'PNFDemo2')
    def static pnfDemo3CmHandle = new NcmpServiceCmHandle(cmHandleId: 'PNFDemo3')

    def 'Query CmHandles with public properties query pair.'() {
        given: 'the DataNodes queried for a given cpsPath are returned from the persistence service.'
            mockResponses()
        when: 'a query on cmhandle public properties is performed with a public property pair'
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandlePublicProperties(publicPropertyPairs)
        then: 'the correct cm handle data objects are returned'
            returnedCmHandlesWithData.keySet().containsAll(expectedCmHandleIds)
            returnedCmHandlesWithData.keySet().size() == expectedCmHandleIds.size()
        where: 'the following data is used'
            scenario                         | publicPropertyPairs                                                                           || expectedCmHandleIds
            'single property matches'        | ['Contact' : 'newemailforstore@bookstore.com']                                                || ['PNFDemo', 'PNFDemo2', 'PNFDemo4']
            'public property does not match' | ['wont_match' : 'wont_match']                                                                 || []
            '2 properties, only one match'   | ['Contact' : 'newemailforstore@bookstore.com', 'Contact2': 'newemailforstore2@bookstore.com'] || ['PNFDemo4']
            '2 properties, no matches'       | ['Contact' : 'newemailforstore@bookstore.com', 'Contact2': '']                                || []
    }

    def 'Query CmHandles using empty public properties query pair.'() {
        when: 'a query on CmHandle public properties is executed using an empty map'
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandlePublicProperties([:])
        then: 'no cm handles are returned'
            returnedCmHandlesWithData.keySet().size() == 0
    }

    def 'Combine two query results where #scenario.'() {
        when: 'two query results in the form of a map of NcmpServiceCmHandles are combined into a single query result'
            def result = objectUnderTest.combineCmHandleQueries(firstQuery, secondQuery)
        then: 'the returned result is the same as the expected result'
            result == expectedResult
        where:
            scenario                                                     | firstQuery                                                 | secondQuery                                                || expectedResult
            'two queries with unique and non unique entries exist'       | ['PNFDemo': pnfDemoCmHandle, 'PNFDemo2': pnfDemo2CmHandle] | ['PNFDemo': pnfDemoCmHandle, 'PNFDemo3': pnfDemo3CmHandle] || ['PNFDemo': pnfDemoCmHandle]
            'the first query contains entries and second query is empty' | ['PNFDemo': pnfDemoCmHandle, 'PNFDemo2': pnfDemo2CmHandle] | [:]                                                        || [:]
            'the second query contains entries and first query is empty' | [:]                                                        | ['PNFDemo': pnfDemoCmHandle, 'PNFDemo3': pnfDemo3CmHandle] || [:]
            'the first query contains entries and second query is null'  | ['PNFDemo': pnfDemoCmHandle, 'PNFDemo2': pnfDemo2CmHandle] | null                                                       || ['PNFDemo': pnfDemoCmHandle, 'PNFDemo2': pnfDemo2CmHandle]
            'the second query contains entries and first query is null'  | null                                                       | ['PNFDemo': pnfDemoCmHandle, 'PNFDemo3': pnfDemo3CmHandle] || ['PNFDemo': pnfDemoCmHandle, 'PNFDemo3': pnfDemo3CmHandle]
            'both queries are empty'                                     | [:]                                                        | [:]                                                        || [:]
            'both queries are null'                                      | null                                                       | null                                                       || null
    }

    def 'Get Cm Handles By State'() {
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

    def 'Get Cm Handles state by Cm-Handle Id'() {
        given: 'a cm handle state to query'
            def cmHandleState = CmHandleState.READY
        and: 'cps data service returns a list of data nodes'
            cpsDataPersistenceService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                '/dmi-registry/cm-handles[@id=\'some-cm-handle\']/state', OMIT_DESCENDANTS) >> new DataNode(leaves: ['cm-handle-state': 'READY'])
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
        when: 'cm Handle Ids are fetched for a given dmi plugin identifier'
            def result = objectUnderTest.getCmHandlesByDmiPluginIdentifier('my-dmi-plugin-identifier')
        then: 'result is the correct size'
            assert result.size() == 4
        and: 'result contains the correct cm handle IDs'
            assert result.cmHandleId.containsAll('PNFDemo', 'PNFDemo2', 'PNFDemo3', 'PNFDemo4')
    }

    void mockResponses() {
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\"Contact\" and @value=\"newemailforstore@bookstore.com\"]/ancestor::cm-handles', _) >> [pnfDemo, pnfDemo2, pnfDemo4]
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\"wont_match\" and @value=\"wont_match\"]/ancestor::cm-handles', _) >> []
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\"Contact2\" and @value=\"newemailforstore2@bookstore.com\"]/ancestor::cm-handles', _) >> [pnfDemo4]
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\"Contact2\" and @value=\"\"]/ancestor::cm-handles', _) >> []
        cpsDataPersistenceService.queryDataNodes(_, _, '//state[@cm-handle-state=\"READY\"]/ancestor::cm-handles', _) >> [pnfDemo, pnfDemo3]
        cpsDataPersistenceService.queryDataNodes(_, _, '//state[@cm-handle-state=\"LOCKED\"]/ancestor::cm-handles', _) >> [pnfDemo2, pnfDemo4]
        cpsDataPersistenceService.queryDataNodes('NCMP-Admin','ncmp-dmi-registry','/dmi-registry/cm-handles[@dmi-service-name=\'my-dmi-plugin-identifier\']',OMIT_DESCENDANTS) >> [pnfDemo, pnfDemo2]
        cpsDataPersistenceService.queryDataNodes('NCMP-Admin','ncmp-dmi-registry','/dmi-registry/cm-handles[@dmi-data-service-name=\'my-dmi-plugin-identifier\']',OMIT_DESCENDANTS) >> [pnfDemo3,pnfDemo4]
        cpsDataPersistenceService.queryDataNodes('NCMP-Admin','ncmp-dmi-registry','/dmi-registry/cm-handles[@dmi-model-service-name=\'my-dmi-plugin-identifier\']',OMIT_DESCENDANTS) >> [pnfDemo2,pnfDemo4]
    }

    def static createDataNode(dataNodeId) {
        return new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'' + dataNodeId + '\']', leaves: ['id':dataNodeId])
    }
}
