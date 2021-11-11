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

import org.onap.cps.spi.model.ModuleReference
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import spock.lang.Shared

class DmiModelOperationsSpec extends DmiOperationsBaseSpec {

    @Shared
    def newModuleReferences = [new ModuleReference('mod1','A'), new ModuleReference('mod2','X')]

    def objectUnderTest = new DmiModelOperations(mockCmHandlePropertiesRetriever, objectMapper, mockDmiRestClient)

    def 'get module references for a persistence cm handle #scenario.'() {
        given: 'a persistence cm handle for #cmHandleId'
            mockPersistenceCmHandleRetrieval(additionalPropertiesObject)
        and: 'a positive response from dmi service when it is called with tha expected parameters'
            def responseFromDmi = new ResponseEntity<String>(HttpStatus.OK)
            mockDmiRestClient.postOperationWithJsonData("${dmiServiceName}/dmi/v1/ch/${cmHandleId}/modules",
                '{"cmHandleProperties":' + expectedAdditionalPropertiesInRequest + '}', [:]) >> responseFromDmi
        when: 'a get module references is called'
            def result = objectUnderTest.getModuleReferences(persistenceCmHandle)
        then: 'the result is the response from dmi service'
            assert result == responseFromDmi
        where:
            scenario               | additionalPropertiesObject || expectedAdditionalPropertiesInRequest
            'with properties'      | [sampleAdditionalProperty] || '{"prop1":"val1"}'
            'with null properties' | null                       || "{}"
            'without properties'   | []                         || "{}"
    }

    def 'get new yang resources from DMI using persistence cm handle #scenario.'() {
        given: 'a persistence cm handle for #cmHandleId'
            mockPersistenceCmHandleRetrieval(additionalPropertiesObject)
        and: 'a positive response from dmi service when it is called with tha expected parameters'
            def responseFromDmi = new ResponseEntity<String>(HttpStatus.OK)
            mockDmiRestClient.postOperationWithJsonData("${dmiServiceName}/dmi/v1/ch/${cmHandleId}/moduleResources",
            '{"data":{"modules":[' + expectedModuleReferencesInRequest + ']},"cmHandleProperties":'+expectedAdditionalPropertiesInRequest+'}',
            [:]) >> responseFromDmi
        when: 'get new yang resources from dmi service'
            def result = objectUnderTest.getNewYangResourcesFromDmi(persistenceCmHandle, unknownModuleReferences)
        then: 'the result is the response from dmi service'
            assert result == responseFromDmi
        where:
            scenario                                | additionalPropertiesObject | unknownModuleReferences || expectedAdditionalPropertiesInRequest | expectedModuleReferencesInRequest
            'with module references and properties' | [sampleAdditionalProperty] | newModuleReferences     || '{"prop1":"val1"}'                    | '{"name":"mod1","revision":"A"},{"name":"mod2","revision":"X"}'
            'without module references'             | [sampleAdditionalProperty] | []                      || '{"prop1":"val1"}'                    | ''
            'without properties'                    | []                         | newModuleReferences     || '{}'                                  | '{"name":"mod1","revision":"A"},{"name":"mod2","revision":"X"}'
    }

    def 'get new yang resources from DMI with additional properties null'() {
        given: 'a persistence cm handle for #cmHandleId'
            mockPersistenceCmHandleRetrieval(null)
        when: 'a get new yang resources from dmi is called'
            objectUnderTest.getNewYangResourcesFromDmi(persistenceCmHandle, [])
        then: 'a null pointer is thrown (we might need to address this later)'
            thrown(NullPointerException)
    }

}
