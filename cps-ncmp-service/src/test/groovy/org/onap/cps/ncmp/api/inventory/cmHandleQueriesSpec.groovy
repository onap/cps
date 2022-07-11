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

class cmHandleQueriesSpec extends Specification {
    def cpsDataPersistenceService = Mock(CpsDataPersistenceService)

    CmHandleQueries objectUnderTest = new CmHandleQueries(cpsDataPersistenceService)

    @Shared
    def static sampleDataNodes = [new DataNode()]

    def static pNFDemo1 = new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'PNFDemo1\']', leaves: ['id':'PNFDemo1'])
    def static pNFDemo2 = new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'PNFDemo2\']', leaves: ['id':'PNFDemo2'])
    def static pNFDemo3 = new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'PNFDemo3\']', leaves: ['id':'PNFDemo3'])
    def static pNFDemo4 = new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'PNFDemo4\']', leaves: ['id':'PNFDemo4'])

    def static pNFDemo1CmHandle = new NcmpServiceCmHandle(cmHandleId: 'PNFDemo1')
    def static pNFDemo2CmHandle = new NcmpServiceCmHandle(cmHandleId: 'PNFDemo2')
    def static pNFDemo3CmHandle = new NcmpServiceCmHandle(cmHandleId: 'PNFDemo3')

    def 'Query CmHandles with public properties query pair.'() {
        given: 'the DataNodes queried for a given cpsPath are returned from the persistence service layer.'
            mockResponses()
        when: 'a query on cmhandle public properties is performed with a public property pair'
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandlePublicProperties(publicPropertyPairs)
        then: 'the correct cm handle data objects are returned'
            returnedCmHandlesWithData.keySet().containsAll(expectedCmHandleIds)
            returnedCmHandlesWithData.keySet().size() == expectedCmHandleIds.size()
        where: 'the following data is used'
            scenario                         | publicPropertyPairs                                                                           || expectedCmHandleIds
            'single property matches'        | ['Contact' : 'newemailforstore@bookstore.com']                                                || ['PNFDemo1', 'PNFDemo2', 'PNFDemo4']
            'public property does not match' | ['wont_match' : 'wont_match']                                                                 || []
            '2 properties, only one match'   | ['Contact' : 'newemailforstore@bookstore.com', 'Contact2': 'newemailforstore2@bookstore.com'] || ['PNFDemo4']
            '2 properties, no matches'       | ['Contact' : 'newemailforstore@bookstore.com', 'Contact2': '']                                || []
    }

    def 'Query CmHandles using empty public properties query pair.'() {
        when: 'a query on CmHandle public properties is executed using an empty map'
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandlePublicProperties([:])
        then: 'no cm handle data objects are returned'
            returnedCmHandlesWithData.keySet().size() == 0
    }

    def 'Combine two query results where #scenario.'() {
        given: 'two query results in the form of a map of NcmpServiceCmHandles'
        when: 'the query results are combined into a single query result'
            def result = objectUnderTest.combineCmHandleQueries(firstQuery, secondQuery)
        then: 'the returned map consists of the expectedResult'
            result == expectedResult
        where:
            scenario                                                     | firstQuery                                                     | secondQuery                                                    || expectedResult
            'two queries with unique and non unique entries exist'       | ['PNFDemo1' : pNFDemo1CmHandle, 'PNFDemo2' : pNFDemo2CmHandle] | ['PNFDemo1' : pNFDemo1CmHandle, 'PNFDemo3' : pNFDemo3CmHandle] || ['PNFDemo1' : pNFDemo1CmHandle]
            'the first query contains entries and second query is empty' | ['PNFDemo1' : pNFDemo1CmHandle, 'PNFDemo2' : pNFDemo2CmHandle] | [:]                                                            || [:]
            'the second query contains entries and first query is empty' | [:]                                                            | ['PNFDemo1' : pNFDemo1CmHandle, 'PNFDemo3' : pNFDemo3CmHandle] || [:]
            'the first query contains entries and second query is null'  | ['PNFDemo1' : pNFDemo1CmHandle, 'PNFDemo2' : pNFDemo2CmHandle] | null                                                           || ['PNFDemo1' : pNFDemo1CmHandle, 'PNFDemo2' : pNFDemo2CmHandle]
            'the second query contains entries and first query is null'  | null                                                           | ['PNFDemo1' : pNFDemo1CmHandle, 'PNFDemo3' : pNFDemo3CmHandle] || ['PNFDemo1' : pNFDemo1CmHandle, 'PNFDemo3' : pNFDemo3CmHandle]
            'both queries are empty'                                     | [:]                                                            | [:]                                                            || [:]
            'both queries are null'                                      | null                                                           | null                                                           || [:]
    }

    def 'Get Cm Handles By State'() {
        given: 'a cm handle state to query'
            def cmHandleState = CmHandleState.ADVISED
        and: 'cps data service returns a list of data nodes'
            cpsDataPersistenceService.queryDataNodes('NCMP-Admin', 'ncmp-dmi-registry',
                '//state[@cm-handle-state="ADVISED"]/ancestor::cm-handles', INCLUDE_ALL_DESCENDANTS) >> sampleDataNodes
        when: 'get cm handles by state is invoked'
            def result = objectUnderTest.getCmHandlesByState(cmHandleState)
        then: 'the returned result is a list of data nodes returned by cps data service'
            assert result == sampleDataNodes
    }

    def 'Get Cm Handles By State and Cm-Handle Id'() {
        given: 'a cm handle state to query'
            def cmHandleState = CmHandleState.READY
        and: 'cps data service returns a list of data nodes'
            cpsDataPersistenceService.queryDataNodes('NCMP-Admin', 'ncmp-dmi-registry',
                '//cm-handles[@id=\'some-cm-handle\']/state[@cm-handle-state="'+ 'READY'+'"]/ancestor::cm-handles', OMIT_DESCENDANTS) >> sampleDataNodes
        when: 'get cm handles by state and id is invoked'
            def result = objectUnderTest.getCmHandlesByIdAndState('some-cm-handle', cmHandleState)
        then: 'the returned result is a list of data nodes returned by cps data service'
            assert result == sampleDataNodes
    }

    def 'Get Cm Handles By Operational Sync State : UNSYNCHRONIZED'() {
        given: 'a cm handle state to query'
            def cmHandleState = CmHandleState.READY
        and: 'cps data service returns a list of data nodes'
            cpsDataPersistenceService.queryDataNodes('NCMP-Admin', 'ncmp-dmi-registry',
                '//state/datastores/operational[@sync-state="'+'UNSYNCHRONIZED'+'"]/ancestor::cm-handles', OMIT_DESCENDANTS) >> sampleDataNodes
        when: 'get cm handles by operational sync state as UNSYNCHRONIZED is invoked'
            def result = objectUnderTest.getCmHandlesByOperationalSyncState(DataStoreSyncState.UNSYNCHRONIZED)
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
            def result = objectUnderTest.getCmHandleDataNodesByCpsPath(cpsPath, INCLUDE_ALL_DESCENDANTS)
        then: 'the returned result is a list of data nodes returned by cps data service'
            assert result.contains(cmHandleDataNode)
    }

    void mockResponses() {
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\"Contact\" and @value=\"newemailforstore@bookstore.com\"]/ancestor::cm-handles', _) >> [pNFDemo1, pNFDemo2, pNFDemo4]
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\"wont_match\" and @value=\"wont_match\"]/ancestor::cm-handles', _) >> []
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\"Contact2\" and @value=\"newemailforstore2@bookstore.com\"]/ancestor::cm-handles', _) >> [pNFDemo4]
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\"Contact2\" and @value=\"\"]/ancestor::cm-handles', _) >> []
        cpsDataPersistenceService.queryDataNodes(_, _, '//state[@cm-handle-state=\"READY\"]/ancestor::cm-handles', _) >> [pNFDemo1, pNFDemo3]
        cpsDataPersistenceService.queryDataNodes(_, _, '//state[@cm-handle-state=\"LOCKED\"]/ancestor::cm-handles', _) >> [pNFDemo2, pNFDemo4]
    }
}
