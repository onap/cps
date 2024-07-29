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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.onap.cps.policyexecutor.stub.api.PolicyExecutorApi;
import org.onap.cps.policyexecutor.stub.model.NcmpDelete;
import org.onap.cps.policyexecutor.stub.model.PolicyExecutionRequest;
import org.onap.cps.policyexecutor.stub.model.PolicyExecutionResponse;
import org.onap.cps.policyexecutor.stub.model.Request;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.policy-executor-base-path}")
@RequiredArgsConstructor
public class PolicyExecutorStubController implements PolicyExecutorApi {

    private final ObjectMapper objectMapper;

    private static final Pattern errorCodePattern = Pattern.compile("(\\d{3})");

    private int decisionCounter = 0;

    @Override
    public ResponseEntity<PolicyExecutionResponse> executePolicyAction(
                                                     final String action,
                                                     final PolicyExecutionRequest policyExecutionRequest,
                                                     final String authorization) {
        if (policyExecutionRequest.getRequests().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
        final Request firstRequest = policyExecutionRequest.getRequests().iterator().next();

        if ("ncmp-delete-schema:1.0.0".equals(firstRequest.getSchema())) {
            final String data = (String) firstRequest.getData();
            final NcmpDelete ncmpDelete;
            try {
                ncmpDelete = objectMapper.readValue(data, NcmpDelete.class);
            } catch (final JsonProcessingException e) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            final String firstTargetIdentifier = ncmpDelete.getTargetIdentifier();

            if (firstTargetIdentifier == null) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }

            final Matcher matcher = errorCodePattern.matcher(firstTargetIdentifier);
            if (matcher.find()) {
                final int errorCode = Integer.parseInt(matcher.group(1));
                return new ResponseEntity<>(HttpStatusCode.valueOf(errorCode));
            }

            final String decisionId = String.valueOf(++decisionCounter);
            final String decision;
            final String message;

            if (firstTargetIdentifier.toLowerCase(Locale.getDefault()).contains("cps-is-great")) {
                decision = "allow";
                message = "All good";
            } else {
                decision = "deny";
                message = "Only FDNs containing 'cps-is-great' are allowed";
            }
            final PolicyExecutionResponse policyExecutionResponse =
                new PolicyExecutionResponse(decisionId, decision, message);
            return ResponseEntity.ok(policyExecutionResponse);
        }
        return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
    }
}
