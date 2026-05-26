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
import java.util.function.BiFunction;
import org.onap.cps.api.model.CompositeQuery;

/**
 * Combines the results of composite query conditions using the AND/OR operator logic. Stateless and static:
 * how a single condition is evaluated is supplied by the caller, keeping the operator logic independent of
 * persistence and caching concerns.
 */
final class CompositeQueryEvaluator {

    private CompositeQueryEvaluator() {
    }

    /**
     * Evaluates a collection of conditions against a scope of xpaths and combines the individual results
     * according to the given operator. Every condition is evaluated against the same full scope, not a
     * sequentially narrowed one, preserving correct set-algebra semantics.
     * The switch expression is exhaustive: adding a new operator to CompositeQueryOperator will not compile
     * until its combining behaviour is defined here.
     *
     * @param operator                 the operator that determines how results are combined
     * @param compositeQueryConditions the conditions to evaluate
     * @param scopeXpaths              set of all xpaths that are in scope for this evaluation
     * @param conditionEvaluator       the function used to resolve each individual condition
     * @return                         the set of xpaths that satisfy the combined conditions; when there are
     *                                 no conditions the full scope is returned unchanged
     */
    static Set<String> evaluate(final CompositeQueryOperator operator,
                                final Collection<CompositeQuery> compositeQueryConditions,
                                final Set<String> scopeXpaths,
                                final BiFunction<CompositeQuery, Set<String>, Set<String>> conditionEvaluator) {
        if (compositeQueryConditions.isEmpty()) {
            return new HashSet<>(scopeXpaths);
        }
        return switch (operator) {
            case AND -> combineWithAnd(compositeQueryConditions, scopeXpaths, conditionEvaluator);
            case OR -> combineWithOr(compositeQueryConditions, scopeXpaths, conditionEvaluator);
        };
    }

    /**
     * Intersection: narrows the running result to each condition's matches in turn. Evaluation short-circuits:
     * once the running intersection is empty, remaining conditions are not evaluated (avoiding their database
     * queries) because the result can no longer become non-empty.
     */
    private static Set<String> combineWithAnd(final Collection<CompositeQuery> compositeQueryConditions,
                                              final Set<String> scopeXpaths,
                                              final BiFunction<CompositeQuery, Set<String>, Set<String>>
                                                  conditionEvaluator) {
        Set<String> combinedXpaths = scopeXpaths;
        for (final CompositeQuery compositeQueryCondition : compositeQueryConditions) {
            if (combinedXpaths.isEmpty()) {
                break;
            }
            combinedXpaths = conditionEvaluator.apply(compositeQueryCondition, combinedXpaths);
        }
        return combinedXpaths;
    }

    /** Union: accumulates the xpaths selected by any condition. */
    private static Set<String> combineWithOr(final Collection<CompositeQuery> compositeQueryConditions,
                                             final Set<String> scopeXpaths,
                                             final BiFunction<CompositeQuery, Set<String>, Set<String>>
                                                 conditionEvaluator) {
        final Set<String> combinedXpaths = new HashSet<>();
        for (final CompositeQuery compositeQueryCondition : compositeQueryConditions) {
            combinedXpaths.addAll(conditionEvaluator.apply(compositeQueryCondition, scopeXpaths));
        }
        return combinedXpaths;
    }
}
