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

package org.onap.cps.ncmp.api.impl

import org.onap.cps.cpspath.parser.PathParsingException
import org.onap.cps.ncmp.api.inventory.CmHandleQueries
import org.onap.cps.ncmp.api.inventory.CmHandleQueriesImpl
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.models.CmHandleQueryServiceParameters
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.exceptions.DataInUseException
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.model.ConditionProperties
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification
import java.util.stream.Collectors

class NetworkCmProxyCmHandlerQueryServiceSpec extends Specification {

    def cmHandleQueries = Mock(CmHandleQueries)
    def partiallyMockedCmHandleQueries = Spy(CmHandleQueriesImpl)
    def mockInventoryPersistence = Mock(InventoryPersistence)

    def static someCmHandleDataNode = new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'some-cmhandle-id\']', leaves: ['id':'some-cmhandle-id'])
    def dmiRegistry = new DataNode(xpath: '/dmi-registry', childDataNodes: createDataNodeList(['PNFDemo1', 'PNFDemo2', 'PNFDemo3', 'PNFDemo4']))

    static def queryResultCmHandleMap = createCmHandleMap(['H1', 'H2'])

    def objectUnderTest = new NetworkCmProxyCmHandlerQueryServiceImpl(cmHandleQueries, mockInventoryPersistence)
    def objectUnderTestSpy = new NetworkCmProxyCmHandlerQueryServiceImpl(partiallyMockedCmHandleQueries, mockInventoryPersistence)

    def 'Retrieve cm handles with cpsPath when combined with no Module Query.'() {
        given: 'a cmHandleWithCpsPath condition property'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = createConditionProperties('cmHandleWithCpsPath', [['cpsPath' : '/some/cps/path']])
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        and: 'cmHandleQueries returns a non null query result'
            cmHandleQueries.queryCmHandleDataNodesByCpsPath('/some/cps/path', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> [new DataNode(leaves: ['id':'some-cmhandle-id'])]
        and: 'CmHandleQueries returns cmHandles with the relevant query result'
            cmHandleQueries.combineCmHandleQueries(*_) >> ['PNFDemo1': new NcmpServiceCmHandle(cmHandleId: 'PNFDemo1'), 'PNFDemo3': new NcmpServiceCmHandle(cmHandleId: 'PNFDemo3')]
        when: 'the query is executed for both cm handle ids and details'
            def returnedCmHandlesJustIds = objectUnderTest.queryCmHandleIds(cmHandleQueryParameters)
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles ids are returned'
            returnedCmHandlesJustIds == ['PNFDemo1', 'PNFDemo3'] as Set
        and: 'the correct ncmp service cm handles are returned'
            returnedCmHandlesWithData.stream().map(CmHandle -> CmHandle.cmHandleId).collect(Collectors.toSet()) == ['PNFDemo1', 'PNFDemo3'] as Set
    }

    def 'Retrieve cm handles with cpsPath where #scenario.'() {
        given: 'a cmHandleWithCpsPath condition property'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = createConditionProperties('cmHandleWithCpsPath', [['cpsPath' : '/some/cps/path']])
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        and: 'cmHandleQueries throws a path parsing exception'
            cmHandleQueries.queryCmHandleDataNodesByCpsPath('/some/cps/path', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> { throw thrownException }
        when: 'the query is executed for both cm handle ids and details'
            objectUnderTest.queryCmHandleIds(cmHandleQueryParameters)
            objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'a data validation exception is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                           | thrownException                                          || expectedException
            'a PathParsingException is thrown' | new PathParsingException('some message', 'some details') || DataValidationException
            'any other Exception is thrown'    | new DataInUseException('some message', 'some details')   || DataInUseException
    }

    def 'Query cm handles with public properties when combined with empty modules query result.'() {
        given: 'a public properties condition property'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = createConditionProperties('hasAllProperties', [['some-property-key': 'some-property-value']])
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        and: 'CmHandleQueries returns cmHandles with the relevant query result'
            cmHandleQueries.combineCmHandleQueries(*_) >> ['PNFDemo1': new NcmpServiceCmHandle(cmHandleId: 'PNFDemo1'), 'PNFDemo3': new NcmpServiceCmHandle(cmHandleId: 'PNFDemo3')]
        when: 'the query is executed for both cm handle ids and details'
            def returnedCmHandlesJustIds = objectUnderTest.queryCmHandleIds(cmHandleQueryParameters)
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles ids are returned'
            returnedCmHandlesJustIds == ['PNFDemo1', 'PNFDemo3'] as Set
        and: 'the correct cm handle data objects are returned'
            returnedCmHandlesWithData.stream().map(dataNode -> dataNode.cmHandleId).collect(Collectors.toSet()) == ['PNFDemo1', 'PNFDemo3'] as Set
    }

    def 'Retrieve cm handles with module names when #scenario from query.'() {
        given: 'a modules condition property'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = createConditionProperties('hasAllModules', [['moduleName': 'some-module-name']])
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        and: 'null is returned from the state and public property queries'
            cmHandleQueries.combineCmHandleQueries(*_) >> null
        and: '#scenario from the modules query'
            mockInventoryPersistence.getCmHandleIdsWithGivenModules(*_) >> cmHandleIdsFromService
        and: 'the same cmHandles are returned from the persistence service layer'
            cmHandleIdsFromService.size() * mockInventoryPersistence.getDataNode(*_) >> returnedCmHandles
        when: 'the query is executed for both cm handle ids and details'
            def returnedCmHandlesJustIds = objectUnderTest.queryCmHandleIds(cmHandleQueryParameters)
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles ids are returned'
            returnedCmHandlesJustIds == cmHandleIdsFromService as Set
        and: 'the correct cm handle data objects are returned'
            returnedCmHandlesWithData.stream().map(dataNode -> dataNode.cmHandleId).collect(Collectors.toSet()) == cmHandleIdsFromService as Set
        where: 'the following data is used'
            scenario                  | cmHandleIdsFromService | returnedCmHandles
            'One anchor returned'     | ['some-cmhandle-id']   | someCmHandleDataNode
            'No anchors are returned' | []                     | null
    }

    def 'Retrieve cm handles with combined queries when #scenario.'() {
        given: 'all condition properties used'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionPubProps = createConditionProperties('hasAllProperties', [['some-property-key': 'some-property-value']])
            def conditionModules = createConditionProperties('hasAllModules', [['moduleName': 'some-module-name']])
            def conditionState = createConditionProperties('cmHandleWithCpsPath', [['cpsPath' : '/some/cps/path']])
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionPubProps, conditionModules, conditionState])
        and: 'cmHandles are returned from the state and public property combined queries'
            cmHandleQueries.combineCmHandleQueries(*_) >> combinedQueryMap
        and: 'cmHandles are returned from the module names query'
            mockInventoryPersistence.getCmHandleIdsWithGivenModules(['some-module-name']) >> anchorsForModuleQuery
        and: 'cmHandleQueries returns a datanode result'
            2 * cmHandleQueries.queryCmHandleDataNodesByCpsPath(*_) >> [someCmHandleDataNode]
        when: 'the query is executed for both cm handle ids and details'
            def returnedCmHandlesJustIds = objectUnderTest.queryCmHandleIds(cmHandleQueryParameters)
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles ids are returned'
            returnedCmHandlesJustIds == expectedCmHandleIds as Set
        and: 'the correct cm handle data objects are returned'
            returnedCmHandlesWithData.stream().map(dataNode -> dataNode.cmHandleId).collect(Collectors.toSet()) == expectedCmHandleIds as Set
        where: 'the following data is used'
            scenario                                 | combinedQueryMap                                                                                                           | anchorsForModuleQuery    || expectedCmHandleIds
            'combined and modules queries intersect' | ['PNFDemo1': new NcmpServiceCmHandle(cmHandleId: 'PNFDemo1')]                                                              | ['PNFDemo1', 'PNFDemo2'] || ['PNFDemo1']
            'only module query results exist'        | [:]                                                                                                                        | ['PNFDemo1', 'PNFDemo2'] || []
            'only combined query results exist'      | ['PNFDemo1': new NcmpServiceCmHandle(cmHandleId: 'PNFDemo1'), 'PNFDemo2': new NcmpServiceCmHandle(cmHandleId: 'PNFDemo2')] | []                       || []
            'neither queries return results'         | [:]                                                                                                                        | []                       || []
            'none intersect'                         | ['PNFDemo1': new NcmpServiceCmHandle(cmHandleId: 'PNFDemo1')]                                                              | ['PNFDemo2']             || []
    }

    def 'Retrieve cm handles when the query is empty.'() {
        given: 'We use an empty query'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
        and: 'the inventory persistence returns the dmi registry datanode with just ids'
            mockInventoryPersistence.getDataNode("/dmi-registry", FetchDescendantsOption.FETCH_DIRECT_CHILDREN_ONLY) >> dmiRegistry
        and: 'the inventory persistence returns the dmi registry datanode with data'
            mockInventoryPersistence.getDataNode("/dmi-registry") >> dmiRegistry
        when: 'the query is executed for both cm handle ids and details'
            def returnedCmHandlesJustIds = objectUnderTest.queryCmHandleIds(cmHandleQueryParameters)
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles are returned'
            returnedCmHandlesJustIds == ['PNFDemo1', 'PNFDemo2', 'PNFDemo3', 'PNFDemo4'] as Set
            returnedCmHandlesWithData.stream().map(d -> d.cmHandleId).collect(Collectors.toSet()) == ['PNFDemo1', 'PNFDemo2', 'PNFDemo3', 'PNFDemo4'] as Set
    }


    def 'Retrieve all CMHandleIds for empty query parameters' () {
        given: 'We query without any parameters'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
        and: 'the inventoryPersistence returns all four CmHandleIds'
            mockInventoryPersistence.getDataNode(*_) >> dmiRegistry
        when: 'the query executed'
            def resultSet = objectUnderTest.queryCmHandleIdsForInventory(cmHandleQueryParameters)
        then: 'the size of the result list equals the size of all cmHandleIds.'
            resultSet.size() == 4
    }

    def 'Retrieve CMHandleIds when #scenario.' () {
        given: 'a query object created with #condition'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = createConditionProperties(conditionName, [['some-key': 'some-value']])
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        and: 'the inventoryPersistence returns different CmHandleIds'
            partiallyMockedCmHandleQueries.queryCmHandlePublicProperties(*_) >> cmHandlesWithMatchingPublicProperties
            partiallyMockedCmHandleQueries.queryCmHandleAdditionalProperties(*_) >> cmHandlesWithMatchingPrivateProperties
        when: 'the query executed'
            def result = objectUnderTestSpy.queryCmHandleIdsForInventory(cmHandleQueryParameters)
        then: 'the expected number of results are returned.'
            assert result.size() == expectedCmHandleIdsSize
        where: 'the following data is used'
            scenario                                          | conditionName                | cmHandlesWithMatchingPublicProperties | cmHandlesWithMatchingPrivateProperties || expectedCmHandleIdsSize
            'all properties, only public matching'            | 'hasAllProperties'           | queryResultCmHandleMap                |  null                                  || 2
            'all properties, no matching cm handles'          | 'hasAllProperties'           | [:]                                   |  [:]                                   || 0
            'additional properties, some matching cm handles' | 'hasAllAdditionalProperties' | [:]                                   | queryResultCmHandleMap                 || 2
            'additional properties, no matching cm handles'   | 'hasAllAdditionalProperties' | null                                  |  [:]                                   || 0
    }

    def 'Retrieve CMHandleIds by different DMI properties with #scenario.' () {
        given: 'a query object created with dmi plugin as condition'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = createConditionProperties('cmHandleWithDmiPlugin', [['some-key': 'some-value']])
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        and: 'the inventoryPersistence returns different CmHandleIds'
            partiallyMockedCmHandleQueries.getCmHandlesByDmiPluginIdentifier(*_) >> cmHandleQueryResult
        when: 'the query executed'
            def result = objectUnderTestSpy.queryCmHandleIdsForInventory(cmHandleQueryParameters)
        then: 'the expected number of results are returned.'
            assert result.size() == expectedCmHandleIdsSize
        where: 'the following data is used'
            scenario       | cmHandleQueryResult             || expectedCmHandleIdsSize
            'some matches' | queryResultCmHandleMap.values() || 2
            'no matches'   | []                              || 0
    }

    static def createCmHandleMap(cmHandleIds) {
        def cmHandleMap = [:]
        cmHandleIds.each{ cmHandleMap[it] = new NcmpServiceCmHandle(cmHandleId : it) }
        return cmHandleMap
    }

    def createConditionProperties(String conditionName, List<Map<String, String>> conditionParameters) {
        return new ConditionProperties(conditionName : conditionName, conditionParameters : conditionParameters)
    }

    def static createDataNodeList(dataNodeIds) {
        def dataNodes =[]
        dataNodeIds.each{ dataNodes << new DataNode(xpath: "/dmi-registry/cm-handles[@id='${it}']", leaves: ['id':it]) }
        return dataNodes
    }
}
