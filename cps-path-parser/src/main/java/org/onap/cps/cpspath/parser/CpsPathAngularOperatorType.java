/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Tech Mahindra.
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

public enum CpsPathAngularOperatorType {
    GT(">"),
    LT("<");
    private final String labels;

    CpsPathAngularOperatorType(final String labels) {
        this.labels = labels;
    }

    public final String getLabels() {
        return this.labels;
    }

    /**
     * Finds the value of the given enumeration.
     **/
    public static CpsPathAngularOperatorType fromString(final String labels) {
        for (final CpsPathAngularOperatorType angularOperators : CpsPathAngularOperatorType.values()) {
            if (angularOperators.labels.equalsIgnoreCase(labels)) {
                return angularOperators;
            }
        }
        return null;
    }

}
