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
     * Converts the old condition properties from {@link CmHandleQueryParameters}
     * to the new schema defined in {@link CmHandleQueryApiParameters}.
     *
     * <p>This method transforms the old module name-based condition properties to the new format.
     * It should only be used for backward compatibility until the old conditions
     * are removed in future releases.
     *
     * <p><b>Important:</b> This method will be removed in next release(release name not finalized yet).</p>
     *
     * @param cmHandleQueryParameters the original query parameters containing old condition properties
     * @return an instance of {@link CmHandleQueryApiParameters} with the transformed condition properties
     * @deprecated This method is deprecated and will be removed in Release 12.
     *     Use the new condition handling approach instead.
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
