/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.data.models.OperationType;
import org.onap.cps.ncmp.api.exceptions.NcmpException;
import org.onap.cps.ncmp.api.exceptions.PolicyExecutorException;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.provmns.RequestPathParameters;
import org.onap.cps.ncmp.impl.provmns.model.PatchItem;
import org.onap.cps.ncmp.impl.provmns.model.Resource;
import org.onap.cps.ncmp.impl.utils.http.RestServiceUrlTemplateBuilder;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyExecutor {

    public static final String ATTRIBUTES_WITH_HASHTAG = "#/attributes";

    @Value("${ncmp.policy-executor.enabled:false}")
    private boolean enabled;

    @Value("${ncmp.policy-executor.defaultDecision:deny}")
    private String defaultDecision;

    @SuppressWarnings("HttpUrlsUsage")
    @Value("${ncmp.policy-executor.server.address:http://policy-executor}")
    private String serverAddress;

    @Value("${ncmp.policy-executor.server.port:8080}")
    private String serverPort;

    @Value("${ncmp.policy-executor.httpclient.all-services.readTimeoutInSeconds:30}")
    private long readTimeoutInSeconds;

    private static final String CHANGE_REQUEST_FORMAT = "cm-legacy";
    private static final String PERMISSION_BASE_PATH = "operation-permission";
    private static final String REQUEST_PATH = "permissions";

    @Qualifier("policyExecutorWebClient")
    private final WebClient policyExecutorWebClient;

    private final ObjectMapper objectMapper;
    private final JsonObjectMapper jsonObjectMapper;

    private static final Throwable NO_ERROR = null;

    /**
     * Use the Policy Executor to check permission for a cm write operation.
     * Wil throw an exception when the operation is not permitted (work in progress)
     *
     * @param yangModelCmHandle   the cm handle involved
     * @param operationType       the write operation
     * @param authorization       the original rest authorization token (can be used to determine the client)
     * @param resourceIdentifier  the resource identifier (can be blank)
     * @param changeRequestAsJson the change details from the original rest request in json format
     */
    public void checkPermission(final YangModelCmHandle yangModelCmHandle,
                                final OperationType operationType,
                                final String authorization,
                                final String resourceIdentifier,
                                final String changeRequestAsJson) {
        log.trace("Policy Executor Enabled: {}", enabled);
        if (enabled) {
            try {
                final ResponseEntity<JsonNode> responseEntity = getPolicyExecutorResponse(yangModelCmHandle,
                                                                                          operationType,
                                                                                          authorization,
                                                                                          resourceIdentifier,
                                                                                          changeRequestAsJson);
                final JsonNode responseBody = responseEntity.getBody();
                if (responseBody == null) {
                    log.warn("No valid response body from Policy Executor, ignored");
                    return;
                }
                processSuccessResponse(responseBody);
            } catch (final RuntimeException runtimeException) {
                processException(runtimeException);
            }
        }
    }

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
                case ADD -> operations.add(
                    buildCreateOperationDetails(OperationType.CREATE, requestPathParameters,
                    patchItem.getValue()));
                case REPLACE -> operations.add(
                    buildCreateOperationDetailsForUpdate(OperationType.UPDATE, requestPathParameters, patchItem));
                case REMOVE -> operations.add(
                    buildDeleteOperationDetails(requestPathParameters.toAlternateId()));
                default -> log.warn("Unsupported Patch Operation Type:{}", patchItem.getOp().getValue());
            };
        }
        return new PatchOperationsDetails("Some Permission Id", CHANGE_REQUEST_FORMAT, operations);
    }

    /**
     * Build a CreateOperationDetails object from ProvMnS request details.
     *
     * @param operationType            Type of operation create, update.
     * @param requestPathParameters    request parameters including uri-ldn-first-part, className and id
     * @param resource                 provided request resource
     * @return CreateOperationDetails object
     */
    public CreateOperationDetails buildCreateOperationDetails(final OperationType operationType,
                                                              final RequestPathParameters requestPathParameters,
                                                              final Object resource) {
        final Map<String, List<OperationEntry>> changeRequest = new HashMap<>();
        final OperationEntry operationEntry = new OperationEntry();

        final String resourceAsJson = jsonObjectMapper.asJsonString(resource);
        String className = requestPathParameters.getClassName();
        try {
            final TypeReference<HashMap<String, Object>> typeReference =
                new TypeReference<HashMap<String, Object>>() {};
            final Map<String, Object> fullValue = objectMapper.readValue(resourceAsJson, typeReference);

            operationEntry.setId(requestPathParameters.getId());
            operationEntry.setAttributes(fullValue.get("attributes"));
            className = isNullEmptyOrBlank(fullValue)
                ? requestPathParameters.getClassName() : fullValue.get("objectClass").toString();
        } catch (final JsonProcessingException exception) {
            log.debug("JSON processing error: {}", exception);
        }
        changeRequest.put(className, List.of(operationEntry));
        return new CreateOperationDetails(operationType.name(),
            requestPathParameters.getUriLdnFirstPart(), changeRequest);
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
        if (patchItem.getPath().contains(ATTRIBUTES_WITH_HASHTAG)) {
            return buildCreateOperationDetailsForUpdateWithHash(operationType, requestPathParameters, patchItem);
        } else {
            return buildCreateOperationDetails(operationType, requestPathParameters, (Resource) patchItem.getValue());
        }
    }

    private CreateOperationDetails buildCreateOperationDetailsForUpdateWithHash(final OperationType operationType,
                                                                      final RequestPathParameters requestPathParameters,
                                                                      final PatchItem patchItem) {
        final Map<String, List<OperationEntry>> changeRequest = new HashMap<>();
        final OperationEntry operationEntry = new OperationEntry();
        final String className = requestPathParameters.getClassName();

        final Map<String, Object> attributeHiearchyAsMap = getAttributeHierarchyMap(patchItem);

        operationEntry.setId(requestPathParameters.getId());
        operationEntry.setAttributes(attributeHiearchyAsMap);
        changeRequest.put(className, List.of(operationEntry));

        return new CreateOperationDetails(operationType.getOperationName(),
                                          requestPathParameters.getUriLdnFirstPart(),
                                          changeRequest);
    }

    private Map<String, Object> getAttributeHierarchyMap(final PatchItem patchItem) {
        final String[] parts = patchItem.getPath().split(ATTRIBUTES_WITH_HASHTAG);

        final String attributeHierarchy = parts[1];
        final String[] attributeHierarchyAsArray = Arrays.stream(attributeHierarchy.split("/"))
                .filter(attributeName -> !attributeName.isEmpty())
                .toArray(String[]::new);

        return buildAttributeHiearchyAsMap(attributeHierarchyAsArray, 0, patchItem.getValue());
    }

    private Map<String, Object> buildAttributeHiearchyAsMap(final String[] parts,
                                                            final int index,
                                                            final Object value) {
        if (index == parts.length - 1) {
            return Map.of(parts[index], value);
        }

        return Map.of(parts[index], buildAttributeHiearchyAsMap(parts, index + 1, value));
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

    private Map<String, Object> getSingleOperationAsMap(final YangModelCmHandle yangModelCmHandle,
                                                        final OperationType operationType,
                                                        final String resourceIdentifier,
                                                        final String changeRequestAsJson) {
        final Map<String, Object> operationAsMap = new HashMap<>(5);
        operationAsMap.put("operation", operationType.getOperationName());
        operationAsMap.put("entityHandleId", yangModelCmHandle.getId());
        operationAsMap.put("resourceIdentifier", resourceIdentifier);
        operationAsMap.put("targetIdentifier", yangModelCmHandle.getAlternateId());
        if (!OperationType.DELETE.equals(operationType)) {
            try {
                final Object changeRequestAsObject = objectMapper.readValue(changeRequestAsJson, Object.class);
                operationAsMap.put("changeRequest", changeRequestAsObject);
            } catch (final JsonProcessingException e) {
                throw new NcmpException("Cannot convert Change Request data to Object",
                    "Invalid Json: " + changeRequestAsJson);
            }
        }
        return operationAsMap;
    }

    private Object createBodyAsObject(final Map<String, Object> operationAsMap) {
        final Collection<Map<String, Object>> operations = Collections.singletonList(operationAsMap);
        final Map<String, Object> permissionRequestAsMap = new HashMap<>(2);
        permissionRequestAsMap.put("changeRequestFormat", CHANGE_REQUEST_FORMAT);
        permissionRequestAsMap.put("operations", operations);
        return permissionRequestAsMap;
    }

    private ResponseEntity<JsonNode> getPolicyExecutorResponse(final YangModelCmHandle yangModelCmHandle,
                                                               final OperationType operationType,
                                                               final String authorization,
                                                               final String resourceIdentifier,
                                                               final String changeRequestAsJson) {
        final Map<String, Object> operationAsMap = getSingleOperationAsMap(yangModelCmHandle,
            operationType,
            resourceIdentifier,
            changeRequestAsJson);

        final Object bodyAsObject = createBodyAsObject(operationAsMap);

        log.debug("Sending permission check to Policy Executor for CMHandle: {} with operation: {}",
                yangModelCmHandle.getId(), operationType);
        log.trace("Policy Executor request body: {}", bodyAsObject);

        final UrlTemplateParameters urlTemplateParameters = RestServiceUrlTemplateBuilder.newInstance()
                .fixedPathSegment(REQUEST_PATH)
                .createUrlTemplateParameters(String.format("%s:%s", serverAddress, serverPort), PERMISSION_BASE_PATH);

        return policyExecutorWebClient.post()
            .uri(urlTemplateParameters.urlTemplate(), urlTemplateParameters.urlVariables())
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .body(BodyInserters.fromValue(bodyAsObject))
            .retrieve()
            .toEntity(JsonNode.class)
            .timeout(Duration.of(readTimeoutInSeconds, ChronoUnit.SECONDS))
            .block();
    }

    private static void processSuccessResponse(final JsonNode responseBody) {
        final String id = responseBody.path("id").asText("unknown id");
        final String permissionResult = responseBody.path("permissionResult").asText("unknown");
        final String messageFromPolicyExecutor = responseBody.path("message").asText();
        processDecision(id, permissionResult, messageFromPolicyExecutor, NO_ERROR);
    }

    private static void processDecision(final String id,
                                        final String permissionResult,
                                        final String details,
                                        final Throwable optionalCauseOfError) {
        log.trace("Policy Executor Decision id: {} ", id);
        if ("allow".equals(permissionResult)) {
            log.trace("Operation allowed.");
        } else {
            log.warn("Policy Executor permission result: {}", permissionResult);
            log.warn("Policy Executor message: {}", details);
            final String message = "Operation not allowed. Decision id " + id + " : " + permissionResult;
            throw new PolicyExecutorException(message, details, optionalCauseOfError);
        }
    }

    private String createErrorDetailsMessage(final Throwable throwable) {
        if (throwable instanceof WebClientResponseException webClientResponseException) {
            return "Policy Executor returned HTTP Status code "
                    + webClientResponseException.getStatusCode().value() + ".";
        }
        if (throwable instanceof WebClientRequestException) {
            return "Network or I/O error while attempting to contact Policy Executor.";
        }
        if (throwable instanceof TimeoutException || throwable.getCause() instanceof TimeoutException) {
            return "Policy Executor request timed out.";
        }
        if (throwable.getCause() instanceof UnknownHostException) {
            return String.format("Cannot connect to Policy Executor (%s:%s).", serverAddress, serverPort);
        }
        return "Unexpected error during Policy Executor call.";
    }

    private void processException(final RuntimeException runtimeException) {
        final String errorDetailsMessage = createErrorDetailsMessage(runtimeException);

        log.warn("Exception during Policy Execution check. Class: {}, Message: {}, Details: {}",
                runtimeException.getClass().getSimpleName(), runtimeException.getMessage(), errorDetailsMessage);

        if (runtimeException instanceof WebClientResponseException
                || runtimeException instanceof WebClientRequestException) {
            processFallbackResponse(errorDetailsMessage, runtimeException);
            return;
        }
        final Throwable nestedThrowable = runtimeException.getCause();
        if (nestedThrowable instanceof TimeoutException || nestedThrowable instanceof UnknownHostException) {
            final String nestedErrorDetailsMessage = createErrorDetailsMessage(nestedThrowable);
            processFallbackResponse(nestedErrorDetailsMessage, nestedThrowable);
            return;
        }
        throw runtimeException;
    }

    private void processFallbackResponse(final String message, final Throwable cause) {
        final String decisionId = "N/A";
        final String decision = defaultDecision;
        final String warning = message + " Falling back to configured default decision: " + defaultDecision;
        log.warn(warning);
        processDecision(decisionId, decision, warning, cause);
    }

    private boolean isNullEmptyOrBlank(final Map<String, Object> jsonObject) {
        try {
            return jsonObject.get("objectClass").toString().isBlank();
        } catch (final NullPointerException exception) {
            return true;
        }
    }
}
