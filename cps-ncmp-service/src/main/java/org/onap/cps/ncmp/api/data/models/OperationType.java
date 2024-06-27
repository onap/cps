/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.data.models;

import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;
import lombok.Getter;
import org.onap.cps.ncmp.impl.data.exceptions.InvalidOperationException;

@Getter
public enum OperationType {

    READ("read"),
    CREATE("create"),
    UPDATE("update"),
    PATCH("patch"),
    DELETE("delete");

    private final String operationName;

    OperationType(final String operationName) {
        this.operationName = operationName;
    }

    @Override
    @JsonValue
    public String toString() {
        return String.valueOf(operationName);
    }

    /**
     * From operation name get operation enum type.
     *
     * @param operationName the operation name
     * @return the operation enum type
     */
    public static OperationType fromOperationName(final String operationName) {
        try {
            return OperationType.valueOf(operationName.toUpperCase(Locale.ENGLISH));
        } catch (final IllegalArgumentException e) {
            throw new InvalidOperationException(operationName + " is an invalid operation name");
        }
    }
}
