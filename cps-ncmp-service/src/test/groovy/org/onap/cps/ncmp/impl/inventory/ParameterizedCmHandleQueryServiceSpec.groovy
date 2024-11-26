/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
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

package org.onap.cps.ncmp.impl.inventory

import org.onap.cps.cpspath.parser.PathParsingException
import org.onap.cps.ncmp.api.inventory.models.CmHandleQueryServiceParameters
import org.onap.cps.ncmp.api.inventory.models.NcmpServiceCmHandle
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.onap.cps.api.parameters.FetchDescendantsOption
import org.onap.cps.api.exceptions.DataInUseException
import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.api.model.ConditionProperties
import org.onap.cps.api.model.DataNode
import spock.lang.Specification

import static org.onap.cps.ncmp.impl.inventory.NcmpPersistence.NCMP_DMI_REGISTRY_PARENT

class ParameterizedCmHandleQueryServiceSpec extends Specification {

    def cmHandleQueries = Mock(CmHandleQueryService)
    def partiallyMockedCmHandleQueries = Spy(CmHandleQueryService)
    def mockInventoryPersistence = Mock(InventoryPersistence)

    def dmiRegistry = new DataNode(xpath: NCMP_DMI_REGISTRY_PARENT, childDataNodes: createDataNodeList(['PNFDemo1', 'PNFDemo2', 'PNFDemo3', 'PNFDemo4']))

    def objectUnderTest = new ParameterizedCmHandleQueryServiceImpl(cmHandleQueries, mockInventoryPersistence)
    def objectUnderTestWithPartiallyMockedQueries = new ParameterizedCmHandleQueryServiceImpl(partiallyMockedCmHandleQueries, mockInventoryPersistence)

    def 'Query cm handle ids with cpsPath.'() {
        given: 'a cmHandleWithCpsPath condition property'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = createConditionProperties('cmHandleWithCpsPath', [['cpsPath' : '/some/cps/path']])
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        and: 'the query get the cm handle datanodes excluding all descendants returns a datanode'
            cmHandleQueries.queryCmHandleAncestorsByCpsPath('/some/cps/path', FetchDescendantsOption.OMIT_DESCENDANTS) >> [new DataNode(leaves: ['id':'some-cmhandle-id', 'alternate-id':'some-alternate-id'])]
        when: 'the query is executed for cm handle ids'
            def result = objectUnderTest.queryCmHandleReferenceIds(cmHandleQueryParameters, outputAlternateId)
        then: 'the correct expected cm handles ids are returned'
            assert result == expectedCmhandleReference
        where: 'the following data is used'
            senario                   | outputAlternateId || expectedCmhandleReference
            'output CmHandle Ids'     | false             || ['some-cmhandle-id'] as Set
            'output Alternate Ids'    | true              || ['some-alternate-id'] as Set
    }

    def 'Query cm handle where  cps path itself is ancestor axis.'() {
        given: 'a cmHandleWithCpsPath condition property'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = createConditionProperties('cmHandleWithCpsPath', [['cpsPath' : '/some/cps/path']])
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        and: 'the query get the cm handle data nodes excluding all descendants returns a datanode'
            cmHandleQueries.queryCmHandleAncestorsByCpsPath('/some/cps/path', FetchDescendantsOption.OMIT_DESCENDANTS) >> [new DataNode(leaves: ['id':'some-cmhandle-id', 'alternate-id':'some-alternate-id'])]
        when: 'the query is executed for cm handle ids'
            def result = objectUnderTest.queryCmHandleIdsForInventory(cmHandleQueryParameters, outputAlternateId)
        then: 'the correct expected cm handles ids are returned'
            assert result == expectedCmhandleReference
        where: 'the following data is used'
            senario                    | outputAlternateId || expectedCmhandleReference
            'outputAlternate is false' | false             || ['some-cmhandle-id'] as Set
            'outputAlternate is true'  | true              || ['some-alternate-id'] as Set
    }

    def 'Cm handle ids query with error: #scenario.'() {
        given: 'a cmHandleWithCpsPath condition property'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = createConditionProperties('cmHandleWithCpsPath', [['cpsPath' : '/some/cps/path']])
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        and: 'cmHandleQueries throws a path parsing exception'
            cmHandleQueries.queryCmHandleAncestorsByCpsPath('/some/cps/path', FetchDescendantsOption.OMIT_DESCENDANTS) >> { throw thrownException }
        when: 'the query is executed for cm handle ids'
            objectUnderTest.queryCmHandleReferenceIds(cmHandleQueryParameters, false)
        then: 'a data validation exception is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario               | thrownException                                          || expectedException
            'PathParsingException' | new PathParsingException('some message', 'some details') || DataValidationException
            'any other Exception'  | new DataInUseException('some message', 'some details')   || DataInUseException
    }

    def 'Cm handle ids cpsPath query for private properties (not allowed).'() {
        given: 'a CpsPath condition property for private properties'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = createConditionProperties('cmHandleWithCpsPath', [['cpsPath' : '/additional-properties']])
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        when: 'the query is executed for cm handle ids'
            def result = objectUnderTest.queryCmHandleReferenceIds(cmHandleQueryParameters, false)
        then: 'empty result is returned'
            assert result.isEmpty()
    }

    def 'Query cm handle ids with module names when #scenario from query.'() {
        given: 'a modules condition property'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = createConditionProperties('hasAllModules', [['moduleName': 'some-module-name']])
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        when: 'the query is executed for cm handle ids'
            def result = objectUnderTest.queryCmHandleReferenceIds(cmHandleQueryParameters, false)
        then: 'the inventory service is called with the correct module names'
            1 * mockInventoryPersistence.getCmHandleReferencesWithGivenModules(['some-module-name'], false) >> cmHandleIdsFromService
        and: 'the correct expected cm handles ids are returned'
            assert result.size() == cmHandleIdsFromService.size()
            assert result.containsAll(cmHandleIdsFromService)
        where: 'the following data is used'
            scenario                  | cmHandleIdsFromService
            'One anchor returned'     | ['some-cmhandle-id']
            'No anchors are returned' | []
    }

    def 'Query cm handles with some trust level query parameters'() {
        given: 'a trust level condition property'
            def trustLevelQueryParameters = new CmHandleQueryServiceParameters()
            def trustLevelConditionProperties = createConditionProperties('cmHandleWithTrustLevel', [['trustLevel': 'COMPLETE'] as Map])
            trustLevelQueryParameters.setCmHandleQueryParameters([trustLevelConditionProperties])
        when: 'the query is being executed'
            objectUnderTest.queryCmHandleReferenceIds(trustLevelQueryParameters, false)
        then: 'the query is being delegated to the cm handle query service with correct parameter'
            1 * cmHandleQueries.queryCmHandlesByTrustLevel(['trustLevel': 'COMPLETE'] as Map, false)
    }

    def 'Query cm handle details with module names when #scenario from query.'() {
        given: 'a modules condition property'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = createConditionProperties('hasAllModules', [['moduleName': 'some-module-name']])
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        when: 'the query is executed for cm handle ids'
            def result = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the inventory service is called with the correct module names'
            1 * mockInventoryPersistence.getCmHandleReferencesWithGivenModules(['some-module-name'], false) >> ['ch1']
        and: 'the inventory service is called with teh correct if and returns a yang model cm handle'
            1 * mockInventoryPersistence.getYangModelCmHandles(['ch1']) >>
                [new YangModelCmHandle(id: 'abc', dmiProperties: [new YangModelCmHandle.Property('name','value')], publicProperties: [])]
        and: 'the expected cm handle(s) are returned as NCMP Service cm handles'
            assert result[0] instanceof NcmpServiceCmHandle
            assert result.size() == 1
            assert result[0].dmiProperties == [name:'value']
    }

    def 'Query cm handle references when the query is empty.'() {
        given: 'We use an empty query'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
        and: 'the inventory persistence returns the dmi registry datanode with just ids'
            mockInventoryPersistence.getDataNode(NCMP_DMI_REGISTRY_PARENT, FetchDescendantsOption.DIRECT_CHILDREN_ONLY) >> [dmiRegistry]
        when: 'the query is executed for both cm handle ids'
            def result = objectUnderTest.queryCmHandleReferenceIds(cmHandleQueryParameters, outputAlternateId)
        then: 'the correct expected cm handles are returned'
            assert result.containsAll(expectedCmhandleReferences)
        where: 'the following data is used'
            senario                    | outputAlternateId || expectedCmhandleReferences
            'outputAlternate is false' | false             || ['PNFDemo1', 'PNFDemo2', 'PNFDemo3', 'PNFDemo4']
            'outputAlternate is true'  | true              || ['alt-PNFDemo1', 'alt-PNFDemo2', 'alt-PNFDemo3', 'alt-PNFDemo4']
    }

    def 'Query cm handle details when the query is empty.'() {
        given: 'We use an empty query'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
        and: 'the inventory persistence returns the dmi registry datanode with just ids'
            mockInventoryPersistence.getDataNode(NCMP_DMI_REGISTRY_PARENT) >> [dmiRegistry]
        when: 'the query is executed for both cm handle details'
            def result = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct cm handles are returned'
            assert result.size() == 4
            assert result.cmHandleId.containsAll('PNFDemo1', 'PNFDemo2', 'PNFDemo3', 'PNFDemo4')
    }

    def 'Query CMHandleId with #scenario.' () {
        given: 'a query object created with #condition'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = createConditionProperties(conditionName, [['some-key': 'some-value']])
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        and: 'the inventoryPersistence returns different CmHandleIds'
            partiallyMockedCmHandleQueries.queryCmHandlePublicProperties(*_) >> cmHandlesWithMatchingPublicProperties
            partiallyMockedCmHandleQueries.queryCmHandleAdditionalProperties(*_) >> cmHandlesWithMatchingPrivateProperties
        when: 'the query executed'
            def result = objectUnderTestWithPartiallyMockedQueries.queryCmHandleIdsForInventory(cmHandleQueryParameters, false)
        then: 'the expected number of results are returned.'
            assert result.size() == expectedCmHandleIdsSize
        where: 'the following data is used'
            scenario                                          | conditionName                | cmHandlesWithMatchingPublicProperties | cmHandlesWithMatchingPrivateProperties || expectedCmHandleIdsSize
            'all properties, only public matching'            | 'hasAllProperties'           | ['h1', 'h2']                          | null                                   || 2
            'all properties, no matching cm handles'          | 'hasAllProperties'           | []                                    | []                                     || 0
            'additional properties, some matching cm handles' | 'hasAllAdditionalProperties' | []                                    | ['h1', 'h2']                           || 2
            'additional properties, no matching cm handles'   | 'hasAllAdditionalProperties' | null                                  | []                                     || 0
    }

    def 'Retrieve alternate ids by different DMI properties.' () {
        given: 'a query object created with dmi plugin as condition'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = createConditionProperties('cmHandleWithDmiPlugin', [['some-key': 'some-value']])
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        and: 'the inventoryPersistence returns different CmHandleIds'
            partiallyMockedCmHandleQueries.getCmHandleReferencesMapByDmiPluginIdentifier(*_) >> [:]
        when: 'the query executed'
            def result = objectUnderTestWithPartiallyMockedQueries.queryCmHandleIdsForInventory(cmHandleQueryParameters, true)
        then: 'the expected number of results are returned.'
            assert result.size() == 0
    }

    def 'Retrieve cm handle ids by different DMI properties.' () {
        given: 'a query object created with dmi plugin as condition'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = createConditionProperties('cmHandleWithDmiPlugin', [['some-key': 'some-value']])
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        and: 'the inventoryPersistence returns different CmHandleIds'
            partiallyMockedCmHandleQueries.getCmHandleReferencesByDmiPluginIdentifier(_, _) >> ['h1','h2']
        when: 'the query executed'
            def result = objectUnderTestWithPartiallyMockedQueries.queryCmHandleIdsForInventory(cmHandleQueryParameters, false)
        then: 'the expected number of results are returned.'
            assert result.size() == 2

    }

    def 'Combine two query results where #scenario.'() {
        when: 'two query results in the form of a map of NcmpServiceCmHandles are combined into a single query result'
            def result = objectUnderTest.combineCmHandleQueryResults(firstQuery, secondQuery)
        then: 'the returned result is the same as the expected result'
            result == expectedResult
        where:
            scenario                                                     | firstQuery              | secondQuery             || expectedResult
            'two queries with unique and non unique entries exist'       | ['PNFDemo', 'PNFDemo2'] | ['PNFDemo', 'PNFDemo3'] || ['PNFDemo']
            'the first query contains entries and second query is empty' | ['PNFDemo', 'PNFDemo2'] | []                      || []
            'the second query contains entries and first query is empty' | []                      | ['PNFDemo', 'PNFDemo3'] || []
            'the first query contains entries and second query is null'  | ['PNFDemo', 'PNFDemo2'] | null                    || ['PNFDemo', 'PNFDemo2']
            'the second query contains entries and first query is null'  | null                    | ['PNFDemo', 'PNFDemo3'] || ['PNFDemo', 'PNFDemo3']
            'both queries are empty'                                     | []                      | []                      || []
            'both queries are null'                                      | null                    | null                    || null
    }

    def createConditionProperties(String conditionName, List<Map<String, String>> conditionParameters) {
        return new ConditionProperties(conditionName : conditionName, conditionParameters : conditionParameters)
    }

    def static createDataNodeList(dataNodeIds) {
        def dataNodes =[]
        dataNodeIds.each{ dataNodes << new DataNode(xpath: "/dmi-registry/cm-handles[@id='${it}']", leaves: ['id':it, 'alternate-id':'alt-' + it]) }
        return dataNodes
    }
}
