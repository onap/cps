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

package org.onap.cps.impl.query;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.onap.cps.api.model.CompositeQuery;
import org.springframework.stereotype.Component;

/**
 * Combines the results of composite query conditions using the AND/OR operator logic.
 */
@Component
public class CompositeQueryEvaluator {

    /**
     * Evaluates a collection of conditions against a scope of xpaths and combines the individual results
     * according to the given operator. Every condition is evaluated against the same full scope, not a
     * sequentially narrowed one, preserving correct set-algebra semantics.
     * The switch expression is exhaustive: adding a new operator to CompositeQueryOperator will not compile
     * until its combining behaviour is defined here.
     *
     * @param compositeQueryConditions the conditions to evaluate
     * @param operator                 the operator that determines how results are combined
     * @param scopeXpaths              set of all xpaths that are in scope for this evaluation
     * @param conditionEvaluator       the evaluator used to resolve each individual condition
     * @return                         the set of xpaths that satisfy the combined conditions; when there are
     *                                 no conditions the full scope is returned unchanged
     */
    public Set<String> evaluate(final Collection<CompositeQuery> compositeQueryConditions,
                                final CompositeQueryOperator operator,
                                final Set<String> scopeXpaths,
                                final ConditionEvaluator conditionEvaluator) {
        if (compositeQueryConditions.isEmpty()) {
            return new HashSet<>(scopeXpaths);
        }
        return switch (operator) {
            case AND -> combineWithAnd(compositeQueryConditions, scopeXpaths, conditionEvaluator);
            case OR -> combineWithOr(compositeQueryConditions, scopeXpaths, conditionEvaluator);
        };
    }

    /**
     * Intersection: starts from the full scope and retains only xpaths selected by every condition.
     * Evaluation short-circuits: once the running intersection is empty, remaining conditions are not
     * evaluated (avoiding their database queries) because the result can no longer become non-empty.
     */
    private static Set<String> combineWithAnd(final Collection<CompositeQuery> compositeQueryConditions,
                                              final Set<String> scopeXpaths,
                                              final ConditionEvaluator conditionEvaluator) {
        final Set<String> combinedXpaths = new HashSet<>(scopeXpaths);
        for (final CompositeQuery compositeQueryCondition : compositeQueryConditions) {
            combinedXpaths.retainAll(conditionEvaluator.evaluate(compositeQueryCondition, scopeXpaths));
            if (combinedXpaths.isEmpty()) {
                return combinedXpaths;
            }
        }
        return combinedXpaths;
    }

    /** Union: accumulates the xpaths selected by any condition. */
    private static Set<String> combineWithOr(final Collection<CompositeQuery> compositeQueryConditions,
                                             final Set<String> scopeXpaths,
                                             final ConditionEvaluator conditionEvaluator) {
        final Set<String> combinedXpaths = new HashSet<>();
        for (final CompositeQuery compositeQueryCondition : compositeQueryConditions) {
            combinedXpaths.addAll(conditionEvaluator.evaluate(compositeQueryCondition, scopeXpaths));
        }
        return combinedXpaths;
    }

    /**
     * Contract for evaluating one composite query condition against a scope of xpaths.
     */
    @FunctionalInterface
    public interface ConditionEvaluator {

        /**
         * Evaluates a single condition against the given scope.
         *
         * @param compositeQueryCondition the condition to evaluate
         * @param scopeXpaths             the xpaths in scope; the full candidate set at the top level but
         *                                narrowed to descendants of selected nodes at nested recursion levels
         * @return                        the xpaths within the scope that satisfy the condition
         */
        Set<String> evaluate(CompositeQuery compositeQueryCondition, Set<String> scopeXpaths);
    }
}
