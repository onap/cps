/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.dmi;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.models.RequiredDmiService;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DmiServiceNameOrganizer {

    /**
     * organizes a map with dmi service name as key for cm handle with its properties.
     *
     * @param yangModelCmHandles list of cm handle model
     */
    public static Map<String, Map<String, Map<String, String>>> getAdditionalPropertiesPerCmHandleIdPerDmiServiceName(
            final Collection<YangModelCmHandle> yangModelCmHandles) {
        final Map<String, Map<String, Map<String, String>>> additionalPropertiesPerCmHandleIdPerServiceName
                = new HashMap<>();
        yangModelCmHandles.forEach(yangModelCmHandle -> {
            final String dmiServiceName = yangModelCmHandle.resolveDmiServiceName(RequiredDmiService.DATA);
            if (!additionalPropertiesPerCmHandleIdPerServiceName.containsKey(dmiServiceName)) {
                final Map<String, Map<String, String>> cmHandleAdditionalPropertiesMap = new HashMap<>();
                cmHandleAdditionalPropertiesMap.put(yangModelCmHandle.getId(),
                        additionalPropertiesAsMap(yangModelCmHandle.getAdditionalProperties()));
                additionalPropertiesPerCmHandleIdPerServiceName.put(dmiServiceName, cmHandleAdditionalPropertiesMap);
            } else {
                additionalPropertiesPerCmHandleIdPerServiceName.get(dmiServiceName)
                        .put(yangModelCmHandle.getId(),
                            additionalPropertiesAsMap(yangModelCmHandle.getAdditionalProperties()));
            }
        });
        return additionalPropertiesPerCmHandleIdPerServiceName;
    }

    private static Map<String, String> additionalPropertiesAsMap(
        final List<YangModelCmHandle.Property> additionalProperties) {
        return additionalProperties.stream().collect(
                Collectors.toMap(YangModelCmHandle.Property::name, YangModelCmHandle.Property::value));
    }
}
