/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021 Bell Canada
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

import static org.onap.cps.ncmp.api.impl.operations.DmiOperations.DataStoreEnum.PASSTHROUGH_OPERATIONAL
import static org.onap.cps.ncmp.api.impl.operations.DmiOperations.DataStoreEnum.PASSTHROUGH_RUNNING
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.CREATE
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.READ
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.UPDATE

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.ncmp.api.impl.exception.NcmpException
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.DataNode
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class NetworkCmProxyDataServiceImplSpec extends Specification {

    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsModuleService = Mock(CpsModuleService)
    def mockCpsAdminService = Mock(CpsAdminService)
    def spyObjectMapper = Spy(ObjectMapper)
    def mockDmiDataOperations = Mock(DmiDataOperations)

    def objectUnderTest = new NetworkCmProxyDataServiceImpl(mockDmiDataOperations, null,
        mockCpsModuleService, mockCpsDataService, mockCpsAdminService, spyObjectMapper)

    def cmHandleXPath = "/dmi-registry/cm-handles[@id='testCmHandle']"

    def dataNode = new DataNode(leaves: ['dmi-service-name': 'testDmiService'])


    def 'Write resource data for pass-through running from DMI using POST #scenario cm handle properties.'() {
        given: 'cpsDataService returns valid datanode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        when: 'get resource data is called'
            objectUnderTest.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'testResourceId', CREATE,
                '{some-json}', 'application/json')
        then: 'DMI called with correct data'
            1 * mockDmiDataOperations.writeResourceDataPassThroughRunningFromDmi('testCmHandle', 'testResourceId',
                CREATE, '{some-json}', 'application/json')
                >> { new ResponseEntity<>(HttpStatus.CREATED) }
    }

    def 'Write resource data for pass-through running from DMI using POST "not found" response (from DMI).'() {
        given: 'cpsDataService returns valid dataNode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'DMI returns a response with 404 status code'
            mockDmiDataOperations.writeResourceDataPassThroughRunningFromDmi('testCmHandle',
                'testResourceId', CREATE,
                '{some-json}', 'application/json')
                >> { new ResponseEntity<>(HttpStatus.NOT_FOUND) }
        when: 'write resource data is called'
            objectUnderTest.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'testResourceId', CREATE,
                '{some-json}', 'application/json')
        then: 'exception is thrown'
            def exceptionThrown = thrown(NcmpException.class)
        and: 'details contains (not found) error code: 404'
            exceptionThrown.details.contains('404')
    }


    def 'Get resource data for pass-through operational from DMI.'() {
        given: 'get data node is called'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'get resource data from DMI is called'
            mockDmiDataOperations.getResourceDataFromDmi(
                'testCmHandle',
                'testResourceId',
                '(a=1,b=2)',
                'testAcceptParam' ,
                PASSTHROUGH_OPERATIONAL) >> new ResponseEntity<>('result-json', HttpStatus.OK)
        when: 'get resource data operational for cm-handle is called'
            def response = objectUnderTest.getResourceDataOperationalForCmHandle('testCmHandle',
                'testResourceId',
                'testAcceptParam',
                '(a=1,b=2)')
        then: 'DMI returns a json response'
            response == 'result-json'
    }

    def 'Get resource data for pass-through operational from DMI with Json Processing Exception.'() {
        given: 'cps data service returns valid data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'objectMapper not able to parse object'
            def mockObjectMapper = Mock(ObjectMapper)
            objectUnderTest.objectMapper = mockObjectMapper
            mockObjectMapper.writeValueAsString(_) >> { throw new JsonProcessingException('testException') }
        and: 'DMI returns NOK response'
            mockDmiDataOperations.getResourceDataFromDmi(*_)
                >> new ResponseEntity<>('NOK-json', HttpStatus.NOT_FOUND)
        when: 'get resource data is called'
            objectUnderTest.getResourceDataOperationalForCmHandle('testCmHandle',
                'testResourceId',
                'testAcceptParam',
                '(a=1,b=2)')
        then: 'exception is thrown with the expected details'
            def exceptionThrown = thrown(NcmpException.class)
            exceptionThrown.details == 'DMI status code: 404, DMI response body: NOK-json'
    }

    def 'Get resource data for pass-through operational from DMI return NOK response.'() {
        given: 'cps data service returns valid data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'DMI returns NOK response'
            mockDmiDataOperations.getResourceDataFromDmi('testCmHandle',
                'testResourceId',
                '(a=1,b=2)',
                'testAcceptParam',
                PASSTHROUGH_OPERATIONAL)
                >> new ResponseEntity<>('NOK-json', HttpStatus.NOT_FOUND)
        when: 'get resource data is called'
            objectUnderTest.getResourceDataOperationalForCmHandle('testCmHandle',
                'testResourceId',
                'testAcceptParam',
                '(a=1,b=2)')
        then: 'exception is thrown'
            def exceptionThrown = thrown(NcmpException.class)
        and: 'details contains the original response'
            exceptionThrown.details.contains('NOK-json')
    }

    def 'Get resource data for pass-through running from DMI.'() {
        given: 'cpsDataService returns valid data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'DMI returns valid response and data'
            mockDmiDataOperations.getResourceDataFromDmi('testCmHandle',
                'testResourceId',
                '(a=1,b=2)',
                'testAcceptParam',
                PASSTHROUGH_RUNNING) >> new ResponseEntity<>('{result-json}', HttpStatus.OK)
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'testResourceId',
                'testAcceptParam',
                '(a=1,b=2)')
        then: 'get resource data returns expected response'
            response == '{result-json}'
    }

    def 'Get resource data for pass-through running from DMI return NOK response.'() {
        given: 'cpsDataService returns valid dataNode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'DMI returns NOK response'
            mockDmiDataOperations.getResourceDataFromDmi('testCmHandle',
                'testResourceId',
                '(a=1,b=2)',
                'testAcceptParam',
                PASSTHROUGH_RUNNING)
                >> new ResponseEntity<>('NOK-json', HttpStatus.NOT_FOUND)
        when: 'get resource data is called'
            objectUnderTest.getResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'testResourceId',
                'testAcceptParam',
                '(a=1,b=2)')
        then: 'exception is thrown'
            def exceptionThrown = thrown(NcmpException.class)
        and: 'details contains the original response'
            exceptionThrown.details.contains('NOK-json')
    }

    def 'Getting Yang Resources.'() {
        when: 'yang resources is called'
            objectUnderTest.getYangResourcesModuleReferences('some cm handle')
        then: 'CPS module services is invoked for the correct dataspace and cm handle'
            1 * mockCpsModuleService.getYangResourcesModuleReferences('NFP-Operational','some cm handle')
    }

    def 'Get cm handle identifiers for the given module names.'() {
        when: 'execute a cm handle search for the given module names'
            objectUnderTest.executeCmHandleHasAllModulesSearch(['some-module-name'])
        then: 'get anchor identifiers is invoked  with the expected parameters'
            1 * mockCpsAdminService.queryAnchorNames('NFP-Operational', ['some-module-name'])
    }

    def 'Update resource data for pass-through running from dmi using POST #scenario DMI properties.'() {
        given: 'a data node'
        and: 'cpsDataService returns valid datanode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        when: 'get resource data is called'
            objectUnderTest.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'testResourceId', UPDATE,
                '{some-json}', 'application/json')
        then: 'DMI called with correct data'
            1 * mockDmiDataOperations.writeResourceDataPassThroughRunningFromDmi('testCmHandle', 'testResourceId',
                UPDATE, '{some-json}', 'application/json')
                >> { new ResponseEntity<>(HttpStatus.OK) }
    }

    def 'Verify error message from handleResponse is correct for #scenario operation.'() {
        given: 'writeResourceDataPassThroughRunningFromDmi fails to return OK HttpStatus'
            mockDmiDataOperations.writeResourceDataPassThroughRunningFromDmi(*_)
                >> new ResponseEntity<>(HttpStatus.NOT_FOUND)
        when: 'get resource data is called'
            def response = objectUnderTest.writeResourceDataPassThroughRunningForCmHandle(
                'testCmHandle',
                'testResourceId',
                givenOperation,
                '{some-json}',
                'application/json')
        then: 'an exception is thrown with the expected error message detailsd with correct operation'
            def exceptionThrown = thrown(NcmpException.class)
            exceptionThrown.getMessage().contains(expectedResponseMessage)
        where:
            scenario | givenOperation || expectedResponseMessage
            'CREATE' | CREATE         || 'Not able to create resource data.'
            'READ'   | READ           || 'Not able to read resource data.'
            'UPDATE' | UPDATE         || 'Not able to update resource data.'
    }
}
