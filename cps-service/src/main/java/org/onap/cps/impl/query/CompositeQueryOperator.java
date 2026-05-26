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

import org.onap.cps.api.exceptions.DataValidationException;

public enum CompositeQueryOperator {

    AND,
    OR;

    /**
     * Parses and normalizes a raw operator string into its corresponding CompositeQueryOperator enum constant,
     * defaulting to AND if operator is not provided.
     *
     * @param operatorName  operator name in composite query
     * @return              the corresponding CompositeQueryOperator enum constant
     */
    public static CompositeQueryOperator toCompositeQueryOperator(final String operatorName) {
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
}

