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

package org.onap.cps.utils;

import com.google.common.base.Strings;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import lombok.experimental.UtilityClass;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.CmHandleQueryParameters;

@UtilityClass
public class CmHandleQueryRestParametersValidator {

    private static final List<String> VALID_PROPERTY_NAMES = Arrays.asList("hasAllProperties", "hasAllModules");
    private static final String ERROR_MESSAGE = "Invalid Query Parameter.";

    /**
     * Validate cm handle query parameters.
     * @param cmHandleQueryParameters name of data to be validated
     */
    public static void validateCmHandleQueryParameters(final CmHandleQueryParameters cmHandleQueryParameters) {
        cmHandleQueryParameters.getCmHandleQueryRestParameters().forEach(
                conditionApiProperty -> {
                    if (Strings.isNullOrEmpty(conditionApiProperty.getConditionName())) {
                        throw new DataValidationException(ERROR_MESSAGE,
                                "Missing 'conditionName' - please supply a valid name.");
                    }
                    if (!VALID_PROPERTY_NAMES.contains(conditionApiProperty.getConditionName())) {
                        throw new DataValidationException(ERROR_MESSAGE,
                                String.format("Wrong 'conditionName': %s - please supply a valid name.",
                                        conditionApiProperty.getConditionName()));
                    }
                    if (conditionApiProperty.getConditionParameters().isEmpty()) {
                        throw new DataValidationException(ERROR_MESSAGE,
                                "Empty 'conditionsParameters' - please supply a valid condition parameter.");
                    }
                    conditionApiProperty.getConditionParameters().forEach(
                            conditionParameter -> {
                                if (conditionParameter.isEmpty()) {
                                    throw new DataValidationException(ERROR_MESSAGE,
                                            "Empty 'conditionsParameter' - please supply a valid condition parameter.");
                                }
                                if (conditionParameter.size() > 1) {
                                    throw new DataValidationException(ERROR_MESSAGE,
                                            "Too many name in one 'conditionsParameter' -"
                                                    + " please supply one name in one condition parameter.");
                                }
                                conditionParameter.forEach((key, value) -> {
                                    if (Strings.isNullOrEmpty(key)) {
                                        throw new DataValidationException(ERROR_MESSAGE,
                                                "Missing 'conditionsParameterName' - please supply a valid name.");
                                    }
                                });
                            }
                    );
                }
        );
    }

    /**
     * Validate module name condition properties.
     * @param conditionProperty name of data to be validated
     */
    public static void validateModuleNameConditionProperties(final Map<String, String> conditionProperty) {
        if (conditionProperty.containsKey("moduleName")) {
            if (conditionProperty.get("moduleName").isEmpty()) {
                throw new DataValidationException(ERROR_MESSAGE,
                        "Missing module condition property value. - please supply a valid value.");
            }
        } else {
            throw new DataValidationException(ERROR_MESSAGE,
                    "Wrong module condition property name. - please supply a valid name.");
        }
    }
}
