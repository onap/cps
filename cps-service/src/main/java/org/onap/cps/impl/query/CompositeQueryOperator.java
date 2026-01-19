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
import java.util.Map;
import java.util.Set;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.api.model.CompositeQuery;
import org.onap.cps.api.model.DataNode;

public enum CompositeQueryOperator {

    AND(true, CompositeQueryOperator::executeAnd),
    OR(false, CompositeQueryOperator::executeOr);

    private final boolean requiresAllConditions;
    private final OperatorEvaluator operatorEvaluator;

    CompositeQueryOperator(final boolean requiresAllConditions,
                           final OperatorEvaluator operatorEvaluator) {
        this.requiresAllConditions = requiresAllConditions;
        this.operatorEvaluator = operatorEvaluator;
    }

    public boolean requiresAllConditions() {
        return requiresAllConditions;
    }

    /**
     * Delegates the evaluation of the composite query to the specific operator implementation
     * (AND/OR) based on the operator type.
     *
     * @param conditions            the logical conditions to evaluate
     * @param xpathToDataNodes      map of xpaths to their corresponding DataNodes
     * @param dataspaceName         the dataspace name
     * @param anchorName            the anchor name
     * @param conditionEvaluator    the evaluator to use for evaluating individual conditions
     * @return                      the set of xpaths that satisfy the composite query conditions
     */
    public Set<String> evaluateOperator(final Collection<CompositeQuery> conditions,
                                        final Map<String, DataNode> xpathToDataNodes,
                                        final String dataspaceName,
                                        final String anchorName,
                                        final ConditionEvaluator conditionEvaluator) {
        return operatorEvaluator.apply(conditions, xpathToDataNodes, dataspaceName, anchorName, conditionEvaluator);
    }

    /**
     * Parses and normalizes a raw operator string into its corresponding CompositeQueryOperator enum constant,
     * defaulting to AND if operator is not provided.
     *
     * @param operatorName  operator name in composite query
     * @return              normalized operator name
     */
    public static CompositeQueryOperator getNormalizedOperator(final String operatorName) {
        if (operatorName == null || operatorName.isBlank()) {
            return AND;
        }
        try {
            return valueOf(operatorName.trim().toUpperCase());
        } catch (final IllegalArgumentException e) {
            throw new DataValidationException("Invalid operator: " + operatorName.trim(),
                "Supported operators are: AND/OR");
        }
    }

    private static List<Set<String>> evaluateAllConditions(final Collection<CompositeQuery> conditions,
                                                           final Map<String, DataNode> xpathToDataNodes,
                                                           final String dataspaceName,
                                                           final String anchorName,
                                                           final ConditionEvaluator conditionEvaluator) {
        final List<Set<String>> results = new ArrayList<>();
        for (final CompositeQuery condition : conditions) {
            results.add(conditionEvaluator.evaluate(condition, xpathToDataNodes, dataspaceName, anchorName));
        }
        return results;
    }

    private static Set<String> executeAnd(final Collection<CompositeQuery> conditions,
                                          final Map<String, DataNode> xpathToDataNodes,
                                          final String dataspaceName,
                                          final String anchorName,
                                          final ConditionEvaluator conditionEvaluator) {
        if (conditions.isEmpty()) {
            return new HashSet<>(xpathToDataNodes.keySet());
        }
        final List<Set<String>> conditionResults = evaluateAllConditions(
            conditions, xpathToDataNodes, dataspaceName, anchorName, conditionEvaluator);
        final Set<String> result = new HashSet<>(xpathToDataNodes.keySet());
        conditionResults.forEach(result::retainAll);
        return result;
    }

    private static Set<String> executeOr(final Collection<CompositeQuery> conditions,
                                         final Map<String, DataNode> xpathToDataNodes,
                                         final String dataspaceName,
                                         final String anchorName,
                                         final ConditionEvaluator conditionEvaluator) {
        if (conditions.isEmpty()) {
            return new HashSet<>(xpathToDataNodes.keySet());
        }
        final List<Set<String>> conditionResults = evaluateAllConditions(
            conditions, xpathToDataNodes, dataspaceName, anchorName, conditionEvaluator);
        final Set<String> result = new HashSet<>();
        conditionResults.forEach(result::addAll);
        return result;
    }

    @FunctionalInterface
    interface OperatorEvaluator {
        Set<String> apply(Collection<CompositeQuery> compositeQueries,
                          Map<String, DataNode> xpathToDataNodes,
                          String dataspaceName,
                          String anchorName,
                          ConditionEvaluator evaluator);
    }

    @FunctionalInterface
    public interface ConditionEvaluator {
        Set<String> evaluate(CompositeQuery condition,
                             Map<String, DataNode> xpathToDataNodes,
                             String dataspaceName,
                             String anchorName);
    }
}

