/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2026 Deutsche Telekom AG
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
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

package org.onap.cps.ri.query

import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.api.model.CompositeQuery
import org.onap.cps.ri.models.AnchorEntity
import org.onap.cps.ri.models.FragmentEntity
import spock.lang.Specification

class CompositeQueryOperatorsSpec extends Specification {

    static def AND = CompositeQueryOperators.AND
    static def OR = CompositeQueryOperators.OR
    def mockAnchorEntity = Mock(AnchorEntity)

    static def objectUnderTest = CompositeQueryOperators

    def flatMap = [1L: Mock(FragmentEntity), 2L: Mock(FragmentEntity), 3L: Mock(FragmentEntity)]

    def 'Resolve operator from string for #scenario.'() {
        expect: 'the correct operator enum value is returned'
            objectUnderTest.fromString(operatorString) == expectedOperator
        where:
            scenario             | operatorString || expectedOperator
            'and lowercase'      | 'and'          || AND
            'and uppercase'      | 'AND'          || AND
            'and mixed case'     | 'And'          || AND
            'and with spaces'    | ' and '        || AND
            'or lowercase'       | 'or'           || OR
            'or uppercase'       | 'OR'           || OR
            'or mixed case'      | 'Or'           || OR
    }

    def 'Resolve operator from null or blank string defaults to AND.'() {
        expect: 'AND is returned as the default'
            objectUnderTest.fromString(operatorString) == AND
        where:
            scenario      | operatorString
            'null'        | null
            'empty'       | ''
            'blank'       | '   '
    }

    def 'Resolve operator from unsupported string throws DataValidationException.'() {
        when: 'an unsupported operator string is provided'
            objectUnderTest.fromString('xor')
        then: 'a DataValidationException is thrown'
            def exception = thrown(DataValidationException)
        and:
            exception.message.contains('Invalid operator: xor')
    }

    def 'requiresAllConditions flag is correct for each operator.'() {
        expect: 'the flag matches the operator semantics'
            operator.requiresAllConditions() == expectedFlag
        where:
            operator                || expectedFlag
            objectUnderTest.AND     || true
            objectUnderTest.OR      || false
    }

    def 'AND operator with empty conditions returns all fragment IDs from the flat map.'() {
        given: 'no conditions'
            def conditions = []
        and: 'a condition evaluator that should not be called'
            def evaluator = Mock(CompositeQueryOperators.ConditionEvaluator)
        when: 'AND operator is evaluated'
            def result = objectUnderTest.AND.evaluateOperator(conditions, flatMap, mockAnchorEntity, evaluator)
        then: 'all IDs from the flat map are returned'
            result == flatMap.keySet()
        and: 'the evaluator is never called'
            0 * evaluator.evaluate(_, _, _)
    }

    def 'OR operator with empty conditions returns all fragment IDs from the flat map.'() {
        given: 'no conditions'
            def conditions = []
        and: 'a condition evaluator that should not be called'
            def evaluator = Mock(CompositeQueryOperators.ConditionEvaluator)
        when: 'OR operator is evaluated'
            def result = objectUnderTest.OR.evaluateOperator(conditions, flatMap, mockAnchorEntity, evaluator)
        then: 'all IDs from the flat map are returned'
            result == flatMap.keySet()
        and: 'the evaluator is never called'
            0 * evaluator.evaluate(_, _, _)
    }

    def 'AND operator returns intersection of all condition results when all conditions match.'() {
        given: 'two conditions built with the builder to ensure correct field defaults'
            def condition1 = CompositeQuery.builder().cpsPath('//NodeA').build()
            def condition2 = CompositeQuery.builder().cpsPath('//NodeB').build()
            def conditions = [condition1, condition2]
        and: 'evaluator returns disjoint non-empty sets for each condition'
            def evaluator = Mock(CompositeQueryOperators.ConditionEvaluator)
            evaluator.evaluate(condition1, flatMap, mockAnchorEntity) >> ([1L] as Set)
            evaluator.evaluate(condition2, flatMap, mockAnchorEntity) >> ([2L] as Set)
        when: 'AND operator is evaluated'
            def result = objectUnderTest.AND.evaluateOperator(conditions, flatMap, mockAnchorEntity, evaluator)
        then: 'the result is empty because there is no overlap'
            result.isEmpty()
    }

    def 'AND operator returns empty set when any condition returns empty.'() {
        given: 'two conditions built with the builder to ensure correct field defaults'
            def condition1 = CompositeQuery.builder().cpsPath('//NodeA').build()
            def condition2 = CompositeQuery.builder().cpsPath('//NodeB').build()
            def conditions = [condition1, condition2]
        and: 'evaluator returns a result for condition1 but empty for condition2'
            def evaluator = Mock(CompositeQueryOperators.ConditionEvaluator)
            evaluator.evaluate(condition1, flatMap, mockAnchorEntity) >> ([1L] as Set)
            evaluator.evaluate(condition2, flatMap, mockAnchorEntity) >> ([] as Set)
        when: 'AND operator is evaluated'
            def result = objectUnderTest.AND.evaluateOperator(conditions, flatMap, mockAnchorEntity, evaluator)
        then: 'an empty set is returned because not all conditions matched'
            result.isEmpty()
    }

    def 'AND operator returns empty set when all conditions return empty.'() {
        given: 'two conditions built with the builder to ensure correct field defaults'
            def condition1 = CompositeQuery.builder().cpsPath('//NodeA').build()
            def condition2 = CompositeQuery.builder().cpsPath('//NodeB').build()
            def conditions = [condition1, condition2]
        and: 'evaluator returns empty for both conditions'
            def evaluator = Mock(CompositeQueryOperators.ConditionEvaluator)
            evaluator.evaluate(condition1, flatMap, mockAnchorEntity) >> ([] as Set)
            evaluator.evaluate(condition2, flatMap, mockAnchorEntity) >> ([] as Set)
        when: 'AND operator is evaluated'
            def result = objectUnderTest.AND.evaluateOperator(conditions, flatMap, mockAnchorEntity, evaluator)
        then: 'an empty set is returned'
            result.isEmpty()
    }

    def 'OR operator returns union of all condition results when all conditions match.'() {
        given: 'two conditions built with the builder to ensure correct field defaults'
            def condition1 = CompositeQuery.builder().cpsPath('//NodeA').build()
            def condition2 = CompositeQuery.builder().cpsPath('//NodeB').build()
            def conditions = [condition1, condition2]
        and: 'evaluator returns disjoint non-empty sets for each condition'
            def evaluator = Mock(CompositeQueryOperators.ConditionEvaluator)
            evaluator.evaluate(condition1, flatMap, mockAnchorEntity) >> ([1L] as Set)
            evaluator.evaluate(condition2, flatMap, mockAnchorEntity) >> ([2L] as Set)
        when: 'OR operator is evaluated'
            def result = objectUnderTest.OR.evaluateOperator(conditions, flatMap, mockAnchorEntity, evaluator)
        then: 'the result is the union of all condition results'
            result == [1L, 2L] as Set
    }

    def 'OR operator returns matched IDs when only one condition matches.'() {
        given: 'two conditions built with the builder to ensure correct field defaults'
            def condition1 = CompositeQuery.builder().cpsPath('//NodeA').build()
            def condition2 = CompositeQuery.builder().cpsPath('//NodeB').build()
            def conditions = [condition1, condition2]
        and: 'evaluator returns a result for condition1 but empty for condition2'
            def evaluator = Mock(CompositeQueryOperators.ConditionEvaluator)
            evaluator.evaluate(condition1, flatMap, mockAnchorEntity) >> ([1L] as Set)
            evaluator.evaluate(condition2, flatMap, mockAnchorEntity) >> ([] as Set)
        when: 'OR operator is evaluated'
            def result = objectUnderTest.OR.evaluateOperator(conditions, flatMap, mockAnchorEntity, evaluator)
        then: 'the result contains only the IDs from the matching condition'
            result == [1L] as Set
    }

    def 'OR operator returns empty set when all conditions return empty.'() {
        given: 'two conditions built with the builder to ensure correct field defaults'
            def condition1 = CompositeQuery.builder().cpsPath('//NodeA').build()
            def condition2 = CompositeQuery.builder().cpsPath('//NodeB').build()
            def conditions = [condition1, condition2]
        and: 'evaluator returns empty for both conditions'
            def evaluator = Mock(CompositeQueryOperators.ConditionEvaluator)
            evaluator.evaluate(condition1, flatMap, mockAnchorEntity) >> ([] as Set)
            evaluator.evaluate(condition2, flatMap, mockAnchorEntity) >> ([] as Set)
        when: 'OR operator is evaluated'
            def result = objectUnderTest.OR.evaluateOperator(conditions, flatMap, mockAnchorEntity, evaluator)
        then: 'an empty set is returned'
            result.isEmpty()
    }

//    def 'AND operator evaluates each condition exactly once.'() {
//        given: 'two conditions built with the builder to ensure correct field defaults'
//            def condition1 = CompositeQuery.builder().cpsPath('//NodeA').build()
//            def condition2 = CompositeQuery.builder().cpsPath('//NodeB').build()
//            def conditions = [condition1, condition2]
//        and: 'an evaluator returning non-empty sets'
//            def evaluator = Mock(CompositeQueryOperators.ConditionEvaluator)
//            evaluator.evaluate(condition1, flatMap, mockAnchorEntity) >> ([1L] as Set)
//            evaluator.evaluate(condition2, flatMap, mockAnchorEntity) >> ([2L] as Set)
//        when: 'AND operator is evaluated'
//            objectUnderTest.AND.evaluateOperator(conditions, flatMap, mockAnchorEntity, evaluator)
//        then: 'the evaluator is called exactly once per condition'
//            1 * evaluator.evaluate(condition1, flatMap, mockAnchorEntity)
//            1 * evaluator.evaluate(condition2, flatMap, mockAnchorEntity)
//    }
//
//    def 'OR operator evaluates each condition exactly once.'() {
//        given: 'two conditions built with the builder to ensure correct field defaults'
//            def condition1 = CompositeQuery.builder().cpsPath('//NodeA').build()
//            def condition2 = CompositeQuery.builder().cpsPath('//NodeB').build()
//            def conditions = [condition1, condition2]
//        and: 'an evaluator returning non-empty sets'
//            def evaluator = Mock(CompositeQueryOperators.ConditionEvaluator)
//            evaluator.evaluate(condition1, flatMap, mockAnchorEntity) >> ([1L] as Set)
//            evaluator.evaluate(condition2, flatMap, mockAnchorEntity) >> ([2L] as Set)
//        when: 'OR operator is evaluated'
//            objectUnderTest.OR.evaluateOperator(conditions, flatMap, mockAnchorEntity, evaluator)
//        then: 'the evaluator is called exactly once per condition'
//            1 * evaluator.evaluate(condition1, flatMap, mockAnchorEntity)
//            1 * evaluator.evaluate(condition2, flatMap, mockAnchorEntity)
//    }

    def 'AND operator with overlapping condition results returns intersection without duplicates.'() {
        given: 'two conditions built with the builder to ensure correct field defaults'
            def condition1 = CompositeQuery.builder().cpsPath('//NodeA').build()
            def condition2 = CompositeQuery.builder().cpsPath('//NodeB').build()
            def conditions = [condition1, condition2]
        and: 'evaluator returns overlapping sets'
            def evaluator = Mock(CompositeQueryOperators.ConditionEvaluator)
            evaluator.evaluate(condition1, flatMap, mockAnchorEntity) >> ([1L, 2L] as Set)
            evaluator.evaluate(condition2, flatMap, mockAnchorEntity) >> ([2L, 3L] as Set)
        when: 'AND operator is evaluated'
            def result = objectUnderTest.AND.evaluateOperator(conditions, flatMap, mockAnchorEntity, evaluator)
        then: 'the result contains only IDs present in all condition results'
            result == [2L] as Set
    }
}
