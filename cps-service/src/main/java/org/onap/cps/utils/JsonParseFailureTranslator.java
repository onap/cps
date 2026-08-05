/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 Deutsche Telekom AG. All rights reserved.
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

package org.onap.cps.utils;

import java.util.Arrays;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.onap.cps.api.exceptions.CpsException;
import org.onap.cps.api.exceptions.DataValidationException;

@UtilityClass
class JsonParseFailureTranslator {

    private static final String DATA_VALIDATION_FAILURE_MESSAGE = "Data Validation Failed";
    private static final String NULL_LEAF_MESSAGE =
        "Validation failed at xpath '%s' : Null value is not allowed for mandatory leaf '%s'.";
    private static final String INVALID_INTEGER_MESSAGE =
        "Validation failed at xpath '%s' : Value '%s' is not a valid integer.";

    private static final JsonParseFailureRule FALLBACK_RULE = new FallbackRule();

    /**
     * Translates the given failure context into a {@link CpsException}.
     * {@link Rule} constants are evaluated in declaration order (most specific first).
     * If no rule matches, {@link FallbackRule} is applied as a last resort.
     *
     * @param context the context captured at the point of failure
     * @return a CpsException with a user-friendly message
     */
    static CpsException translate(final JsonNodeFailureContext context) {
        return Stream.concat(Arrays.stream(Rule.values()), Stream.of(FALLBACK_RULE))
            .filter(rule -> rule.matches(context))
            .findFirst()
            .map(rule -> rule.toException(context))
            .orElseThrow();
    }

    private enum Rule implements JsonParseFailureRule {

        NULL_LEAF {
            @Override
            public boolean matches(final JsonNodeFailureContext context) {
                return context.isLeaf() && context.lastValueNull();
            }

            @Override
            public DataValidationException toException(final JsonNodeFailureContext context) {
                return new DataValidationException(DATA_VALIDATION_FAILURE_MESSAGE,
                    String.format(NULL_LEAF_MESSAGE, context.xpath(), context.nodeName()));
            }
        },

        INVALID_INTEGER {
            @Override
            public boolean matches(final JsonNodeFailureContext context) {
                return context.isLeaf() && context.exception().getCause() instanceof NumberFormatException;
            }

            @Override
            public DataValidationException toException(final JsonNodeFailureContext context) {
                return new DataValidationException(DATA_VALIDATION_FAILURE_MESSAGE,
                    String.format(INVALID_INTEGER_MESSAGE, context.xpath(), context.lastStringValue()));
            }
        }
    }

    private static class FallbackRule implements JsonParseFailureRule {

        @Override
        public boolean matches(final JsonNodeFailureContext context) {
            return true;
        }

        @Override
        public DataValidationException toException(final JsonNodeFailureContext context) {
            return new DataValidationException(DATA_VALIDATION_FAILURE_MESSAGE,
                context.exception().getMessage(), context.exception());
        }
    }
}