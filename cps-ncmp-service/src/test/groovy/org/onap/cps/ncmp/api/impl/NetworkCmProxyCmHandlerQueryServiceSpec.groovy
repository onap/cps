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

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.NetworkCmProxyCmHandlerQueryService
import org.onap.cps.spi.CpsAdminPersistenceService
import org.onap.cps.spi.CpsDataPersistenceService
import org.onap.cps.spi.model.Anchor
import org.onap.cps.spi.model.CmHandleQueryParameters
import org.onap.cps.spi.model.ConditionProperties
import org.onap.cps.spi.model.DataNode
import org.onap.cps.utils.JsonObjectMapper
import spock.lang.Specification

import java.util.stream.Collectors

class NetworkCmProxyCmHandlerQueryServiceSpec extends Specification {

    def cpsDataPersistenceService = Mock(CpsDataPersistenceService)
    def cpsAdminPersistenceService = Mock(CpsAdminPersistenceService)

    NetworkCmProxyCmHandlerQueryService objectUnderTest = new NetworkCmProxyCmHandlerQueryServiceImpl(
        cpsDataPersistenceService, cpsAdminPersistenceService, new JsonObjectMapper(new ObjectMapper())
    )

    def 'Retrieve cm handles when #scenario.'() {
        given: 'condition properties'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
            cmHandleQueryParameters.setCmHandleQueryParameters([])
        and: 'set public properties'
            if (publicProperties != null) {
                def conditionProperties = new ConditionProperties()
                conditionProperties.conditionName = 'hasAllProperties'
                conditionProperties.conditionParameters = publicProperties
                cmHandleQueryParameters.cmHandleQueryParameters.add(conditionProperties)
            }
        and: 'set module names'
            if (moduleNames != null) {
                def conditionProperties = new ConditionProperties()
                conditionProperties.conditionName = 'hasAllModules'
                conditionProperties.conditionParameters = moduleNames
                cmHandleQueryParameters.cmHandleQueryParameters.add(conditionProperties)
            }
        and: 'mock services'
            mockResponses()
        when: 'the service is invoked'
            def returnedCmHandles = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles are returned'
            returnedCmHandles.stream().map(d -> d.leaves.get('id').toString()).collect(Collectors.toList()) == expectedCmHandleIds
        where: 'the following data is used'
            scenario                                       | moduleNames                                                             | publicProperties                                                                                  || expectedCmHandleIds
            'combined queries with particularly intersect' | [['moduleName' : 'MODULE-NAME-001']]                                    | [['Contact' : 'newemailforstore@bookstore.com']]                                                  || ['PNFDemo2', 'PNFDemo1']
            'combined queries with empty intersect'        | [['moduleName' : 'MODULE-NAME-004']]                                    | [['Contact' : 'newemailforstore@bookstore.com']]                                                  || []
            'combined queries with total intersect'        | [['moduleName' : 'MODULE-NAME-002']]                                    | [['Contact2' : 'newemailforstore2@bookstore.com']]                                                || ['PNFDemo4']
            'single matching module name'                  | [['moduleName' : 'MODULE-NAME-001']]                                    | null                                                                                              || ['PNFDemo2', 'PNFDemo3', 'PNFDemo1']
            'module name dont match'                       | [['moduleName' : 'MODULE-NAME-004']]                                    | null                                                                                              || []
            '2 module names, only one match (and)'         | [['moduleName' : 'MODULE-NAME-002'], ['moduleName': 'MODULE-NAME-003']] | null                                                                                              || ['PNFDemo4']
            '2 module names, no match (and)'               | [['moduleName' : 'MODULE-NAME-002'], ['moduleName': 'MODULE-NAME-004']] | null                                                                                              || []
            'single matching property'                     | null                                                                    | [['Contact' : 'newemailforstore@bookstore.com']]                                                  || ['PNFDemo1', 'PNFDemo2', 'PNFDemo4']
            'public property dont match'                   | null                                                                    | [['wont_match' : 'wont_match']]                                                                   || []
            '2 properties, only one match (and)'           | null                                                                    | [['Contact' : 'newemailforstore@bookstore.com'], ['Contact2': 'newemailforstore2@bookstore.com']] || ['PNFDemo4']
            '2 properties, no match (and)'                 | null                                                                    | [['Contact' : 'newemailforstore@bookstore.com'], ['Contact2': '']]                                || []
            'query is empty'                               | null                                                                    | null                                                                                              || ['PNFDemo1', 'PNFDemo2', 'PNFDemo3', 'PNFDemo4']
    }

    void mockResponses() {
        def pNFDemo1 = new DataNode(xpath: 'cmHandle/id[\'PNFDemo1\']', leaves: ['id':'PNFDemo1'])
        def pNFDemo2 = new DataNode(xpath: 'cmHandle/id[\'PNFDemo2\']', leaves: ['id':'PNFDemo2'])
        def pNFDemo3 = new DataNode(xpath: 'cmHandle/id[\'PNFDemo3\']', leaves: ['id':'PNFDemo3'])
        def pNFDemo4 = new DataNode(xpath: 'cmHandle/id[\'PNFDemo4\']', leaves: ['id':'PNFDemo4'])

        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\'Contact\' and @value=\'newemailforstore@bookstore.com\']/ancestor::cm-handles', _)
                >> [pNFDemo1, pNFDemo2, pNFDemo4]
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\'wont_match\' and @value=\'wont_match\']/ancestor::cm-handles', _)
                >> []
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\'Contact2\' and @value=\'newemailforstore2@bookstore.com\']/ancestor::cm-handles', _)
                >> [pNFDemo4]
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties[@name=\'Contact2\' and @value=\'\']/ancestor::cm-handles', _)
                >> []
        cpsDataPersistenceService.queryDataNodes(_, _, '//public-properties/ancestor::cm-handles', _)
                >> [pNFDemo1, pNFDemo2, pNFDemo3, pNFDemo4]

        cpsDataPersistenceService.queryDataNodes(_, _, '//cm-handles[@id=\'PNFDemo1\']', _) >> [pNFDemo1]
        cpsDataPersistenceService.queryDataNodes(_, _, '//cm-handles[@id=\'PNFDemo2\']', _) >> [pNFDemo2]
        cpsDataPersistenceService.queryDataNodes(_, _, '//cm-handles[@id=\'PNFDemo3\']', _) >> [pNFDemo3]
        cpsDataPersistenceService.queryDataNodes(_, _, '//cm-handles[@id=\'PNFDemo4\']', _) >> [pNFDemo4]
        cpsAdminPersistenceService.queryAnchors(_, ['MODULE-NAME-001']) >> [new Anchor(name: 'PNFDemo2'), new Anchor(name: 'PNFDemo3'), new Anchor(name: 'PNFDemo1')]
        cpsAdminPersistenceService.queryAnchors(_, ['MODULE-NAME-004']) >> []
        cpsAdminPersistenceService.queryAnchors(_, ['MODULE-NAME-003', 'MODULE-NAME-002']) >> [new Anchor(name: 'PNFDemo4')]
        cpsAdminPersistenceService.queryAnchors(_, ['MODULE-NAME-002', 'MODULE-NAME-003']) >> [new Anchor(name: 'PNFDemo4')]
        cpsAdminPersistenceService.queryAnchors(_, ['MODULE-NAME-004', 'MODULE-NAME-002']) >> []
        cpsAdminPersistenceService.queryAnchors(_, ['MODULE-NAME-002', 'MODULE-NAME-004']) >> []
        cpsAdminPersistenceService.queryAnchors(_, ['MODULE-NAME-002']) >> [new Anchor(name: 'PNFDemo2'), new Anchor(name: 'PNFDemo4')]
    }
}
