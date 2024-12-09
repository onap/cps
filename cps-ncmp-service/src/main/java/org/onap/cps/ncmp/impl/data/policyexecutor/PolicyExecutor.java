/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.data.models.OperationType;
import org.onap.cps.ncmp.api.exceptions.NcmpException;
import org.onap.cps.ncmp.api.exceptions.PolicyExecutorException;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.ncmp.impl.utils.http.RestServiceUrlTemplateBuilder;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyExecutor {

    @Value("${ncmp.policy-executor.enabled:false}")
    private boolean enabled;

    @Value("${ncmp.policy-executor.defaultDecision:deny}")
    private String defaultDecision;

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

    private void processException(final RuntimeException runtimeException) {
        if (runtimeException instanceof WebClientResponseException) {
            final WebClientResponseException webClientResponseException = (WebClientResponseException) runtimeException;
            log.warn("HTTP Error Message: {}", webClientResponseException.getMessage());
            final int httpStatusCode = webClientResponseException.getStatusCode().value();
            processFallbackResponse("Policy Executor returned HTTP Status code " + httpStatusCode + ".",
                webClientResponseException);
        } else {
            final Throwable cause = runtimeException.getCause();
            if (cause instanceof TimeoutException) {
                processFallbackResponse("Policy Executor request timed out.", cause);
            } else if (cause instanceof UnknownHostException) {
                final String message
                    = String.format("Cannot connect to Policy Executor (%s:%s).", serverAddress, serverPort);
                processFallbackResponse(message, cause);
            } else {
                log.warn("Request to Policy Executor failed with unexpected exception", runtimeException);
                throw runtimeException;
            }
        }
    }

    private void processFallbackResponse(final String message, final Throwable cause) {
        final String decisionId = "N/A";
        final String decision = defaultDecision;
        final String warning = message + " Falling back to configured default decision: " + defaultDecision;
        log.warn(warning);
        processDecision(decisionId, decision, warning, cause);
    }

}
