/*
 * ============LICENSE_START=======================================================
 * Copyright (c) 2025 OpenInfra Foundation Europe. All rights reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
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

package org.onap.cps.ncmp.impl.cmnotificationsubscription.cmavc;

import com.fasterxml.jackson.annotation.JsonValue;

/*
 Enum for valid Cm Avc Event operations.
 */
public enum CmAvcOperationEnum {

    READ("read"),

    CREATE("create"),

    UPDATE("update"),

    PATCH("patch"),

    DELETE("delete");

    private final String value;

    CmAvcOperationEnum(final String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    /**
     * Returns the Operation Enum.
     *
     * @param value string operation
     * @return CmAvcOperationEnum
     */
    public static CmAvcOperationEnum fromValue(final String value) {
        for (final CmAvcOperationEnum b : CmAvcOperationEnum.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

}
