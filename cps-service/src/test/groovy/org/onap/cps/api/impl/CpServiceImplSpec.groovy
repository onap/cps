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


import org.onap.cps.spi.DataPersistenceService
import spock.lang.Specification

class CpServiceImplSpec extends Specification {

    def mockDataPersistenceService = Mock(DataPersistenceService)
    def objectUnderTest = new CpServiceImpl()

    def setup() {
        objectUnderTest.dataPersistenceService = mockDataPersistenceService
    }

    def 'Cps Service provides to its client the id assigned by the system when storing a data structure'() {
        given: 'that data persistence service is giving id 123 to a data structure it is asked to store'
            mockDataPersistenceService.storeJsonStructure(_) >> 123
        expect: 'Cps service returns the same id when storing data structure'
            objectUnderTest.storeJsonStructure('') == 123
    }

    def 'Read a JSON object with a valid identifier'(){
        given: 'that the data persistence service returns a JSON structure for identifier 1'
            mockDataPersistenceService.getJsonById(1) >> '{name : hello}'
        expect: 'that the same JSON structure is returned by CPS'
            objectUnderTest.getJsonById(1) == '{name : hello}'
    }

    def 'Read a JSON object with an identifier that does not exist'(){
        given: 'that the data persistence service throws an exception'
            def exceptionThrownByPersistenceService = new IllegalStateException()
            mockDataPersistenceService.getJsonById(_) >> {throw exceptionThrownByPersistenceService}
        when: 'we try to get the JSON structure'
            objectUnderTest.getJsonById(1);
        then: 'the same exception is thrown by CPS'
            thrown(IllegalStateException)
    }

    def 'Delete a JSON object with a valid identifier'(){
        given: 'that the data persistence service can delete a JSON structure for identifier 1'
            mockDataPersistenceService.deleteJsonById(1)
        expect: 'No exception is thrown when we delete a JSON structure with identifier 1'
            objectUnderTest.deleteJsonById(1)
    }

    def 'Delete a JSON object with an identifier that does not exist'(){
        given: 'that the data persistence service throws an exception'
            mockDataPersistenceService.deleteJsonById(_) >> {throw new IllegalStateException()}
        when: 'we try to delete a JSON structure'
            objectUnderTest.deleteJsonById(100);
        then: 'the same exception is thrown by CPS'
            thrown(IllegalStateException)
    }
}
