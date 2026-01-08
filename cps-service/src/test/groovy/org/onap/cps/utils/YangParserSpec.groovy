/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
 *  Modifications Copyright (C) 2024 Deutsche Telekom AG
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
import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.api.model.Anchor
import org.onap.cps.impl.YangTextSchemaSourceSetCache
import org.onap.cps.yang.TimedYangTextSchemaSourceSetBuilder
import org.onap.cps.yang.YangTextSchemaSourceSet
import org.opendaylight.yangtools.yang.common.QName
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode
import org.opendaylight.yangtools.yang.model.api.ListSchemaNode
import org.opendaylight.yangtools.yang.model.api.Module
import org.opendaylight.yangtools.yang.model.api.SchemaContext
import spock.lang.Specification

class YangParserSpec extends Specification {

    def mockYangParserHelper = Mock(YangParserHelper)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)
    def mockTimedYangTextSchemaSourceSetBuilder = Mock(TimedYangTextSchemaSourceSetBuilder)

    def objectUnderTest = new YangParser(mockYangParserHelper, mockYangTextSchemaSourceSetCache, mockTimedYangTextSchemaSourceSetBuilder)

    def anchor = new Anchor(dataspaceName: 'my dataspace', schemaSetName: 'my schema')
    def yangResourceContentPerName = TestUtils.getYangResourcesAsMap('bookstore.yang')
    def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
    def mockSchemaContext = Mock(SchemaContext)
    def containerNodeFromYangUtils = Mock(ContainerNode)

    def noParent = ''
    def validateOnly = true
    def validateAndParse = false

    def setup() {
        mockYangTextSchemaSourceSetCache.get('my dataspace', 'my schema') >> mockYangTextSchemaSourceSet
        mockYangTextSchemaSourceSet.schemaContext() >> mockSchemaContext
    }

    def 'Convert RestConf-style path to CPS path'() {
        given: 'a RestConf-style path'
            def restConfStylePath = '/bookstore:book=Book1'
            def expectedCpsPath = '/book[@name=\'Book1\']'
        and: 'a schema context that contains the matching node'
            def mockedBookNode = Mock(ListSchemaNode) {
                getQName() >> QName.create('bookstore', 'book')
                getKeyDefinition() >> [QName.create('bookstore', 'name')]
            }
            mockSchemaContext.getChildNodes() >> [mockedBookNode]
            mockedBookNode.getChildNodes() >> []
        when: 'restconf style path is converted to cps path'
            def result = objectUnderTest.getCpsPathFromRestConfStylePath(anchor, restConfStylePath)
        then: 'the schema context is retrieved from the cache for the correct anchor'
            1 * mockYangTextSchemaSourceSetCache.get('my dataspace', 'my schema') >> mockYangTextSchemaSourceSet
        and: 'the CPS path is returned correctly'
            assert result == expectedCpsPath
    }

    def 'Parsing data.'() {
        given: 'the yang parser (utility) always returns a container node'
            mockYangParserHelper.parseData(ContentType.JSON, 'some json', mockSchemaContext, noParent, validateAndParse) >> containerNodeFromYangUtils
        when: 'parsing some json data'
            def result = objectUnderTest.parseData(ContentType.JSON, 'some json', anchor, noParent)
        then: 'the schema source set for the correct dataspace and schema set is retrieved form the cache'
            1 * mockYangTextSchemaSourceSetCache.get('my dataspace', 'my schema') >> mockYangTextSchemaSourceSet
        and: 'the result is the same container node as return from yang utils'
            assert result == containerNodeFromYangUtils
        and: 'nothing is removed from the cache'
            0 * mockYangTextSchemaSourceSetCache.removeFromCache(*_)
    }

    def 'Parsing data with exception on first attempt.'() {
        given: 'the yang parser throws an exception on the first attempt only'
            mockYangParserHelper.parseData(ContentType.JSON, 'some json', mockSchemaContext, noParent, validateAndParse)  >> { throw new DataValidationException(noParent, noParent) } >> containerNodeFromYangUtils
        when: 'attempt to parse some data'
            def result = objectUnderTest.parseData(ContentType.JSON, 'some json', anchor, noParent)
        then: 'the cache is cleared for the correct dataspace and schema'
            1 * mockYangTextSchemaSourceSetCache.removeFromCache('my dataspace', 'my schema')
        and: 'the result is the same container node as return from yang utils (no exception thrown!)'
            assert result == containerNodeFromYangUtils
    }

    def 'Parsing data with exception on all attempts.'() {
        given: 'the yang parser always throws an exception'
            mockYangParserHelper.parseData(ContentType.JSON, 'some json', mockSchemaContext, noParent, validateAndParse)  >> { throw new DataValidationException(noParent, noParent) }
        when: 'attempt to parse some data'
            objectUnderTest.parseData(ContentType.JSON, 'some json', anchor, noParent)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the cache is cleared for the correct dataspace and schema (but that did not help)'
            1 * mockYangTextSchemaSourceSetCache.removeFromCache('my dataspace', 'my schema')
    }

    def 'Parsing data with yang resource to context map.'() {
        given: 'the schema source set for the yang resource map is returned'
            mockTimedYangTextSchemaSourceSetBuilder.getYangTextSchemaSourceSet(yangResourceContentPerName) >> mockYangTextSchemaSourceSet
        when: 'parsing some json data'
            def result = objectUnderTest.parseData(ContentType.JSON, 'some json', yangResourceContentPerName, noParent)
        then: 'the yang parser helper always returns a container node'
            1 * mockYangParserHelper.parseData(ContentType.JSON, 'some json', mockSchemaContext, noParent, validateAndParse) >> containerNodeFromYangUtils
        and: 'the result is the same container node as return from yang utils'
            assert result == containerNodeFromYangUtils
    }

    def 'Validating #scenario data using Yang parser with cache retrieval.'() {
        given: 'the yang parser (utility) is set up and schema context is available'
            mockYangParserHelper.parseData(contentType, 'some json', mockSchemaContext, noParent, validateOnly)
        when: 'attempt to parse data with no parent node xpath'
            objectUnderTest.validateData(contentType, 'some json or xml data', anchor, noParent)
        then: 'the correct schema set is retrieved from the cache for the dataspace and schema'
            1 * mockYangTextSchemaSourceSetCache.get('my dataspace', 'my schema') >> mockYangTextSchemaSourceSet
        and: 'no cache entries are removed during validation'
            0 * mockYangTextSchemaSourceSetCache.removeFromCache(*_)
        where:
            scenario | contentType
            'JSON'   | ContentType.JSON
            'XML'    | ContentType.XML
    }

    def 'Validating data when parsing fails on first attempt and recovers.'() {
        given: 'the Yang parser throws an exception on the first attempt but succeeds on the second'
            mockYangParserHelper.parseData(ContentType.JSON, 'some json', mockSchemaContext, noParent, validateOnly)  >> { throw new DataValidationException(noParent, noParent) } >> null
        when: 'attempting to parse JSON data'
            objectUnderTest.validateData(ContentType.JSON, 'some json', anchor, noParent)
        then: 'the cache is cleared for the correct dataspace and schema after the first failure'
            1 * mockYangTextSchemaSourceSetCache.removeFromCache('my dataspace', 'my schema')
        and: 'no exceptions are thrown after the second attempt'
            noExceptionThrown()
    }

    def 'Validating data with repeated parsing failures leading to exception.'() {
        given: 'the yang parser throws an exception on the first attempt only'
            mockYangParserHelper.parseData(ContentType.JSON, 'some json', mockSchemaContext, noParent, validateOnly)  >> { throw new DataValidationException(noParent, noParent) }
        when: 'attempting to parse JSON data'
            objectUnderTest.validateData(ContentType.JSON, 'some json', anchor, noParent)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the cache is cleared for the correct dataspace and schema after the failure'
            1 * mockYangTextSchemaSourceSetCache.removeFromCache('my dataspace', 'my schema')
    }

    def 'Get module and root node references using an anchor'() {
        given: 'a schema context with module and root node'
            def mockModule = Mock(Module) {
                getName() >> 'bookstore'
            }
            def mockRootNode = Mock(DataSchemaNode) {
                getQName() >> QName.create('bookstore', 'book')
            }
            mockModule.getChildNodes() >> [mockRootNode]
            mockSchemaContext.getModules() >> [mockModule]
        when: 'we get module and root nodes references for the anchor'
            def result = objectUnderTest.getRootNodeReferences(anchor)
        then: 'the result contains expected module:rootnode combination'
            assert result == ['bookstore:book'] as Set
    }

}
