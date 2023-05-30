/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Tech Mahindra Ltd
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

package org.onap.cps.cpspath.parser;

import java.util.HashMap;
import java.util.Map;

public enum CpsPathComparativeOperator {
    EQ("="),
    GT(">"),
    LT("<"),
    GE(">="),
    LE("<=");

    private final String label;

    CpsPathComparativeOperator(final String label) {
        this.label = label;
    }

    public final String getLabel() {
        return this.label;
    }

    private static final Map<String, CpsPathComparativeOperator> cpsPathComparativeOperatorPerLabel = new HashMap<>();

    static {
        for (final CpsPathComparativeOperator cpsPathComparativeOperator : CpsPathComparativeOperator.values()) {
            cpsPathComparativeOperatorPerLabel.put(cpsPathComparativeOperator.label, cpsPathComparativeOperator);
        }
    }

    /**
     * Finds the value of the given enumeration.
     *
     * @param label value of the enum
     * @return a comparativeOperatorType
     */
    public static CpsPathComparativeOperator fromString(final String label) {
        if (!cpsPathComparativeOperatorPerLabel.containsKey(label)) {
            throw new PathParsingException("Incomplete leaf condition (no operator)");
        }
        return cpsPathComparativeOperatorPerLabel.get(label);
    }
}

