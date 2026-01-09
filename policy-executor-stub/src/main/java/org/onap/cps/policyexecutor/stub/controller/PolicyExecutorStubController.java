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

package org.onap.cps.policyexecutor.stub.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.policyexecutor.stub.api.OperationPermissionApi;
import org.onap.cps.policyexecutor.stub.model.Operation;
import org.onap.cps.policyexecutor.stub.model.PermissionRequest;
import org.onap.cps.policyexecutor.stub.model.PermissionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/operation-permission/v1")
@RequiredArgsConstructor
@Slf4j
public class PolicyExecutorStubController implements OperationPermissionApi {

    private final Sleeper sleeper;
    private final ObjectMapper objectMapper;

    private static final Pattern PATTERN_SIMULATION = Pattern.compile("policySimulation=(\\w+_\\w+)");
    private static final Pattern PATTERN_HTTP_ERROR = Pattern.compile("httpError_(\\d{3})");
    private static final Pattern PATTERN_SLOW_RESPONSE = Pattern.compile("slowResponse_(\\d{1,3})");
    private static final Pattern PATTERN_POLICY_RESPONSE = Pattern.compile("policyResponse_(\\w+)");

    private int decisionCounter = 0;

    @Override
    public ResponseEntity<PermissionResponse> initiatePermissionRequest(final String contentType,
                                                                        final PermissionRequest permissionRequest,
                                                                        final String accept,
                                                                        final String authorization) {
        log.info("Stub Policy Executor Invoked");
        log.info("Permission Request: {}", formatPermissionRequest(permissionRequest));
        if (permissionRequest.getOperations().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        final Operation firstOperation = permissionRequest.getOperations().iterator().next();
        log.info("1st Operation: {} for resource: {}", firstOperation.getOperation(),
                                                       firstOperation.getResourceIdentifier());
        if (!"delete".equals(firstOperation.getOperation()) && firstOperation.getChangeRequest() == null) {
            log.warn("Change Request is required for {} operations", firstOperation.getOperation());
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return createPolicyExecutionResponse(firstOperation.getResourceIdentifier());
    }

    private ResponseEntity<PermissionResponse> createPolicyExecutionResponse(final String resourceIdentifier) {
        final String id = String.valueOf(++decisionCounter);
        String permissionResult = "allow";
        String message = "all good";
        Matcher matcher = PATTERN_SIMULATION.matcher(resourceIdentifier);
        if (matcher.find()) {
            final String simulation = matcher.group(1);
            matcher = PATTERN_SLOW_RESPONSE.matcher(simulation);
            if (matcher.matches()) {
                try {
                    final int slowResponseTimeInSeconds = Integer.parseInt(matcher.group(1));
                    sleeper.haveALittleRest(slowResponseTimeInSeconds);
                } catch (final InterruptedException e) {
                    log.trace("Sleep interrupted, re-interrupting the thread");
                    Thread.currentThread().interrupt(); // Re-interrupt the thread
                }
            }
            matcher = PATTERN_HTTP_ERROR.matcher(simulation);
            if (matcher.matches()) {
                final int errorCode = Integer.parseInt(matcher.group(1));
                log.warn("Stub is mocking an error response, code: {}", errorCode);
                return new ResponseEntity<>(HttpStatusCode.valueOf(errorCode));
            }
            matcher = PATTERN_POLICY_RESPONSE.matcher(simulation);
            if (matcher.matches()) {
                permissionResult = matcher.group(1);
                message = "Stub is mocking a policy response: " + permissionResult;
            }
        }
        log.info("Decision: {} ({})", permissionResult, message);
        return ResponseEntity.ok(new PermissionResponse(id, permissionResult, message));
    }

    @SneakyThrows
    private String formatPermissionRequest(final PermissionRequest permissionRequest)  {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(permissionRequest);
    }



}
