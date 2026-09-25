/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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
import org.opendaylight.yangtools.yang.model.api.DataSchemaNode
import org.opendaylight.yangtools.yang.model.api.LeafSchemaNode
import org.opendaylight.yangtools.yang.model.api.type.RangeConstraint
import org.opendaylight.yangtools.yang.model.api.type.RangeRestrictedTypeDefinition
import spock.lang.Specification

class JsonParseFailureRuleSpec extends Specification {

    def 'Null Leaf rule matches only when node is a leaf with null value: #scenario.'() {
        given: 'a failure context for the given scenario'
            def context = createContextForNullLeaf(isLeaf, lastValueNull)
        expect: 'the rule matches only when the node is a leaf and value is null'
            JsonParseFailureRule.NULL_LEAF.matches(context) == expectedMatch
        where: 'the following scenarios are tested'
            scenario                            | isLeaf | lastValueNull || expectedMatch
            'leaf node with null value'         | true   | true          || true
            'leaf node with non-null value'     | true   | false         || false
            'non-leaf node with null value'     | false  | true          || false
            'non-leaf node with non-null value' | false  | false         || false
    }

    def 'Null Leaf rule generates exception with correct message and cause.'() {
        given: 'an original exception and Null Leaf context'
            def originalException = new IllegalArgumentException('test')
            def context = new JsonNodeFailureContext(originalException, Mock(LeafSchemaNode), '/bookstore/bookstore-name', 'bookstore-name', null, true, Set.of(), false)
        when: 'the rule converts the context to an exception'
            def result = JsonParseFailureRule.NULL_LEAF.toException(context)
        then: 'the exception contains the expected message and preserves the original cause'
            with(result) {
                message == 'Data Validation Failed'
                details == "Validation failed at xpath '/bookstore/bookstore-name' : Null value is not allowed for mandatory leaf 'bookstore-name'."
                cause.is(originalException)
            }
    }

    def 'Invalid Integer rule matches integer parsing validation failures: #scenario.'(){
        given: 'a failure context for the given scenario'
            def context = createContextForInvalidInteger(isLeaf, exception)
        expect: 'the rule matches only for leaf nodes with NumberFormatException in the chain'
            JsonParseFailureRule.INVALID_INTEGER.matches(context) == expectedMatch
        where: 'the following scenarios are tested'
            scenario                                         | isLeaf | exception                                                               || expectedMatch
            'leaf with direct NumberFormatException'         | true   | new NumberFormatException('invalid')                                    || true
            'leaf with wrapped NumberFormatException'        | true   | new RuntimeException(new NumberFormatException())                       || true
            'leaf with deeply wrapped NumberFormatException' | true   | new RuntimeException(new RuntimeException(new NumberFormatException())) || true
            'leaf without NumberFormatException'             | true   | new IllegalArgumentException('other error')                             || false
            'non-leaf with NumberFormatException'            | false  | new NumberFormatException('invalid')                                    || false
    }

    def 'Invalid Integer rule generates exception with correct message and cause.'() {
        given: 'an original exception and Invalid Integer context'
            def originalException = new NumberFormatException('invalid')
            def context = new JsonNodeFailureContext(originalException, Mock(LeafSchemaNode), '/bookstore/price', 'price', 'not-a-number', false, Set.of(), false)
        when: 'the rule converts the context to an exception'
            def result = JsonParseFailureRule.INVALID_INTEGER.toException(context)
        then: 'the exception contains the expected message and preserves the original cause'
            with(result) {
                message == 'Data Validation Failed'
                details == "Validation failed at xpath '/bookstore/price' : Value 'not-a-number' is not a valid integer."
                cause.is(originalException)
            }
    }

    def 'Unknown Field rule matches when schema node not found and node name present: #scenario.'() {
        given: 'a failure context with the given schema node and node name state'
            def context = createContextForUnknownField(nodeName, schemaNodeNotFound)
        expect: 'the rule matches only when schema node not found and node name is non-empty'
            JsonParseFailureRule.UNKNOWN_FIELD.matches(context) == expectedMatch
        where: 'the following scenarios are tested'
            scenario                                     | schemaNodeNotFound | nodeName    || expectedMatch
            'schema node found with non-empty node name' | false              | 'some-node' || false
            'schema node found with empty node name'     | false              | ''          || false
            'schema node not found with empty node name' | true               | ''          || false
            'schema node not found with null node name'  | true               | null        || false
            'schema node not found with node name'       | true               | 'some-node' || true
    }

    def 'Unknown Field rule generates exception with correct message and cause.'() {
        given: 'an original exception and Unknown Field context'
            def originalException = new IllegalArgumentException('test')
            def context = new JsonNodeFailureContext(originalException, null, '/bookstore/bookstore-names', 'bookstore-names', null, false, Set.of(), true)
        when: 'the rule converts the context to an exception'
            def result = JsonParseFailureRule.UNKNOWN_FIELD.toException(context)
        then: 'the exception contains the expected message and preserves the original cause'
            with(result) {
                message == 'Data Validation Failed'
                details == "Validation failed at xpath '/bookstore/bookstore-names' : Unknown field 'bookstore-names'. Field is not defined in the YANG schema."
                cause.is(originalException)
            }
    }

    def 'Out of Range rule does not match invalid contexts: #scenario.'() {
        given: 'a failure context for the given invalid scenario'
            def context = createContextForOutOfRange(dataSchemaNode, lastStringValue)
        expect: 'the rule does not match'
            !JsonParseFailureRule.OUT_OF_RANGE.matches(context)
        where: 'the following invalid scenarios are tested'
            scenario                               | dataSchemaNode       | lastStringValue
            'non-leaf schema node'                 | Mock(DataSchemaNode) | '5'
            'missing value'                        | Mock(LeafSchemaNode) | null
            'schema node without range constraint' | Mock(LeafSchemaNode) | '5'
    }

    def 'Out of Range rule does not match non-numeric values: #scenario.'() {
        given: 'a failure context with a non-numeric value'
            def context = createContextForOutOfRange(createLeafWithRange(Range.closed(0, 10)), value)
        expect: 'the rule does not match'
            !JsonParseFailureRule.OUT_OF_RANGE.matches(context)
        where: 'the following non-numeric values are tested'
            scenario             | value
            'text value'         | 'not-a-number'
            'empty string'       | ''
            'special characters' | '12@34'
    }

    def 'Out of Range rule correctly evaluates values against range constraints: #scenario.'() {
        given: 'a failure context with a leaf restricted by the given range'
            def context = createContextForOutOfRange(createLeafWithRange(range), value)
        expect: 'the rule matches only when the value is outside the allowed range'
            JsonParseFailureRule.OUT_OF_RANGE.matches(context) == isOutOfRange
        where: 'the following ranges and values are tested'
            scenario                                 | range               | value   || isOutOfRange
            'value within closed range'              | Range.closed(0, 10) | '5'     || false
            'value at closed lower boundary'         | Range.closed(0, 10) | '0'     || false
            'value at closed upper boundary'         | Range.closed(0, 10) | '10'    || false
            'value at open lower boundary (excluded)' | Range.open(0, 10)   | '0'     || true
            'value at open upper boundary (excluded)' | Range.open(0, 10)   | '10'    || true
            'value inside open boundaries'           | Range.open(0, 10)   | '1'     || false
            'value below closed lower boundary'      | Range.closed(0, 10) | '-1'    || true
            'value above closed upper boundary'      | Range.closed(0, 10) | '11'    || true
            'value satisfies at-least range'         | Range.atLeast(0)    | '1000'  || false
            'value satisfies at-most range'          | Range.atMost(10)    | '-1000' || false
            'value below at-least boundary'          | Range.atLeast(0)    | '-1'    || true
            'value above at-most boundary'           | Range.atMost(10)    | '11'    || true
    }

    def 'Out of Range rule formats allowed ranges in exception message: #scenario.'() {
        given: 'a failure context with Out of Range value for the given range'
            def context = createContextForOutOfRange(createLeafWithRange(range), outOfRangeValue)
        expect: 'the exception message contains the expected formatted range'
            JsonParseFailureRule.OUT_OF_RANGE.toException(context).details == expectedMessage
        where: 'the following range types are tested'
            scenario             | range               | outOfRangeValue || expectedMessage
            'closed range'       | Range.closed(0, 10) | '-1'            || "Validation failed at xpath '/some/xpath' : Value -1 is outside the allowed range 0..10."
            'unbounded upper'    | Range.atLeast(0)    | '-1'            || "Validation failed at xpath '/some/xpath' : Value -1 is outside the allowed range 0..max."
            'unbounded lower'    | Range.atMost(10)    | '11'            || "Validation failed at xpath '/some/xpath' : Value 11 is outside the allowed range min..10."
            'single value'       | Range.singleton(5)  | '4'             || "Validation failed at xpath '/some/xpath' : Value 4 is outside the allowed range 5."
    }

    def 'Out of Range Rule preserves original exception as cause.'() {
        given: 'an original exception and Out of Range context'
            def originalException = new IllegalArgumentException('test')
            def context = createContextForOutOfRange(createLeafWithRange(Range.closed(0, 10)), '99', originalException)
        expect: 'the exception preserves the original exception as its cause'
            JsonParseFailureRule.OUT_OF_RANGE.toException(context).cause.is(originalException)
    }

    def 'Fallback rule matches any context and preserves exception details.'() {
        given: 'an original exception and Fallback context'
            def originalException = new RuntimeException('Unexpected validation error')
            def context = new JsonNodeFailureContext(originalException, null, '/some/xpath', '', null, false, Set.of(), false)
        when: 'the fallback rule is applied'
            def result = JsonParseFailureRule.FALLBACK.toException(context)
        then: 'the fallback rule always matches'
            JsonParseFailureRule.FALLBACK.matches(context)
        and: 'the exception contains the original message and cause'
            with(result) {
                message == 'Data Validation Failed'
                details == 'Unexpected validation error'
                cause.is(originalException)
            }
    }

    private JsonNodeFailureContext createContextForNullLeaf(boolean isLeaf, boolean lastValueNull) {
        new JsonNodeFailureContext(new Exception(), isLeaf ? Mock(LeafSchemaNode) : Mock(DataSchemaNode),
            '/some/xpath', 'some-node', null, lastValueNull, Set.of(), false)
    }

    private JsonNodeFailureContext createContextForInvalidInteger(boolean isLeaf, Exception exception) {
        new JsonNodeFailureContext(exception, isLeaf ? Mock(LeafSchemaNode) : Mock(DataSchemaNode),
            '/some/xpath', 'some-node', 'not-a-number', false, Set.of(), false)
    }

    private JsonNodeFailureContext createContextForUnknownField(String nodeName, boolean schemaNodeNotFound) {
        new JsonNodeFailureContext(new Exception(), null, '/some/xpath', nodeName, null, false, Set.of(), schemaNodeNotFound)
    }

    private JsonNodeFailureContext createContextForOutOfRange(DataSchemaNode dataSchemaNode, String lastStringValue,
                                                              Exception exception = new Exception()) {
        new JsonNodeFailureContext(exception, dataSchemaNode, '/some/xpath', 'some-node', lastStringValue, false, Set.of(), false)
    }

    private LeafSchemaNode createLeafWithRange(Range<Integer>... allowedRanges) {
        def rangeSet = TreeRangeSet.<Integer> create()
        allowedRanges.each { rangeSet.add(it) }
        def rangeConstraint = Mock(RangeConstraint) {
            getAllowedRanges() >> rangeSet
        }
        def rangeRestrictedType = Mock(RangeRestrictedTypeDefinition) {
            getRangeConstraint() >> Optional.of(rangeConstraint)
        }
        Mock(LeafSchemaNode) {
            getType() >> rangeRestrictedType
        }
    }
}
