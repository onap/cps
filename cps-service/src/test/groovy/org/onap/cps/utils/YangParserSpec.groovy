/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.model.Anchor
import org.onap.cps.yang.YangTextSchemaSourceSet
import org.opendaylight.yangtools.yang.data.api.schema.ContainerNode
import org.opendaylight.yangtools.yang.model.api.SchemaContext
import spock.lang.Specification
import org.onap.cps.api.impl.YangTextSchemaSourceSetCache

class YangParserSpec extends Specification {

    def mockYangParserHelper = Mock(YangParserHelper)
    def mockYangTextSchemaSourceSetCache = Mock(YangTextSchemaSourceSetCache)

    def objectUnderTest = new YangParser(mockYangParserHelper, mockYangTextSchemaSourceSetCache)

    def anchor = new Anchor(dataspaceName: 'my dataspace', schemaSetName: 'my schema')
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

}
