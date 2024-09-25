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

    @Qualifier("policyExecutorWebClient")
    private final WebClient policyExecutorWebClient;

    private final ObjectMapper objectMapper;

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
                if (responseEntity.getBody() == null) {
                    log.warn("No valid response body from Policy Executor, ignored");
                    return;
                }
                processSuccessResponse(responseEntity.getBody());
            } catch (final RuntimeException runtimeException) {
                processException(runtimeException);
            }
        }
    }

    private Map<String, Object> getSingleRequestAsMap(final YangModelCmHandle yangModelCmHandle,
                                                      final OperationType operationType,
                                                      final String resourceIdentifier,
                                                      final String changeRequestAsJson) {
        final Map<String, Object> data = new HashMap<>(4);
        data.put("cmHandleId", yangModelCmHandle.getId());
        data.put("resourceIdentifier", resourceIdentifier);
        data.put("targetIdentifier", yangModelCmHandle.getAlternateId());
        if (!OperationType.DELETE.equals(operationType)) {
            try {
                final Object changeRequestAsObject = objectMapper.readValue(changeRequestAsJson, Object.class);
                data.put("cmChangeRequest", changeRequestAsObject);
            } catch (final JsonProcessingException e) {
                throw new NcmpException("Cannot convert Change Request data to Object",
                    "Invalid Json: " + changeRequestAsJson);
            }
        }
        final Map<String, Object> request = new HashMap<>(2);
        request.put("schema", getAssociatedPolicyDataSchemaName(operationType));
        request.put("data", data);
        return request;
    }

    private static String getAssociatedPolicyDataSchemaName(final OperationType operationType) {
        return "urn:cps:org.onap.cps.ncmp.policy-executor.ncmp-" + operationType.getOperationName() + "-schema:1.0.0";
    }

    private Object createBodyAsObject(final List<Object> requests) {
        final Map<String, Object> bodyAsMap = new HashMap<>(2);
        bodyAsMap.put("decisionType", "allow");
        bodyAsMap.put("requests", requests);
        return bodyAsMap;
    }

    private ResponseEntity<JsonNode> getPolicyExecutorResponse(final YangModelCmHandle yangModelCmHandle,
                                                               final OperationType operationType,
                                                               final String authorization,
                                                               final String resourceIdentifier,
                                                               final String changeRequestAsJson) {
        final Map<String, Object> requestAsMap = getSingleRequestAsMap(yangModelCmHandle,
            operationType,
            resourceIdentifier,
            changeRequestAsJson);

        final Object bodyAsObject = createBodyAsObject(Collections.singletonList(requestAsMap));

        final UrlTemplateParameters urlTemplateParameters = RestServiceUrlTemplateBuilder.newInstance()
                .fixedPathSegment("execute")
                .createUrlTemplateParameters(String.format("%s:%s", serverAddress, serverPort),
                        "policy-executor/api");

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
        final String decisionId = responseBody.path("decisionId").asText("unknown id");
        final String decision = responseBody.path("decision").asText("unknown");
        final String messageFromPolicyExecutor = responseBody.path("message").asText();
        processDecision(decisionId, decision, messageFromPolicyExecutor);
    }

    private static void processDecision(final String decisionId, final String decision, final String details) {
        log.trace("Policy Executor decision id: {} ", decisionId);
        if ("allow".equals(decision)) {
            log.trace("Operation allowed.");
        } else {
            log.warn("Policy Executor decision: {}", decision);
            log.warn("Policy Executor message: {}", details);
            final String message = "Operation not allowed. Decision id " + decisionId + " : " + decision;
            throw new PolicyExecutorException(message, details);
        }
    }

    private void processException(final RuntimeException runtimeException) {
        if (runtimeException instanceof WebClientResponseException) {
            final WebClientResponseException webClientResponseException = (WebClientResponseException) runtimeException;
            final int httpStatusCode = webClientResponseException.getStatusCode().value();
            processFallbackResponse("Policy Executor returned HTTP Status code " + httpStatusCode + ".");
        } else {
            final Throwable cause = runtimeException.getCause();
            if (cause instanceof TimeoutException) {
                processFallbackResponse("Policy Executor request timed out.");
            } else {
                if (cause instanceof UnknownHostException) {
                    final String message =
                        String.format("Cannot connect to Policy Executor (%s:%s).", serverAddress, serverPort);
                    processFallbackResponse(message);
                } else {
                    log.warn("Request to Policy Executor failed with unexpected exception", runtimeException);
                    throw runtimeException;
                }
            }
        }
    }

    private void processFallbackResponse(final String message) {
        final String decisionId = "N/A";
        final String decision = defaultDecision;
        final String warning = message + " Falling back to configured default decision: " + defaultDecision;
        log.warn(warning);
        processDecision(decisionId, decision, warning);
    }

}
