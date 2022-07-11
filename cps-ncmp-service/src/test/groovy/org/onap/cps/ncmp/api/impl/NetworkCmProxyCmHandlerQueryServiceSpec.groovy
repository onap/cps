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
import org.onap.cps.ncmp.api.NetworkCmProxyCmHandlerQueryService
import org.onap.cps.ncmp.api.inventory.InventoryPersistence
import org.onap.cps.ncmp.api.inventory.InventoryQuery
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.exceptions.CpsException
import org.onap.cps.spi.exceptions.DataInUseException
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.model.Anchor
import org.onap.cps.spi.model.CmHandleQueryServiceParameters
import org.onap.cps.spi.model.ConditionProperties
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification

import java.util.stream.Collectors

class NetworkCmProxyCmHandlerQueryServiceSpec extends Specification {

    def inventoryQuery = Mock(InventoryQuery)
    def inventoryPersistence = Mock(InventoryPersistence)

    def someCmHandleDataNode = new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'some-cmhandle-id\']', leaves: ['id':'some-cmhandle-id'])
    def static pNFDemo1 = new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'PNFDemo1\']', leaves: ['id':'PNFDemo1'])
    def static pNFDemo2 = new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'PNFDemo2\']', leaves: ['id':'PNFDemo2'])
    def static pNFDemo3 = new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'PNFDemo3\']', leaves: ['id':'PNFDemo3'])
    def static pNFDemo4 = new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'PNFDemo4\']', leaves: ['id':'PNFDemo4'])
    def dmiRegistry = new DataNode(xpath: '/dmi-registry', childDataNodes: [pNFDemo1, pNFDemo2, pNFDemo3, pNFDemo4])

    NetworkCmProxyCmHandlerQueryService objectUnderTest = new NetworkCmProxyCmHandlerQueryServiceImpl(inventoryQuery, inventoryPersistence)

    def 'Retrieve cm handles with cpsPath when combined with no Module Query.'() {
        given: 'a condition property'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = new ConditionProperties()
            conditionProperties.conditionName = 'cmHandleWithCpsPath'
            conditionProperties.conditionParameters = [['cpsPath' : '/some/cps/path']]
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        and: 'inventoryPersistence returns a non null query result'
            inventoryPersistence.getCmHandleDataNodesByCpsPath('/some/cps/path'+"/ancestor::cm-handles", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> [new DataNode(leaves: ['id':'some-cmhandle-id'])]
        and: 'inventoryQuery returns cmHandles with the relevant query result'
            inventoryQuery.combineCmHandleQueries(*_) >> ['PNFDemo1': new NcmpServiceCmHandle(cmHandleId: 'PNFDemo1'), 'PNFDemo3': new NcmpServiceCmHandle(cmHandleId: 'PNFDemo3')]
        when: 'a query is execute (with and without Data)'
            def returnedCmHandlesJustIds = objectUnderTest.queryCmHandleIds(cmHandleQueryParameters)
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles ids are returned'
            returnedCmHandlesJustIds == ['PNFDemo1', 'PNFDemo3'] as Set
        and: 'the correct ncmp service cm handles are returned'
            returnedCmHandlesWithData.stream().map(CmHandle -> CmHandle.cmHandleId).collect(Collectors.toSet()) == ['PNFDemo1', 'PNFDemo3'] as Set
    }

    def 'Retrieve cm handles with cpsPath where a #scenario.'() {
        given: 'a condition property'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = new ConditionProperties()
            conditionProperties.conditionName = 'cmHandleWithCpsPath'
            conditionProperties.conditionParameters = [['cpsPath' : '/some/cps/path']]
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        and: 'inventoryPersistence throws a path parsing exception'
            inventoryPersistence.getCmHandleDataNodesByCpsPath('/some/cps/path'+"/ancestor::cm-handles", FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> { throw thrownException }
        when: 'a query is executed (with and without Data)'
            objectUnderTest.queryCmHandleIds(cmHandleQueryParameters)
            objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'a data validation exception is thrown'
            thrown(expectedException)
        where: 'the following data is used'
            scenario                           | thrownException                                          || expectedException
            'a PathParsingException is thrown' | new PathParsingException('some message', 'some details') || DataValidationException
            'any other Exception is thrown'    | new DataInUseException('some message', 'some details')   || DataInUseException
    }

    def 'Retrieve cm handles with public properties when combined with Empty Modules Query result.'() {
        given: 'a condition property'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = new ConditionProperties()
            conditionProperties.conditionName = 'hasAllProperties'
            conditionProperties.conditionParameters = [['some-property-key': 'some-property-value']]
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        and: 'inventoryQuery returns cmHandles with the relevant query result'
            inventoryQuery.combineCmHandleQueries(*_) >> ['PNFDemo1': new NcmpServiceCmHandle(cmHandleId: 'PNFDemo1'), 'PNFDemo3': new NcmpServiceCmHandle(cmHandleId: 'PNFDemo3')]
        when: 'a query is execute (with and without Data)'
            def returnedCmHandlesJustIds = objectUnderTest.queryCmHandleIds(cmHandleQueryParameters)
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles ids are returned'
            returnedCmHandlesJustIds == ['PNFDemo1', 'PNFDemo3'] as Set
        and: 'the correct cm handle data objects are returned'
            returnedCmHandlesWithData.stream().map(dataNode -> dataNode.cmHandleId).collect(Collectors.toSet()) == ['PNFDemo1', 'PNFDemo3'] as Set
    }

    def 'Retrieve cm handles with module names when #scenario from query.'() {
        given: 'a condition property'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = new ConditionProperties()
            conditionProperties.conditionName = 'hasAllModules'
            conditionProperties.conditionParameters = [['moduleName':'some-module-name']]
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        and: 'null is returned from the state and public property queries'
            inventoryQuery.combineCmHandleQueries(*_) >> null
        and: '#scenario from the modules query'
            inventoryPersistence.queryAnchors(*_) >> returnedAnchors
        and: 'the same cmHandles are returned from the persistence service layer'
            returnedAnchors.size() * inventoryPersistence.getDataNode(*_) >> returnedCmHandles
        when: 'the service is invoked'
            def returnedCmHandlesJustIds = objectUnderTest.queryCmHandleIds(cmHandleQueryParameters)
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles are returned'
            returnedCmHandlesJustIds == expectedCmHandleIds as Set
            returnedCmHandlesWithData.stream().map(dataNode -> dataNode.cmHandleId).collect(Collectors.toSet()) == expectedCmHandleIds as Set
        where: 'the following data is used'
            scenario                  | returnedAnchors                                              | returnedCmHandles    || expectedCmHandleIds
            'One anchor returned'     | [new Anchor(name: 'PNFDemo1')]                               | pNFDemo1             || ['PNFDemo1']
            'No anchors are returned' | []                                                           | null                 || []
    }

    def 'Retrieve cm handles with combined queries when #scenario.'() {
        given: 'condition properties'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionPubProps = new ConditionProperties()
            conditionPubProps.conditionName = 'hasAllProperties'
            conditionPubProps.conditionParameters = [['some-property-key': 'some-property-value']]
            def conditionModules = new ConditionProperties()
            conditionModules.conditionName = 'hasAllModules'
            conditionModules.conditionParameters = [['moduleName': 'some-module-name']]
            def conditionState = new ConditionProperties()
            conditionState.conditionName = 'cmHandleWithCpsPath'
            conditionState.conditionParameters = [['cpsPath' : 'some/cps/path']]
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionPubProps, conditionModules, conditionState])
        and: 'cmHandles are returned from the state and public property combined queries'
            inventoryQuery.combineCmHandleQueries(*_) >> combinedQueryMap
        and: 'cmHandles are returned from the module names query'
            inventoryPersistence.queryAnchors(['some-module-name']) >> anchorsForModuleQuery
        and: 'inventoryPersistence returns a non datanode result'
            2 * inventoryPersistence.getCmHandleDataNodesByCpsPath(*_) >> [someCmHandleDataNode]
        when: 'the service is invoked'
            def returnedCmHandlesJustIds = objectUnderTest.queryCmHandleIds(cmHandleQueryParameters)
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles are returned'
            returnedCmHandlesJustIds == expectedCmHandleIds as Set
            returnedCmHandlesWithData.stream().map(d -> d.cmHandleId).collect(Collectors.toSet()) == expectedCmHandleIds as Set
        where: 'the following data is used'
            scenario                                 | combinedQueryMap                                                                                                           | anchorsForModuleQuery                                        || expectedCmHandleIds
            'combined and modules queries intersect' | ['PNFDemo1' : new NcmpServiceCmHandle(cmHandleId:'PNFDemo1')]                                                              | [new Anchor(name: 'PNFDemo1'), new Anchor(name: 'PNFDemo2')] || ['PNFDemo1']
            'only module query results exist'        | [:]                                                                                                                        | [new Anchor(name: 'PNFDemo1'), new Anchor(name: 'PNFDemo2')] || []
            'only combined query results exist'      | ['PNFDemo1' : new NcmpServiceCmHandle(cmHandleId:'PNFDemo1'), 'PNFDemo2' : new NcmpServiceCmHandle(cmHandleId:'PNFDemo2')] | []                                                           || []
            'neither queries return results'         | [:]                                                                                                                        | []                                                           || []
            'none intersect'                         | ['PNFDemo1' : new NcmpServiceCmHandle(cmHandleId:'PNFDemo1')]                                                              | [new Anchor(name: 'PNFDemo2')]                               || []
    }

    def 'Retrieve cm handles when the query is empty.'() {
        given: 'mock services'
            inventoryPersistence.getDataNode("/dmi-registry") >> dmiRegistry
            inventoryPersistence.getAnchors() >> [new Anchor(name: 'PNFDemo1'), new Anchor(name: 'PNFDemo2'), new Anchor(name: 'PNFDemo3'), new Anchor(name: 'PNFDemo4')]
        when: 'the service is invoked'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def returnedCmHandlesJustIds = objectUnderTest.queryCmHandleIds(cmHandleQueryParameters)
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles are returned'
            returnedCmHandlesJustIds == ['PNFDemo1', 'PNFDemo2', 'PNFDemo3', 'PNFDemo4'] as Set
            returnedCmHandlesWithData.stream().map(d -> d.cmHandleId).collect(Collectors.toSet()) == ['PNFDemo1', 'PNFDemo2', 'PNFDemo3', 'PNFDemo4'] as Set
    }
}
