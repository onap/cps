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
import org.onap.cps.spi.exceptions.ModelValidationException
import org.onap.cps.spi.model.ModuleReference
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_ALLOWED
import static org.onap.cps.spi.CascadeDeleteAllowed.CASCADE_DELETE_PROHIBITED

@SpringBootTest
@EnableCaching
@ContextConfiguration(classes = [YangTextSchemaSourceSetCache.class, CpsModuleServiceImpl.class])
class CpsModuleServiceImplSpec extends Specification {

    @SpringBean
    CpsModulePersistenceService mockModuleStoreService = Mock()

    @SpringBean
    CacheManager cacheManager = new CaffeineCacheManager("yangSchema")

    @Autowired
    CpsModuleServiceImpl objectUnderTest

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

    def 'Get schema set by name and dataspace.'() {
        given: 'an already present schema set'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap('bookstore.yang')
            mockModuleStoreService.getYangSchemaResources('someDataspace', 'someSchemaSet') >> yangResourcesNameToContentMap
        when: 'get schema set method is invoked'
            def result = objectUnderTest.getSchemaSet('someDataspace', 'someSchemaSet')
        then: 'the correct schema set is returned'
            result.getName().contains('someSchemaSet')
            result.getDataspaceName().contains('someDataspace')
            result.getModuleReferences().contains(new ModuleReference('stores', 'org:onap:ccsdk:sample', '2020-09-15'))
    }

    def 'Schema set caching.'() {
        given: 'an  schema set'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap('bookstore.yang')
        when: 'get schema set method is invoked twice'
            2.times {
                objectUnderTest.getSchemaSet('someDataspace', 'someSchemaSet')
            }
        then: 'the persistency service called only once'
            1 * mockModuleStoreService.getYangSchemaResources('someDataspace', 'someSchemaSet') >> yangResourcesNameToContentMap
    }

    def 'Delete set by name and dataspace with #cascadeDeleteOption.'() {
        when: 'schema set deletion is requested'
            objectUnderTest.deleteSchemaSet(dataspaceName, schemaSetname, cascadeDeleteOption)
        then: 'persistence service method is invoked with same parameters'
            mockModuleStoreService.deleteSchemaSet(dataspaceName, schemaSetname, cascadeDeleteOption)
        where: 'following parameters are used'
            dataspaceName | schemaSetname   | cascadeDeleteOption
            'dataspace-1' | 'schemas-set-1' | CASCADE_DELETE_ALLOWED
            'dataspace-2' | 'schemas-set-2' | CASCADE_DELETE_PROHIBITED
    }
}
