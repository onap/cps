/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation
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


package org.onap.cps.ncmp.api.impl.yangmodels;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.onap.cps.ncmp.api.impl.inventory.CompositeState;
import org.onap.cps.ncmp.api.impl.operations.RequiredDmiService;
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle;

/**
 * Cm Handle which follows the Yang resource dmi registry model when persisting data to DMI or the DB.
 * Yang model CmHandle
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class YangModelCmHandle {

    @EqualsAndHashCode.Include
    private String id;

    @JsonProperty("dmi-service-name")
    private String dmiServiceName;

    @JsonProperty("dmi-data-service-name")
    private String dmiDataServiceName;

    @JsonProperty("state")
    private CompositeState compositeState;

    @JsonProperty("dmi-model-service-name")
    private String dmiModelServiceName;

    @JsonProperty("module-set-tag")
    private String moduleSetTag;

    @JsonProperty("alternate-id")
    private String alternateId;

    @JsonProperty("additional-properties")
    private List<Property> dmiProperties;

    @JsonProperty("public-properties")
    private List<Property> publicProperties;

    /**
     * Creates a deep copy of Yang Model Cm Handle.
     *
     * @param original Yang Model Cm Handle
     * @return instance of yangModelCmHandle
     */
    public static YangModelCmHandle deepCopyOf(final YangModelCmHandle original) {
        final YangModelCmHandle copy = new YangModelCmHandle();
        copy.id = original.getId();
        copy.dmiServiceName = original.getDmiServiceName();
        copy.dmiDataServiceName = original.getDmiDataServiceName();
        copy.dmiModelServiceName = original.getDmiModelServiceName();
        copy.compositeState =
                original.getCompositeState() == null ? null : new CompositeState(original.getCompositeState());
        copy.dmiProperties = original.getDmiProperties() == null ? null : new ArrayList<>(original.getDmiProperties());
        copy.publicProperties =
                original.getPublicProperties() == null ? null : new ArrayList<>(original.getPublicProperties());
        copy.alternateId = original.getAlternateId();
        return copy;
    }

    /**
     * Create a yangModelCmHandle.
     *
     * @param dmiServiceName      dmi service name
     * @param dmiDataServiceName  dmi data service name
     * @param dmiModelServiceName dmi model service name
     * @param ncmpServiceCmHandle the cm handle
     * @return instance of yangModelCmHandle
     */
    public static YangModelCmHandle toYangModelCmHandle(final String dmiServiceName,
                                                        final String dmiDataServiceName,
                                                        final String dmiModelServiceName,
                                                        final NcmpServiceCmHandle ncmpServiceCmHandle,
                                                        final String moduleSetTag,
                                                        final String alternateId) {
        final YangModelCmHandle yangModelCmHandle = new YangModelCmHandle();
        yangModelCmHandle.setId(ncmpServiceCmHandle.getCmHandleId());
        yangModelCmHandle.setDmiServiceName(dmiServiceName);
        yangModelCmHandle.setDmiDataServiceName(dmiDataServiceName);
        yangModelCmHandle.setDmiModelServiceName(dmiModelServiceName);
        yangModelCmHandle.setModuleSetTag(moduleSetTag == null ? StringUtils.EMPTY : moduleSetTag);
        yangModelCmHandle.setAlternateId(alternateId);
        yangModelCmHandle.setDmiProperties(asYangModelCmHandleProperties(ncmpServiceCmHandle.getDmiProperties()));
        yangModelCmHandle.setPublicProperties(asYangModelCmHandleProperties(
                ncmpServiceCmHandle.getPublicProperties()));
        yangModelCmHandle.setCompositeState(ncmpServiceCmHandle.getCompositeState());
        return yangModelCmHandle;
    }

    /**
     * Resolve a dmi service name.
     *
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

    private static List<Property> asYangModelCmHandleProperties(final Map<String, String> propertiesAsMap) {
        final List<Property> yangModelCmHandleProperties = new ArrayList<>(propertiesAsMap.size());
        for (final Map.Entry<String, String> entry : propertiesAsMap.entrySet()) {
            yangModelCmHandleProperties.add(new YangModelCmHandle.Property(entry.getKey(), entry.getValue()));
        }
        return yangModelCmHandleProperties;
    }

    private static boolean isNullEmptyOrBlank(final String serviceName) {
        return Strings.isNullOrEmpty(serviceName) || serviceName.isBlank();
    }

    @AllArgsConstructor
    @Data
    public static class Property {

        @JsonProperty()
        private final String name;

        @JsonProperty()
        private final String value;
    }

}
