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

import spock.lang.Specification

class CompositeQueryEvaluatorSpec extends Specification {

    def conditionEvaluator = { condition, scope -> scope }

    def 'Evaluate with no conditions returns the full scope unchanged.'() {
        given: 'an empty collection of conditions and a scope of xpaths'
            def scopeXpaths = ['/xpath-1', '/xpath-2'] as Set
        when: 'evaluate is called directly with an empty conditions collection'
            def result = CompositeQueryEvaluator.evaluate(CompositeQueryOperator.AND, [], scopeXpaths, conditionEvaluator)
        then: 'the full scope is returned unchanged'
            result == scopeXpaths
    }
}