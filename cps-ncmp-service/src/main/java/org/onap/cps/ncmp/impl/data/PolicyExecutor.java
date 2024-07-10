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

package org.onap.cps.ncmp.impl.data;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.data.models.OperationType;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

    private final String payloadTypePrefix = "cm_";

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
        if (enabled) {
            final String payloadType = payloadTypePrefix + operationType.getOperationName();
            log.info("Policy Executor Enabled");
            log.info("Address               : {}", serverAddress);
            log.info("Port                  : {}", serverPort);
            log.info("Authorization         : {}", authorization);
            log.info("Payload Type          : {}", payloadType);
            log.info("Target FDN            : {}", yangModelCmHandle.getAlternateId());
            log.info("CM Handle Id          : {}", yangModelCmHandle.getId());
            log.info("Resource Identifier   : {}", resourceIdentifier);
            log.info("Change Request (json) : {}", changeRequestAsJson);
        }
    }
}
