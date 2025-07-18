/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2025 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.utils

import org.onap.cps.TestUtils
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode
import org.opendaylight.yangtools.yang.model.api.SchemaContext
import spock.lang.Specification

import static RestConfStylePathToCpsPathUtil.convertToCpsPath

class RestConfStylePathToCpsPathUtilSpec extends Specification {

    SchemaContext schemaContext

    def setup() {
        def yangResources = TestUtils.getYangResourcesAsMap('bookstore.yang')
        schemaContext = YangTextSchemaSourceSetBuilder.of(yangResources).getSchemaContext()
    }

    def 'Convert RestConf style paths when (#scenario) to CPS paths'() {
        expect: 'the path to be correctly converted'
            assert convertToCpsPath(inputPath, schemaContext) == expectedPath
        where: 'following scenarios are used'
            scenario                                              | inputPath                                                | expectedPath
            'Nested path with multiple keyed lists'               | '/stores:bookstore/categories=fiction/books=WarAndPeace' | '/bookstore/categories[@code=\'fiction\']/books[@title=\'WarAndPeace\']'
            'Single keyed list path with module prefix'           | '/book-store:bookstore/book-store:categories=fiction'    | '/bookstore/categories[@code=\'fiction\']'
            'Path to leaf node under container'                   | '/book-store:bookstore/book-store:bookstore-name'        | '/bookstore/bookstore-name'
            'Keyed path where node is not a list (no key filter)' | '/book-store:bookstore/book-store:bookstore-name=value'  | '/bookstore/bookstore-name'
            'Null input path returns empty result'                | null                                                     | ''
            'Blank input path returns empty result'               | '   '                                                    | ''
    }

    def 'Throw error for unknown segment in valid path'() {
        when: 'we have an unknown segment in the restconf style path'
            convertToCpsPath('/stores:bookstore/unknown-segment-of-path', schemaContext)
        then: 'exception is thrown'
            def exception = thrown(IllegalArgumentException)
            assert exception.message.contains('Schema node not found')
    }

    def 'Should throw exception if list node has no key'() {
        given: 'mocked books as list schema node'
            def booksQName = QName.create('org:onap:ccsdk:sample', '2020-09-15', 'books')
            def books = Mock(ListSchemaNode) {
                getQName() >> booksQName
                getKeyDefinition() >> []
            }
        when: 'path with no key is used'
            RestConfStylePathToCpsPathUtil.buildKeyFilter(books, '/books=no-key')
        then: 'exception is thrown'
            def exception = thrown(IllegalArgumentException)
            assert exception.message.contains('No key defined')
    }
}
