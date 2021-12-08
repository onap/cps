/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.operations

import com.fasterxml.jackson.core.JsonProcessingException
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration
import org.onap.cps.ncmp.api.impl.exception.NcmpException
import org.onap.cps.spi.model.ModuleReference
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared

@SpringBootTest
@ContextConfiguration(classes = [NcmpConfiguration.DmiProperties, DmiModelOperations])
class DmiModelOperationsSpec extends DmiOperationsBaseSpec {

    @Shared
    def newModuleReferences = [new ModuleReference('mod1','A'), new ModuleReference('mod2','X')]

    @Autowired
    DmiModelOperations objectUnderTest

    def 'Retrieving module references.'() {
        given: 'a persistence cm handle for #cmHandleId'
            mockPersistenceCmHandleRetrieval([])
        and: 'a positive response from dmi service when it is called with the expected parameters'
            def moduleReferencesAsLisOfMaps = [[moduleName:'mod1',revision:'A'],[moduleName:'mod2',revision:'X']]
            def responseFromDmi = new ResponseEntity([schemas:moduleReferencesAsLisOfMaps], HttpStatus.OK)
            mockDmiRestClient.postOperationWithJsonData("${dmiServiceName}/dmi/v1/ch/${cmHandleId}/modules",
                '{"cmHandleProperties":{}}', [:]) >> responseFromDmi
        when: 'get module references is called'
            def result = objectUnderTest.getModuleReferences(persistenceCmHandle)
        then: 'the result consists of expected module references'
            assert result == [new ModuleReference(moduleName:'mod1',revision:'A'), new ModuleReference(moduleName:'mod2',revision:'X')]
    }

    def 'Retrieving module references edge case: #scenario.'() {
        given: 'a persistence cm handle for #cmHandleId'
            mockPersistenceCmHandleRetrieval([])
        and: 'any response from dmi service when it is called with the expected parameters'
            // TODO (toine): production code ignores any error code from DMI, this should be improved in future
            def responseFromDmi = new ResponseEntity(bodyAsMap, HttpStatus.NO_CONTENT)
            mockDmiRestClient.postOperationWithJsonData(*_) >> responseFromDmi
        when: 'get module references is called'
            def result = objectUnderTest.getModuleReferences(persistenceCmHandle)
        then: 'the result is empty'
            assert result == []
        where: 'the dmi response body has the following content'
            scenario       | bodyAsMap
            'no modules'   | [schemas:[]]
            'modules null' | [schemas:null]
            'no schema'    | [something:'else']
            'no body'      | null
    }

    def 'Retrieving module references, additional property handling:  #scenario.'() {
        given: 'a persistence cm handle for #cmHandleId'
            mockPersistenceCmHandleRetrieval(additionalPropertiesObject)
        and: 'a positive response from dmi service when it is called with tha expected parameters'
            def responseFromDmi = new ResponseEntity<String>(HttpStatus.OK)
            mockDmiRestClient.postOperationWithJsonData("${dmiServiceName}/dmi/v1/ch/${cmHandleId}/modules",
                '{"cmHandleProperties":' + expectedAdditionalPropertiesInRequest + '}', [:]) >> responseFromDmi
        when: 'a get module references is called'
            def result = objectUnderTest.getModuleReferences(persistenceCmHandle)
        then: 'the result is the response from dmi service'
            assert result == []
        where: 'the following additional properties are used'
            scenario               | additionalPropertiesObject || expectedAdditionalPropertiesInRequest
            'with properties'      | [sampleAdditionalProperty] || '{"prop1":"val1"}'
            'with null properties' | null                       || '{}'
            'without properties'   | []                         || '{}'
    }

    def 'Retrieving yang resources.'() {
        given: 'a persistence cm handle for #cmHandleId'
            mockPersistenceCmHandleRetrieval([])
        and: 'a positive response from dmi service when it is called with tha expected parameters'
            def responseFromDmi = new ResponseEntity([[moduleName: 'mod1', revision: 'A', yangSource: 'some yang source'],
                                                      [moduleName: 'mod2', revision: 'C', yangSource: 'other yang source']], HttpStatus.OK)
            def expectedModuleReferencesInRequest = '{"name":"mod1","revision":"A"},{"name":"mod2","revision":"X"}'
            mockDmiRestClient.postOperationWithJsonData("${dmiServiceName}/dmi/v1/ch/${cmHandleId}/moduleResources",
                '{"data":{"modules":[' + expectedModuleReferencesInRequest + ']},"cmHandleProperties":{}}', [:]) >> responseFromDmi
        when: 'get new yang resources from dmi service'
            def result = objectUnderTest.getNewYangResourcesFromDmi(persistenceCmHandle, newModuleReferences)
        then: 'the result has the 2 expected yang (re)sources (order is not guaranteed)'
            assert result.size() == 2
            assert result.get('mod1') == 'some yang source'
            assert result.get('mod2') == 'other yang source'
    }

    def 'Retrieving yang resources, edge case: scenario.'() {
        given: 'a persistence cm handle for #cmHandleId'
            mockPersistenceCmHandleRetrieval([])
        and: 'a positive response from dmi service when it is called with tha expected parameters'
            // TODO (toine): production code ignores any error code from DMI, this should be improved in future
            def responseFromDmi = new ResponseEntity(responseFromDmiBody, HttpStatus.NO_CONTENT)
            mockDmiRestClient.postOperationWithJsonData(*_) >> responseFromDmi
        when: 'get new yang resources from dmi service'
            def result = objectUnderTest.getNewYangResourcesFromDmi(persistenceCmHandle, newModuleReferences)
        then: 'the result is empty'
            assert result == [:]
        where: 'the dmi response body has the following content'
            scenario      | responseFromDmiBody
            'empty array' | []
    }

    def 'Retrieving yang resources, additional property handling #scenario.'() {
        given: 'a persistence cm handle for #cmHandleId'
            mockPersistenceCmHandleRetrieval(additionalPropertiesObject)
        and: 'a positive response from dmi service when it is called with the expected parameters'
            def responseFromDmi = new ResponseEntity<>([[moduleName: 'mod1', revision: 'A', yangSource: 'some yang source']], HttpStatus.OK)
            mockDmiRestClient.postOperationWithJsonData("${dmiServiceName}/dmi/v1/ch/${cmHandleId}/moduleResources",
            '{"data":{"modules":[' + expectedModuleReferencesInRequest + ']},"cmHandleProperties":'+expectedAdditionalPropertiesInRequest+'}',
            [:]) >> responseFromDmi
        when: 'get new yang resources from dmi service'
            def result = objectUnderTest.getNewYangResourcesFromDmi(persistenceCmHandle, unknownModuleReferences)
        then: 'the result is the response from dmi service'
            assert result == [mod1:'some yang source']
        where: 'the following additional properties are used'
            scenario                                | additionalPropertiesObject | unknownModuleReferences || expectedAdditionalPropertiesInRequest | expectedModuleReferencesInRequest
            'with module references and properties' | [sampleAdditionalProperty] | newModuleReferences     || '{"prop1":"val1"}'                    | '{"name":"mod1","revision":"A"},{"name":"mod2","revision":"X"}'
            'without module references'             | [sampleAdditionalProperty] | []                      || '{"prop1":"val1"}'                    | ''
            'without properties'                    | []                         | newModuleReferences     || '{}'                                  | '{"name":"mod1","revision":"A"},{"name":"mod2","revision":"X"}'
    }

    def 'Retrieving yang resources from dmi with additional properties null.'() {
        given: 'a persistence cm handle for #cmHandleId'
            mockPersistenceCmHandleRetrieval(null)
        when: 'a get new yang resources from dmi is called'
            objectUnderTest.getNewYangResourcesFromDmi(persistenceCmHandle, [])
        then: 'a null pointer is thrown (we might need to address this later)'
            thrown(NullPointerException)
    }

    def 'Retrieving module references with Json processing exception.'() {
        given: 'a persistence cm handle for #cmHandleId'
            mockPersistenceCmHandleRetrieval([])
        and: 'a Json processing exception occurs'
            spyObjectMapper.writeValueAsString(_) >> {throw (new JsonProcessingException(''))}
        when: 'a dmi operation is executed'
            objectUnderTest.getModuleReferences(persistenceCmHandle)
        then: 'an ncmp exception is thrown'
            def exceptionThrown = thrown(NcmpException)
        and: 'the message indicates a parsing error'
            exceptionThrown.message.toLowerCase().contains('parsing error')
    }

}
