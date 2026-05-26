/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG
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

package org.onap.cps.impl.query

import org.onap.cps.api.exceptions.DataValidationException
import org.onap.cps.api.model.CompositeQuery
import spock.lang.Specification

class CompositeQueryValidatorSpec extends Specification {

    def 'Composite query with an invalid operator on #scenario.'() {
        given: 'a query carrying an unsupported operator'
            def condition = CompositeQuery.builder().cpsPath('//books').operator(conditionOperator).build()
            def query = CompositeQuery.builder().cpsPath('//categories').operator(queryOperator)
                .conditions([condition]).build()
        when: 'the composite query is validated'
            CompositeQueryValidator.validateCompositeQuery(query)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
        where: 'the invalid operator appears on the following level'
            scenario                     | queryOperator | conditionOperator
            'invalid top-level operator' | 'invalid'     | 'and'
            'invalid nested operator'    | 'and'         | 'invalid'
    }

    def 'Composite query with a missing cps path on a condition.'() {
        given: 'a query whose condition has no cps path'
            def condition = CompositeQuery.builder().build()
            def query = CompositeQuery.builder().cpsPath('//categories').conditions([condition]).build()
        when: 'the composite query is validated'
            CompositeQueryValidator.validateCompositeQuery(query)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
    }

    def 'Composite query exceeding the maximum nesting depth.'() {
        given: 'a query with conditions nested more than 10 levels deep'
            def query = CompositeQuery.builder().cpsPath('//categories').build()
            def deepestCondition = query
            (1..11).each {
                def nestedCondition = CompositeQuery.builder().cpsPath('//books').build()
                deepestCondition.setConditions([nestedCondition])
                deepestCondition = nestedCondition
            }
        when: 'the composite query is validated'
            CompositeQueryValidator.validateCompositeQuery(query)
        then: 'a data validation exception is thrown'
            thrown(DataValidationException)
    }

    def 'Composite query with #scenario conditions.'() {
        given: 'a query with a null or empty conditions list'
            def query = CompositeQuery.builder().cpsPath('//categories').conditions(conditions).build()
        when: 'the composite query is validated'
            CompositeQueryValidator.validateCompositeQuery(query)
        then: 'the conditions are normalized to an empty list'
            query.conditions == []
        where: 'the following conditions lists are used'
            scenario | conditions
            'null'   | null
            'empty'  | []
    }
}
