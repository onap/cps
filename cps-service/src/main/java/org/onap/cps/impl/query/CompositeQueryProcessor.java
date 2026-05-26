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

import static org.onap.cps.impl.query.CompositeQueryOperator.getNormalizedOperator;

import java.util.Collection;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.onap.cps.api.exceptions.DataValidationException;
import org.onap.cps.api.model.CompositeQuery;
import org.onap.cps.api.model.DataNode;
import org.onap.cps.api.parameters.FetchDescendantsOption;
import org.onap.cps.spi.CpsDataPersistenceService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CompositeQueryProcessor {

    private static final int MAXIMUM_CONDITION_NESTING_DEPTH = 10;

    private final CpsDataPersistenceService cpsDataPersistenceService;
    private final CompositeQueryEvaluator compositeQueryEvaluator;

    /**
     * Executes a composite query against the given dataspace and anchor.
     * The query is first validated and normalized in a single upfront pass so that invalid input fails fast
     * before any database access. A new {@link CompositeQueryExecution} then carries the per-query state
     * through the three phases of the algorithm: fetch the candidate trees, compute the selected xpaths and
     * build the filtered result trees.
     *
     * @param dataspaceName          the dataspace name
     * @param anchorName             the anchor name
     * @param compositeQuery         the composite query containing cpsPath, operator and conditions
     * @param fetchDescendantsOption option controlling how many levels of descendants to include
     * @return                       collection of DataNodes matching the composite query
     */
    public Collection<DataNode> processCompositeQuery(final String dataspaceName,
                                                      final String anchorName,
                                                      final CompositeQuery compositeQuery,
                                                      final FetchDescendantsOption fetchDescendantsOption) {
        normalizeCompositeQuery(compositeQuery, 0);
        final CompositeQueryExecution compositeQueryExecution = new CompositeQueryExecution(
            cpsDataPersistenceService, compositeQueryEvaluator, dataspaceName, anchorName);
        return compositeQueryExecution.execute(compositeQuery, fetchDescendantsOption);
    }

    /**
     * Validates and normalizes the composite query and all its (nested) conditions in one upfront pass:
     * every cps path must be present, every operator must parse to a supported value and the nesting of
     * conditions may not exceed {@code MAXIMUM_CONDITION_NESTING_DEPTH}. A missing conditions collection
     * (possible after Jackson deserialization which bypasses the builder default) is replaced by an empty
     * collection so that later phases never need to check for null.
     *
     * @param compositeQuery the composite query (condition) to validate and normalize
     * @param nestingDepth   the current nesting depth, starting at 0 for the top-level query
     */
    private static void normalizeCompositeQuery(final CompositeQuery compositeQuery, final int nestingDepth) {
        if (nestingDepth > MAXIMUM_CONDITION_NESTING_DEPTH) {
            throw new DataValidationException(
                "Maximum nesting depth of " + MAXIMUM_CONDITION_NESTING_DEPTH + " exceeded",
                "Reduce the nesting of composite query conditions");
        }
        if (compositeQuery.getCpsPath() == null || compositeQuery.getCpsPath().isBlank()) {
            throw new DataValidationException("cps path is missing",
                "Each composite query (condition) requires a cps path");
        }
        getNormalizedOperator(compositeQuery.getOperator());
        if (compositeQuery.getConditions() == null) {
            compositeQuery.setConditions(Collections.emptyList());
            return;
        }
        for (final CompositeQuery compositeQueryCondition : compositeQuery.getConditions()) {
            normalizeCompositeQuery(compositeQueryCondition, nestingDepth + 1);
        }
    }
}
