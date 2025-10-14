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
     * @param stem       first part of the FDN
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
            case "replace" -> replace(configurationOperation, stem);
            case "merge" -> merge(configurationOperation, stem);
            default -> delete(configurationOperation, stem);
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

        result.setChangeRequest(changeRequest);
        return result;
    }

    private static ConfigurationManagementOperation replace(final Operation configurationOperation, final String stem) {
        final ConfigurationManagementOperation result = initBaseOperation();
        final Map<String, String> classData = resolveClassData(configurationOperation, stem, result);
        final OperationEntry value = createBaseEntry(classData);

        final Value fullValue = configurationOperation.getValue();
        value.setAttributes(fullValue.getAttributes());

        finalizeChangeRequest(result, classData, value);
        return result;
    }

    private static ConfigurationManagementOperation merge(final Operation configurationOperation, final String stem) {
        final ConfigurationManagementOperation result = initBaseOperation();
        final Map<String, String> classData = resolveClassData(configurationOperation, stem, result);
        final OperationEntry value = createBaseEntry(classData);

        final String attributeName = getAttributeName(configurationOperation.getPath());
        value.setAttributes(Map.of(attributeName, configurationOperation.getValue()));

        finalizeChangeRequest(result, classData, value);
        return result;
    }

    private static ConfigurationManagementOperation delete(final Operation configurationOperation, final String stem) {
        final ConfigurationManagementOperation result = new ConfigurationManagementOperation();
        result.setOperation("delete");
        result.setTargetIdentifier(stem + configurationOperation.getPath());
        return result;
    }

    private static ConfigurationManagementOperation initBaseOperation() {
        final ConfigurationManagementOperation result = new ConfigurationManagementOperation();
        result.setOperation("update");
        return result;
    }

    private static Map<String, String> resolveClassData(
            final Operation configurationOperation,
            final String stem,
            final ConfigurationManagementOperation result) {

        final Map<String, String> classData;
        if (configurationOperation.getPath().startsWith("#/")
                || configurationOperation.getPath().startsWith("/attribute")) {
            result.setTargetIdentifier("/");
            classData = getClassNameAndId(stem);
        } else {
            result.setTargetIdentifier(stem + configurationOperation.getPath());
            classData = getClassNameAndId(configurationOperation.getPath());
        }
        return classData;
    }

    private static OperationEntry createBaseEntry(final Map<String, String> classData) {
        final OperationEntry value = new OperationEntry();
        value.setId(classData.get("id"));
        return value;
    }

    private static void finalizeChangeRequest(
            final ConfigurationManagementOperation result,
            final Map<String, String> classData,
            final OperationEntry value) {

        final String className = (classData.get("className") == null
                || classData.get("className").isEmpty()) ? "attributes" : classData.get("className");

        final Map<String, List<OperationEntry>> changeRequest = new HashMap<>();
        changeRequest.put(className, List.of(value));
        result.setChangeRequest(changeRequest);
    }

    private static Map<String, String> getClassNameAndId(final String path) {
        final Map<String, String> result = new HashMap<>();
        final String lastPart = path.substring(path.lastIndexOf('/') + 1);
        final String[] parts = lastPart.split("=");
        result.put("className", parts[0]);
        result.put("id", parts[1]);
        return result;
    }

    private static String getAttributeName(final String path) {
        final String[] parts = path.split("#/?", 2);
        final String afterHash = parts.length > 1 ? parts[1] : "";
        return afterHash.substring(afterHash.lastIndexOf('/') + 1);
    }
}
