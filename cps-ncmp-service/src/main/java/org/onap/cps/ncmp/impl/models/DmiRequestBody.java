/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import org.onap.cps.ncmp.api.data.models.OperationType;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Builder
@JsonPropertyOrder({"operation", "dataType", "data", "cmHandleProperties", "requestId", "moduleSetTag"})
public class DmiRequestBody {

    @JsonProperty("operation")
    private OperationType operationType;
    private String dataType;
    private String data;
    @JsonProperty("cmHandleProperties")
    private Map<String, String> additionalProperties;
    private String requestId;
    private String moduleSetTag;

    /**
     * Set additional Properties by converting a list of YangModelCmHandle.Property objects.
     *
     * @param yangModelCmHandleProperties list of cm handle additional properties
     */
    public void asAdditionalProperties(
        final List<YangModelCmHandle.Property> yangModelCmHandleProperties) {
        additionalProperties = new LinkedHashMap<>();
        for (final YangModelCmHandle.Property additionalProperty : yangModelCmHandleProperties) {
            additionalProperties.put(additionalProperty.getName(), additionalProperty.getValue());
        }
    }

}
