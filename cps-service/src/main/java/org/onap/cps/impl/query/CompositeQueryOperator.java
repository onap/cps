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

import java.util.Set;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.api.model.CompositeQuery;

public enum CompositeQueryOperator {

    AND(true),
    OR(false);

    private final boolean requiresAllConditions;

    CompositeQueryOperator(final boolean requiresAllConditions) {
        this.requiresAllConditions = requiresAllConditions;
    }

    /** Distinguishes AND (true) from OR (false); drives operator dispatch in CompositeQueryEvaluator and nested match
     * invalidation in resolveNestedMatches. */
    public boolean requiresAllConditions() {
        return requiresAllConditions;
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

    @FunctionalInterface
    public interface ConditionEvaluator {
        /** scopeXpaths is the full candidate set at the top level but narrowed to descendants of matched nodes at
         * nested recursion levels. */
        Set<String> evaluate(CompositeQuery condition,
                             Set<String> scopeXpaths,
                             String dataspaceName,
                             String anchorName);
    }
}

