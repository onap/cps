/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 TechMahindra Ltd
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

public enum CpsPathBooleanOperatorType {
    AND("and"),
    OR("or");

    private final String operatorValue;

    CpsPathBooleanOperatorType(final String operatorValue) {
        this.operatorValue = operatorValue;
    }

    public String getValues() {
        return this.operatorValue;
    }

    /**
     * Finds the value of the given enumeration.
     *
     * @param operatorValue value of the enum
     * @return a booleanOperatorType
     */
    public static CpsPathBooleanOperatorType fromString(final String operatorValue) {
        for (final CpsPathBooleanOperatorType booleanOperatorType : CpsPathBooleanOperatorType.values()) {
            if (booleanOperatorType.operatorValue.equalsIgnoreCase(operatorValue)) {
                return booleanOperatorType;
            }
        }
        return null;
    }
}
