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

import org.onap.cps.ncmp.api.NetworkCmProxyCmHandlerQueryService
import org.onap.cps.spi.CpsAdminPersistenceService
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.model.Anchor
import org.onap.cps.spi.model.CmHandleQueryServiceParameters
import org.onap.cps.spi.model.ConditionProperties
import org.onap.cps.spi.model.DataNode
import spock.lang.Specification

import java.util.stream.Collectors

class NetworkCmProxyCmHandlerQueryServiceSpec extends Specification {

    def cpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def cpsAdminPersistenceService = Mock(CpsAdminPersistenceService)

    NetworkCmProxyCmHandlerQueryService objectUnderTest = new NetworkCmProxyCmHandlerQueryServiceImpl(
        cpsDataPersistenceService, cpsAdminPersistenceService)

    def 'Retrieve cm handles with public properties when #scenario.'() {
        given: 'a condition property'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = new ConditionProperties()
            conditionProperties.conditionName = 'hasAllProperties'
            conditionProperties.conditionParameters = publicProperties
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        and: 'mock services'
            mockResponses()
        when: 'a query is execute (with and without Data)'
            def returnedCmHandlesJustIds = objectUnderTest.queryCmHandleIds(cmHandleQueryParameters)
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles ids are returned'
            returnedCmHandlesJustIds == expectedCmHandleIds as Set
        and: 'the correct cm handle data objects are returned'
            returnedCmHandlesWithData.stream().map(dataNode -> dataNode.cmHandleId).collect(Collectors.toSet()) == expectedCmHandleIds as Set
        where: 'the following data is used'
            scenario                         | publicProperties                                                                                  || expectedCmHandleIds
            'single property matches'        | [['Contact' : 'newemailforstore@bookstore.com']]                                                  || ['PNFDemo1', 'PNFDemo2', 'PNFDemo4']
            'public property does not match' | [['wont_match' : 'wont_match']]                                                                   || []
            '2 properties, only one match'   | [['Contact' : 'newemailforstore@bookstore.com'], ['Contact2': 'newemailforstore2@bookstore.com']] || ['PNFDemo4']
            '2 properties, no matches'       | [['Contact' : 'newemailforstore@bookstore.com'], ['Contact2': '']]                                || []
    }

    def 'Retrieve cm handles with module names when #scenario.'() {
        given: 'a condition property'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties = new ConditionProperties()
            conditionProperties.conditionName = 'hasAllModules'
            conditionProperties.conditionParameters = moduleNames
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
        and: 'mock services'
            mockResponses()
        when: 'the service is invoked'
            def returnedCmHandlesJustIds = objectUnderTest.queryCmHandleIds(cmHandleQueryParameters)
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles are returned'
            returnedCmHandlesJustIds == expectedCmHandleIds as Set
            returnedCmHandlesWithData.stream().map(dataNode -> dataNode.cmHandleId).collect(Collectors.toSet()) == expectedCmHandleIds as Set
        where: 'the following data is used'
            scenario                         | moduleNames                                                             || expectedCmHandleIds
            'single matching module name'    | [['moduleName' : 'MODULE-NAME-001']]                                    || ['PNFDemo3', 'PNFDemo1', 'PNFDemo2']
            'module name dont match'         | [['moduleName' : 'MODULE-NAME-004']]                                    || []
            '2 module names, only one match' | [['moduleName' : 'MODULE-NAME-002'], ['moduleName': 'MODULE-NAME-003']] || ['PNFDemo4']
            '2 module names, no matches'     | [['moduleName' : 'MODULE-NAME-002'], ['moduleName': 'MODULE-NAME-004']] || []
    }

    def 'Retrieve cm handles with combined queries when #scenario.'() {
        given: 'condition properties'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def conditionProperties1 = new ConditionProperties()
            conditionProperties1.conditionName = 'hasAllProperties'
            conditionProperties1.conditionParameters = publicProperties
            def conditionProperties2 = new ConditionProperties()
            conditionProperties2.conditionName = 'hasAllModules'
            conditionProperties2.conditionParameters = moduleNames
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties1,conditionProperties2])
        and: 'mock services'
            mockResponses()
        when: 'the service is invoked'
            def returnedCmHandlesJustIds = objectUnderTest.queryCmHandleIds(cmHandleQueryParameters)
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles are returned'
            returnedCmHandlesJustIds == expectedCmHandleIds as Set
            returnedCmHandlesWithData.stream().map(d -> d.cmHandleId).collect(Collectors.toSet()) == expectedCmHandleIds as Set
        where: 'the following data is used'
            scenario                 | moduleNames                          | publicProperties                                   || expectedCmHandleIds
            'particularly intersect' | [['moduleName' : 'MODULE-NAME-001']] | [['Contact' : 'newemailforstore@bookstore.com']]   || ['PNFDemo1', 'PNFDemo2']
            'empty intersect'        | [['moduleName' : 'MODULE-NAME-004']] | [['Contact' : 'newemailforstore@bookstore.com']]   || []
            'total intersect'        | [['moduleName' : 'MODULE-NAME-002']] | [['Contact2' : 'newemailforstore2@bookstore.com']] || ['PNFDemo4']
    }

    def 'Retrieve cm handles when the query is empty.'() {
        given: 'mock services'
            mockResponses()
        when: 'the service is invoked'
            def cmHandleQueryParameters = new CmHandleQueryServiceParameters()
            def returnedCmHandlesJustIds = objectUnderTest.queryCmHandleIds(cmHandleQueryParameters)
            def returnedCmHandlesWithData = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles are returned'
            returnedCmHandlesJustIds == ['PNFDemo1', 'PNFDemo2', 'PNFDemo3', 'PNFDemo4'] as Set
            returnedCmHandlesWithData.stream().map(d -> d.cmHandleId).collect(Collectors.toSet()) == ['PNFDemo1', 'PNFDemo2', 'PNFDemo3', 'PNFDemo4'] as Set
    }

    void mockResponses() {
        def pNFDemo1 = new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'PNFDemo1\']', leaves: ['id':'PNFDemo1'])
        def pNFDemo2 = new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'PNFDemo2\']', leaves: ['id':'PNFDemo2'])
        def pNFDemo3 = new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'PNFDemo3\']', leaves: ['id':'PNFDemo3'])
        def pNFDemo4 = new DataNode(xpath: '/dmi-registry/cm-handles[@id=\'PNFDemo4\']', leaves: ['id':'PNFDemo4'])

        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\'Contact\' and @value=\'newemailforstore@bookstore.com\']/ancestor::cm-handles', _) >> [pNFDemo1, pNFDemo2, pNFDemo4]
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\'wont_match\' and @value=\'wont_match\']/ancestor::cm-handles', _) >> []
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\'Contact2\' and @value=\'newemailforstore2@bookstore.com\']/ancestor::cm-handles', _) >> [pNFDemo4]
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\'Contact2\' and @value=\'\']/ancestor::cm-handles', _) >> []
        cpsDataPersistenceService.queryDataNodes(_, _, '/dmi-registry/cm-handles', _) >> [pNFDemo1, pNFDemo2, pNFDemo3, pNFDemo4]

        cpsDataPersistenceService.getDataNode(_, _, '/dmi-registry/cm-handles[@id=\'PNFDemo1\']', _) >> pNFDemo1
        cpsDataPersistenceService.getDataNode(_, _, '/dmi-registry/cm-handles[@id=\'PNFDemo2\']', _) >> pNFDemo2
        cpsDataPersistenceService.getDataNode(_, _, '/dmi-registry/cm-handles[@id=\'PNFDemo3\']', _) >> pNFDemo3
        cpsDataPersistenceService.getDataNode(_, _, '/dmi-registry/cm-handles[@id=\'PNFDemo4\']', _) >> pNFDemo4

        cpsAdminPersistenceService.queryAnchors(_, ['MODULE-NAME-001']) >> [new Anchor(name: 'PNFDemo1'), new Anchor(name: 'PNFDemo2'), new Anchor(name: 'PNFDemo3')]
        cpsAdminPersistenceService.queryAnchors(_, ['MODULE-NAME-004']) >> []
        cpsAdminPersistenceService.queryAnchors(_, ['MODULE-NAME-003', 'MODULE-NAME-002']) >> [new Anchor(name: 'PNFDemo4')]
        cpsAdminPersistenceService.queryAnchors(_, ['MODULE-NAME-002', 'MODULE-NAME-003']) >> [new Anchor(name: 'PNFDemo4')]
        cpsAdminPersistenceService.queryAnchors(_, ['MODULE-NAME-004', 'MODULE-NAME-002']) >> []
        cpsAdminPersistenceService.queryAnchors(_, ['MODULE-NAME-002', 'MODULE-NAME-004']) >> []
        cpsAdminPersistenceService.queryAnchors(_, ['MODULE-NAME-002']) >> [new Anchor(name: 'PNFDemo2'), new Anchor(name: 'PNFDemo4')]
        cpsAdminPersistenceService.getAnchors(_) >> [new Anchor(name: 'PNFDemo1'), new Anchor(name: 'PNFDemo2'), new Anchor(name: 'PNFDemo3'), new Anchor(name: 'PNFDemo4')]
    }
}
