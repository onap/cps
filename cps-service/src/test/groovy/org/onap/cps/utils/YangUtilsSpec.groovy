/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
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

package org.onap.cps.utils

import org.onap.cps.TestUtils
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.common.Revision
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode
import spock.lang.Specification
import spock.lang.Unroll

class YangUtilsSpec extends Specification{
    def 'Parsing a valid Json String.'() {
        given: 'a yang model (file)'
            def jsonData = org.onap.cps.TestUtils.getResourceFileContent('bookstore.json')
        and: 'a model for that data'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        when: 'the json data is parsed'
            NormalizedNode<?, ?> result = YangUtils.parseJsonData(jsonData, schemaContext)
        then: 'the result is a normalized node of the correct type'
            result.nodeType == QName.create('org:onap:ccsdk:sample', '2020-09-15', 'bookstore')
    }

    @Unroll
    def 'Parsing invalid data: #description.'() {
        given: 'a yang model (file)'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        when: 'invalid data is parsed'
            YangUtils.parseJsonData(invalidJson, schemaContext)
        then: 'an exception is thrown'
            thrown(IllegalStateException)
        where: 'the following invalid json is provided'
            invalidJson                                       | description
            '{incomplete json'                                | 'incomplete json'
            '{"test:bookstore": {"address": "Parnell st." }}' | 'json with un-modelled data'
    }
}
