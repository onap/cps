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

    def fragmentEntities = [1L: Mock(FragmentEntity), 2L: Mock(FragmentEntity), 3L: Mock(FragmentEntity)]

    def 'Resolve operator from string for #scenario.'() {
        expect: 'the correct operator enum value is returned'
            objectUnderTest.getNormalizedOperator(operatorString) == expectedOperator
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

    def 'Resolve operator from #scenario string.'() {
        expect: 'AND is returned as the default operator'
            objectUnderTest.getNormalizedOperator(operatorString) == AND
        where:
            scenario      | operatorString
            'null'        | null
            'empty'       | ''
            'blank'       | '   '
    }

    def 'Resolve operator from unsupported string throws expected exception.'() {
        when: 'an unsupported operator string is provided'
            objectUnderTest.getNormalizedOperator('xor')
        then: 'a DataValidationException is thrown'
            def exception = thrown(DataValidationException)
        and: 'exception has correct message'
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

    def '#scenario operator with empty conditions.'() {
        given: 'no conditions'
            def conditions = []
        and: 'a condition evaluator'
            def evaluator = Mock(CompositeQueryOperators.ConditionEvaluator)
        when: 'AND operator is evaluated'
            def result = operator.evaluateOperator(conditions, fragmentEntities, mockAnchorEntity, evaluator)
        then: 'all IDs from the flat map are returned'
            result == fragmentEntities.keySet()
        and: 'the evaluator is never called'
            0 * evaluator.evaluate(_, _, _)
        where:
            scenario | operator
            'AND'    | objectUnderTest.AND
            'OR'     | objectUnderTest.OR
    }

    def 'AND operator with overlapping fragments returns intersection without duplicates.'() {
        given: 'two conditions built with the builder to ensure correct field defaults'
            def condition1 = CompositeQuery.builder().cpsPath('//NodeA').build()
            def condition2 = CompositeQuery.builder().cpsPath('//NodeB').build()
            def conditions = [condition1, condition2]
        and: 'evaluator returns overlapping sets'
            def evaluator = Mock(CompositeQueryOperators.ConditionEvaluator)
            evaluator.evaluate(condition1, fragmentEntities, mockAnchorEntity) >> ([1L, 2L] as Set)
            evaluator.evaluate(condition2, fragmentEntities, mockAnchorEntity) >> ([2L, 3L] as Set)
        when: 'AND operator is evaluated'
            def result = objectUnderTest.AND.evaluateOperator(conditions, fragmentEntities, mockAnchorEntity, evaluator)
        then: 'the result contains only IDs present in all condition results'
            result == [2L] as Set
    }

    def 'AND operator returns empty set when #scenario.'() {
        given: 'two conditions built'
            def condition1 = CompositeQuery.builder().cpsPath('//NodeA').build()
            def condition2 = CompositeQuery.builder().cpsPath('//NodeB').build()
            def conditions = [condition1, condition2]
        and: 'evaluator returns a result for condition1 and condition2'
            def evaluator = Mock(CompositeQueryOperators.ConditionEvaluator)
            evaluator.evaluate(condition1, fragmentEntities, mockAnchorEntity) >> resultOfCondition1
            evaluator.evaluate(condition2, fragmentEntities, mockAnchorEntity) >> resultOfCondition2
        when: 'AND operator is evaluated'
            def result = objectUnderTest.AND.evaluateOperator(conditions, fragmentEntities, mockAnchorEntity, evaluator)
        then: 'an empty set is returned because not all conditions matched'
            result.isEmpty()
        where:
            scenario                      | resultOfCondition1 | resultOfCondition2
            'the conditions are disjoint' | [1L] as Set        | [2L] as Set
            'one condition is empty'      | [1L] as Set        | [] as Set
            'all conditions is empty'     | [] as Set          | [] as Set
    }

    def 'OR operator returns #scenario.'() {
        given: 'two conditions built with the builder to ensure correct field defaults'
            def condition1 = CompositeQuery.builder().cpsPath('//NodeA').build()
            def condition2 = CompositeQuery.builder().cpsPath('//NodeB').build()
            def conditions = [condition1, condition2]
        and: 'evaluator returns disjoint non-empty sets for each condition'
            def evaluator = Mock(CompositeQueryOperators.ConditionEvaluator)
            evaluator.evaluate(condition1, fragmentEntities, mockAnchorEntity) >> resultOfCondition1
            evaluator.evaluate(condition2, fragmentEntities, mockAnchorEntity) >> resultOfCondition2
        when: 'OR operator is evaluated'
            def result = objectUnderTest.OR.evaluateOperator(conditions, fragmentEntities, mockAnchorEntity, evaluator)
        then: 'the result is the union of all condition results'
            result == expectedResponse
        where:
            scenario                                            | resultOfCondition1 | resultOfCondition2 | expectedResponse
            'union of all condition, when all conditions match' | ([1L] as Set)      | ([2L] as Set)      | [1L, 2L] as Set
            'matched IDs when only one condition matches'       | ([1L] as Set)      | ([] as Set)        | [1L] as Set
            'empty set when all conditions return empty'        | ([] as Set)        | ([] as Set)        | [] as Set
    }
}
