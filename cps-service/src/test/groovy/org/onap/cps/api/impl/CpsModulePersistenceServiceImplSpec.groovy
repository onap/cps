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
import org.onap.cps.spi.CpsModulePersistenceService
import org.onap.cps.spi.exceptions.CpsException
import org.opendaylight.yangtools.yang.common.Revision
import org.opendaylight.yangtools.yang.model.api.SchemaContext
import spock.lang.Specification

class CpsModulePersistenceServiceImplSpec extends Specification {
    def mockModuleStoreService = Mock(CpsModulePersistenceService)
    def objectUnderTest = new CpsModuleServiceImpl()

    def setup() {
        objectUnderTest.cpsModulePersistenceService = mockModuleStoreService
    }

    def 'Parse and Validate a Yang Model with a Valid Yang Model'() {
        given: 'a yang model (file)'
            def yangModel = TestUtils.getResourceFileContent('bookstore.yang')
        when: 'a valid model is parsed and validated'
            def result = objectUnderTest.parseAndValidateModel(Collections.singletonMap("bookstore", yangModel));
        then: 'Verify a schema context for that model is created with the correct identity'
            assertModule(result)
    }

    def 'Parse and Validate a Yang Model Using a File'() {
        given: 'a yang file that contains a yang model'
            File file = new File(ClassLoader.getSystemClassLoader().getResource('bookstore.yang').getFile())
        when: 'a model is parsed and validated'
            def result = objectUnderTest.parseAndValidateModel(Collections.singletonList(file))
        then: 'Verify a schema context for that model is created with the correct identity'
            assertModule(result)

    }

    def assertModule(SchemaContext schemaContext) {
        def optionalModule = schemaContext.findModule('stores', Revision.of('2020-09-15'))
        return schemaContext.modules.size() == 1 && optionalModule.isPresent()
    }

    def 'Parse and Validate an Invalid Model'() {
        given: 'a yang file that contains a invalid yang model'
            File file = new File(ClassLoader.getSystemClassLoader().getResource('invalid.yang').getFile())
        when: 'the model is parsed and validated'
            objectUnderTest.parseAndValidateModel(Collections.singletonList(file))
        then: 'a CpsValidationException is thrown'
            thrown(CpsException)
    }

    def 'Store a SchemaContext'() {
        expect: 'No exception to be thrown when a valid model (schema) is stored'
            objectUnderTest.storeSchemaContext(Stub(SchemaContext.class), "sampleDataspace")
    }

}
