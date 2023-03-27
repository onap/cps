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
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.utils;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.onap.cps.ncmp.api.impl.operations.RequiredDmiService;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.springframework.stereotype.Service;

@Service
public class DmiServiceNameOrganizer {

    /**
     * organizes a map with dmi service name as key for cm handle with its properties.
     *
     * @param yangModelCmHandles list of cm handle model
     */
    public Map<String, Map<String, Map<String, String>>> organizeByDmiServiceName(
            final Collection<YangModelCmHandle> yangModelCmHandles) {
        final Map<String, Map<String, Map<String, String>>> dmiServiceNameCmHandlePropertiesMap
                = new HashMap<>();
        yangModelCmHandles.forEach(yangModelCmHandle -> {
            final String dmiServiceName = yangModelCmHandle.resolveDmiServiceName(RequiredDmiService.DATA);
            if (!dmiServiceNameCmHandlePropertiesMap.containsKey(dmiServiceName)) {
                final Map<String, Map<String, String>> cmHandleDmiPropertiesMap = new HashMap<>();
                cmHandleDmiPropertiesMap.put(yangModelCmHandle.getId(),
                        dmiPropertiesAsMap(yangModelCmHandle.getDmiProperties()));
                dmiServiceNameCmHandlePropertiesMap.put(dmiServiceName, cmHandleDmiPropertiesMap);
            } else {
                dmiServiceNameCmHandlePropertiesMap.get(dmiServiceName)
                        .put(yangModelCmHandle.getId(), dmiPropertiesAsMap(yangModelCmHandle.getDmiProperties()));
            }
        });
        return dmiServiceNameCmHandlePropertiesMap;
    }

    private Map<String, String> dmiPropertiesAsMap(final List<YangModelCmHandle.Property> dmiProperties) {
        return dmiProperties.stream().collect(
                Collectors.toMap(YangModelCmHandle.Property::getName, YangModelCmHandle.Property::getValue));
    }
}
