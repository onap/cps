/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
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
import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.impl.YangTextSchemaSourceSetCache
import org.onap.cps.cache.AnchorDataCacheEntry
import org.onap.cps.spi.model.Anchor
import org.onap.cps.yang.YangTextSchemaSourceSet
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import spock.lang.Specification

class PrefixResolverSpec extends Specification {

    def mockCpsAdminService = Mock(CpsAdminService)

    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)

    def mockAnchorDataCache = Mock(IMap<String, AnchorDataCacheEntry>)

    def objectUnderTest = new PrefixResolver(mockCpsAdminService, mockYangTextSchemaSourceSetCache, mockAnchorDataCache)

    def 'get prefix for a data node using its schema context'() {
        given: 'an anchor for the test-tree model'
            def anchor = new Anchor(dataspaceName: 'testDataspace', name: 'testAnchor')
        and: 'the system can get this anchor'
            mockCpsAdminService.getAnchor('testDataspace', 'testAnchor') >> anchor
        and: 'the system cache contains the schema context for the tre-tree module'
            def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
            mockYangTextSchemaSourceSetCache.get(*_) >> mockYangTextSchemaSourceSet
            mockYangTextSchemaSourceSet.getSchemaContext() >> schemaContext
        expect: ' the prefix of the yang module'
            assert objectUnderTest.getPrefix('testDataspace', 'testAnchor', xpath) == expectedPrefix
        where:
            xpath                         || expectedPrefix
            '/test-tree'                  || 'tree'
            '/test-tree/with/descendants' || 'tree'
            '/test-tree[@id=1]'           || 'tree'
            '/not-defined'                || ''
            'invalid'                     || ''
    }

}
