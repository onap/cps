/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2020 Bell Canada. All rights reserved.
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.api.impl

import org.onap.cps.TestUtils
import org.onap.cps.exceptions.CpsValidationException
import org.onap.cps.spi.DataPersistencyService
import org.opendaylight.yangtools.yang.common.Revision
import org.opendaylight.yangtools.yang.model.api.SchemaContext
import spock.lang.Specification

class CpServiceImplSpec extends Specification {

    def mockDataPersistencyService = Mock(DataPersistencyService)
    def objectUnderTest = new CpServiceImpl()

    def setup() {
        objectUnderTest.dataPersistencyService = mockDataPersistencyService
    }

    def 'Cps Service provides to its client the id assigned by the system when storing a data structure'() {
        given: 'that data persistency service is giving id 123 to a data structure it is asked to store'
            mockDataPersistencyService.storeJsonStructure(_) >> 123
        expect: 'Cps service returns the same id when storing data structure'
            objectUnderTest.storeJsonStructure('') == 123
    }

    def 'Parse and Validate a Yang Model with a Valid Yang Model'() {
        given: 'a yang model (file)'
            def yangModel = TestUtils.getResourceFileContent('bookstore.yang')
        when: 'a valid model is parsed and validated'
            def result = objectUnderTest.parseAndValidateModel(yangModel)
        then: 'Verify a schema context for that model is created with the correct identity'
            assertModule(result)
    }

    def 'Parse and Validate a Yang Model Using a File'() {
        given: 'a yang file that contains a yang model'
            File file = new File(ClassLoader.getSystemClassLoader().getResource('bookstore.yang').getFile())
        when: 'a model is parsed and validated'
            def result = objectUnderTest.parseAndValidateModel(file)
        then: 'Verify a schema context for that model is created with the correct identity'
            assertModule(result)

    }

    def assertModule(SchemaContext schemaContext){
        def optionalModule = schemaContext.findModule('stores', Revision.of('2020-09-15'))
        return schemaContext.modules.size() == 1 && optionalModule.isPresent()
    }

    def 'Parse and Validate an Invalid Model'() {
        given: 'a yang file that contains a invalid yang model'
            File file = new File(ClassLoader.getSystemClassLoader().getResource('invalid.yang').getFile())
        when: 'the model is parsed and validated'
            objectUnderTest.parseAndValidateModel(file)
        then: 'a CpsValidationException is thrown'
            thrown(CpsValidationException)
    }

    def 'Store a SchemaContext'() {
        expect: 'No exception to be thrown when a valid model (schema) is stored'
            objectUnderTest.storeSchemaContext(Stub(SchemaContext.class), "sampleDataspace")
    }

    def 'Read a JSON object with a valid identifier'(){
        given: 'that the data persistence service returns a JSON structure for identifier 1'
            mockDataPersistencyService.getJsonById(1) >> '{name : hello}'
        expect: 'that the same JSON structure is returned by CPS'
            objectUnderTest.getJsonById(1) == '{name : hello}'
    }

    def 'Read a JSON object with an identifier that does not exist'(){
        given: 'that the data persistence service throws an exception'
            def exceptionThrownByPersistenceService = new IllegalStateException()
            mockDataPersistencyService.getJsonById(_) >> {throw exceptionThrownByPersistenceService}
        when: 'we try to get the JSON structure'
            objectUnderTest.getJsonById(1);
        then: 'the same exception is thrown by CPS'
            thrown(IllegalStateException)
    }

    def 'Delete a JSON object with a valid identifier'(){
        given: 'that the data persistence service can delete a JSON structure for identifier 1'
            mockDataPersistencyService.deleteJsonById(1)
        expect: 'No exception is thrown when we delete a JSON structure with identifier 1'
            objectUnderTest.deleteJsonById(1)
    }

    def 'Delete a JSON object with an identifier that does not exist'(){
        given: 'that the data persistence service throws an exception'
            mockDataPersistencyService.deleteJsonById(_) >> {throw new IllegalStateException()}
        when: 'we try to delete a JSON structure'
            objectUnderTest.deleteJsonById(100);
        then: 'the same exception is thrown by CPS'
            thrown(IllegalStateException)
    }
}
