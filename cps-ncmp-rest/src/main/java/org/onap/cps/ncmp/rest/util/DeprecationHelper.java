/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.rest.util;

import java.util.ArrayList;
import java.util.Collections;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.inventory.models.CmHandleQueryApiParameters;
import org.onap.cps.ncmp.api.inventory.models.ConditionApiProperties;
import org.onap.cps.ncmp.rest.model.CmHandleQueryParameters;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeprecationHelper {

    private final JsonObjectMapper jsonObjectMapper;

    /**
     * Convert the old condition properties to the new schema.
     * !!! remove it after the old condition removed !!!
     * it only works for module names
     *
     * @deprecated this method will be removed in Release 12 (No Name know yet)
     *
     * @param cmHandleQueryParameters the original input parameter
     */
    @Deprecated
    public CmHandleQueryApiParameters mapOldConditionProperties(
                                           final CmHandleQueryParameters cmHandleQueryParameters) {
        final CmHandleQueryApiParameters cmHandleQueryApiParameters =
                jsonObjectMapper.convertToValueType(cmHandleQueryParameters, CmHandleQueryApiParameters.class);
        if (cmHandleQueryParameters.getConditions() != null
                && cmHandleQueryApiParameters.getCmHandleQueryParameters().isEmpty()) {
            cmHandleQueryApiParameters.setCmHandleQueryParameters(new ArrayList<>());
            cmHandleQueryParameters.getConditions().parallelStream().forEach(
                oldConditionProperty -> {
                    if (oldConditionProperty.getConditionParameters() != null
                            && oldConditionProperty.getName() != null) {
                        final ConditionApiProperties conditionApiProperties = new ConditionApiProperties();
                        conditionApiProperties.setConditionName(oldConditionProperty.getName());
                        conditionApiProperties.setConditionParameters(new ArrayList<>());
                        oldConditionProperty.getConditionParameters().parallelStream().forEach(
                            oldConditionParameter ->
                                conditionApiProperties.getConditionParameters().add(Collections
                                    .singletonMap("moduleName", oldConditionParameter.getModuleName()))
                        );
                        cmHandleQueryApiParameters.getCmHandleQueryParameters().add(conditionApiProperties);
                    }
                }
            );
        }

        return cmHandleQueryApiParameters;
    }
}
