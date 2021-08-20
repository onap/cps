/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
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

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsQueryService
import org.onap.cps.ncmp.api.impl.exception.NcmpException
import org.onap.cps.ncmp.api.impl.operation.DmiOperations
import org.onap.cps.ncmp.api.models.CmHandle
import org.onap.cps.ncmp.api.models.DmiPluginRegistration
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.model.DataNode
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Shared
import spock.lang.Specification

class NetworkCmProxyDataServiceImplSpec extends Specification {

    @Shared
    def persistenceCmHandle = new CmHandle()
    @Shared
    def cmHandlesArray = ['cmHandle001']

    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsQueryService = Mock(CpsQueryService)
    def mockDmiOperations = Mock(DmiOperations)
    def objectUnderTest = new NetworkCmProxyDataServiceImpl(mockDmiOperations, mockCpsDataService, mockCpsQueryService, new ObjectMapper())

    def cmHandle = 'some handle'
    def cmHandleXPath = "/dmi-registry/cm-handles[@id='testCmHandle']"

    def expectedDataspaceName = 'NFP-Operational'
    def 'Query data nodes by cps path with #fetchDescendantsOption.'() {
        given: 'a cm Handle and a cps path'
            def cpsPath = '/cps-path'
        when: 'queryDataNodes is invoked'
            objectUnderTest.queryDataNodes(cmHandle, cpsPath, fetchDescendantsOption)
        then: 'the persistence service is called once with the correct parameters'
            1 * mockCpsQueryService.queryDataNodes(expectedDataspaceName, cmHandle, cpsPath, fetchDescendantsOption)
        where: 'all fetch descendants options are supported'
            fetchDescendantsOption << FetchDescendantsOption.values()
    }
    def 'Create full data node: #scenario.'() {
        given: 'a cm handle and root xpath'
            def jsonData = 'some json'
        when: 'createDataNode is invoked'
            objectUnderTest.createDataNode(cmHandle, xpath, jsonData)
        then: 'the CPS service method is invoked once with the expected parameters'
            1 * mockCpsDataService.saveData(expectedDataspaceName, cmHandle, jsonData)
        where: 'following parameters were used'
            scenario           | xpath
            'no xpath'         | ''
            'root level xpath' | '/'
    }
    def 'Create child data node.'() {
        given: 'a cm handle and parent node xpath'
            def jsonData = 'some json'
            def xpath = '/test-node'
        when: 'createDataNode is invoked'
            objectUnderTest.createDataNode(cmHandle, xpath, jsonData)
        then: 'the CPS service method is invoked once with the expected parameters'
            1 * mockCpsDataService.saveData(expectedDataspaceName, cmHandle, xpath, jsonData)
    }
    def 'Add list-node elements.'() {
        given: 'a cm handle and parent node xpath'
            def jsonData = 'some json'
            def xpath = '/test-node'
        when: 'addListNodeElements is invoked'
            objectUnderTest.addListNodeElements(cmHandle, xpath, jsonData)
        then: 'the CPS service method is invoked once with the expected parameters'
            1 * mockCpsDataService.saveListNodeData(expectedDataspaceName, cmHandle, xpath, jsonData)
    }
    def 'Update data node leaves.'() {
        given: 'a cm Handle and a cps path'
            def xpath = '/xpath'
            def jsonData = 'some json'
        when: 'updateNodeLeaves is invoked'
            objectUnderTest.updateNodeLeaves(cmHandle, xpath, jsonData)
        then: 'the persistence service is called once with the correct parameters'
            1 * mockCpsDataService.updateNodeLeaves(expectedDataspaceName, cmHandle, xpath, jsonData)
    }
    def 'Replace data node tree.'() {
        given: 'a cm Handle and a cps path'
            def xpath = '/xpath'
            def jsonData = 'some json'
        when: 'replaceNodeTree is invoked'
            objectUnderTest.replaceNodeTree(cmHandle, xpath, jsonData)
        then: 'the persistence service is called once with the correct parameters'
            1 * mockCpsDataService.replaceNodeTree(expectedDataspaceName, cmHandle, xpath, jsonData)
    }

    def 'Register or re-register a DMI Plugin with #scenario cm handles.'() {
        given: 'a registration '
            def dmiPluginRegistration = new DmiPluginRegistration()
            dmiPluginRegistration.dmiPlugin = 'my-server'
            persistenceCmHandle.cmHandleID = '123'
            persistenceCmHandle.cmHandleProperties = [name1: 'value1', name2: 'value2']
            dmiPluginRegistration.createdCmHandles = createdCmHandles
            dmiPluginRegistration.updatedCmHandles = updatedCmHandles
            dmiPluginRegistration.removedCmHandles = removedCmHandles
            def expectedJsonData = '{"cm-handles":[{"id":"123","dmi-service-name":"my-server","additional-properties":[{"name":"name1","value":"value1"},{"name":"name2","value":"value2"}]}]}'
        when: 'registration is updated'
            objectUnderTest.updateDmiPluginRegistration(dmiPluginRegistration)
        then: 'the CPS save list node data is invoked with the expected parameters'
            expectedCallsToSaveNode * mockCpsDataService.saveListNodeData('NCMP-Admin', 'ncmp-dmi-registry', '/dmi-registry', expectedJsonData)
        and: 'update Node and Child Data Nodes is invoked with correct parameters'
            expectedCallsToUpdateNode * mockCpsDataService.updateNodeLeavesAndExistingDescendantLeaves('NCMP-Admin',  'ncmp-dmi-registry', '/dmi-registry', expectedJsonData)
        and : 'delete list data node is invoked with the correct parameters'
            expectedCallsToDeleteListDataNode * mockCpsDataService.deleteListNodeData('NCMP-Admin', 'ncmp-dmi-registry', "/dmi-registry/cm-handles[@id='cmHandle001']")
        where:
            scenario                        | createdCmHandles       | updatedCmHandles       | removedCmHandles || expectedCallsToSaveNode   | expectedCallsToUpdateNode | expectedCallsToDeleteListDataNode
            'create'                        | [persistenceCmHandle ] | []                     | []               || 1                         | 0                         | 0
            'update'                        | []                     | [persistenceCmHandle ] | []               || 0                         | 1                         | 0
            'delete'                        | []                     | []                     | cmHandlesArray   || 0                         | 0                         | 1
            'create, update and delete'     | [persistenceCmHandle ] | [persistenceCmHandle ] | cmHandlesArray   || 1                         | 1                         | 1

    }

    def 'Register a DMI Plugin for the given cmHandle without additional properties.'() {
        given: 'a registration without cmHandle properties '
            def dmiPluginRegistration = new DmiPluginRegistration()
            dmiPluginRegistration.dmiPlugin = 'my-server'
            persistenceCmHandle.cmHandleID = '123'
            persistenceCmHandle.cmHandleProperties = null
            dmiPluginRegistration.createdCmHandles = [persistenceCmHandle ]
            def expectedJsonData = '{"cm-handles":[{"id":"123","dmi-service-name":"my-server","additional-properties":[]}]}'
        when: 'registration is updated'
            objectUnderTest.updateDmiPluginRegistration(dmiPluginRegistration)
        then: 'the CPS save list node data is invoked with the expected parameters'
            1 * mockCpsDataService.saveListNodeData('NCMP-Admin', 'ncmp-dmi-registry', '/dmi-registry', expectedJsonData)
    }

    def 'Get resource data for pass-through operational from dmi.'() {
        given: 'data node representing cmHandle and its properties'
            def cmHandleDataNode = prepareCmHandleDataNode()
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataOperationalForCmHandle('testCmHandle',
            'testResourceId',
            'testAcceptParam',
            'testFieldQuery',
            5)
        then: 'cps data service is being called once to get data node'
            1 * mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> cmHandleDataNode
        and: 'dmi operation is being called to get resource data'
            1 * mockDmiOperations.getResourceDataOperationalFromDmi('testDmiService',
                    'testCmHandle',
                    'testResourceId',
                    'testFieldQuery',
                    5,
                    'testAcceptParam',
            '{"operation":"read","cmHandleProperties":{"testName":"testValue"}}') >> new ResponseEntity<>('result-json', HttpStatus.OK)
        and: 'dmi returns ok response'
            response == 'result-json'
    }

    def 'Get resource data for pass-through operational from dmi threw parsing exception.'() {
        given: 'data node representing cmHandle and its properties'
            def cmHandleDataNode = prepareCmHandleDataNode()
        and: 'cps data service returns valid cmHandle data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> cmHandleDataNode
        and: 'objectMapper not able to parse object'
            def mockObjectMapper = Mock(ObjectMapper)
            objectUnderTest.objectMapper = mockObjectMapper
            mockObjectMapper.writeValueAsString(_) >> { throw new JsonProcessingException("testException") }
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataOperationalForCmHandle('testCmHandle',
                    'testResourceId',
                    'testAcceptParam',
                    'testFieldQuery',
                    5)
        then: 'exception is thrown'
            thrown(NcmpException.class)
    }

    def 'Get resource data for pass-through operational from dmi return NOK response.'() {
        given: 'data node representing cmHandle and its properties'
            def cmHandleDataNode = prepareCmHandleDataNode()
        and: 'cps data service returns valid cmHandle data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> cmHandleDataNode
        and: 'dmi returns NOK response'
            mockDmiOperations.getResourceDataOperationalFromDmi('testDmiService',
                    'testCmHandle',
                    'testResourceId',
                    'testFieldQuery',
                    5,
                    'testAcceptParam',
                    '{"operation":"read","cmHandleProperties":{"testName":"testValue"}}')
                    >> new ResponseEntity<>('NOK-json', HttpStatus.NOT_FOUND)
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataOperationalForCmHandle('testCmHandle',
                    'testResourceId',
                    'testAcceptParam',
                    'testFieldQuery',
                    5)
        then: 'exception is thrown'
            thrown(NcmpException.class)
    }

    def 'Get resource data for pass-through running from dmi.'() {
        given: 'data node representing cmHandle and its properties'
            def cmHandleDataNode = prepareCmHandleDataNode()
        and: 'cpsDataService returns valid dataNode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> cmHandleDataNode
        and: 'dmi returns valid response and data'
            mockDmiOperations.getResourceDataPassThroughRunningFromDmi('testDmiService',
                    'testCmHandle',
                    'testResourceId',
                    'testFieldQuery',
                    5,
                    'testAcceptParam',
                    '{"operation":"read","cmHandleProperties":{"testName":"testValue"}}') >> new ResponseEntity<>('{result-json}', HttpStatus.OK)
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataPassThroughRunningForCmHandle('testCmHandle',
                    'testResourceId',
                    'testAcceptParam',
                    'testFieldQuery',
                    5)
        then: 'get resource data returns expected response'
            response == '{result-json}'
    }

    def 'Get resource data for pass-through running from dmi threw parsing exception.'() {
        given: 'data node representing cmHandle and its properties'
            def cmHandleDataNode = prepareCmHandleDataNode()
        and: 'cpsDataService returns valid dataNode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> cmHandleDataNode
        and: 'objectMapper not able to parse object'
            def mockObjectMapper = Mock(ObjectMapper)
            objectUnderTest.objectMapper = mockObjectMapper
            mockObjectMapper.writeValueAsString(_) >> { throw new JsonProcessingException("testException") }
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataPassThroughRunningForCmHandle('testCmHandle',
                    'testResourceId',
                    'testAcceptParam',
                    'testFieldQuery',
                    5)
        then: 'exception is thrown'
            thrown(NcmpException.class)
    }

    def 'Get resource data for pass-through running from dmi return NOK response.'() {
        given: 'data node representing cmHandle and its properties'
            def cmHandleDataNode = prepareCmHandleDataNode()
        and: 'cpsDataService returns valid dataNode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> cmHandleDataNode
        and: 'dmi returns NOK response'
            mockDmiOperations.getResourceDataPassThroughRunningFromDmi('testDmiService',
                    'testCmHandle',
                    'testResourceId',
                    'testFieldQuery',
                    5,
                    'testAcceptParam',
                    '{"operation":"read","cmHandleProperties":{"testName":"testValue"}}')
                    >> new ResponseEntity<>('NOK-json', HttpStatus.NOT_FOUND)
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataPassThroughRunningForCmHandle('testCmHandle',
                    'testResourceId',
                    'testAcceptParam',
                    'testFieldQuery',
                    5)
        then: 'exception is thrown'
            thrown(NcmpException.class)
    }

    def 'Write resource data for pass-through running from dmi using POST.'() {
        given: 'data node representing cmHandle and its properties'
            def cmHandleDataNode = prepareCmHandleDataNode()
        and: 'cpsDataService returns valid dataNode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> cmHandleDataNode
        when: 'get resource data is called'
            objectUnderTest.createResourceDataPassThroughRunningForCmHandle('testCmHandle',
                    'testResourceId',
                    '{some-json}', 'application/json')
        then: 'dmi called with correct data'
            1 * mockDmiOperations.createResourceDataPassThroughRunningFromDmi('testDmiService',
                'testCmHandle',
                'testResourceId',
                '{"operation":"create","dataType":"application/json","data":"{some-json}","cmHandleProperties":{"testName":"testValue"}}')
                >> { new ResponseEntity<>(HttpStatus.CREATED) }
    }

    def 'Write resource data for pass-through running from dmi using POST "not found" response (from DMI).'() {
        given: 'data node representing cmHandle and its properties'
            def cmHandleDataNode = prepareCmHandleDataNode()
        and: 'cpsDataService returns valid dataNode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> cmHandleDataNode
        and: 'dmi throws exception'
            1 * mockDmiOperations.createResourceDataPassThroughRunningFromDmi(_ as String, _ as String, _ as String, _ as String)
                    >> { new ResponseEntity<>(HttpStatus.NOT_FOUND) }
        when: 'get resource data is called'
            objectUnderTest.createResourceDataPassThroughRunningForCmHandle('testCmHandle',
                    'testResourceId',
                    '{some-json}', 'application/json')
        then: 'exception is thrown'
            thrown(NcmpException.class)
    }

    private DataNode prepareCmHandleDataNode() {
        def cmHandleDataNode = new DataNode()
        cmHandleDataNode.leaves = ['dmi-service-name': 'testDmiService']
        def cmHandlePropertyDataNode = new DataNode()
        cmHandlePropertyDataNode.leaves = ['name': 'testName', 'value': 'testValue']
        cmHandleDataNode.childDataNodes = [cmHandlePropertyDataNode]
        cmHandleDataNode
    }
}
