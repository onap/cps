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

package org.onap.cps.impl.query;

import static org.onap.cps.impl.query.CompositeQueryOperator.toCompositeQueryOperator;

import java.util.Collections;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.api.model.CompositeQuery;

/**
 * Validates a composite query and all its (nested) conditions in one upfront pass, so that invalid input
 * fails before any database access.
 */
public final class CompositeQueryValidator {

    private static final int MAXIMUM_CONDITION_NESTING_DEPTH = 10;
    private static final int INITIAL_DEPTH_COUNTER = 0;

    private CompositeQueryValidator() {
    }

    /**
     * Validates the composite query and all its (nested) conditions: every cps path must be present, every
     * operator must parse to a supported value and the nesting of conditions may not exceed
     * {@code MAXIMUM_CONDITION_NESTING_DEPTH}. A missing conditions collection (possible after Jackson
     * deserialization which bypasses the builder default) is replaced by an empty collection so that later
     * phases never need to check for null.
     *
     * @param compositeQuery the composite query to validate
     */
    public static void validateCompositeQuery(final CompositeQuery compositeQuery) {
        validateCompositeQuery(compositeQuery, INITIAL_DEPTH_COUNTER);
    }

    private static void validateCompositeQuery(final CompositeQuery compositeQuery, final int nestingDepth) {
        if (nestingDepth > MAXIMUM_CONDITION_NESTING_DEPTH) {
            throw new DataValidationException(
                "Maximum nesting depth of " + MAXIMUM_CONDITION_NESTING_DEPTH + " exceeded",
                "Reduce the nesting of composite query conditions");
        }
        if (compositeQuery.getCpsPath() == null || compositeQuery.getCpsPath().isBlank()) {
            throw new DataValidationException("cps path is missing",
                "Each composite query (condition) requires a cps path");
        }
        toCompositeQueryOperator(compositeQuery.getOperator());
        if (compositeQuery.getConditions() == null) {
            compositeQuery.setConditions(Collections.emptyList());
        }
        for (final CompositeQuery compositeQueryCondition : compositeQuery.getConditions()) {
            validateCompositeQuery(compositeQueryCondition, nestingDepth + 1);
        }
    }
}
