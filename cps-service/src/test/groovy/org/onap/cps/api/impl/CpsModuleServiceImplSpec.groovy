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
import org.onap.cps.spi.CpsModulePersistenceService;
import org.onap.cps.spi.exceptions.ModelValidationException
import org.onap.cps.spi.model.ModuleReference
import org.onap.cps.yang.YangTextSchemaSourceSet
import spock.lang.Specification

class CpsModuleServiceImplSpec extends Specification {
    CpsModulePersistenceService mockModuleStoreService = Mock()
    CpsModuleServiceImpl objectUnderTest = new CpsModuleServiceImpl()

    def setup() {
        objectUnderTest.cpsModulePersistenceService = mockModuleStoreService
    }

    def 'Create schema set'() {
        given: 'Valid yang resource as name-to-content map'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap('bookstore.yang')
        when: 'Create schema set method is invoked'
            objectUnderTest.createSchemaSet('someDataspace', 'someSchemaSet', yangResourcesNameToContentMap)
        then: 'Parameters are validated and processing is delegated to persistence service'
            1 * mockModuleStoreService.storeSchemaSet('someDataspace', 'someSchemaSet', yangResourcesNameToContentMap)
    }

    def 'Create schema set from invalid resources'() {
        given: 'Invalid yang resource as name-to-content map'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap('invalid.yang')
        when: 'Create schema set method is invoked'
            objectUnderTest.createSchemaSet('someDataspace', 'someSchemaSet', yangResourcesNameToContentMap)
        then: 'Model validation exception is thrown'
            thrown(ModelValidationException.class)
    }

    def 'Get schema set by name and namespace'() {
        given: 'an already present schema set'
            def schemaSetName = 'my_schema_set';
            def dataspaceName = 'test-dataspace';
            def revision = '2020-09-15';
            def namespace = 'org:onap:ccsdk:sample';
            def moduleReference = new ModuleReference(namespace, revision);
            def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
            mockYangTextSchemaSourceSet.getModuleReferences() >> [ moduleReference ]
            mockModuleStoreService.getYangTextSchemaSourceSet(dataspaceName, schemaSetName) >> mockYangTextSchemaSourceSet
        when: 'get schema set method is invoked'
            def result = objectUnderTest.getSchemaSet(dataspaceName, schemaSetName)
        then: 'schema set model is returned as a result'
            1 * mockModuleStoreService.getYangTextSchemaSourceSet(dataspaceName, schemaSetName)
            result.getName().contains(schemaSetName)
            result.getDataspaceName().contains(dataspaceName)
            result.getModuleReferences().contains(moduleReference)
    }
}
