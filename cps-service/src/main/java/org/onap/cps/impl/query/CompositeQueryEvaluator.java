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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.onap.cps.api.model.CompositeQuery;
import org.springframework.stereotype.Component;

/**
 * Evaluates composite query conditions using the AND/OR operator logic.
 * Extracted from {@link CompositeQueryOperator} to separate evaluation behaviour
 * from enum identity and parsing.
 */
@Component
public class CompositeQueryEvaluator {

    /**
     * Evaluates a collection of sub-conditions against a scope of XPaths
     * using the provided operator (AND/OR) and condition evaluator.
     *
     * @param conditions         the logical sub-conditions to evaluate
     * @param operator           the operator that determines how results are combined (AND/OR)
     * @param scopeXpaths        set of all XPaths that are in scope for this evaluation
     * @param dataspaceName      the dataspace name
     * @param anchorName         the anchor name
     * @param conditionEvaluator the evaluator used to resolve each individual condition
     * @return                   the set of XPaths that satisfy the combined conditions
     */
    public Set<String> evaluate(final Collection<CompositeQuery> conditions,
                                final CompositeQueryOperator operator,
                                final Set<String> scopeXpaths,
                                final String dataspaceName,
                                final String anchorName,
                                final CompositeQueryOperator.ConditionEvaluator conditionEvaluator) {
        if (operator.requiresAllConditions()) {
            return executeAnd(conditions, scopeXpaths, dataspaceName, anchorName, conditionEvaluator);
        }
        return executeOr(conditions, scopeXpaths, dataspaceName, anchorName, conditionEvaluator);
    }

    /** Evaluates every condition against the same scopeXpaths independently — each condition sees the full scope,
     * not a sequentially narrowed one, preserving correct set-algebra semantics. */
    private static List<Set<String>> getAllSubConditionsResult(final Collection<CompositeQuery> conditions,
                                                               final Set<String> scopeXpaths,
                                                               final String dataspaceName,
                                                               final String anchorName,
                                                               final CompositeQueryOperator.ConditionEvaluator
                                                                   conditionEvaluator) {
        final List<Set<String>> results = new ArrayList<>(conditions.size());
        for (final CompositeQuery condition : conditions) {
            results.add(conditionEvaluator.evaluate(condition, scopeXpaths, dataspaceName, anchorName));
        }
        return results;
    }

    /** Intersection: starts from scopeXpaths and retains only xpaths present in every condition result;
     * empty conditions returns the full scope unchanged. */
    private static Set<String> executeAnd(final Collection<CompositeQuery> conditions,
                                          final Set<String> scopeXpaths,
                                          final String dataspaceName,
                                          final String anchorName,
                                          final CompositeQueryOperator.ConditionEvaluator conditionEvaluator) {
        if (conditions.isEmpty()) {
            return new HashSet<>(scopeXpaths);
        }
        final List<Set<String>> conditionResults = getAllSubConditionsResult(
            conditions, scopeXpaths, dataspaceName, anchorName, conditionEvaluator);
        final Set<String> result = new HashSet<>(scopeXpaths);
        conditionResults.forEach(result::retainAll);
        return result;
    }

    /** Union: accumulates xpaths from all condition results; empty conditions returns the full scope unchanged. */
    private static Set<String> executeOr(final Collection<CompositeQuery> conditions,
                                         final Set<String> scopeXpaths,
                                         final String dataspaceName,
                                         final String anchorName,
                                         final CompositeQueryOperator.ConditionEvaluator conditionEvaluator) {
        if (conditions.isEmpty()) {
            return new HashSet<>(scopeXpaths);
        }
        final List<Set<String>> conditionResults = getAllSubConditionsResult(
            conditions, scopeXpaths, dataspaceName, anchorName, conditionEvaluator);
        final Set<String> result = new HashSet<>();
        conditionResults.forEach(result::addAll);
        return result;
    }
}

