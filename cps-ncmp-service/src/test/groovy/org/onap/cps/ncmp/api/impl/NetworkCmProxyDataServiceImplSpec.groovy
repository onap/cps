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

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.api.CpsQueryService
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration
import org.onap.cps.ncmp.api.impl.exception.NcmpException
import org.onap.cps.ncmp.api.impl.operation.DmiOperations
import org.onap.cps.ncmp.api.models.CmHandle
import org.onap.cps.ncmp.api.models.DmiPluginRegistration
import org.onap.cps.ncmp.api.models.PersistenceCmHandle
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.exceptions.DataNodeNotFoundException
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.model.DataNode
import org.onap.cps.spi.model.ModuleReference
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
    def mockCpsModuleService = Mock(CpsModuleService)
    def mockCpsAdminService = Mock(CpsAdminService)
    def mockDmiProperties = Mock(NcmpConfiguration.DmiProperties)
    def spyObjectMapper = Spy(ObjectMapper)
    def mockRequestDetails = Mock(DmiRequestBodyBuilder)

    def objectUnderTest = new NetworkCmProxyDataServiceImpl(mockDmiOperations, mockCpsModuleService,
            mockCpsDataService, mockCpsQueryService, mockCpsAdminService, spyObjectMapper)

    def cmHandle = 'some handle'
    def noTimestamp = null
    def cmHandleXPath = "/dmi-registry/cm-handles[@id='testCmHandle']"
    def expectedDataspaceName = 'NFP-Operational'


    def 'Get data node.'() {
        when: 'queryDataNodes is invoked'
            objectUnderTest.getDataNode(cmHandle, 'some xpath', fetchDescendantsOption)
        then: 'the persistence data service is called once with the correct parameters'
            1 * mockCpsDataService.getDataNode(expectedDataspaceName, cmHandle, 'some xpath', fetchDescendantsOption)
        where: 'all fetch descendants options are supported'
            fetchDescendantsOption << FetchDescendantsOption.values()
    }

    def 'Query data nodes by cps path with #fetchDescendantsOption.'() {
        given: 'a cm Handle and a cps path'
            def cpsPath = '/cps-path'
        when: 'queryDataNodes is invoked'
            objectUnderTest.queryDataNodes(cmHandle, cpsPath, fetchDescendantsOption)
        then: 'the persistence query service is called once with the correct parameters'
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
            1 * mockCpsDataService.saveData(expectedDataspaceName, cmHandle, jsonData, noTimestamp)
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
            1 * mockCpsDataService.saveData(expectedDataspaceName, cmHandle, xpath, jsonData, noTimestamp)
    }

    def 'Add list-node elements.'() {
        given: 'a cm handle and parent node xpath'
            def jsonData = 'some json'
            def xpath = '/test-node'
        when: 'addListNodeElements is invoked'
            objectUnderTest.addListNodeElements(cmHandle, xpath, jsonData)
        then: 'the CPS service method is invoked once with the expected parameters'
            1 * mockCpsDataService.saveListElements(expectedDataspaceName, cmHandle, xpath, jsonData, noTimestamp)
    }

    def 'Update data node leaves.'() {
        given: 'a cm Handle and a cps path'
            def xpath = '/xpath'
            def jsonData = 'some json'
        when: 'updateNodeLeaves is invoked'
            objectUnderTest.updateNodeLeaves(cmHandle, xpath, jsonData)
        then: 'the persistence service is called once with the correct parameters'
            1 * mockCpsDataService.updateNodeLeaves(expectedDataspaceName, cmHandle, xpath, jsonData, noTimestamp)
    }

    def 'Replace data node tree.'() {
        given: 'a cm Handle and a cps path'
            def xpath = '/xpath'
            def jsonData = 'some json'
        when: 'replaceNodeTree is invoked'
            objectUnderTest.replaceNodeTree(cmHandle, xpath, jsonData)
        then: 'the persistence service is called once with the correct parameters'
            1 * mockCpsDataService.replaceNodeTree(expectedDataspaceName, cmHandle, xpath, jsonData, noTimestamp)
    }

    def 'Register or re-register a DMI Plugin with #scenario cm handles.'() {
        given: 'a registration '
            def objectUnderTest = getObjectUnderTestWithModelSyncDisabled()
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin:'some-plugin')
            dmiPluginRegistration.dmiPlugin = 'my-server'
            persistenceCmHandle.cmHandleID = '123'
            persistenceCmHandle.cmHandleProperties = [name1: 'value1', name2: 'value2']
            dmiPluginRegistration.createdCmHandles = createdCmHandles
            dmiPluginRegistration.updatedCmHandles = updatedCmHandles
            dmiPluginRegistration.removedCmHandles = removedCmHandles
            def expectedJsonData = '{"cm-handles":[{"id":"123","dmi-service-name":"my-server","additional-properties":[{"name":"name1","value":"value1"},{"name":"name2","value":"value2"}]}]}'
        when: 'registration is updated'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'cps save list elements is invoked with the expected parameters'
            expectedCallsToSaveNode * mockCpsDataService.saveListElements('NCMP-Admin', 'ncmp-dmi-registry',
                '/dmi-registry', expectedJsonData, noTimestamp)
        and: 'update node and child data nodes is invoked with correct parameters'
            expectedCallsToUpdateNode * mockCpsDataService.updateNodeLeavesAndExistingDescendantLeaves('NCMP-Admin',
                'ncmp-dmi-registry', '/dmi-registry', expectedJsonData, noTimestamp)
        and : 'delete list or list element is invoked with the correct parameters'
            expectedCallsToDeleteListElement * mockCpsDataService.deleteListOrListElement('NCMP-Admin',
                'ncmp-dmi-registry', "/dmi-registry/cm-handles[@id='cmHandle001']", noTimestamp)

        where:
            scenario                        | createdCmHandles      | updatedCmHandles      | removedCmHandles || expectedCallsToSaveNode   | expectedCallsToUpdateNode | expectedCallsToDeleteListElement
            'create'                        | [persistenceCmHandle] | []                    | []               || 1                         | 0                         | 0
            'update'                        | []                    | [persistenceCmHandle] | []               || 0                         | 1                         | 0
            'delete'                        | []                    | []                    | cmHandlesArray   || 0                         | 0                         | 1
            'create, update and delete'     | [persistenceCmHandle] | [persistenceCmHandle] | cmHandlesArray   || 1                         | 1                         | 1
            'no valid data'                 | null                  | null                  |  null            || 0                         | 0                         | 0
    }

    def 'Register a DMI Plugin for the given cmHandle without additional properties.'() {
        given: 'a registration without cmHandle properties '
            NetworkCmProxyDataServiceImpl objectUnderTest = getObjectUnderTestWithModelSyncDisabled()
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin:'some-plugin')
            dmiPluginRegistration.dmiPlugin = 'my-server'
            persistenceCmHandle.cmHandleID = '123'
            persistenceCmHandle.cmHandleProperties = null
            dmiPluginRegistration.createdCmHandles = [persistenceCmHandle]
            def expectedJsonData = '{"cm-handles":[{"id":"123","dmi-service-name":"my-server","additional-properties":[]}]}'
        when: 'registration is updated'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'the cps save list element is invoked with the expected parameters'
            1 * mockCpsDataService.saveListElements('NCMP-Admin', 'ncmp-dmi-registry',
                '/dmi-registry', expectedJsonData, noTimestamp)
    }

    def 'Register a DMI Plugin with JSON processing errors during #scenario.'() {
        given: 'a registration without cmHandle properties '
            NetworkCmProxyDataServiceImpl objectUnderTest = getObjectUnderTestWithModelSyncDisabled()
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin:'some-plugin')
            dmiPluginRegistration.createdCmHandles = createdCmHandles
            dmiPluginRegistration.updatedCmHandles = updatedCmHandles
        and: 'an JSON processing exception occurs'
            spyObjectMapper.writeValueAsString(_) >> { throw (new JsonProcessingException('')) }
        when: 'registration is updated'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        where:
            scenario | createdCmHandles      | updatedCmHandles
            'create' | [persistenceCmHandle] | []
            'update' | []                    | [persistenceCmHandle]
    }

    def 'Register a DMI Plugin with no data found during delete.'() {
        given: 'a registration without cmHandle properties '
            NetworkCmProxyDataServiceImpl objectUnderTest = getObjectUnderTestWithModelSyncDisabled()
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin:'some-plugin')
            dmiPluginRegistration.removedCmHandles = ['some cm handle']
        and: 'an JSON processing exception occurs'
            mockCpsDataService.deleteListOrListElement(*_) >>  { throw (new DataNodeNotFoundException('','')) }
        when: 'registration is updated'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'no exception is thrown'
            noExceptionThrown()
    }

    def 'Get resource data for pass-through operational from dmi.'() {
        given: 'data node representing cmHandle and its properties'
            def cmHandleDataNode = getCmHandleDataNodeForTest(true)
        and: 'data node is got from data service'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> cmHandleDataNode
        and: 'resource data is got from DMI'
            mockDmiOperations.getResourceDataOperationalFromDmi('testDmiService',
                'testCmHandle',
                'testResourceId',
                '(a=1,b=2)',
                'testAcceptParam',
                '{"operation":"read","cmHandleProperties":{"testName":"testValue"}}') >> new ResponseEntity<>('result-json', HttpStatus.OK)
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataOperationalForCmHandle('testCmHandle',
            'testResourceId',
            'testAcceptParam',
            '(a=1,b=2)')
        then: 'dmi returns ok response'
            response == 'result-json'
    }

    def 'Get resource data for pass-through operational from dmi threw parsing exception.'() {
        given: 'data node representing cmHandle and its properties'
            def cmHandleDataNode = getCmHandleDataNodeForTest(true)
        and: 'cps data service returns valid cmHandle data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> cmHandleDataNode
        and: 'objectMapper not able to parse object'
            def mockObjectMapper = Mock(ObjectMapper)
            objectUnderTest.objectMapper = mockObjectMapper
            mockObjectMapper.writeValueAsString(_) >> { throw new JsonProcessingException('testException') }
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataOperationalForCmHandle('testCmHandle',
                    'testResourceId',
                    'testAcceptParam',
                    '(a=1,b=2)')
        then: 'exception is thrown with the expected details'
            def exceptionThrown = thrown(NcmpException.class)
            exceptionThrown.details == 'testException'
    }

    def 'Get resource data for pass-through operational from dmi return NOK response.'() {
        given: 'data node representing cmHandle and its properties'
            def cmHandleDataNode = getCmHandleDataNodeForTest(true)
        and: 'cps data service returns valid cmHandle data node'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> cmHandleDataNode
        and: 'dmi returns NOK response'
            mockDmiOperations.getResourceDataOperationalFromDmi('testDmiService',
                    'testCmHandle',
                    'testResourceId',
                    '(a=1,b=2)',
                    'testAcceptParam',
                    '{"operation":"read","cmHandleProperties":{"testName":"testValue"}}')
                    >> new ResponseEntity<>('NOK-json', HttpStatus.NOT_FOUND)
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataOperationalForCmHandle('testCmHandle',
                    'testResourceId',
                    'testAcceptParam',
                    '(a=1,b=2)')
        then: 'exception is thrown'
            def exceptionThrown = thrown(NcmpException.class)
        and: 'details contains the original response'
            exceptionThrown.details.contains('NOK-json')
    }

    def 'Get resource data for pass-through running from dmi.'() {
        given: 'data node representing cmHandle and its properties'
            def cmHandleDataNode = getCmHandleDataNodeForTest(true)
        and: 'cpsDataService returns valid dataNode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> cmHandleDataNode
        and: 'dmi returns valid response and data'
            mockDmiOperations.getResourceDataPassThroughRunningFromDmi('testDmiService',
                    'testCmHandle',
                    'testResourceId',
                    '(a=1,b=2)',
                    'testAcceptParam',
                    '{"operation":"read","cmHandleProperties":{"testName":"testValue"}}') >> new ResponseEntity<>('{result-json}', HttpStatus.OK)
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataPassThroughRunningForCmHandle('testCmHandle',
                    'testResourceId',
                    'testAcceptParam',
                    '(a=1,b=2)')
        then: 'get resource data returns expected response'
            response == '{result-json}'
    }

    def 'Get resource data for pass-through running from dmi threw parsing exception.'() {
        given: 'data node representing cmHandle and its properties'
            def cmHandleDataNode = getCmHandleDataNodeForTest(true)
        and: 'cpsDataService returns valid dataNode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> cmHandleDataNode
        and: 'objectMapper not able to parse object'
            def mockObjectMapper = Mock(ObjectMapper)
            objectUnderTest.objectMapper = mockObjectMapper
            mockObjectMapper.writeValueAsString(_) >> { throw new JsonProcessingException('testException') }
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataPassThroughRunningForCmHandle('testCmHandle',
                    'testResourceId',
                    'testAcceptParam',
                    '(a=1,b=2)')
        then: 'exception is thrown with the expected details'
            def exceptionThrown = thrown(NcmpException.class)
            exceptionThrown.details == 'testException'
    }

    def 'Get resource data for pass-through running from dmi return NOK response.'() {
        given: 'data node representing cmHandle and its properties'
            def cmHandleDataNode = getCmHandleDataNodeForTest(true)
        and: 'cpsDataService returns valid dataNode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> cmHandleDataNode
        and: 'dmi returns NOK response'
            mockDmiOperations.getResourceDataPassThroughRunningFromDmi('testDmiService',
                    'testCmHandle',
                    'testResourceId',
                    '(a=1,b=2)',
                    'testAcceptParam',
                    '{"operation":"read","cmHandleProperties":{"testName":"testValue"}}')
                    >> new ResponseEntity<>('NOK-json', HttpStatus.NOT_FOUND)
        when: 'get resource data is called'
            def response = objectUnderTest.getResourceDataPassThroughRunningForCmHandle('testCmHandle',
                    'testResourceId',
                    'testAcceptParam',
                    '(a=1,b=2)')
        then: 'exception is thrown'
            def exceptionThrown = thrown(NcmpException.class)
        and: 'details contains the original response'
            exceptionThrown.details.contains('NOK-json')
    }

    def 'Write resource data for pass-through running from dmi using POST #scenario cm handle properties.'() {
        given: 'data node representing cmHandle #scenario cm handle properties'
            def cmHandleDataNode = getCmHandleDataNodeForTest(includeCmHandleProperties)
        and: 'cpsDataService returns valid cm-handle datanode'
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
                '{"operation":"create","dataType":"application/json","data":"{some-json}","cmHandleProperties":'
                + expectedJsonForCmhandleProperties+ '}')
                >> { new ResponseEntity<>(HttpStatus.CREATED) }
        where:
            scenario  | includeCmHandleProperties || expectedJsonForCmhandleProperties
            'with'    | true                      || '{"testName":"testValue"}'
            'without' | false                     || '{}'
    }

    def 'Write resource data for pass-through running from dmi using POST "not found" response (from DMI).'() {
        given: 'data node representing cmHandle and its properties'
            def cmHandleDataNode = getCmHandleDataNodeForTest(true)
        and: 'cpsDataService returns valid dataNode'
            mockCpsDataService.getDataNode('NCMP-Admin', 'ncmp-dmi-registry',
                    cmHandleXPath, FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS) >> cmHandleDataNode
        and: 'dmi throws exception'
            mockDmiOperations.createResourceDataPassThroughRunningFromDmi(_ as String, _ as String, _ as String, _ as String)
                    >> { new ResponseEntity<>(HttpStatus.NOT_FOUND) }
        when: 'get resource data is called'
            objectUnderTest.createResourceDataPassThroughRunningForCmHandle('testCmHandle',
                    'testResourceId',
                    '{some-json}', 'application/json')
        then: 'exception is thrown'
            def exceptionThrown = thrown(NcmpException.class)
        and: 'details contains (not found) error code: 404'
            exceptionThrown.details.contains('404')
    }

    def 'Sync model for a (new) cm handle with #scenario'() {
        given: 'persistence cm handle is given'
            def cmHandleForModelSync = new PersistenceCmHandle(id:'some cm handle', dmiServiceName: 'some service name')
        and: 'additional properties are set as required'
            if (additionalProperties!=null) {
                cmHandleForModelSync.setAdditionalProperties(additionalProperties)
            }
        and: 'dmi operations returns some module references'
            def jsonData = TestUtils.getResourceFileContent('cmHandleModules.json')
            def expectedJsonBody = '{"cmHandleProperties":' + expectedJsonForAdditionalProperties + '}'
            mockDmiProperties.getAuthUsername() >> 'someUser'
            mockDmiProperties.getAuthPassword() >> 'somePassword'
            def moduleReferencesFromCmHandleAsJson = new ResponseEntity<String>(jsonData, HttpStatus.OK)
            mockDmiOperations.getResourceFromDmiWithJsonData('some service name', expectedJsonBody, 'some cm handle', 'modules') >> moduleReferencesFromCmHandleAsJson
        and: 'CPS-Core returns list of known modules'
            mockCpsModuleService.getYangResourceModuleReferences(_) >> existingModuleResourcesInCps
        and: 'DMI-Plugin returns resource(s) for "new" module(s)'
            def moduleResources = new ResponseEntity<String>(sdncReponseBody, HttpStatus.OK)
            def jsonDataToFetchYangResource = '{"data":{"modules":[{"name":"module1","revision":"1"}]},"cmHandleProperties":' + expectedJsonForAdditionalProperties + '}'
            mockDmiOperations.getResourceFromDmiWithJsonData('some service name', jsonDataToFetchYangResource, 'some cm handle', 'moduleResources') >> moduleResources
        when: 'module Sync is triggered'
            objectUnderTest.syncModulesAndCreateAnchor(cmHandleForModelSync)
        then: 'the CPS module service is called once with the correct parameters'
            1 * mockCpsModuleService.createSchemaSetFromModules(expectedDataspaceName, cmHandleForModelSync.getId(), expectedYangResourceToContentMap, expectedKnownModules)
        and: 'admin service create anchor method has been called with correct parameters'
            1 * mockCpsAdminService.createAnchor(expectedDataspaceName, cmHandleForModelSync.getId(), cmHandleForModelSync.getId())
        where: 'the following responses are received from SDNC'
            scenario                         | additionalProperties | existingModuleResourcesInCps                                                  | sdncReponseBody                                                                     || expectedYangResourceToContentMap | expectedKnownModules                                                       | expectedJsonForAdditionalProperties
            'one unknown module'             | ['name1':'value1']   | [new ModuleReference('module2', '2'), new ModuleReference('module3', '3')]    | '[{"moduleName" : "module1", "revision" : "1","yangSource": "[some yang source]"}]' || [module1: 'some yang source']    | [new ModuleReference('module2', '2')]                                      |'{"name1":"value1"}'
            'no add. properties'             | [:]                  | [new ModuleReference('module2', '2'), new ModuleReference('module3', '3')]    | '[{"moduleName" : "module1", "revision" : "1","yangSource": "[some yang source]"}]' || [module1: 'some yang source']    | [new ModuleReference('module2', '2')]                                      |'{}'
            'additional properties is null'  | null                 | [new ModuleReference('module2', '2'), new ModuleReference('module3', '3')]    | '[{"moduleName" : "module1", "revision" : "1","yangSource": "[some yang source]"}]' || [module1: 'some yang source']    | [new ModuleReference('module2', '2')]                                      |'{}'
            'no unknown module'              | [:]                  | [new ModuleReference('module1', '1'),    new ModuleReference('module2', '2')] | '[]'                                                                                || [:]                              | [new ModuleReference('module1', '1'), new ModuleReference('module2', '2')] |'{}'
    }

    def 'Getting Yang Resources.'() {
        when: 'yang resources is called'
            objectUnderTest.getYangResourcesModuleReferences('some cm handle')
        then: 'CPS module services is invoked for the correct dataspace and cm handle'
            1 * mockCpsModuleService.getYangResourcesModuleReferences('NFP-Operational','some cm handle')
    }

    def 'Create the request body to get yang resources from DMI.'() {
        given: 'the expected json request'
            def expectedRequestBody = '{"data":{"modules":[{"name":"module1","revision":"1"},{"name":"module2","revision":"2"}]},"cmHandleProperties":{"name1":"value1"}}'
        and: 'module references and cm handle properties'
            def moduleReferences = [new ModuleReference('module1', '1'),new ModuleReference('module2', '2')]
            def cmHandleProperties = ['name1':'value1']
        when: 'get request body to fetch yang resources from DMI is called'
            def result = objectUnderTest.getRequestBodyToFetchYangResourceFromDmi(moduleReferences, cmHandleProperties)
        then: 'the result is the same as the expected request body'
            result == expectedRequestBody
    }

    def 'Get cm handle identifiers for the given module names.'() {
        when: 'execute a cm handle search for the given module names'
            objectUnderTest.executeCmHandleHasAllModulesSearch(['some-module-name'])
        then: 'get anchor identifiers is invoked  with the expected parameters'
            1 * mockCpsAdminService.queryAnchorNames('NFP-Operational', ['some-module-name'])
    }

    def 'Dmi plugin registration with #scenario'() {
        given: 'a registration '
            def objectUnderTest = getObjectUnderTestWithModelSyncDisabled()
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin:dmiPlugin, dmiModelPlugin:dmiModelPlugin,
                dmiDataPlugin:dmiDataPlugin)
            dmiPluginRegistration.createdCmHandles = [persistenceCmHandle]
        when: 'registration is called with correct DMI plugin information'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'no NcmpException is thrown and registration is called'
            1 * objectUnderTest.parseAndCreateCmHandlesInDmiRegistrationAndSyncModule(dmiPluginRegistration)
        where:
            scenario                          | dmiPlugin  | dmiModelPlugin | dmiDataPlugin
            'combined DMI plugin'             | 'service1' | ''             | ''
            'data & model DMI plugins'        | ''         | 'service1'     | 'service2'
            'data & model using same service' | ''         | 'service1'     | 'service1'
    }

    def 'Invalid dmi plugin registration with #scenario'() {
        given: 'a registration '
            def objectUnderTest = getObjectUnderTestWithModelSyncDisabled()
            def dmiPluginRegistration = new DmiPluginRegistration(dmiPlugin:dmiPlugin, dmiModelPlugin:dmiModelPlugin,
                dmiDataPlugin:dmiDataPlugin)
            dmiPluginRegistration.createdCmHandles = [persistenceCmHandle]
        when: 'registration is called with incorrect DMI plugin information'
            objectUnderTest.updateDmiRegistrationAndSyncModule(dmiPluginRegistration)
        then: 'an NcmpException is thrown with correct message details'
            def exceptionThrown = thrown(NcmpException)
            assert exceptionThrown.getMessage().contains(expectedMessageDetails)
        and: 'registration is not called'
            0 * objectUnderTest.parseAndCreateCmHandlesInDmiRegistrationAndSyncModule(dmiPluginRegistration)
        where:
            scenario              | dmiPlugin  | dmiModelPlugin | dmiDataPlugin || expectedMessageDetails
            'no DMI plugin'       | ''         | ''             | ''            || 'No DMI plugin service names'
            'all DMI plugins'     | 'service1' | 'service2'     | 'service3'    || 'Invalid combination of plugin service names'
            'no model DMI plugin' | 'service1' | ''             | 'service2'    || 'Invalid combination of plugin service names'
            'no data DMI plugin'  | 'service1' | 'service2'     | ''            || 'Invalid combination of plugin service names'
    }

    def getObjectUnderTestWithModelSyncDisabled() {
        def objectUnderTest = Spy(new NetworkCmProxyDataServiceImpl(mockDmiOperations, mockCpsModuleService,
                mockCpsDataService, mockCpsQueryService, mockCpsAdminService, spyObjectMapper))
        objectUnderTest.syncModulesAndCreateAnchor(_) >> null
        return objectUnderTest
    }

    def getCmHandleDataNodeForTest(boolean includeCmHandleProperties) {
        def cmHandleDataNode = new DataNode()
        cmHandleDataNode.leaves = ['dmi-service-name': 'testDmiService']
        if (includeCmHandleProperties) {
            def cmHandlePropertyDataNode = new DataNode()
            cmHandlePropertyDataNode.leaves = ['name': 'testName', 'value': 'testValue']
            cmHandleDataNode.childDataNodes = [cmHandlePropertyDataNode]
        }
        return cmHandleDataNode
    }

}
