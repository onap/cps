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
        log.trace("Policy Executor Enabled:", enabled);
        if (enabled) {
            log.info("Policy Executor Enabled");
            log.info("Address               : {}", serverAddress);
            log.info("Port                  : {}", serverPort);
            log.info("Authorization         : {}", authorization);
            log.info("Operation Type        : {}", operationType.getOperationName());
            log.info("Target Id             : {}", yangModelCmHandle.getAlternateId());
            log.info("CM Handle Id          : {}", yangModelCmHandle.getId());
            log.info("Resource Identifier   : {}", resourceIdentifier);
            log.info("Change Request (json) : {}", changeRequestAsJson);

            final String serviceBaseUrl = serverAddress + ":" + serverPort;

            final Map<String, Object> requestAsMap = getSingleRequestAsMap(yangModelCmHandle,
                operationType,
                resourceIdentifier,
                changeRequestAsJson);

            final Object bodyAsObject = createBodyAsObject(Collections.singletonList(requestAsMap));

            final UrlTemplateParameters urlTemplateParameters = RestServiceUrlTemplateBuilder.newInstance()
                .createUrlTemplateParameters(serviceBaseUrl, "execute");

            final ResponseEntity<JsonNode> responseEntity = policyExecutorWebClient.post()
                .uri(urlTemplateParameters.urlTemplate(), urlTemplateParameters.urlVariables())
                .headers(httpHeaders -> configureHttpHeaders(httpHeaders, authorization))
                .body(BodyInserters.fromValue(bodyAsObject))
                .retrieve()
                .toEntity(JsonNode.class)
                .onErrorMap(this::handleClientException)
                .block();

            if (responseEntity == null || responseEntity.getBody() == null) {
                log.warn("No valid response from policy, ignored");
                return;
            }

            if (responseEntity.getStatusCode().is2xxSuccessful()) {
                log.info("Policy Executor Decision ID: {} ", responseEntity.getBody().path("decisionId").asText());
                final String decision = responseEntity.getBody().path("decision").asText("unknown");
                if ("allow".equals(decision)) {
                    log.info("Policy Executor allows the operation");
                } else {
                    log.warn("Policy Executor Decision: {}", decision);
                    log.warn("Policy Executor Message: {}", responseEntity.getBody().path("message").asText());
                }
            } else {
                log.warn("Policy Executor Invocation failed with status {}",
                    responseEntity.getStatusCode().value());
            }
        }
    }

    private void configureHttpHeaders(final HttpHeaders httpHeaders, final String authorization) {
        httpHeaders.add(HttpHeaders.AUTHORIZATION, authorization);
    }

    private Throwable handleClientException(final Throwable throwable) {
        log.warn("Policy Executor Invocation failed: {}", throwable.getMessage());
        return null;
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
        request.put("schema", operationType.getAssociatedPolicyDataSchemaName());
        request.put("data", data);
        return request;
    }

    private Object createBodyAsObject(final List<Object> requests) {
        final Map<String, Object> bodyAsMap = new HashMap<>(2);
        bodyAsMap.put("decisionType", "allow");
        bodyAsMap.put("requests", requests);
        return bodyAsMap;
    }

}
