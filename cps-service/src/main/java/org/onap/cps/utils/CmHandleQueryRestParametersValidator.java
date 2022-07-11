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
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.spi.exceptions.DataValidationException;
import org.onap.cps.spi.model.CmHandleQueryServiceParameters;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CmHandleQueryRestParametersValidator {

    /**
     * Validate cm handle query parameters.
     * @param cmHandleQueryServiceParameters name of data to be validated
     */
    public static void validateCmHandleQueryParameters(
            final CmHandleQueryServiceParameters cmHandleQueryServiceParameters) {
        cmHandleQueryServiceParameters.getCmHandleQueryParameters().forEach(
                conditionApiProperty -> {
                    if (Strings.isNullOrEmpty(conditionApiProperty.getConditionName())) {
                        throwDataValidationException("Missing 'conditionName' - please supply a valid name.");
                    }
                    if (Arrays.stream(ValidQueryProperties.values()).noneMatch(validQueryProperty ->
                        validQueryProperty.getQueryProperty().equals(conditionApiProperty.getConditionName()))) {
                        throwDataValidationException(
                                String.format("Wrong 'conditionName': %s - please supply a valid name.",
                                conditionApiProperty.getConditionName()));
                    }
                    if (conditionApiProperty.getConditionParameters().isEmpty()) {
                        throwDataValidationException(
                                "Empty 'conditionsParameters' - please supply a valid condition parameter.");
                    }
                    conditionApiProperty.getConditionParameters().forEach(
                            CmHandleQueryRestParametersValidator::validateConditionParameter
                    );
                }
        );
    }

    private static void validateConditionParameter(final Map<String, String> conditionParameter) {
        if (conditionParameter.isEmpty()) {
            throwDataValidationException(
                    "Empty 'conditionsParameter' - please supply a valid condition parameter.");
        }
        if (conditionParameter.size() > 1) {
            throwDataValidationException("Too many name in one 'conditionsParameter' -"
                    + " please supply one name in one condition parameter.");
        }
        conditionParameter.forEach((key, value) -> {
            if (Strings.isNullOrEmpty(key)) {
                throwDataValidationException(
                        "Missing 'conditionsParameterName' - please supply a valid name.");
            }
        });
    }

    /**
     * Validate module name condition properties.
     * @param conditionProperty name of data to be validated
     */
    public static void validateModuleNameConditionProperties(final Map<String, String> conditionProperty) {
        if (conditionProperty.containsKey("moduleName") && !conditionProperty.get("moduleName").isEmpty()) {
            return;
        }
        throwDataValidationException("Wrong module condition property. - please supply a valid condition property.");
    }

    /**
     * Validate CPS path condition properties.
     * @param conditionProperty name of data to be validated
     */
    public static boolean validateCpsPathConditionProperties(final Map<String, String> conditionProperty) {
        if (conditionProperty.isEmpty()) {
            return true;
        }
        if (conditionProperty.size() > 1) {
            throwDataValidationException("Only one condition property is allowed for the CPS path query.");
        }
        if (!conditionProperty.containsKey("cpsPath")) {
            throwDataValidationException(
                "Wrong CPS path condition property. - expecting \"cpsPath\" as the condition property.");
        }
        final String cpsPath = conditionProperty.get("cpsPath");
        if (cpsPath.isBlank()) {
            throwDataValidationException(
                "Wrong CPS path. - please supply a valid CPS path.");
        }
        if (cpsPath.contains("/additional-properties")) {
            log.debug("{} - Private metadata cannot be queried. Nothing to be returned",
                cpsPath);
            return false;
        }
        return true;
    }

    private static void throwDataValidationException(final String details) {
        throw new DataValidationException("Invalid Query Parameter.", details);
    }

}
