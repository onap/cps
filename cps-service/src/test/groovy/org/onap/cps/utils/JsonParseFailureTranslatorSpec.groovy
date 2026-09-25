/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG. All rights reserved.
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

import com.google.common.collect.Range
import com.google.common.collect.TreeRangeSet
import org.onap.cps.api.exceptions.DataValidationException
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode
import org.opendaylight.yangtools.yang.model.api.type.RangeConstraint
import org.opendaylight.yangtools.yang.model.api.type.RangeRestrictedTypeDefinition
import spock.lang.Shared
import spock.lang.Specification

class JsonParseFailureTranslatorSpec extends Specification {

    @Shared
    def mockLeafSchemaNode = Mock(LeafSchemaNode)

    def 'Translating a failure context that matches a specific rule: #scenario.'() {
        given: 'a failure context'
            def context = new JsonNodeFailureContext(exception, schemaNode, xpath, nodeName, lastStringValue, lastValueNull, Set.of(), schemaNodeNotFound)
        when: 'the failure context is translated into a validation exception'
            def result = JsonParseFailureTranslator.translate(context)
        then: 'the resulting exception has the expected type, message and details'
            assert result.class == DataValidationException
            assert result.message == 'Data Validation Failed'
            assert result.details == expectedDetails
        and: 'the original exception is preserved as the cause only when the matched rule supports it'
            assert result.cause == (preservesCause ? exception : null)
        where: 'the following specific-rule failure scenarios are provided'
            scenario                         | schemaNode                                                | exception                                                                                                                        | xpath                                        | nodeName          | lastStringValue  | lastValueNull | schemaNodeNotFound | preservesCause || expectedDetails
            'null leaf'                      | mockLeafSchemaNode                                        | new NullPointerException()                                                                                                       | '/bookstore/bookstore-name'                  | 'bookstore-name'  | null             | true          | false              | true           || "Validation failed at xpath '/bookstore/bookstore-name' : Null value is not allowed for mandatory leaf 'bookstore-name'."
            'invalid integer'                | mockLeafSchemaNode                                        | new IllegalArgumentException('invalid value', new NumberFormatException('For input string: "two"'))                              | '/bookstore/categories/books/price'          | 'price'           | 'two'            | false         | false              | true           || "Validation failed at xpath '/bookstore/categories/books/price' : Value 'two' is not a valid integer."
            'out of range (single range)'    | leafWithRange(Range.closed(0, 65535))                     | new IllegalArgumentException('Failed to parse json data. Invalid range: -2')                                                     | '/bookstore/premises/addresses/house-number' | 'house-number'    | '-2'             | false         | false              | true           || "Validation failed at xpath '/bookstore/premises/addresses/house-number' : Value -2 is outside the allowed range 0..65535."
            'out of range (multiple ranges)' | leafWithRange(Range.closed(0, 10), Range.closed(20, 30))  | new IllegalArgumentException('boom')                                                                                             | '/some/xpath'                                | 'some-node'       | '15'             | false         | false              | true           || "Validation failed at xpath '/some/xpath' : Value 15 is outside the allowed range 0..10 | 20..30."
            'unknown field'                  | null                                                      | new IllegalArgumentException('Failed to parse json data. Schema node with name bookstore-names was not found under bookstore.')  | '/bookstore/bookstore-names'                 | 'bookstore-names' | null             | false         | true               | true           || "Validation failed at xpath '/bookstore/bookstore-names' : Unknown field 'bookstore-names'. Field is not defined in the YANG schema."
    }

    def 'Translating a failure context when no specific rule matches: #scenario.'() {
        given: 'a failure context for which no specific validation rule applies'
            def context = new JsonNodeFailureContext(exception, schemaNode, 'some xpath', 'some node', null, lastValueNull, Set.of(), false)
        when: 'the failure context is translated into a validation exception'
            def result = JsonParseFailureTranslator.translate(context)
        then: 'the fallback exception has the expected type and message'
            assert result.class == DataValidationException
            assert result.message == 'Data Validation Failed'
        and: 'the original exception is preserved as the cause'
            assert result.cause.is(exception)
        where: 'the following fallback scenarios are provided'
            scenario                                          | schemaNode          | exception                                               | lastValueNull
            'unresolved schema node'                          | null                | new NullPointerException('unexpected internal error')   | false
            'null-valued non-leaf node'                       | null                | new NullPointerException()                              | true
            'leaf node with non-range exception message'      | mockLeafSchemaNode  | new RuntimeException('some unrelated leaf error')       | false
    }

    def leafWithRange(Range<Integer>... allowedRanges) {
        def rangeSet = TreeRangeSet.<Integer> create()
        allowedRanges.each { rangeSet.add(it) }
        def mockRangeConstraint = Mock(RangeConstraint) {
            getAllowedRanges() >> rangeSet
        }
        def mockRangeRestrictedType = Mock(RangeRestrictedTypeDefinition) {
            getRangeConstraint() >> Optional.of(mockRangeConstraint)
        }
        Mock(LeafSchemaNode) {
            getType() >> mockRangeRestrictedType
        }
    }
}
