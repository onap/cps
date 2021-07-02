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
 *  ============LICENSE_END=========================================================
 */


package org.onap.cps.ncmp.api.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DmiRegistry.
 */
@Getter
@Setter
@NoArgsConstructor
@SuppressWarnings("all")
public class PersistenceCmHandle {

    @JsonProperty()
    private String id;

    @JsonProperty("dmi-service-name")
    private String dmiServiceName;

    @JsonProperty("additional-properties")
    private List<AdditionalProperty> additionalProperty;

    /**
     * Add Additional Properties map, key and value pair.
     * @param additionalPropertiesAsMap Map of Additional Properties
     */
    public void addAdditionalProperties(final Map<String, String> additionalPropertiesAsMap) {
        additionalProperty = new ArrayList<>(additionalPropertiesAsMap.size());
        for (final Map.Entry<String, String> entry : additionalPropertiesAsMap.entrySet()) {
            additionalProperty.add(new AdditionalProperty(entry.getKey(), entry.getValue()));
        }
    }

    @AllArgsConstructor
    private static class AdditionalProperty {

        @JsonProperty()
        private final String name;

        @JsonProperty()
        private final String value;
    }

}
