/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada
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

package org.onap.cps.ncmp.impl.inventory.sync

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.impl.dmi.DmiOperationsBaseSpec
import org.onap.cps.ncmp.impl.dmi.DmiProperties
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters
import org.onap.cps.api.model.ModuleReference
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared

import static org.onap.cps.ncmp.api.data.models.OperationType.READ
import static org.onap.cps.ncmp.impl.models.RequiredDmiService.MODEL

@SpringBootTest
@ContextConfiguration(classes = [DmiProperties, DmiModelOperations])
class DmiModelOperationsSpec extends DmiOperationsBaseSpec {

    def expectedModulesUrlTemplateWithVariables = new UrlTemplateParameters('myServiceName/dmi/v1/ch/{cmHandleId}/modules', ['cmHandleId': cmHandleId])
    def expectedModuleResourcesUrlTemplateWithVariables = new UrlTemplateParameters('myServiceName/dmi/v1/ch/{cmHandleId}/moduleResources', ['cmHandleId': cmHandleId])

    @Shared
    def newModuleReferences = [new ModuleReference('mod1','A'), new ModuleReference('mod2','X')]

    @Autowired
    DmiModelOperations objectUnderTest

    @SpringBean
    JsonObjectMapper spiedJsonObjectMapper = Spy(new JsonObjectMapper(new ObjectMapper()))

    def NO_AUTH_HEADER = null

    def 'Retrieving module references.'() {
        given: 'a cm handle'
            mockYangModelCmHandleRetrieval([])
        and: 'a positive response from DMI service when it is called with the expected parameters'
            def moduleReferencesAsLisOfMaps = [[moduleName: 'mod1', revision: 'A'], [moduleName: 'mod2', revision: 'X']]
            def responseFromDmi = new ResponseEntity([schemas: moduleReferencesAsLisOfMaps], HttpStatus.OK)
            mockDmiRestClient.synchronousPostOperationWithJsonData(MODEL, expectedModulesUrlTemplateWithVariables, '{"cmHandleProperties":{},"moduleSetTag":""}', READ, NO_AUTH_HEADER) >> responseFromDmi
        when: 'get module references is called'
            def result = objectUnderTest.getModuleReferences(yangModelCmHandle)
        then: 'the result consists of expected module references'
            assert result == [new ModuleReference(moduleName: 'mod1', revision: 'A'), new ModuleReference(moduleName: 'mod2', revision: 'X')]
    }

    def 'Retrieving module references edge case: #scenario.'() {
        given: 'a cm handle'
            mockYangModelCmHandleRetrieval([])
        and: 'any response from DMI service when it is called with the expected parameters'
            // TODO (toine): production code ignores any error code from DMI, this should be improved in future
            def responseFromDmi = new ResponseEntity(bodyAsMap, HttpStatus.NO_CONTENT)
            mockDmiRestClient.synchronousPostOperationWithJsonData(*_) >> responseFromDmi
        when: 'get module references is called'
            def result = objectUnderTest.getModuleReferences(yangModelCmHandle)
        then: 'the result is empty'
            assert result == []
        where: 'the DMI response body has the following content'
            scenario       | bodyAsMap
            'no modules'   | [schemas:[]]
            'modules null' | [schemas:null]
            'no schema'    | [something:'else']
            'no body'      | null
    }

    def 'Retrieving module references, DMI property handling:  #scenario.'() {
        given: 'a cm handle'
            mockYangModelCmHandleRetrieval(dmiProperties)
        and: 'a positive response from DMI service when it is called with tha expected parameters'
            def responseFromDmi = new ResponseEntity<String>(HttpStatus.OK)
            mockDmiRestClient.synchronousPostOperationWithJsonData(MODEL, expectedModulesUrlTemplateWithVariables,
                    '{"cmHandleProperties":' + expectedAdditionalPropertiesInRequest + ',"moduleSetTag":""}', READ, NO_AUTH_HEADER) >> responseFromDmi
        when: 'a get module references is called'
            def result = objectUnderTest.getModuleReferences(yangModelCmHandle)
        then: 'the result is the response from DMI service'
            assert result == []
        where: 'the following DMI properties are used'
            scenario             | dmiProperties               || expectedAdditionalPropertiesInRequest
            'with properties'    | [yangModelCmHandleProperty] || '{"prop1":"val1"}'
            'without properties' | []                          || '{}'
    }

    def 'Retrieving yang resources.'() {
        given: 'a cm handle'
            mockYangModelCmHandleRetrieval([])
        and: 'a positive response from DMI service when it is called with the expected parameters'
            def responseFromDmi = new ResponseEntity([[moduleName: 'mod1', revision: 'A', yangSource: 'some yang source'],
                                                      [moduleName: 'mod2', revision: 'C', yangSource: 'other yang source']], HttpStatus.OK)
            def expectedModuleReferencesInRequest = '{"name":"mod1","revision":"A"},{"name":"mod2","revision":"X"}'
            mockDmiRestClient.synchronousPostOperationWithJsonData(MODEL, expectedModuleResourcesUrlTemplateWithVariables,
                    '{"data":{"modules":[' + expectedModuleReferencesInRequest + ']},"cmHandleProperties":{}}', READ, NO_AUTH_HEADER) >> responseFromDmi
        when: 'get new yang resources from DMI service'
            def result = objectUnderTest.getNewYangResourcesFromDmi(yangModelCmHandle, newModuleReferences)
        then: 'the result has the 2 expected yang (re)sources (order is not guaranteed)'
            assert result.size() == 2
            assert result.get('mod1') == 'some yang source'
            assert result.get('mod2') == 'other yang source'
    }

    def 'Retrieving yang resources, edge case: scenario.'() {
        given: 'a cm handle'
            mockYangModelCmHandleRetrieval([])
        and: 'a positive response from DMI service when it is called with tha expected parameters'
            // TODO (toine): production code ignores any error code from DMI, this should be improved in future
            def responseFromDmi = new ResponseEntity(responseFromDmiBody, HttpStatus.NO_CONTENT)
            mockDmiRestClient.synchronousPostOperationWithJsonData(*_) >> responseFromDmi
        when: 'get new yang resources from DMI service'
            def result = objectUnderTest.getNewYangResourcesFromDmi(yangModelCmHandle, newModuleReferences)
        then: 'the result is empty'
            assert result == [:]
        where: 'the DMI response body has the following content'
            scenario      | responseFromDmiBody
            'empty array' | []
            'null array'  | null
    }

    def 'Retrieving yang resources, DMI property handling #scenario.'() {
        given: 'a cm handle'
            mockYangModelCmHandleRetrieval(dmiProperties)
        and: 'a positive response from DMI service when it is called with the expected moduleSetTag, modules and properties'
            def responseFromDmi = new ResponseEntity<>([[moduleName: 'mod1', revision: 'A', yangSource: 'some yang source']], HttpStatus.OK)
            mockDmiRestClient.synchronousPostOperationWithJsonData(MODEL, expectedModuleResourcesUrlTemplateWithVariables,
                    '{"data":{"modules":[{"name":"mod1","revision":"A"},{"name":"mod2","revision":"X"}]},"cmHandleProperties":' + expectedAdditionalPropertiesInRequest + '}',
                    READ, NO_AUTH_HEADER) >> responseFromDmi
        when: 'get new yang resources from DMI service'
            def result = objectUnderTest.getNewYangResourcesFromDmi(yangModelCmHandle, newModuleReferences)
        then: 'the result is the response from DMI service'
            assert result == [mod1:'some yang source']
        where: 'the following DMI properties are used'
            scenario                                | dmiProperties               || expectedAdditionalPropertiesInRequest
            'with module references and properties' | [yangModelCmHandleProperty] || '{"prop1":"val1"}'
            'without properties'                    | []                          || '{}'
    }

    def 'Retrieving yang resources  #scenario'() {
        given: 'a cm handle'
            mockYangModelCmHandleRetrieval([], moduleSetTag)
        and: 'a positive response from DMI service when it is called with the expected moduleSetTag'
            def responseFromDmi = new ResponseEntity<>([[moduleName: 'mod1', revision: 'A', yangSource: 'some yang source']], HttpStatus.OK)
            mockDmiRestClient.synchronousPostOperationWithJsonData(MODEL, expectedModuleResourcesUrlTemplateWithVariables,
                '{' + expectedModuleSetTagInRequest + '"data":{"modules":[{"name":"mod1","revision":"A"},{"name":"mod2","revision":"X"}]},"cmHandleProperties":{}}', READ, NO_AUTH_HEADER) >> responseFromDmi
        when: 'get new yang resources from DMI service'
            def result = objectUnderTest.getNewYangResourcesFromDmi(yangModelCmHandle, newModuleReferences)
        then: 'the result is the response from DMI service'
            assert result == [mod1:'some yang source']
        where: 'the following Module Set Tags are used'
            scenario                               | moduleSetTag       || expectedModuleSetTagInRequest
            'Without module set tag'               | ''                 || ''
            'With module set tag'                  | 'moduleSetTag1'    || '"moduleSetTag":"moduleSetTag1",'
            'Special characters in module set tag' | 'module:set#tag$2' || '"moduleSetTag":"module:set#tag$2",'
    }

    def 'Retrieving yang resources from DMI with no module references.'() {
        given: 'a cm handle'
            mockYangModelCmHandleRetrieval([])
        when: 'a get new yang resources from DMI is called with no module references'
            def result = objectUnderTest.getNewYangResourcesFromDmi(yangModelCmHandle, [])
        then: 'no resources are returned'
            assert result == [:]
        and: 'no request is sent to DMI'
            0 * mockDmiRestClient.synchronousPostOperationWithJsonData(*_)
    }

    def 'Retrieving yang resources from DMI with null DMI properties.'() {
        given: 'a cm handle'
            mockYangModelCmHandleRetrieval(null)
        when: 'a get new yang resources from DMI is called'
            objectUnderTest.getNewYangResourcesFromDmi(yangModelCmHandle, [new ModuleReference('mod1', 'A')])
        then: 'a null pointer is thrown (we might need to address this later)'
            thrown(NullPointerException)
    }

    def 'Retrieving module references with Json processing exception.'() {
        given: 'a cm handle'
            mockYangModelCmHandleRetrieval([])
        and: 'a Json processing exception occurs'
            spiedJsonObjectMapper.asJsonString(_) >> {throw (new JsonProcessingException('parsing error'))}
        when: 'a DMI operation is executed'
            objectUnderTest.getModuleReferences(yangModelCmHandle)
        then: 'an ncmp exception is thrown'
            def exceptionThrown = thrown(JsonProcessingException)
        and: 'the message indicates a parsing error'
            exceptionThrown.message.toLowerCase().contains('parsing error')
    }
}
