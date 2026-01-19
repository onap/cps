package org.onap.cps.ri.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import org.onap.cps.api.exceptions.CpsPathException;
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
                                      final ConditionEvaluator evaluator) {
        return operatorEvaluator.apply(conditions, idsToFragmentEntities, anchor, evaluator);
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

        throw new CpsPathException(
            String.format("Unsupported operator: '%s'. Supported operators are: %s",
                operatorName,
                Arrays.stream(values())
                    .map(CompositeQueryOperators::getOperatorName)
                    .toList())
        );
    }

    private static List<Set<Long>> evaluateAllConditions(
        final Collection<CompositeQuery> conditions,
        final Map<Long, FragmentEntity> idsToFragmentEntities,
        final AnchorEntity anchor,
        final ConditionEvaluator evaluator) {
        final List<Set<Long>> results = new ArrayList<>();
        for (final CompositeQuery condition : conditions) {
            results.add(evaluator.evaluate(condition, idsToFragmentEntities, anchor));
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

        final boolean allConditionsSatisfied = conditionResults.stream()
            .noneMatch(Set::isEmpty);

        if (allConditionsSatisfied) {
            final Set<Long> result = new HashSet<>();
            conditionResults.forEach(result::addAll);
            return result;
        }

        return Collections.emptySet();
    }

    private static Set<Long> executeOr(final Collection<CompositeQuery> conditions,
                                      final Map<Long, FragmentEntity> idsToFragmentEntities,
                                      final AnchorEntity anchor,
                                      final ConditionEvaluator evaluator) {
        if (conditions.isEmpty()) {
            return new HashSet<>(idsToFragmentEntities.keySet());
        }

        final List<Set<Long>> conditionResults = evaluateAllConditions(
            conditions, idsToFragmentEntities, anchor, evaluator);

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

