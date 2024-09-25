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

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.policyexecutor.stub.api.PolicyExecutorApi;
import org.onap.cps.policyexecutor.stub.model.NcmpDelete;
import org.onap.cps.policyexecutor.stub.model.PolicyExecutionRequest;
import org.onap.cps.policyexecutor.stub.model.PolicyExecutionResponse;
import org.onap.cps.policyexecutor.stub.model.Request;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PolicyExecutorStubController implements PolicyExecutorApi {

    private final ObjectMapper objectMapper;
    private static final Pattern ERROR_CODE_PATTERN = Pattern.compile("(\\d{3})");
    private int decisionCounter = 0;

    @Override
    public ResponseEntity<PolicyExecutionResponse> executePolicyAction(
                                                     final String action,
                                                     final PolicyExecutionRequest policyExecutionRequest,
                                                     final String authorization) {
        log.info("Stub Policy Executor Invoked (only supports 'delete' operations)");
        if (policyExecutionRequest.getRequests().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        final Request firstRequest = policyExecutionRequest.getRequests().iterator().next();
        log.info("1st Request Schema:{}", firstRequest.getSchema());
        if (firstRequest.getSchema().contains("ncmp-delete-schema:1.0.0")) {
            return handleNcmpDeleteSchema(firstRequest);
        }
        log.warn("This stub only supports 'delete' operations");
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<PolicyExecutionResponse> handleNcmpDeleteSchema(final Request request) {
        final NcmpDelete ncmpDelete = objectMapper.convertValue(request.getData(), NcmpDelete.class);

        final String targetIdentifier = ncmpDelete.getTargetIdentifier();

        if (targetIdentifier == null) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        final Matcher matcher = ERROR_CODE_PATTERN.matcher(targetIdentifier);
        if (matcher.find()) {
            final int errorCode = Integer.parseInt(matcher.group(1));
            return new ResponseEntity<>(HttpStatusCode.valueOf(errorCode));
        }

        return createPolicyExecutionResponse(targetIdentifier);
    }

    @SneakyThrows
    private ResponseEntity<PolicyExecutionResponse> createPolicyExecutionResponse(final String targetIdentifier) {
        final String decisionId = String.valueOf(++decisionCounter);
        final String decision;
        final String message;
        if (targetIdentifier.toLowerCase(Locale.getDefault()).contains("slow")) {
            TimeUnit.SECONDS.sleep(10);
        }
        if (targetIdentifier.toLowerCase(Locale.getDefault()).contains("cps-is-great")) {
            decision = "allow";
            message = "All good";
        } else {
            decision = "deny";
            message = "Only FDNs containing 'cps-is-great' are allowed";
        }
        log.info("Decision: {} ({})", decision, message);
        final PolicyExecutionResponse policyExecutionResponse =
            new PolicyExecutionResponse(decisionId, decision, message);

        return ResponseEntity.ok(policyExecutionResponse);
    }

}
