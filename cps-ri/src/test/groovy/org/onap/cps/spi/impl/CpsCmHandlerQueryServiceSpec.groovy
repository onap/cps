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

package org.onap.cps.spi.impl

import org.onap.cps.spi.CpsCmHandlerQueryService
import org.onap.cps.spi.model.CmHandleQueryParameters
import org.onap.cps.spi.model.ConditionProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql

import java.util.stream.Collectors

class CpsCmHandlerQueryServiceSpec extends CpsPersistenceSpecBase {

    @Autowired
    CpsCmHandlerQueryService objectUnderTest

    static final String SET_FRAGMENT_DATA = '/data/fragment.sql'

    @Sql([CLEAR_DATA, SET_FRAGMENT_DATA])
    def 'Retrieve cm handles with public properties when #scenario.'() {
        when: 'the service is invoked'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
            def conditionProperties = new ConditionProperties()
            conditionProperties.conditionName = 'hasAllProperties'
            conditionProperties.conditionParameters = publicProperties
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
            def returnedCmHandles = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles are returned'
            returnedCmHandles.stream().map(d -> {return d.xpath.split('\'')[1]}).collect(Collectors.toList()) == expectedCmHandleIds
        where: 'the following data is used'
            scenario                                       | publicProperties                                                                                  || expectedCmHandleIds
            'single matching property'                     | [['Contact' : 'newemailforstore@bookstore.com']]                                                  || ['PNFDemo', 'PNFDemo2', 'PNFDemo4']
            'public property dont match'                   | [['wont_match' : 'wont_match']]                                                                   || []
            '2 properties, only one match (and)'           | [['Contact' : 'newemailforstore@bookstore.com'], ['Contact2': 'newemailforstore2@bookstore.com']] || ['PNFDemo4']
            '2 properties, no match (and)'                 | [['Contact' : 'newemailforstore@bookstore.com'], ['Contact2': '']]                                || []
    }

    @Sql([CLEAR_DATA, SET_FRAGMENT_DATA])
    def 'Retrieve cm handles with module names when #scenario.'() {
        when: 'the service is invoked'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
            def conditionProperties = new ConditionProperties()
            conditionProperties.conditionName = 'hasAllModules'
            conditionProperties.conditionParameters = moduleNames
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties])
            def returnedCmHandles = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles are returned'
            returnedCmHandles.stream().map(d -> {return d.xpath.split('\'')[1]}).collect(Collectors.toList()) == expectedCmHandleIds
        where: 'the following data is used'
            scenario                               | moduleNames                                                        || expectedCmHandleIds
            'single matching module name'          | [['moduleName' : 'MODULE-NAME-001']]                                    || ['PNFDemo2', 'PNFDemo', 'PNFDemo3']
            'module name dont match'               | [['moduleName' : 'MODULE-NAME-004']]                                    || []
            '2 module names, only one match (and)' | [['moduleName' : 'MODULE-NAME-002'], ['moduleName': 'MODULE-NAME-003']] || ['PNFDemo4']
            '2 module names, no match (and)'       | [['moduleName' : 'MODULE-NAME-002'], ['moduleName': 'MODULE-NAME-004']] || []
    }

    @Sql([CLEAR_DATA, SET_FRAGMENT_DATA])
    def 'Retrieve cm handles with combined queries when #scenario.'() {
        when: 'the service is invoked'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
            def conditionProperties1 = new ConditionProperties()
            conditionProperties1.conditionName = 'hasAllProperties'
            conditionProperties1.conditionParameters = publicProperties
            def conditionProperties2 = new ConditionProperties()
            conditionProperties2.conditionName = 'hasAllModules'
            conditionProperties2.conditionParameters = moduleNames
            cmHandleQueryParameters.setCmHandleQueryParameters([conditionProperties1,conditionProperties2])
            def returnedCmHandles = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles are returned'
            returnedCmHandles.stream().map(d -> {return d.xpath.split('\'')[1]}).collect(Collectors.toList()) == expectedCmHandleIds
        where: 'the following data is used'
            scenario                 | moduleNames                          | publicProperties                                   || expectedCmHandleIds
            'particularly intersect' | [['moduleName' : 'MODULE-NAME-001']] | [['Contact' : 'newemailforstore@bookstore.com']]   || ['PNFDemo2', 'PNFDemo']
            'empty intersect'        | [['moduleName' : 'MODULE-NAME-004']] | [['Contact' : 'newemailforstore@bookstore.com']]   || []
            'total intersect'        | [['moduleName' : 'MODULE-NAME-002']] | [['Contact2' : 'newemailforstore2@bookstore.com']] || ['PNFDemo4']
    }

    @Sql([CLEAR_DATA, SET_FRAGMENT_DATA])
    def 'Retrieve cm handles when the query is empty.'() {
        when: 'the service is invoked'
            def cmHandleQueryParameters = new CmHandleQueryParameters()
            def returnedCmHandles = objectUnderTest.queryCmHandles(cmHandleQueryParameters)
        then: 'the correct expected cm handles are returned'
            returnedCmHandles.stream().map(d -> {return d.xpath.split('\'')[1]}).collect(Collectors.toList()) == ['PNFDemo', 'PNFDemo2', 'PNFDemo3', 'PNFDemo4']
    }
}
