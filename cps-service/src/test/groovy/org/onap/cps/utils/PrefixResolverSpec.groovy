/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
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

import org.onap.cps.TestUtils
import org.onap.cps.api.impl.YangTextSchemaSourceSetCache
import org.onap.cps.api.model.Anchor
import org.onap.cps.yang.YangTextSchemaSourceSet
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import spock.lang.Specification

class PrefixResolverSpec extends Specification {

    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)

    def objectUnderTest = new PrefixResolver(mockYangTextSchemaSourceSetCache)

    def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)

    def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('test-tree.yang')

    def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()

    def anchor = new Anchor(dataspaceName: 'testDataspace', name: 'testAnchor')

    def 'get xpath prefix using node schema context'() {
        given: 'the schema source cache contains the schema context for the test-tree module'
            mockYangTextSchemaSourceSet.getSchemaContext() >> schemaContext
            mockYangTextSchemaSourceSetCache.get(*_) >> mockYangTextSchemaSourceSet
        when: 'the prefix of the yang module is retrieved'
            def result = objectUnderTest.getPrefix(anchor, xpath)
        then: 'the expected prefix is returned'
            result == expectedPrefix
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

}
