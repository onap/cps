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

    def mockCpsDataService = Mock(CpsDataService)
    def mockCpsQueryService = Mock(CpsQueryService)
    def mockDmiOperations = Mock(DmiOperations)
    def objectUnderTest = new NetworkCmProxyDataServiceImpl(mockDmiOperations, mockCpsDataService, mockCpsQueryService, new ObjectMapper())

    def cmHandle = 'some handle'

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
            def dmiRegistryAnchor = 'ncmp-dmi-registry'
            def dmiPluginRegistration = new DmiPluginRegistration()
            dmiPluginRegistration.dmiPlugin = 'my-server'
            persistenceCmHandle.cmHandleID = '123'
            persistenceCmHandle.cmHandleProperties = [name1: 'value1', name2: 'value2']
            dmiPluginRegistration.createdCmHandles = createdCmHandles
            dmiPluginRegistration.updatedCmHandles = updatedCmHandles
            def expectedJsonData = '{"cm-handles":[{"id":"123","dmi-service-name":"my-server","additional-properties":[{"name":"name1","value":"value1"},{"name":"name2","value":"value2"}]}]}'
        when: 'registration is updated'
            objectUnderTest.updateDmiPluginRegistration(dmiPluginRegistration)
        then: 'the CPS save list node data is invoked with the expected parameters'
            expectedCallsToSaveNode * mockCpsDataService.saveListNodeData('NCMP-Admin', 'ncmp-dmi-registry', '/dmi-registry', expectedJsonData)
        and: 'update Node and Child Data Nodes is invoked with correct parameter'
            expectedCallsToUpdateNode * mockCpsDataService.updateNodeLeavesAndExistingDescendantLeaves('NCMP-Admin', dmiRegistryAnchor, '/dmi-registry', expectedJsonData)
        where:
            scenario                           | createdCmHandles       | updatedCmHandles       || expectedCallsToSaveNode   | expectedCallsToUpdateNode
            'create'                | [persistenceCmHandle ] | []                     || 1                         | 0
            'update'                | []                     | [persistenceCmHandle ] || 0                         | 1
            'create and update'     | [persistenceCmHandle ] | [persistenceCmHandle ] || 1                         | 1

    }
    def 'Get resource data for pass-through operational from dmi.'() {
        given: 'xpath'
            def xpath = "/dmi-registry/cm-handles[@id='testCmHandle']"
        and: 'data node'
            def dataNode = new DataNode()
            dataNode.leaves = ['dmi-service-name':'testDmiService']
            def childDataNode = new DataNode()
            childDataNode.leaves = ['name':'testName','value':'testValue']
            dataNode.childDataNodes = [childDataNode]
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataOperationalFoCmHandle('testCmHandle',
            'testResourceId',
            'testAcceptParam',
            'testFieldQuery',
            5)
        then: 'cps data service is being called once to get data node'
            1 * mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    xpath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'dmi operation is being calle to get resource data'
            1 * mockDmiOperations.getResouceDataFromDmi('testDmiService',
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
        given: 'xpath'
            def xpath = "/dmi-registry/cm-handles[@id='testCmHandle']"
        and: 'data node'
            def dataNode = new DataNode()
            dataNode.leaves = ['dmi-service-name':'testDmiService']
            def childDataNode = new DataNode()
            childDataNode.leaves = ['name':'testName','value':'testValue']
            dataNode.childDataNodes = [childDataNode]
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    xpath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'objectMapper not able to parse object'
            def mockObjectMapper = Mock(ObjectMapper)
            objectUnderTest.objectMapper = mockObjectMapper
            mockObjectMapper.writeValueAsString(_) >> { throw new JsonProcessingException("testException") }
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataOperationalFoCmHandle('testCmHandle',
                    'testResourceId',
                    'testAcceptParam',
                    'testFieldQuery',
                    5)
        then: 'exception is thrown'
            thrown(NcmpException.class)
    }
    def 'Get resource data for pass-through operational from dmi return NOK response.'() {
        given: 'xpath'
            def xpath = "/dmi-registry/cm-handles[@id='testCmHandle']"
        and: 'data node'
            def dataNode = new DataNode()
            dataNode.leaves = ['dmi-service-name':'testDmiService']
            def childDataNode = new DataNode()
            childDataNode.leaves = ['name':'testName','value':'testValue']
            dataNode.childDataNodes = [childDataNode]
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    xpath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> dataNode
        and: 'dmi returns NOK response'
            mockDmiOperations.getResouceDataFromDmi('testDmiService',
                    'testCmHandle',
                    'testResourceId',
                    'testFieldQuery',
                    5,
                    'testAcceptParam',
                    '{"operation":"read","cmHandleProperties":{"testName":"testValue"}}')
                    >> new ResponseEntity<>('NOK-json', HttpStatus.NOT_FOUND)
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataOperationalFoCmHandle('testCmHandle',
                    'testResourceId',
                    'testAcceptParam',
                    'testFieldQuery',
                    5)
        then: 'exception is thrown'
            thrown(NcmpException.class)
    }
}
