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

package org.onap.cps.ri.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.api.model.CompositeQuery;
import org.onap.cps.ri.models.AnchorEntity;
import org.onap.cps.ri.models.FragmentEntity;

public enum CompositeQueryOperators {

    AND("and", true, CompositeQueryOperators::executeAnd),
    OR("or", false, CompositeQueryOperators::executeOr);

    @Getter
    private final String operatorName;
    private final boolean requiresAllConditions;
    private final OperatorEvaluator operatorEvaluator;

    CompositeQueryOperators(final String operatorName, final boolean requiresAllConditions,
                            final OperatorEvaluator operatorEvaluator) {
        this.operatorName = operatorName;
        this.requiresAllConditions = requiresAllConditions;
        this.operatorEvaluator = operatorEvaluator;
    }

    public boolean requiresAllConditions() {
        return requiresAllConditions;
    }


    public Set<Long> evaluateOperator(final Collection<CompositeQuery> conditions,
                                      final Map<Long, FragmentEntity> idsToFragmentEntities,
                                      final AnchorEntity anchor,
                                      final ConditionEvaluator conditionEvaluator) {
        return operatorEvaluator.apply(conditions, idsToFragmentEntities, anchor, conditionEvaluator);
    }

    public static CompositeQueryOperators fromString(final String operatorName) {
        if (operatorName == null || operatorName.isBlank()) {
            return AND;
        }

        final String normalizedName = operatorName.trim().toLowerCase();

        for (final CompositeQueryOperators operator : values()) {
            if (operator.operatorName.equals(normalizedName)) {
                return operator;
            }
        }

        throw new DataValidationException("Invalid operator: " + normalizedName, "Supported operators are: AND/OR");
    }

    private static List<Set<Long>> evaluateAllConditions(final Collection<CompositeQuery> conditions,
                                                         final Map<Long, FragmentEntity> idsToFragmentEntities,
                                                         final AnchorEntity anchor,
                                                         final ConditionEvaluator conditionEvaluator) {
        final List<Set<Long>> results = new ArrayList<>();
        for (final CompositeQuery condition : conditions) {
            results.add(conditionEvaluator.evaluate(condition, idsToFragmentEntities, anchor));
        }
        return results;
    }

    private static Set<Long> executeAnd(final Collection<CompositeQuery> conditions,
                                        final Map<Long, FragmentEntity> idsToFragmentEntities,
                                        final AnchorEntity anchor,
                                        final ConditionEvaluator evaluator) {
        if (conditions.isEmpty()) {
            return new HashSet<>(idsToFragmentEntities.keySet());
        }

        final List<Set<Long>> conditionResults = evaluateAllConditions(
            conditions, idsToFragmentEntities, anchor, evaluator);

        final Set<Long> result = new HashSet<>(idsToFragmentEntities.keySet());
        conditionResults.forEach(result::retainAll);
        return result;
    }

    private static Set<Long> executeOr(final Collection<CompositeQuery> conditions,
                                      final Map<Long, FragmentEntity> idsToFragmentEntities,
                                      final AnchorEntity anchor,
                                      final ConditionEvaluator conditionEvaluator) {
        if (conditions.isEmpty()) {
            return new HashSet<>(idsToFragmentEntities.keySet());
        }

        final List<Set<Long>> conditionResults = evaluateAllConditions(
            conditions, idsToFragmentEntities, anchor, conditionEvaluator);

        final Set<Long> result = new HashSet<>();
        conditionResults.forEach(result::addAll);
        return result;
    }

    @FunctionalInterface
    interface OperatorEvaluator {
        Set<Long> apply(Collection<CompositeQuery> compositeQueries,
                       Map<Long, FragmentEntity> idsToFragmentEntities,
                       AnchorEntity anchor,
                       ConditionEvaluator evaluator);
    }

    @FunctionalInterface
    public interface ConditionEvaluator {
        Set<Long> evaluate(CompositeQuery condition,
                           Map<Long, FragmentEntity> idsToFragmentEntities,
                           AnchorEntity anchor);
    }
}
