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

package org.onap.cps.ncmp.impl.data.policyexecutor;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.data.models.OperationType;
import org.onap.cps.ncmp.api.exceptions.ProvMnSException;
import org.onap.cps.ncmp.impl.provmns.RequestPathParameters;
import org.onap.cps.ncmp.impl.provmns.model.PatchItem;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationDetailsFactory {

    private static final String ATTRIBUTE_NAME_SEPARATOR = "/";
    private static final String REGEX_FOR_LEADING_AND_TRAILING_SEPARATORS = "(^/)|(/$)";

    private final JsonObjectMapper jsonObjectMapper;
    private final ObjectMapper objectMapper;

    /**
     * Build a PatchOperationDetails object from ProvMnS request details.
     *
     * @param requestPathParameters    request parameters including uri-ldn-first-part, className and id
     * @param patchItems               provided request list of patch Items
     * @return CreateOperationDetails object
     */
    public PatchOperationsDetails buildPatchOperationDetails(final RequestPathParameters requestPathParameters,
                                                             final List<PatchItem> patchItems) {
        final List<Object> operations = new ArrayList<>(patchItems.size());
        for (final PatchItem patchItem : patchItems) {
            switch (patchItem.getOp()) {
                case ADD -> operations.add(buildCreateOperationDetails(OperationType.CREATE, requestPathParameters,
                                        patchItem.getValue()));
                case REPLACE -> operations.add(buildCreateOperationDetailsForUpdate(OperationType.UPDATE,
                                        requestPathParameters,
                                        patchItem));
                case REMOVE -> operations.add(buildDeleteOperationDetails(requestPathParameters.toAlternateId()));
                default -> log.warn("Unsupported Patch Operation Type:{}", patchItem.getOp().getValue());
            }
        }
        return new PatchOperationsDetails("Some Permission Id", "cm-legacy", operations);
    }

    /**
     * Build a CreateOperationDetails object from ProvMnS request details.
     *
     * @param operationType            Type of operation create, update.
     * @param requestPathParameters    request parameters including uri-ldn-first-part, className and id
     * @param resourceAsObject         provided request payload
     * @return CreateOperationDetails object
     */
    public CreateOperationDetails buildCreateOperationDetails(final OperationType operationType,
                                                              final RequestPathParameters requestPathParameters,
                                                              final Object resourceAsObject) {

        final ResourceObjectDetails resourceObjectDetails = createResourceObjectDetails(resourceAsObject,
                                                                                        requestPathParameters);

        final OperationEntry operationEntry = new OperationEntry(resourceObjectDetails.id(),
                resourceObjectDetails.attributes());

        final Map<String, List<OperationEntry>> operationEntriesPerObjectClass =
                Map.of(resourceObjectDetails.objectClass(), List.of(operationEntry));

        return new CreateOperationDetails(
                operationType.name(),
                requestPathParameters.getUriLdnFirstPart(),
                operationEntriesPerObjectClass
        );
    }

    /**
     * Build a CreateOperationDetails object from ProvMnS request details.
     *
     * @param operationType            Type of operation create, update.
     * @param requestPathParameters    request parameters including uri-ldn-first-part, className and id
     * @param patchItem                 provided request
     * @return CreateOperationDetails object
     */
    public CreateOperationDetails buildCreateOperationDetailsForUpdate(final OperationType operationType,
                                                                     final RequestPathParameters requestPathParameters,
                                                                     final PatchItem patchItem) {
        if (patchItem.getPath().contains("#/attributes")) {
            return buildCreateOperationDetailsForUpdateWithHash(operationType, requestPathParameters, patchItem);
        } else {
            return buildCreateOperationDetails(operationType, requestPathParameters, patchItem.getValue());
        }
    }

    /**
     * Builds a DeleteOperationDetails object from provided alternate id.
     *
     * @param alternateId        alternate id for request
     * @return DeleteOperationDetails object
     */
    public DeleteOperationDetails buildDeleteOperationDetails(final String alternateId) {
        return new DeleteOperationDetails(OperationType.DELETE.name(), alternateId);
    }

    private ResourceObjectDetails createResourceObjectDetails(final Object resourceAsObject,
                                                              final RequestPathParameters requestPathParameters) {
        try {
            final String resourceAsJson = jsonObjectMapper.asJsonString(resourceAsObject);
            final TypeReference<Map<String, Object>> typeReference = new TypeReference<>() {};
            final Map<String, Object> resourceAsMap = objectMapper.readValue(resourceAsJson, typeReference);

            return new ResourceObjectDetails(requestPathParameters.getId(),
                                             extractObjectClass(resourceAsMap, requestPathParameters),
                                             resourceAsMap.get("attributes"));
        } catch (final JsonProcessingException e) {
            log.debug("JSON processing error: {}", e.getMessage());
            throw new ProvMnSException("Cannot convert Resource Object", e.getMessage());
        }
    }

    private static String extractObjectClass(final Map<String, Object> resourceAsMap,
                                             final RequestPathParameters requestPathParameters) {
        final String objectClass = (String) resourceAsMap.get("objectClass");
        if (Strings.isNullOrEmpty(objectClass)) {
            return requestPathParameters.getClassName();
        }
        return objectClass;
    }


    private CreateOperationDetails buildCreateOperationDetailsForUpdateWithHash(final OperationType operationType,
                                                                     final RequestPathParameters requestPathParameters,
                                                                     final PatchItem patchItem) {
        final Map<String, List<OperationEntry>> operationEntriesPerObjectClass = new HashMap<>();
        final String className = requestPathParameters.getClassName();

        final Map<String, Object> attributeHierarchyAsMap = createNestedMap(patchItem);

        final OperationEntry operationEntry = new OperationEntry(requestPathParameters.getId(),
                                                                 attributeHierarchyAsMap);
        operationEntriesPerObjectClass.put(className, List.of(operationEntry));

        return new CreateOperationDetails(operationType.getOperationName(),
                requestPathParameters.getUriLdnFirstPart(),
                operationEntriesPerObjectClass);
    }

    private Map<String, Object> createNestedMap(final PatchItem patchItem) {
        final Map<String, Object> attributeHierarchyMap = new HashMap<>();
        Map<String, Object> currentLevel = attributeHierarchyMap;

        final String[] attributeHierarchyNames = patchItem.getPath().split("#/attributes")[1]
                .replaceAll(REGEX_FOR_LEADING_AND_TRAILING_SEPARATORS, "")
                .split(ATTRIBUTE_NAME_SEPARATOR);

        for (int level = 0; level < attributeHierarchyNames.length; level++) {
            final String attributeName = attributeHierarchyNames[level];

            if (isLastLevel(attributeHierarchyNames, level)) {
                currentLevel.put(attributeName, patchItem.getValue());
            } else {
                final Map<String, Object> nextLevel = new HashMap<>();
                currentLevel.put(attributeName, nextLevel);
                currentLevel = nextLevel;
            }
        }
        return attributeHierarchyMap;
    }

    private boolean isLastLevel(final String[] attributeNamesArray, final int level) {
        return level == attributeNamesArray.length - 1;
    }
}

