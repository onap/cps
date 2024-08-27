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

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.data.models.OperationType;
import org.onap.cps.ncmp.api.exceptions.PolicyExecutorException;
import org.onap.cps.ncmp.api.exceptions.ServerNcmpException;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyExecutor {

    @Value("${ncmp.policy-executor.enabled:false}")
    private boolean enabled;

    @Value("${ncmp.policy-executor.server.address:http://policy-executor}")
    private String serverAddress;

    @Value("${ncmp.policy-executor.server.port:8080}")
    private String serverPort;

    @Qualifier("policyExecutorWebClient")
    private final WebClient policyExecutorWebClient;

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
            final ResponseEntity<JsonNode> responseEntity =
                getPolicyExecutorResponse(yangModelCmHandle, operationType, authorization, resourceIdentifier,
                    changeRequestAsJson);

            if (responseEntity == null) {
                log.warn("No valid response from policy, ignored");
                return;
            }

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                if (responseEntity.getBody() == null) {
                    log.warn("No valid response body from policy, ignored");
                    return;
                }
                processResponse(responseEntity.getBody());
            } else {
                log.warn("Policy Executor invocation failed with status {}",
                    responseEntity.getStatusCode().value());
                throw new ServerNcmpException("Policy Executor invocation failed", "HTTP status code: "
                    + responseEntity.getStatusCode().value());
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
            data.put("cmChangeRequest", changeRequestAsJson);
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
        final String serviceBaseUrl = serverAddress + ":" + serverPort;

        final Map<String, Object> requestAsMap = getSingleRequestAsMap(yangModelCmHandle,
            operationType,
            resourceIdentifier,
            changeRequestAsJson);

        final Object bodyAsObject = createBodyAsObject(Collections.singletonList(requestAsMap));

        final UrlTemplateParameters urlTemplateParameters = RestServiceUrlTemplateBuilder.newInstance()
            .fixedPathSegment("execute")
            .createUrlTemplateParameters(serviceBaseUrl, "");

        return policyExecutorWebClient.post()
            .uri(urlTemplateParameters.urlTemplate(), urlTemplateParameters.urlVariables())
            .headers(httpHeaders -> configureHttpHeaders(httpHeaders, authorization))
            .body(BodyInserters.fromValue(bodyAsObject))
            .retrieve()
            .toEntity(JsonNode.class)
            .onErrorMap(this::handleClientException)
            .block();
    }

    private void configureHttpHeaders(final HttpHeaders httpHeaders, final String authorization) {
        httpHeaders.add(HttpHeaders.AUTHORIZATION, authorization);
    }

    private Throwable handleClientException(final Throwable throwable) {
        log.warn("Policy Executor invocation failed: {}", throwable.getMessage());
        return null;
    }


    private static void processResponse(final JsonNode responseBody) {
        final String decisionId = responseBody.path("decisionId").asText("unknown id");
        log.trace("Policy Executor Decision ID: {} ", decisionId);
        final String decision = responseBody.path("decision").asText("unknown");
        if ("allow".equals(decision)) {
            log.trace("Policy Executor allows the operation");
        } else {
            log.warn("Policy Executor decision: {}", decision);
            final String details = responseBody.path("message").asText();
            log.warn("Policy Executor message: {}", details);
            final String message = "Policy Executor did not allow request. Decision #"
                + decisionId + " : " + decision;
            throw new PolicyExecutorException(message, details);
        }
    }

}
