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

package org.onap.cps.impl.query

import org.onap.cps.api.model.CompositeQuery
import spock.lang.Shared
import spock.lang.Specification

class CompositeQueryEvaluatorSpec extends Specification {

    @Shared def AND = CompositeQueryOperator.AND
    @Shared def OR  = CompositeQueryOperator.OR
    @Shared final String XPATH_1 = '/bookstore/categories[@code=1]'
    @Shared final String XPATH_2 = '/bookstore/categories[@code=2]'
    @Shared final String XPATH_3 = '/bookstore/categories[@code=3]'

    def objectUnderTest = new CompositeQueryEvaluator()
    def scopeXpaths = [XPATH_1, XPATH_2, XPATH_3] as Set<String>

    def '#scenario operator with empty conditions.'() {
        given: 'no conditions'
            def conditions = []
        and: 'a condition evaluator'
            def conditionEvaluator = Mock(CompositeQueryEvaluator.ConditionEvaluator)
        when: 'the operator is evaluated'
            def result = objectUnderTest.evaluate(conditions, operator, scopeXpaths, conditionEvaluator)
        then: 'all xpaths from the scope are returned'
            result == scopeXpaths
        and: 'the condition evaluator is never called'
            0 * conditionEvaluator.evaluate(_, _)
        where: 'the following operators are used'
            scenario | operator
            'AND'    | AND
            'OR'     | OR
    }

    def 'AND operator with overlapping condition results.'() {
        given: 'two conditions'
            def condition1 = CompositeQuery.builder().cpsPath('//NodeA').build()
            def condition2 = CompositeQuery.builder().cpsPath('//NodeB').build()
            def conditions = [condition1, condition2]
        and: 'condition evaluator returns overlapping sets'
            def conditionEvaluator = Mock(CompositeQueryEvaluator.ConditionEvaluator)
            conditionEvaluator.evaluate(condition1, scopeXpaths) >> ([XPATH_1, XPATH_2] as Set)
            conditionEvaluator.evaluate(condition2, scopeXpaths) >> ([XPATH_2, XPATH_3] as Set)
        when: 'AND operator is evaluated'
            def result = objectUnderTest.evaluate(conditions, AND, scopeXpaths, conditionEvaluator)
        then: 'the result contains only xpaths present in all condition results'
            result == [XPATH_2] as Set
    }

    def 'AND operator when #scenario.'() {
        given: 'two conditions'
            def condition1 = CompositeQuery.builder().cpsPath('//NodeA').build()
            def condition2 = CompositeQuery.builder().cpsPath('//NodeB').build()
            def conditions = [condition1, condition2]
        and: 'condition evaluator returns results for each condition'
            def conditionEvaluator = Mock(CompositeQueryEvaluator.ConditionEvaluator)
            conditionEvaluator.evaluate(condition1, scopeXpaths) >> resultOfCondition1
            conditionEvaluator.evaluate(condition2, scopeXpaths) >> resultOfCondition2
        when: 'AND operator is evaluated'
            def result = objectUnderTest.evaluate(conditions, AND, scopeXpaths, conditionEvaluator)
        then: 'an empty set is returned because not all conditions matched'
            result.isEmpty()
        where: 'the following combinations of conditions are evaluated'
            scenario                      | resultOfCondition1 | resultOfCondition2
            'the conditions are disjoint' | [XPATH_1] as Set   | [XPATH_2] as Set
            'one condition is empty'      | [XPATH_1] as Set   | [] as Set
            'all conditions are empty'    | [] as Set          | [] as Set
    }

    def 'AND operator short-circuits when the running intersection becomes empty.'() {
        given: 'two conditions where the first selects nothing'
            def condition1 = CompositeQuery.builder().cpsPath('//NodeA').build()
            def condition2 = CompositeQuery.builder().cpsPath('//NodeB').build()
            def conditions = [condition1, condition2]
        and: 'a condition evaluator'
            def conditionEvaluator = Mock(CompositeQueryEvaluator.ConditionEvaluator)
        when: 'AND operator is evaluated'
            def result = objectUnderTest.evaluate(conditions, AND, scopeXpaths, conditionEvaluator)
        then: 'the first condition is evaluated and selects nothing'
            1 * conditionEvaluator.evaluate(condition1, scopeXpaths) >> ([] as Set)
        and: 'the second condition is never evaluated'
            0 * conditionEvaluator.evaluate(condition2, _)
        and: 'an empty set is returned'
            result.isEmpty()
    }

    def 'OR operator with #scenario.'() {
        given: 'two conditions'
            def condition1 = CompositeQuery.builder().cpsPath('//NodeA').build()
            def condition2 = CompositeQuery.builder().cpsPath('//NodeB').build()
            def conditions = [condition1, condition2]
        and: 'condition evaluator returns results for each condition'
            def conditionEvaluator = Mock(CompositeQueryEvaluator.ConditionEvaluator)
            conditionEvaluator.evaluate(condition1, scopeXpaths) >> resultOfCondition1
            conditionEvaluator.evaluate(condition2, scopeXpaths) >> resultOfCondition2
        when: 'OR operator is evaluated'
            def result = objectUnderTest.evaluate(conditions, OR, scopeXpaths, conditionEvaluator)
        then: 'the result is the union of all condition results'
            result == expectedResponse
        where: 'the following combinations of conditions are used'
            scenario                                            | resultOfCondition1   | resultOfCondition2   | expectedResponse
            'union of all conditions when all conditions match' | ([XPATH_1] as Set)   | ([XPATH_2] as Set)   | [XPATH_1, XPATH_2] as Set
            'matched xpaths when only one condition matches'    | ([XPATH_1] as Set)   | ([] as Set)          | [XPATH_1] as Set
            'empty set when all conditions return empty'        | ([] as Set)          | ([] as Set)          | [] as Set
    }
}
