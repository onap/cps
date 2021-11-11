/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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

package org.onap.cps.ncmp.api.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.exception.NcmpException;

@Slf4j
@JsonInclude(Include.NON_NULL)
@Getter
@Builder
public class DmiRequestBody {
    public enum OperationEnum {
        READ("read"),
        CREATE("create"),
        UPDATE("update");
        private String value;

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
    private Map<String, String> cmHandleProperties;
    private static ObjectMapper objectMapper;

    /**
     * Set CmHandleProperties by converting a list of PersistenceCmHandle.AdditionalProperty objects.
     *
     * @param cmHandlePropertiesAsList the cm handle additional properties
     */
    public void asCmHandleProperties(
        final List<PersistenceCmHandle.AdditionalProperty> cmHandlePropertiesAsList) {
        final boolean isCmHandlePropertiesNullOrEmpty =
            cmHandlePropertiesAsList == null || cmHandlePropertiesAsList.isEmpty();
        if (isCmHandlePropertiesNullOrEmpty) {
            cmHandleProperties = Collections.emptyMap();
        } else {
            cmHandleProperties = new LinkedHashMap<>();
            for (final PersistenceCmHandle.AdditionalProperty additionalProperty : cmHandlePropertiesAsList) {
                cmHandleProperties.put(additionalProperty.getName(),
                    additionalProperty.getValue());
            }
        }
    }

    /**
     * Convert DmiRequestBody to JSON.
     *
     * @param dmiRequestBody the dmi request body
     * @return DmiRequestBody as JSON
     */
    public static String toBodyAsString(final DmiRequestBody dmiRequestBody) {
        try {
            return objectMapper.writeValueAsString(dmiRequestBody);
        } catch (final JsonProcessingException e) {
            log.error("Parsing error occurred while converting Object to JSON.");
            throw new NcmpException("Parsing error occurred while converting given object to JSON.",
                e.getMessage());
        }
    }

}
