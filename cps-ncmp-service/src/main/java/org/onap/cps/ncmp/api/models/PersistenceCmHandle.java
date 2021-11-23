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
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.onap.cps.ncmp.api.impl.operations.RequiredDmiService;

/**
 * DmiRegistry.
 */
@Getter
@Setter
@NoArgsConstructor
public class PersistenceCmHandle {

    private String id;

    @JsonProperty("dmi-service-name")
    private String dmiServiceName;

    @JsonProperty("dmi-data-service-name")
    private String dmiDataServiceName;

    @JsonProperty("dmi-model-service-name")
    private String dmiModelServiceName;

    @JsonProperty("additional-properties")
    private List<AdditionalProperty> additionalProperties;

    /**
     * Create a persistenceCmHandle.
     * @param dmiServiceName dmi service name
     * @param dmiDataServiceName dmi data service name
     * @param dmiModelServiceName dmi model service name
     * @param cmHandle the cm handle
     * @return instance of persistenceCmHandle
     */
    public static PersistenceCmHandle toPersistenceCmHandle(final String dmiServiceName,
                                                            final String dmiDataServiceName,
                                                            final String dmiModelServiceName,
                                                            final CmHandle cmHandle) {
        final PersistenceCmHandle persistenceCmHandle = new PersistenceCmHandle();
        persistenceCmHandle.setId(cmHandle.getCmHandleID());
        persistenceCmHandle.setDmiServiceName(dmiServiceName);
        persistenceCmHandle.setDmiDataServiceName(dmiDataServiceName);
        persistenceCmHandle.setDmiModelServiceName(dmiModelServiceName);
        if (cmHandle.getCmHandleProperties() == null) {
            persistenceCmHandle.asAdditionalProperties(Collections.emptyMap());
        } else {
            persistenceCmHandle.asAdditionalProperties(cmHandle.getCmHandleProperties());
        }
        return persistenceCmHandle;
    }

    /**
     * Set Additional Properties map, key and value pair.
     * @param additionalPropertiesAsMap Map of Additional Properties
     */
    public void asAdditionalProperties(final Map<String, String> additionalPropertiesAsMap) {
        additionalProperties = new ArrayList<>(additionalPropertiesAsMap.size());
        for (final Map.Entry<String, String> entry : additionalPropertiesAsMap.entrySet()) {
            additionalProperties.add(new AdditionalProperty(entry.getKey(), entry.getValue()));
        }
    }

    /**
     * Resolve a dmi service name.
     * @param requiredService indicates what typo of service is required
     * @return dmi service name
     */
    public String resolveDmiServiceName(final RequiredDmiService requiredService) {
        if (isNullEmptyOrBlank(dmiServiceName)) {
            if (RequiredDmiService.DATA.equals(requiredService)) {
                return dmiDataServiceName;
            }
            return dmiModelServiceName;
        }
        return dmiServiceName;
    }

    private static boolean isNullEmptyOrBlank(final String serviceName) {
        return Strings.isNullOrEmpty(serviceName) || serviceName.isBlank();
    }

    @AllArgsConstructor
    @Data
    public static class AdditionalProperty {

        @JsonProperty()
        private final String name;

        @JsonProperty()
        private final String value;
    }

}
