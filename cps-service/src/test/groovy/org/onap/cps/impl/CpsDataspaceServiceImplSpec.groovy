/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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

package org.onap.cps.impl


import org.onap.cps.impl.utils.CpsValidator
import org.onap.cps.spi.CpsAdminPersistenceService
import org.onap.cps.api.model.Dataspace
import spock.lang.Specification

class CpsDataspaceServiceImplSpec extends Specification {
    def mockCpsAdminPersistenceService = Mock(CpsAdminPersistenceService)
    def mockCpsValidator = Mock(CpsValidator)
    def objectUnderTest = new CpsDataspaceServiceImpl(mockCpsAdminPersistenceService,mockCpsValidator)

    def 'Create dataspace method invokes persistence service.'() {
        when: 'create dataspace method is invoked'
            objectUnderTest.createDataspace('someDataspace')
        then: 'the persistence service method is invoked with same parameters'
            1 * mockCpsAdminPersistenceService.createDataspace('someDataspace')
        and: 'the CpsValidator is called on the dataspaceName'
            1 * mockCpsValidator.validateNameCharacters('someDataspace')
    }

    def 'Retrieve dataspace.'() {
        given: 'a dataspace is already created'
            def dataspace = new Dataspace(name: "someDataspace")
            mockCpsAdminPersistenceService.getDataspace('someDataspace') >> dataspace
        expect: 'the dataspace provided by persistence service is returned as result'
          assert objectUnderTest.getDataspace('someDataspace') == dataspace
    }

    def 'Retrieve all dataspaces.'() {
        given: 'that all given dataspaces are already created'
        def dataspaces = [new Dataspace(name: "test-dataspace1"), new Dataspace(name: "test-dataspace2")]
            mockCpsAdminPersistenceService.getAllDataspaces() >> dataspaces
        expect: 'the dataspace provided by persistence service is returned as result'
           assert objectUnderTest.getAllDataspaces() == dataspaces
    }

    def 'Delete dataspace.'() {
        when: 'delete dataspace is invoked'
            objectUnderTest.deleteDataspace('someDataspace')
        then: 'associated persistence service method is invoked with correct parameter'
            1 * mockCpsAdminPersistenceService.deleteDataspace('someDataspace')
        and: 'the CpsValidator is called on the dataspaceName'
            1 * mockCpsValidator.validateNameCharacters('someDataspace')
    }

}
