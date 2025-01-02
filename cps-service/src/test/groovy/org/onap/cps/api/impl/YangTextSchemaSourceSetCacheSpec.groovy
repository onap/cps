/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Bell Canada
 *  Modifications Copyright (C) 2022-2025 Nordix Foundation
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

package org.onap.cps.api.impl

import org.onap.cps.TestUtils
import org.onap.cps.impl.utils.CpsValidator
import org.onap.cps.spi.CpsModulePersistenceService
import org.onap.cps.yang.YangTextSchemaSourceSet
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cache.Cache
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.caffeine.CaffeineCacheManager
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@SpringBootTest
@EnableCaching
@ContextConfiguration(classes = [YangTextSchemaSourceSetCache, CaffeineCacheManager])
class YangTextSchemaSourceSetCacheSpec extends Specification {

    @SpringBean
    CpsModulePersistenceService mockModuleStoreService = Mock()

    @SpringBean
    CpsValidator mockCpsValidator = Mock(CpsValidator)

    @Autowired
    YangTextSchemaSourceSetCache objectUnderTest

    @Autowired
    CacheManager cacheManager

    Cache yangResourceCacheImpl;

    def setup() {
        yangResourceCacheImpl = cacheManager.getCache('yangSchema')
        yangResourceCacheImpl.clear()
    }


    def 'Cache Miss: Fetch data from Module persistence'() {
        given: 'cache is empty'
            yangResourceCacheImpl.clear()
        and: 'a schema set exists'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def expectedYangTextSchemaSourceSet = YangTextSchemaSourceSetBuilder.of(yangResourcesNameToContentMap)
        when: 'schema-set information is asked'
            def result = objectUnderTest.get('my-dataspace', 'my-schemaset')
        then: 'information fetched from cps module persistence'
            1 * mockModuleStoreService.getYangSchemaResources('my-dataspace', 'my-schemaset')
                    >> yangResourcesNameToContentMap
        and: 'stored in the cache'
            def cachedValue = getCachedValue('my-dataspace', 'my-schemaset')
            assert cachedValue.getModuleReferences() == expectedYangTextSchemaSourceSet.getModuleReferences()
        and: 'the response is as expected'
            assert result.getModuleReferences() == expectedYangTextSchemaSourceSet.getModuleReferences()
        and: 'the CpsValidator is called on the dataspaceName'
            1 * mockCpsValidator.validateNameCharacters('my-dataspace')
    }

    def 'Cache Hit: Respond from cache'() {
        given: 'a schema set exists'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def expectedYangTextSchemaSourceSet = YangTextSchemaSourceSetBuilder.of(yangResourcesNameToContentMap)
        and: 'stored in cache'
            yangResourceCacheImpl.put(getCacheKey('my-dataspace', 'my-schemaset'), expectedYangTextSchemaSourceSet)
        when: 'schema-set information is asked'
            def result = objectUnderTest.get('my-dataspace', 'my-schemaset')
        then: 'expected value is returned'
            result.getModuleReferences() == expectedYangTextSchemaSourceSet.getModuleReferences()
        and: 'module persistence is not invoked'
            0 * mockModuleStoreService.getYangSchemaResources(_, _)
    }

    def 'Cache Update: when no data exist in the cache'() {
        given: 'a schema set exists'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def yangTextSchemaSourceSet = YangTextSchemaSourceSetBuilder.of(yangResourcesNameToContentMap)
        when: 'cache is updated'
            objectUnderTest.updateCache('my-dataspace', 'my-schemaset', yangTextSchemaSourceSet)
        then: 'cached value is same as expected'
            def cachedValue = getCachedValue('my-dataspace', 'my-schemaset')
            cachedValue.getModuleReferences() == yangTextSchemaSourceSet.getModuleReferences()
        and: 'the CpsValidator is called on the dataspaceName'
            1 * mockCpsValidator.validateNameCharacters('my-dataspace')
    }

    def 'Cache Evict:with invalid #scenario'() {
        given: 'a schema set exists in cache'
            def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def yangTextSchemaSourceSet = YangTextSchemaSourceSetBuilder.of(yangResourcesNameToContentMap)
            yangResourceCacheImpl.put(getCacheKey('my-dataspace', 'my-schemaset'), yangTextSchemaSourceSet)
            def cachedValue = getCachedValue('my-dataspace', 'my-schemaset')
            assert cachedValue.getModuleReferences() == yangTextSchemaSourceSet.getModuleReferences()
        when: 'cache is evicted for schemaset'
            objectUnderTest.removeFromCache('my-dataspace', 'my-schemaset')
        then: 'cached does not have value'
            assert getCachedValue('my-dataspace', 'my-schemaset') == null
        and: 'the CpsValidator is called on the dataspaceName'
            1 * mockCpsValidator.validateNameCharacters('my-dataspace')
    }

    def 'Cache Evict: remove when does not exist'() {
        given: 'cache is empty'
            yangResourceCacheImpl.clear()
        when: 'cache is evicted for schemaset'
            objectUnderTest.removeFromCache('my-dataspace', 'my-schemaset')
        then: 'cached does not have value'
            assert getCachedValue('my-dataspace', 'my-schemaset') == null
        and: 'the CpsValidator is called on the dataspaceName'
            1 * mockCpsValidator.validateNameCharacters('my-dataspace')
    }

    def getCachedValue(dataSpace, schemaSet) {
        yangResourceCacheImpl.get(getCacheKey(dataSpace, schemaSet), YangTextSchemaSourceSet)
    }

    def getCacheKey(dataSpace, schemaSet) {
        return new String("${dataSpace}-${schemaSet}")
    }


}
