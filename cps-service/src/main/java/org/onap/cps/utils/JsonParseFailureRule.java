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

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Collectors;
import org.onap.cps.api.exceptions.CpsException;
import org.onap.cps.api.exceptions.DataValidationException;
import org.opendaylight.yangtools.yang.model.api.TypedDataSchemaNode;
import org.opendaylight.yangtools.yang.model.api.type.RangeConstraint;
import org.opendaylight.yangtools.yang.model.api.type.RangeRestrictedTypeDefinition;

/**
 * Rules used to translate a JSON parsing/validation failure into a user-friendly {@link CpsException}.
 * Constants are declared in the order they must be evaluated (most specific first), with
 * {@link #FALLBACK} always matching last so a result is always produced.
 */
enum JsonParseFailureRule {

    NULL_LEAF {
        @Override
        boolean matches(final JsonNodeFailureContext context) {
            return context.isLeaf() && context.lastValueNull();
        }

        @Override
        CpsException toException(final JsonNodeFailureContext context) {
            return new DataValidationException(DATA_VALIDATION_FAILURE_MESSAGE,
                String.format(NULL_LEAF_MESSAGE, context.xpath(), context.nodeName()), context.exception());
        }
    },

    INVALID_INTEGER {
        @Override
        boolean matches(final JsonNodeFailureContext context) {
            return context.isLeaf() && containsNumberFormatException(context.exception());
        }

        @Override
        CpsException toException(final JsonNodeFailureContext context) {
            return new DataValidationException(DATA_VALIDATION_FAILURE_MESSAGE,
                String.format(INVALID_INTEGER_MESSAGE, context.xpath(), context.lastStringValue()),
                context.exception());
        }
    },

    OUT_OF_RANGE {
        @Override
        boolean matches(final JsonNodeFailureContext context) {
            if (!context.isLeaf() || context.lastStringValue() == null) {
                return false;
            }
            final String value = context.lastStringValue();
            if (!parseNumberValue(value).isPresent()) {
                return false;
            }
            return findRangeConstraintFromContext(context)
                .map(rangeConstraint -> !isWithinRange(rangeConstraint, value))
                .orElse(false);
        }

        @Override
        CpsException toException(final JsonNodeFailureContext context) {
            final RangeConstraint<?> rangeConstraint = findRangeConstraintFromContext(context).orElseThrow();
            return new DataValidationException(DATA_VALIDATION_FAILURE_MESSAGE,
                String.format(OUT_OF_RANGE_MESSAGE, context.xpath(), context.lastStringValue(),
                    formatAllowedRanges(rangeConstraint)),
                context.exception());
        }
    },

    UNKNOWN_FIELD {
        @Override
        boolean matches(final JsonNodeFailureContext context) {
            if (!context.schemaNodeNotFound()) {
                return false;
            }
            final String nodeName = context.nodeName();
            return nodeName != null && !nodeName.isEmpty();
        }

        @Override
        CpsException toException(final JsonNodeFailureContext context) {
            return new DataValidationException(DATA_VALIDATION_FAILURE_MESSAGE,
                String.format(UNKNOWN_FIELD_MESSAGE, context.xpath(), context.nodeName()), context.exception());
        }
    },

    FALLBACK {
        @Override
        @SuppressFBWarnings(value = "NP_BOOLEAN_RETURN_NULL",
            justification = "FALLBACK always returns true - the false branch is intentionally unreachable")
        boolean matches(final JsonNodeFailureContext context) {
            return true;
        }

        @Override
        CpsException toException(final JsonNodeFailureContext context) {
            return new DataValidationException(DATA_VALIDATION_FAILURE_MESSAGE,
                getMessageOrEmpty(context.exception()), context.exception());
        }
    };

    private static final String DATA_VALIDATION_FAILURE_MESSAGE = "Data Validation Failed";
    private static final String NULL_LEAF_MESSAGE =
        "Validation failed at xpath '%s' : Null value is not allowed for mandatory leaf '%s'.";
    private static final String INVALID_INTEGER_MESSAGE =
        "Validation failed at xpath '%s' : Value '%s' is not a valid integer.";
    private static final String OUT_OF_RANGE_MESSAGE =
        "Validation failed at xpath '%s' : Value %s is outside the allowed range %s.";
    private static final String UNKNOWN_FIELD_MESSAGE =
        "Validation failed at xpath '%s' : Unknown field '%s'. Field is not defined in the YANG schema.";

    /**
     * Returns {@code true} if this rule applies to the given failure context.
     *
     * @param context the context captured at the point of failure
     * @return {@code true} if the rule matches
     */
    abstract boolean matches(JsonNodeFailureContext context);

    /**
     * Converts the failure context into a {@link CpsException}.
     * <p>
     * This method must only be invoked after {@link #matches(JsonNodeFailureContext)}
     * has returned {@code true} for the same context. Callers must not invoke {@code toException} independently
     * because a rule may assume its preconditions are satisfied and behave incorrectly otherwise.
     * </p>
     *
     * @param context the context captured at the point of failure
     * @return a {@link CpsException} (or a more specific subtype) with a user-friendly message
     */
    abstract CpsException toException(JsonNodeFailureContext context);

    private static Optional<? extends RangeConstraint<?>> findRangeConstraint(
            final TypedDataSchemaNode typedDataSchemaNode) {
        if (typedDataSchemaNode.getType() instanceof RangeRestrictedTypeDefinition<?, ?> rangeRestrictedType) {
            return rangeRestrictedType.getRangeConstraint();
        }
        return Optional.empty();
    }

    private static boolean isWithinRange(final RangeConstraint<?> rangeConstraint, final String actualValue) {
        return parseNumberValue(actualValue)
            .map(value -> rangeConstraint.getAllowedRanges().asRanges().stream()
                .anyMatch(range -> contains(range, value)))
            .orElse(false);
    }

    private static Optional<BigDecimal> parseNumberValue(final String value) {
        try {
            return Optional.of(new BigDecimal(value));
        } catch (final NumberFormatException numberFormatException) {
            return Optional.empty();
        }
    }

    private static boolean contains(final Range<? extends Number> range, final BigDecimal value) {
        if (range.hasLowerBound()) {
            final BigDecimal lowerEndpoint = new BigDecimal(range.lowerEndpoint().toString());
            final int comparison = value.compareTo(lowerEndpoint);
            if (range.lowerBoundType() == BoundType.OPEN ? comparison <= 0 : comparison < 0) {
                return false;
            }
        }
        if (range.hasUpperBound()) {
            final BigDecimal upperEndpoint = new BigDecimal(range.upperEndpoint().toString());
            final int comparison = value.compareTo(upperEndpoint);
            return range.upperBoundType() == BoundType.OPEN ? comparison < 0 : comparison <= 0;
        }
        return true;
    }

    private static String formatAllowedRanges(final RangeConstraint<?> rangeConstraint) {
        return rangeConstraint.getAllowedRanges().asRanges().stream()
            .map(JsonParseFailureRule::formatRange)
            .collect(Collectors.joining(" | "));
    }

    private static String formatRange(final Range<? extends Number> range) {
        final String lowerEndpoint = range.hasLowerBound() ? range.lowerEndpoint().toString() : "min";
        final String upperEndpoint = range.hasUpperBound() ? range.upperEndpoint().toString() : "max";
        return lowerEndpoint.equals(upperEndpoint) ? lowerEndpoint : lowerEndpoint + ".." + upperEndpoint;
    }

    private static String getMessageOrEmpty(final Exception exception) {
        final String message = exception.getMessage();
        return message == null ? "" : message;
    }

    private static boolean containsNumberFormatException(final Exception exception) {
        if (exception instanceof NumberFormatException) {
            return true;
        }
        Throwable cause = exception.getCause();
        while (cause != null) {
            if (cause instanceof NumberFormatException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static Optional<? extends RangeConstraint<?>> findRangeConstraintFromContext(
        final JsonNodeFailureContext context) {
        final TypedDataSchemaNode typedDataSchemaNode = (TypedDataSchemaNode) context.dataSchemaNode();
        return findRangeConstraint(typedDataSchemaNode);
    }
}

