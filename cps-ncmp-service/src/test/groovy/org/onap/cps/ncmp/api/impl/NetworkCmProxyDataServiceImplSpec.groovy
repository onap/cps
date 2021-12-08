/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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
import org.onap.cps.api.CpsQueryService
import org.onap.cps.ncmp.api.impl.exception.NcmpException
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperations
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.DataNode
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Specification

class NetworkCmProxyDataServiceImplSpec extends Specification {

    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsQueryService = Mock(CpsQueryService)
    def mockCpsModuleService = Mock(CpsModuleService)
    def mockCpsAdminService = Mock(CpsAdminService)
    def spyObjectMapper = Spy(ObjectMapper)
    def mockDmiDataOperations = Mock(DmiDataOperations)

    def objectUnderTest = new NetworkCmProxyDataServiceImpl(mockDmiDataOperations, null,
        mockCpsModuleService, mockCpsDataService, mockCpsQueryService, mockCpsAdminService, spyObjectMapper)

    def cmHandle = 'some handle'
    def noTimestamp = null
    def cmHandleXPath = "/dmi-registry/cm-handles[@id='testCmHandle']"
    def expectedDataspaceName = 'NFP-Operational'

    def 'Create full data node: #scenario.'() {
        given: 'json data'
            def jsonData = 'some json'
        when: 'createDataNode is invoked'
            objectUnderTest.createDataNode(cmHandle, xpath, jsonData)
        then: 'save data is invoked once with the expected parameters'
            1 * mockCpsDataService.saveData(expectedDataspaceName, cmHandle, jsonData, noTimestamp)
        where: 'following parameters were used'
            scenario           | xpath
            'no xpath'         | ''
            'root level xpath' | '/'
    }

    def 'Create child data node.'() {
        given: 'json data and xpath'
            def jsonData = 'some json'
            def xpath = '/test-node'
        when: 'create data node is invoked'
            objectUnderTest.createDataNode(cmHandle, xpath, jsonData)
        then: 'save data is invoked once with the expected parameters'
            1 * mockCpsDataService.saveData(expectedDataspaceName, cmHandle, xpath, jsonData, noTimestamp)
    }

    def 'Add list-node elements.'() {
        given: 'json data and xpath'
            def jsonData = 'some json'
            def xpath = '/test-node'
        when: 'add list node element is invoked'
            objectUnderTest.addListNodeElements(cmHandle, xpath, jsonData)
        then: 'the save list elements is invoked once with the expected parameters'
            1 * mockCpsDataService.saveListElements(expectedDataspaceName, cmHandle, xpath, jsonData, noTimestamp)
    }

    def 'Write resource data for pass-through running from dmi using POST #scenario cm handle properties.'() {
        given: 'a data node'
            def dataNode = getDataNode(includeCmHandleProperties)
        and: 'cpsDataService returns valid datanode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        when: 'get resource data is called'
            objectUnderTest.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'testResourceId', CREATE,
                '{some-json}', 'application/json')
        then: 'dmi called with correct data'
            1 * mockDmiDataOperations.writeResourceDataPassThroughRunningFromDmi('testCmHandle', 'testResourceId',
                CREATE, '{some-json}', 'application/json')
                >> { new ResponseEntity<>(HttpStatus.CREATED) }
        where:
            scenario  | includeCmHandleProperties || expectedJsonForCmhandleProperties
            'with'    | true                      || '{"testName":"testValue"}'
            'without' | false                     || '{}'
    }

    def 'Write resource data for pass-through running from dmi using POST "not found" response (from DMI).'() {
        given: 'a data node'
            def dataNode = getDataNode(true)
        and: 'cpsDataService returns valid dataNode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'dmi returns a response with 404 status code'
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

    def 'Get data node.'() {
        when: 'get data node is invoked'
            objectUnderTest.getDataNode(cmHandle, 'some xpath', fetchDescendantsOption)
        then: 'the persistence data service is called once with the correct parameters'
            1 * mockCpsDataService.getDataNode(expectedDataspaceName, cmHandle, 'some xpath', fetchDescendantsOption)
        where: 'all fetch descendants options are supported'
            fetchDescendantsOption << FetchDescendantsOption.values()
    }

    def 'Get resource data for pass-through operational from dmi.'() {
        given: 'a data node'
            def dataNode = getDataNode(true)
        and: 'get data node is called'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'get resource data from dmi is called'
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
        then: 'dmi returns a json response'
            response == 'result-json'
    }

    def 'Get resource data for pass-through operational from dmi with Json Processing Exception.'() {
        given: 'a data node'
            def dataNode = getDataNode(true)
        and: 'cps data service returns valid data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'objectMapper not able to parse object'
            def mockObjectMapper = Mock(ObjectMapper)
            objectUnderTest.objectMapper = mockObjectMapper
            mockObjectMapper.writeValueAsString(_) >> { throw new JsonProcessingException('testException') }
        and: 'dmi returns NOK response'
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

    def 'Get resource data for pass-through operational from dmi return NOK response.'() {
        given: 'a data node'
            def dataNode = getDataNode(true)
        and: 'cps data service returns valid data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'dmi returns NOK response'
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

    def 'Get resource data for pass-through running from dmi.'() {
        given: 'a data node'
            def dataNode = getDataNode(true)
        and: 'cpsDataService returns valid data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'dmi returns valid response and data'
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

    def 'Get resource data for pass-through running from dmi return NOK response.'() {
        given: 'a data node'
            def dataNode = getDataNode(true)
        and: 'cpsDataService returns valid dataNode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'dmi returns NOK response'
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

    def 'Update data node leaves.'() {
        given: 'json data and xpath'
            def jsonData = 'some json'
            def xpath = '/xpath'
        when: 'update node leaves is invoked'
            objectUnderTest.updateNodeLeaves(cmHandle, xpath, jsonData)
        then: 'the persistence service is called once with the correct parameters'
            1 * mockCpsDataService.updateNodeLeaves(expectedDataspaceName, cmHandle, xpath, jsonData, noTimestamp)
    }

    def 'Replace data node tree.'() {
        given: 'json data and xpath'
            def jsonData = 'some json'
            def xpath = '/xpath'
        when: 'replace node tree is invoked'
            objectUnderTest.replaceNodeTree(cmHandle, xpath, jsonData)
        then: 'the persistence service is called once with the correct parameters'
            1 * mockCpsDataService.replaceNodeTree(expectedDataspaceName, cmHandle, xpath, jsonData, noTimestamp)
    }

    def 'Update resource data for pass-through running from dmi using POST #scenario cm handle properties.'() {
        given: 'a data node'
            def dataNode = getDataNode(includeCmHandleProperties)
        and: 'cpsDataService returns valid datanode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        when: 'get resource data is called'
            objectUnderTest.writeResourceDataPassThroughRunningForCmHandle('testCmHandle',
                'testResourceId', UPDATE,
                '{some-json}', 'application/json')
        then: 'dmi called with correct data'
            1 * mockDmiDataOperations.writeResourceDataPassThroughRunningFromDmi('testCmHandle', 'testResourceId',
                UPDATE, '{some-json}', 'application/json')
                >> { new ResponseEntity<>(HttpStatus.OK) }
        where:
            scenario  | includeCmHandleProperties || expectedJsonForCmhandleProperties
            'with'    | true                      || '{"testName":"testValue"}'
            'without' | false                     || '{}'
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

    def 'Query data nodes by cps path with #fetchDescendantsOption.'() {
        given: 'a cps path'
            def cpsPath = '/cps-path'
        when: 'query data nodes is invoked'
            objectUnderTest.queryDataNodes(cmHandle, cpsPath, fetchDescendantsOption)
        then: 'the persistence query service is called once with the correct parameters'
            1 * mockCpsQueryService.queryDataNodes(expectedDataspaceName, cmHandle, cpsPath, fetchDescendantsOption)
        where: 'all fetch descendants options are supported'
            fetchDescendantsOption << FetchDescendantsOption.values()
    }

    def getDataNode(boolean includeCmHandleProperties) {
        def dataNode = new DataNode()
        dataNode.leaves = ['dmi-service-name': 'testDmiService']
        if (includeCmHandleProperties) {
            def cmHandlePropertyDataNode = new DataNode()
            cmHandlePropertyDataNode.leaves = ['name': 'testName', 'value': 'testValue']
            dataNode.childDataNodes = [cmHandlePropertyDataNode]
        }
        return dataNode
    }
}
