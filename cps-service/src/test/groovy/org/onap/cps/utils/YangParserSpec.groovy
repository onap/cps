/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
 *  Modifications Copyright (C) 2024 TechMahindra Ltd.
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
import org.onap.cps.spi.model.Anchor
import org.onap.cps.yang.TimedYangTextSchemaSourceSetBuilder
import org.onap.cps.yang.YangTextSchemaSourceSet
import org.onap.cps.yang.YangTextSchemaSourceSetBuilder
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode
import org.opendaylight.yangtools.yang.model.api.SchemaContext
import spock.lang.Specification
import org.onap.cps.api.impl.YangTextSchemaSourceSetCache

class YangParserSpec extends Specification {

    def mockYangParserHelper = Mock(YangParserHelper)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)
    def mockTimedYangTextSchemaSourceSetBuilder = Mock(TimedYangTextSchemaSourceSetBuilder)

    def objectUnderTest = new YangParser(mockYangParserHelper, mockYangTextSchemaSourceSetCache, mockTimedYangTextSchemaSourceSetBuilder)

    def anchor = new Anchor(dataspaceName: 'my dataspace', schemaSetName: 'my schema')
    def yangResourcesNameToContentMap = TestUtils.getYangResourcesAsMap('bookstore.yang')
    def mockYangTextSchemaSourceSet = Mock(YangTextSchemaSourceSet)
    def mockSchemaContext = Mock(SchemaContext)
    def containerNodeFromYangUtils = Mock(ContainerNode)

    def noParent = ''

    def setup() {
        mockYangTextSchemaSourceSetCache.get('my dataspace', 'my schema') >> mockYangTextSchemaSourceSet
        mockYangTextSchemaSourceSet.getSchemaContext() >> mockSchemaContext
    }

    def 'Parsing data.'() {
        given: 'the yang parser (utility) always returns a container node'
            mockYangParserHelper.parseData(ContentType.JSON, 'some json', mockSchemaContext, noParent) >> containerNodeFromYangUtils
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
            mockYangParserHelper.parseData(ContentType.JSON, 'some json', mockSchemaContext, noParent)  >> { throw new DataValidationException(noParent, noParent) } >> containerNodeFromYangUtils
        when: 'attempt to parse some data'
            def result = objectUnderTest.parseData(ContentType.JSON, 'some json', anchor, noParent)
        then: 'the cache is cleared for the correct dataspace and schema'
            1 * mockYangTextSchemaSourceSetCache.removeFromCache('my dataspace', 'my schema')
        and: 'the result is the same container node as return from yang utils (no exception thrown!)'
            assert result == containerNodeFromYangUtils
    }

    def 'Parsing data with exception on all attempts.'() {
        given: 'the yang parser always throws an exception'
            mockYangParserHelper.parseData(ContentType.JSON, 'some json', mockSchemaContext, noParent)  >> { throw new DataValidationException(noParent, noParent) }
        when: 'attempt to parse some data'
            objectUnderTest.parseData(ContentType.JSON, 'some json', anchor, noParent)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        and: 'the cache is cleared for the correct dataspace and schema (but that did not help)'
            1 * mockYangTextSchemaSourceSetCache.removeFromCache('my dataspace', 'my schema')
    }

    def 'Parsing data with yang resource to context map.'() {
        given: 'the schema source set for the yang resource map is returned'
            mockTimedYangTextSchemaSourceSetBuilder.getYangTextSchemaSourceSet(yangResourcesNameToContentMap) >> mockYangTextSchemaSourceSet
        when: 'parsing some json data'
            def result = objectUnderTest.parseData(ContentType.JSON, 'some json', yangResourcesNameToContentMap, noParent)
        then: 'the yang parser helper always returns a container node'
            1 * mockYangParserHelper.parseData(ContentType.JSON, 'some json', mockSchemaContext, noParent) >> containerNodeFromYangUtils
        and: 'the result is the same container node as return from yang utils'
            assert result == containerNodeFromYangUtils
    }

    def 'Validating #scenario data using Yang parser.'() {
        given: 'the yang parser (utility) is invoked to validate data'
            mockYangParserHelper.parseAndValidateData(mockSchemaContext,  'some json', noParent, contentType)
        when: 'attempt to parse the json data'
            objectUnderTest.parseAndValidateData(anchor, noParent, 'some json or xml data', contentType)
        then: 'the schema source set for the correct dataspace and schema set is retrieved form the cache'
            1 * mockYangTextSchemaSourceSetCache.get('my dataspace', 'my schema') >> mockYangTextSchemaSourceSet
        and: 'nothing is removed from the cache'
            0 * mockYangTextSchemaSourceSetCache.removeFromCache(*_)
        where:
            scenario | contentType
            'JSON'   | ContentType.JSON
            'XML'    | ContentType.XML
    }

    def 'Validating data by parsing with exception on first attempt.'() {
        given: 'the yang parser throws an exception on the first attempt only'
            mockYangParserHelper.parseAndValidateData(mockSchemaContext,  'some json', noParent, ContentType.JSON)  >> { throw new DataValidationException(noParent, noParent) } >> null
        when: 'attempt to parse some data'
            objectUnderTest.parseAndValidateData(anchor, noParent, 'some json', ContentType.JSON)
        then: 'the cache is cleared for the correct dataspace and schema'
            1 * mockYangTextSchemaSourceSetCache.removeFromCache('my dataspace', 'my schema')
        and: 'no exception thrown'
            noExceptionThrown()
    }

}
