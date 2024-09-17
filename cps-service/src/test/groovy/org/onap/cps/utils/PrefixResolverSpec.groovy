/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
 *  Modifications Copyright (C) 2021-2022 Bell Canada.
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

package org.onap.cps.utils

import com.hazelcast.map.IMap
import org.onap.cps.TestUtils
import org.onap.cps.api.CpsAnchorService
import org.onap.cps.api.impl.YangTextSchemaSourceSetCache
import org.onap.cps.cache.AnchorDataCacheEntry
import org.onap.cps.spi.model.Anchor
import org.onap.cps.yang.YangTextSchemaSourceSet
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import spock.lang.Specification

class PrefixResolverSpec extends Specification {

    def mockCpsAnchorService = Mock(CpsAnchorService)

    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)

    def mockAnchorDataCache = Mock(IMap<String, AnchorDataCacheEntry>)

    def objectUnderTest = new PrefixResolver(mockCpsAnchorService, mockYangTextSchemaSourceSetCache, mockAnchorDataCache)

    def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)

    def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('test-tree.yang')

    def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()

    def anchor = new Anchor(dataspaceName: 'testDataspace', name: 'testAnchor')

    def setup() {
        given: 'the system can get the anchor'
            mockCpsAnchorService.getAnchor('testDataspace', 'testAnchor') >> anchor
        and: 'the schema source cache contains the schema context for the test-tree module'
            mockYangTextSchemaSourceSet.getSchemaContext() >> schemaContext
    }

    def 'get xpath prefix using node schema context'() {
        when: 'the prefix of the yang module is retrieved'
            def result = objectUnderTest.getPrefix(anchor, xpath)
        then: 'the expected prefix is returned'
            result == expectedPrefix
        and: 'the cache is updated for the given anchor with a map of prefixes per top level container (just one one this case)'
            1 * mockAnchorDataCache.put('testAnchor',_ , _ ,_) >> { args -> {
                def prefixPerContainerName = args[1].getProperty("prefixPerContainerName")
                assert prefixPerContainerName.size() == 1
                assert prefixPerContainerName.get('test-tree') == 'tree'
                }
            }
        and: 'schema source cache is used (i.e. need to build schema context)'
            1 * mockYangTextSchemaSourceSetCache.get(*_) >> mockYangTextSchemaSourceSet
        where: 'the following scenarios are applied'
            xpath                         || expectedPrefix
            '/test-tree'                  || 'tree'
            '/test-tree/with/descendants' || 'tree'
            '/test-tree[@id=1]'           || 'tree'
            '/test-tree[@id=1]/child'     || 'tree'
            '/test-tree[@id="[1]"]/child' || 'tree'
            '//test-tree'                 || ''
            '/not-defined'                || ''
    }

    def 'get prefix with populated anchor data cache with #scenario cache entry'() {
        given: 'anchor data cache is populated for the anchor with a prefix for top level container named #cachedTopLevelContainerName'
            def anchorDataCacheEntry = new AnchorDataCacheEntry()
            def prefixPerContainerName = [(cachedTopLevelContainerName): 'cachedPrefix']
            anchorDataCacheEntry.setProperty('prefixPerContainerName',prefixPerContainerName)
            mockAnchorDataCache.containsKey('testAnchor') >> true
            mockAnchorDataCache.get('testAnchor') >> anchorDataCacheEntry
        when: 'the prefix of the yang module is retrieved'
            def result = objectUnderTest.getPrefix(anchor, '/test-tree')
        then: 'the expected prefix is returned'
            result == expectedPrefix
        and: 'schema source cache is not used (i.e. no need to build schema context)'
            0 * mockYangTextSchemaSourceSetCache.get(*_)
        where: 'the following scenarios are applied'
            scenario       | cachedTopLevelContainerName || expectedPrefix
            'matching'     | 'test-tree'                 || 'cachedPrefix'
            'non-matching' | 'other'                     || ''
    }

    def 'get prefix with other (non relevant) data in anchor data cache'() {
        given: 'anchor data cache is populated with non relevant other property'
            def anchorDataCacheEntry = new AnchorDataCacheEntry()
            anchorDataCacheEntry.setProperty('something else', 'does not matter')
            mockAnchorDataCache.containsKey('testAnchor') >> true
            mockAnchorDataCache.get('testAnchor') >> anchorDataCacheEntry
        when: 'the prefix of the yang module is retrieved'
            def result = objectUnderTest.getPrefix(anchor, '/test-tree')
        then: 'the expected prefix is returned'
            result == 'tree'
        and: 'schema source cache is used (i.e. need to build schema context)'
            1 * mockYangTextSchemaSourceSetCache.get(*_) >> mockYangTextSchemaSourceSet
    }

}
