/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025-2026 OpenInfra Foundation Europe
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

import static java.util.Collections.emptyMap;
import static org.onap.cps.ncmp.api.data.models.OperationType.CREATE;
import static org.onap.cps.ncmp.api.data.models.OperationType.UPDATE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.data.models.OperationType;
import org.onap.cps.ncmp.api.exceptions.ProvMnSException;
import org.onap.cps.ncmp.impl.provmns.ParameterHelper;
import org.onap.cps.ncmp.impl.provmns.RequestParameters;
import org.onap.cps.ncmp.impl.provmns.model.PatchItem;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class OperationDetailsFactory {

    public static final OperationDetails DELETE_OPERATION_DETAILS  = new OperationDetails("delete", "", emptyMap());

    private static final String ATTRIBUTE_NAME_SEPARATOR = "/";
    private static final String REGEX_FOR_LEADING_AND_TRAILING_SEPARATORS = "(^/)|(/$)";

    private final JsonObjectMapper jsonObjectMapper;

    /**
     * Create OperationDetails object from ProvMnS request details.
     *
     * @param requestParameters request parameters including uri-ldn-first-part, className and id
     * @param patchItem provided request payload
     * @return OperationDetails object
     */
    public OperationDetails buildOperationDetails(final RequestParameters requestParameters,
                                                  final PatchItem patchItem) {
        final OperationDetails operationDetails;
        switch (patchItem.getOp()) {
            case ADD:
                operationDetails = buildOperationDetailsForPatchItem(CREATE, requestParameters, patchItem);
                break;
            case REPLACE:
                if (patchItem.getPath().contains("#/attributes")) {
                    operationDetails = buildOperationDetailsForPatchItemWithHash(requestParameters, patchItem);
                } else {
                    operationDetails = buildOperationDetailsForPatchItem(UPDATE, requestParameters, patchItem);
                }
                break;
            case REMOVE:
                operationDetails = DELETE_OPERATION_DETAILS;
                break;
            default:
                throw new ProvMnSException("PATCH", HttpStatus.UNPROCESSABLE_ENTITY,
                    "Unsupported Patch Operation Type: " + patchItem.getOp().getValue(), patchItem.getOp().getValue());
        }
        return operationDetails;
    }

    /**
     * Build a OperationDetails object from ProvMnS request details.
     *
     * @param operationType        Type of operation create, update.
     * @param requestParameters    request parameters including uri-ldn-first-part, className and id
     * @param resourceAsObject     provided request payload
     * @return OperationDetails object
     */
    public OperationDetails buildOperationDetails(final OperationType operationType,
                                                  final RequestParameters requestParameters,
                                                  final Object resourceAsObject) {
        return toOperationDetails(operationType, requestParameters, resourceAsObject);
    }

    /**
     * Build OperationDetails for a specific patch item.
     *
     * @param operationType the type of operation (CREATE, UPDATE)
     * @param requestParameters request parameters including uri-ldn-first-part, className and id
     * @param patchItem the patch item containing operation details
     * @return OperationDetails object for the patch item
     */
    public OperationDetails buildOperationDetailsForPatchItem(final OperationType operationType,
                                                              final RequestParameters requestParameters,
                                                              final PatchItem patchItem) {
        final Map<String, Object> resourceAsObject = new HashMap<>(2);
        resourceAsObject.put("id", requestParameters.id());
        resourceAsObject.put("attributes", patchItem.getValue());
        return toOperationDetails(operationType, requestParameters, resourceAsObject);
    }

    private OperationDetails buildOperationDetailsForPatchItemWithHash(final RequestParameters requestParameters,
                                                                       final PatchItem patchItem) {
        final Map<String, Object> attributeHierarchyAsMap = createNestedMap(patchItem);
        final String id = ParameterHelper.removeTrailingHash(requestParameters.id());
        final OperationEntry operationEntry = new OperationEntry(id, attributeHierarchyAsMap);
        final String targetIdentifier = ParameterHelper.extractParentFdn(requestParameters.fdn());
        final Map<String, List<OperationEntry>> operationEntriesPerObjectClass = new HashMap<>();
        operationEntriesPerObjectClass.put(requestParameters.className(), List.of(operationEntry));
        return new OperationDetails(UPDATE.getOperationName(), targetIdentifier, operationEntriesPerObjectClass);
    }

    private OperationDetails toOperationDetails(final OperationType operationType,
                                                final RequestParameters requestParameters,
                                                final Object resourceAsObject) {
        final ResourceObjectDetails resourceObjectDetails = createResourceObjectDetails(resourceAsObject,
            requestParameters);
        final OperationEntry operationEntry = new OperationEntry(resourceObjectDetails.id(),
            resourceObjectDetails.attributes());
        final Map<String, List<OperationEntry>> changeRequestAsMap =
            Map.of(resourceObjectDetails.objectClass(), List.of(operationEntry));
        final String targetIdentifier = ParameterHelper.extractParentFdn(requestParameters.fdn());
        return new OperationDetails(operationType.getOperationName(), targetIdentifier, changeRequestAsMap);
    }

    @SuppressWarnings("unchecked")
    private ResourceObjectDetails createResourceObjectDetails(final Object resourceAsObject,
                                                              final RequestParameters requestParameters) {
        final String resourceAsJson = jsonObjectMapper.asJsonString(resourceAsObject);
        final Map<String, Object> resourceAsMap = jsonObjectMapper.convertJsonString(resourceAsJson, Map.class);
        return new ResourceObjectDetails(requestParameters.id(),
                                         requestParameters.className(),
                                         resourceAsMap.get("attributes"));
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

