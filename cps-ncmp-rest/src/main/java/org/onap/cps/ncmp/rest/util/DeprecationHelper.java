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
import org.onap.cps.ncmp.api.models.CmHandleQueryApiParameters;
import org.onap.cps.ncmp.api.models.ConditionApiProperties;
import org.onap.cps.ncmp.rest.model.CmHandleQueryRestParameters;
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
     * @param cmHandleQueryRestParameters the original input parameter
     */
    @Deprecated //this method wil be removed in Release 12 (NO Name know yet)
    public CmHandleQueryApiParameters mapOldConditionProperties(
                                           final CmHandleQueryRestParameters cmHandleQueryRestParameters) {
        final CmHandleQueryApiParameters cmHandleQueryApiParameters =
                jsonObjectMapper.convertToValueType(cmHandleQueryRestParameters, CmHandleQueryApiParameters.class);
        if (cmHandleQueryRestParameters.getConditions() != null
                && cmHandleQueryApiParameters.getCmHandleQueryRestParameters() == null) {
            cmHandleQueryApiParameters.setCmHandleQueryRestParameters(new ArrayList<>());
            cmHandleQueryRestParameters.getConditions().forEach(
                    oldConditionProperty -> {
                        if (oldConditionProperty.getConditionParameters() != null
                                && oldConditionProperty.getName() != null) {
                            final ConditionApiProperties conditionApiProperties = new ConditionApiProperties();
                            conditionApiProperties.setConditionName(oldConditionProperty.getName());
                            conditionApiProperties.setConditionParameters(new ArrayList<>());
                            oldConditionProperty.getConditionParameters().forEach(
                                    oldConditionParameter ->
                                            conditionApiProperties.getConditionParameters().add(Collections
                                                    .singletonMap("moduleName", oldConditionParameter.getModuleName()))
                            );
                            cmHandleQueryApiParameters.getCmHandleQueryRestParameters().add(conditionApiProperties);
                        }
                    }
            );
        }

        return cmHandleQueryApiParameters;
    }
}
