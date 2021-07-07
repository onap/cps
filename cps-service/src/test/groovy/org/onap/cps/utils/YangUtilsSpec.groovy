/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Nordix Foundation
 *  Modifications Copyright (C) 2021 Pantheon.tech
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
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.data.api.schema.NormalizedNode
import spock.lang.Specification

class YangUtilsSpec extends Specification {
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

    def 'Parsing invalid data: #description.'() {
        given: 'a yang model (file)'
            def yangResourceNameToContent = TestUtils.getYangResourcesAsMap('bookstore.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourceNameToContent).getSchemaContext()
        when: 'invalid data is parsed'
            YangUtils.parseJsonData(invalidJson, schemaContext)
        then: 'an exception is thrown'
            thrown(DataValidationException)
        where: 'the following invalid json is provided'
            invalidJson                                       | description
            '{incomplete json'                                | 'incomplete json'
            '{"test:bookstore": {"address": "Parnell st." }}' | 'json with un-modelled data'
            '{" }'                                            | 'json with syntax exception'
    }

    def 'Parsing json data fragment by xpath for #scenario.'() {
        given: 'schema context'
            def yangResourcesMap = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourcesMap).getSchemaContext()
        when: 'json string is parsed'
            def result = YangUtils.parseJsonData(jsonData, schemaContext, parentNodeXpath)
        then: 'result represents a node of expected type'
            result.nodeType == QName.create('org:onap:cps:test:test-tree', '2020-02-02', nodeName)
        where:
            scenario                    | jsonData                                                                      | parentNodeXpath                       || nodeName
            'list element as container' | '{ "branch": { "name": "B", "nest": { "name": "N", "birds": ["bird"] } } }'   | '/test-tree'                          || 'branch'
            'list element within list'  | '{ "branch": [{ "name": "B", "nest": { "name": "N", "birds": ["bird"] } }] }' | '/test-tree'                          || 'branch'
            'container element'         | '{ "nest": { "name": "N", "birds": ["bird"] } }'                              | '/test-tree/branch[@name=\'Branch\']' || 'nest'
    }

    def 'Parsing json data fragment by xpath error scenario: #scenario.'() {
        given: 'schema context'
            def yangResourcesMap = TestUtils.getYangResourcesAsMap('test-tree.yang')
            def schemaContext = YangTextSchemaSourceSetBuilder.of(yangResourcesMap).getSchemaContext()
        when: 'json string is parsed'
            YangUtils.parseJsonData('{"nest": {"name" : "Nest", "birds": ["bird"]}}', schemaContext,
                    parentNodeXpath)
        then: 'expected exception is thrown'
            thrown(DataValidationException)
        where:
            scenario                             | parentNodeXpath
            'xpath has no identifiers'           | '/'
            'xpath has no valid identifiers'     | '/[@name=\'Name\']'
            'invalid parent path'                | '/test-bush'
            'another invalid parent path'        | '/test-tree/branch[@name=\'Branch\']/nest/name/last-name'
            'fragment does not belong to parent' | '/test-tree/'
    }
}
