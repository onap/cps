/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.operations;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import org.onap.cps.ncmp.api.models.PersistenceCmHandle;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Builder
public class DmiRequestBody {
    public enum OperationEnum {
        READ("read"),
        CREATE("create"),
        UPDATE("update"),
        PATCH("patch"),
        DELETE("delete");
        private final String value;

        OperationEnum(final String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return String.valueOf(value);
        }
    }

    private OperationEnum operation;
    private String dataType;
    private String data;
    @JsonProperty("cmHandleProperties")
    private Map<String, String> dmiProperties;
    private String requestId;

    /**
     * Set DMI Properties by converting a list of PersistenceCmHandle.Property objects.
     *
     * @param dmiPropertiesAsList list of cm handle dmi properties
     */
    public void asDmiProperties(
        final List<PersistenceCmHandle.Property> dmiPropertiesAsList) {
        dmiProperties = new LinkedHashMap<>();
        for (final PersistenceCmHandle.Property dmiProperty : dmiPropertiesAsList) {
            dmiProperties.put(dmiProperty.getName(), dmiProperty.getValue());
        }
    }

}
