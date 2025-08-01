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

package org.onap.cps.ncmp.impl.data.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Getter
@Builder
public class DmiOperationCmHandle {
    private String id;

    @JsonProperty("cmHandleProperties")
    private Map<String, String> additionalProperties;
    private String moduleSetTag;

    /**
     * Builds Dmi Operation Cm Handle object with all its associated properties.
     */
    public static DmiOperationCmHandle buildDmiOperationCmHandle(final String cmHandleId,
                                                                 final Map<String, String> additionalProperties,
                                                                 final String moduleSetTag) {
        return DmiOperationCmHandle.builder().id(cmHandleId)
                .additionalProperties(additionalProperties).moduleSetTag(moduleSetTag)
                .build();
    }
}
