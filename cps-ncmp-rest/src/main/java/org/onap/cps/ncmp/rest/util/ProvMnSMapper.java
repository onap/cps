/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.onap.cps.ncmp.rest.provmns.model.mapped.ConfigurationManagementOperation;
import org.onap.cps.ncmp.rest.provmns.model.mapped.ConfigurationManagementPatchInput;
import org.onap.cps.ncmp.rest.provmns.model.mapped.OperationEntry;
import org.onap.cps.ncmp.rest.provmns.model.request.Operation;
import org.onap.cps.ncmp.rest.provmns.model.request.Value;

public class ProvMnSMapper {

    /**
     * Converts a list of Operations to ConfigurationManagementPatchInput.
     *
     * @param operations list of 3gpp operations
     * @param stem first part of the FDN
     * @return ConfigurationManagementPatchInput object containing mapped parameters
     */
    public static ConfigurationManagementPatchInput mapOperations(final List<Operation> operations, final String stem) {

        final List<ConfigurationManagementOperation> res = operations.stream()
                .map(configurationOperation -> mapByOperation(configurationOperation, stem)).toList();

        return new ConfigurationManagementPatchInput(res);
    }

    private static ConfigurationManagementOperation mapByOperation(final Operation configurationOperation, 
                                                                   final String stem) {
        return switch (configurationOperation.getOp()) {
            case "add" -> create(configurationOperation, stem);
            case "replace", "merge" -> replace(configurationOperation, stem);
            case "remove" -> delete(configurationOperation, stem);
            default -> null;
        };
    }

    private static ConfigurationManagementOperation create(final Operation configurationOperation, final String stem) {
        final ConfigurationManagementOperation result = new ConfigurationManagementOperation();
        final Map<String, String> classData = getClassNameAndId(configurationOperation.getPath());
        final Map<String, List<OperationEntry>> changeRequest = new HashMap<>();
        final OperationEntry value = new OperationEntry();

        result.setOperation("create");
        result.setTargetIdentifier(stem + configurationOperation.getPath());

        final Value fullValue = configurationOperation.getValue();
        final Object flattenedMap = fullValue.getAttributes();

        value.setId(classData.get("id"));
        value.setAttributes(flattenedMap);

        changeRequest.put(classData.get("className"), List.of(value));

        result.setChangeRequests(changeRequest);
        return result;
    }

    private static ConfigurationManagementOperation replace(final Operation configurationOperation, final String stem) {
        final Map<String, String> classData;
        final Map<String, List<OperationEntry>> changeRequest = new HashMap<>();
        final OperationEntry value = new OperationEntry();
        final ConfigurationManagementOperation result = new ConfigurationManagementOperation();
        result.setOperation("update");
        if (configurationOperation.getPath().startsWith("#/")
                || configurationOperation.getPath().startsWith("/attribute")) {
            result.setTargetIdentifier("/");
            classData = getClassNameAndId(stem);
        } else {
            result.setTargetIdentifier(stem + configurationOperation.getPath());
            classData = getClassNameAndId(configurationOperation.getPath());
        }
        if (configurationOperation.getValue() instanceof Map<?, ?>) {
            final Value fullValue = configurationOperation.getValue();
            value.setId(classData.get("id"));
            value.setAttributes(fullValue);

        } else {
            value.setId(classData.get("id"));
            final String attributeName = getAttributeName(configurationOperation.getPath());
            value.setAttributes(Map.of(attributeName, configurationOperation.getValue()));
        }

        String className = classData.get("className");
        if (className == null || className.isEmpty()) {
            className = "attributes";
        }

        changeRequest.put(className, List.of(value));
        result.setChangeRequests(changeRequest);

        return result;
    }

    private static ConfigurationManagementOperation delete(final Operation configurationOperation, final String stem) {
        final ConfigurationManagementOperation result = new ConfigurationManagementOperation();
        result.setOperation("delete");
        result.setTargetIdentifier(stem + configurationOperation.getPath());
        return result;
    }

    private static Map<String, String> getClassNameAndId(final String path) {
        final Map<String, String> result = new HashMap<>();
        final String lastPart = path.substring(path.lastIndexOf('/') + 1);
        final String[] parts = lastPart.split("=");
        if (parts.length == 2) {
            result.put("className", parts[0]);
            result.put("id", parts[1]);
        }
        return result;
    }

    private static String getAttributeName(final String path) {
        final String[] parts = path.split("#/?", 2);
        final String afterHash = parts.length > 1 ? parts[1] : "";
        return afterHash.substring(afterHash.lastIndexOf('/') + 1);
    }
}
