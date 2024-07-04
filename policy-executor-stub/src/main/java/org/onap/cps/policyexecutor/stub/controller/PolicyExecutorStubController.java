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
import org.onap.cps.policyexecutor.stub.api.PolicyExecutorApi;
import org.onap.cps.policyexecutor.stub.model.PolicyExecutionRequest;
import org.onap.cps.policyexecutor.stub.model.PolicyExecutionResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("${rest.api.policy-executor-base-path}")
public class PolicyExecutorStubController implements PolicyExecutorApi {

    private final Pattern errorCodePattern = Pattern.compile("(\\d{3})");
    private int decisionCounter = 0;

    @Override
    public ResponseEntity<PolicyExecutionResponse> executePolicyAction(final String action,
                                                     final PolicyExecutionRequest policyExecutionRequest) {
        if (policyExecutionRequest.getPayload().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }

        final String firstTargetFdn = policyExecutionRequest.getPayload().iterator().next().getTargetFdn();

        final Matcher matcher = errorCodePattern.matcher(firstTargetFdn);
        if (matcher.find()) {
            final int errorCode = Integer.parseInt(matcher.group(1));
            return new ResponseEntity<>(HttpStatusCode.valueOf(errorCode));
        }

        final PolicyExecutionResponse policyExecutionResponse = new PolicyExecutionResponse();
        policyExecutionResponse.setDecisionId(String.valueOf(++decisionCounter));

        if (firstTargetFdn.toLowerCase(Locale.getDefault()).contains("cps-is-great")) {
            policyExecutionResponse.setDecision("permit");
        } else {
            policyExecutionResponse.setDecision("deny");
            policyExecutionResponse.setMessage("Only FDNs containing 'cps-is-great' are permitted");
        }
        return ResponseEntity.ok(policyExecutionResponse);
    }
}
