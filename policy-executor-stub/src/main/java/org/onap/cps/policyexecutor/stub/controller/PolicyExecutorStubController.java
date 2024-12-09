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

package org.onap.cps.policyexecutor.stub.controller;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
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
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("(\\d{3})");
    private int decisionCounter = 0;
    private static int slowResponseTimeInSeconds = 40;

    @Override
    public ResponseEntity<PermissionResponse> initiatePermissionRequest(final String contentType,
                                                                        final PermissionRequest permissionRequest,
                                                                        final String accept,
                                                                        final String authorization) {
        log.info("Stub Policy Executor Invoked");
        if (permissionRequest.getOperations().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        final Operation firstOperation = permissionRequest.getOperations().iterator().next();
        log.info("1st Operation: {}", firstOperation.getOperation());
        if (!"delete".equals(firstOperation.getOperation()) && firstOperation.getChangeRequest() == null) {
            log.warn("Change Request is required for " + firstOperation.getOperation() + " operations");
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        return handleOperation(firstOperation);
    }

    private ResponseEntity<PermissionResponse> handleOperation(final Operation operation) {
        final String targetIdentifier = operation.getTargetIdentifier();

        final Matcher matcher = ERROR_CODE_PATTERN.matcher(targetIdentifier);
        if (matcher.find()) {
            final int errorCode = Integer.parseInt(matcher.group(1));
            log.warn("Stub is mocking an error response, code: " + errorCode);
            return new ResponseEntity<>(HttpStatusCode.valueOf(errorCode));
        }

        return createPolicyExecutionResponse(targetIdentifier);
    }

    private ResponseEntity<PermissionResponse> createPolicyExecutionResponse(final String targetIdentifier) {
        final String id = String.valueOf(++decisionCounter);
        final String permissionResult;
        final String message;
        if (targetIdentifier.toLowerCase(Locale.getDefault()).contains("slow")) {
            try {
                sleeper.haveALittleRest(slowResponseTimeInSeconds);
            } catch (final InterruptedException e) {
                log.trace("Sleep interrupted, re-interrupting the thread");
                Thread.currentThread().interrupt(); // Re-interrupt the thread
            }
        }
        if (targetIdentifier.toLowerCase(Locale.getDefault()).contains("cps-is-great")) {
            permissionResult = "allow";
            message = "All good";
        } else {
            permissionResult = "deny";
            message = "Only FDNs containing 'cps-is-great' are allowed";
        }
        log.info("Decision: {} ({})", permissionResult, message);
        return ResponseEntity.ok(new PermissionResponse(id, permissionResult, message));
    }

}
