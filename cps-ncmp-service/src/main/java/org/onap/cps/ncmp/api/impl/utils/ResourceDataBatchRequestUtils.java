/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation
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
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.operations.CmHandle;
import org.onap.cps.ncmp.api.impl.operations.DmiBatchRequestBody;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.models.BatchOperationDefinition;
import org.onap.cps.ncmp.api.models.ResourceDataBatchRequest;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceDataBatchRequestUtils {

    /**
     * populate batch request (DmiBatchRequestBody) and group it for dmi service.
     *
     * @param resourceDataBatchRequest            batch request details for resource data
     * @param yangModelCmHandles cm handle represented as Yang Models
     * @return {@code Map<String, List<DmiBatchRequestBody>>} dmi service url having batch request body details
     */
    public static Map<String, List<DmiBatchRequestBody>> processPerOperationInBatchRequest(
            final ResourceDataBatchRequest resourceDataBatchRequest,
            final Collection<YangModelCmHandle> yangModelCmHandles) {

        final Map<String, List<DmiBatchRequestBody>> operationsOutPerDmiServiceName = new HashMap<>();

        final Map<String, Map<String, Map<String, String>>> dmiServiceNameCmHandlePropertiesMap =
                DmiServiceNameOrganizer.getDmiPropertiesPerCmHandleIdPerServiceName(yangModelCmHandles);

        resourceDataBatchRequest.getBatchOperationDefinitions().forEach(batchOperationDefinition -> {

            final DmiBatchRequestBody dmiBatchRequestBody
                    = DmiBatchRequestBody.getDmiBatchRequestBody(batchOperationDefinition);

            processPerCmHandlePerOperation(batchOperationDefinition, dmiBatchRequestBody,
                    operationsOutPerDmiServiceName, dmiServiceNameCmHandlePropertiesMap);
        });
        return operationsOutPerDmiServiceName;
    }

    private static void processPerCmHandlePerOperation(final BatchOperationDefinition
                                                               batchOperationDefinition,
                                                       final DmiBatchRequestBody dmiBatchRequestBody,
                                                       final Map<String, List<DmiBatchRequestBody>>
                                                               operationsOutPerDmiServiceName,
                                                       final Map<String, Map<String, Map<String, String>>>
                                                               dmiServiceNameCmHandlePropertiesMap) {

        batchOperationDefinition.getCmHandleIds().forEach(requestedCmHandleId ->

                dmiServiceNameCmHandlePropertiesMap.entrySet().forEach(dmiServiceNameCmHandlePropertyEntry -> {

                    final String dmiServiceName = dmiServiceNameCmHandlePropertyEntry.getKey();
                    final Map<String, Map<String, String>> cmHandleIdWithProperties
                            = dmiServiceNameCmHandlePropertyEntry.getValue();

                    populateOperationOurPerDmiServiceName(batchOperationDefinition.getOperationId(),
                            dmiBatchRequestBody, dmiServiceName, requestedCmHandleId, cmHandleIdWithProperties,
                            operationsOutPerDmiServiceName);
                }));
    }

    private static void populateOperationOurPerDmiServiceName(final String operationId,
                                                              final DmiBatchRequestBody dmiBatchRequestBody,
                                                              final String dmiServiceName,
                                                              final String requestedCmHandleId,
                                                              final Map<String, Map<String, String>>
                                                                      cmHandleIdWithProperties,
                                                              final Map<String, List<DmiBatchRequestBody>>
                                                                      operationsOutPerDmiServiceName) {
        if (operationsOutPerDmiServiceName.containsKey(dmiServiceName)) {
            if (cmHandleIdWithProperties.containsKey(requestedCmHandleId)) {
                addDmiBatchRequestBodyPerServiceName(operationId, dmiBatchRequestBody, dmiServiceName,
                        requestedCmHandleId, cmHandleIdWithProperties.get(requestedCmHandleId),
                        operationsOutPerDmiServiceName);
            } else {
                // TODO Need to publish an error response to client given topic.
                //  Code should be implemented into https://jira.onap.org/browse/CPS-1583 (
                //  NCMP : Handle non-existing cm handles)
                log.warn("cm handle {} not found", requestedCmHandleId);
            }
        } else {
            if (cmHandleIdWithProperties.containsKey(requestedCmHandleId)) {
                setCmHandlesToDmiBatchRequestBody(dmiBatchRequestBody, requestedCmHandleId,
                        cmHandleIdWithProperties.get(requestedCmHandleId));
                addDmiBatchRequestBodyToOperationsOutPerDmiServiceName(dmiBatchRequestBody, dmiServiceName,
                        operationsOutPerDmiServiceName);
            } else {
                // TODO Need to publish an error response to client given topic.
                //  Code should be implemented into https://jira.onap.org/browse/CPS-1583 (
                //  NCMP : Handle non-existing cm handles)
                log.warn("cm handle {} not found", requestedCmHandleId);
            }
        }
    }

    private static void addDmiBatchRequestBodyPerServiceName(final String operationId,
                                                             final DmiBatchRequestBody dmiBatchRequestBody,
                                                             final String dmiServiceName,
                                                             final String requestedCmHandleId,
                                                             final Map<String, String> cmHandleProperties,
                                                             final Map<String, List<DmiBatchRequestBody>>
                                                                     operationsOutPerDmiServiceName) {
        operationsOutPerDmiServiceName.get(dmiServiceName).stream()
                .filter(existingDmiBatchRequestBody ->
                        existingDmiBatchRequestBody.getOperationId().equalsIgnoreCase(operationId))
                .findAny()
                .ifPresentOrElse(existingDmiBatchRequestBody -> {
                    setCmHandlesToDmiBatchRequestBody(existingDmiBatchRequestBody, requestedCmHandleId,
                            cmHandleProperties);
                },
                        () -> {
                            setCmHandlesToDmiBatchRequestBody(dmiBatchRequestBody, requestedCmHandleId,
                                    cmHandleProperties);
                            addDmiBatchRequestBodyToOperationsOutPerDmiServiceName(dmiBatchRequestBody, dmiServiceName,
                                    operationsOutPerDmiServiceName);
                        });
    }

    private static void addDmiBatchRequestBodyToOperationsOutPerDmiServiceName(final DmiBatchRequestBody
                                                                                       dmiBatchRequestBody,
                                                                               final String dmiServiceName,
                                                                               final Map<String,
                                                                                       List<DmiBatchRequestBody>>
                                                                                       operationsOutPerDmiServiceName) {
        if (operationsOutPerDmiServiceName.get(dmiServiceName) == null) {
            final List<DmiBatchRequestBody> dmiBatchRequestBodies = new ArrayList<>();
            dmiBatchRequestBodies.add(dmiBatchRequestBody);
            operationsOutPerDmiServiceName.put(dmiServiceName, dmiBatchRequestBodies);
        } else {
            operationsOutPerDmiServiceName.get(dmiServiceName).add(dmiBatchRequestBody);
        }
    }

    private static void setCmHandlesToDmiBatchRequestBody(final DmiBatchRequestBody dmiBatchRequestBody,
                                                          final String requestedCmHandleId,
                                                          final Map<String, String> cmHandleProperties) {
        final CmHandle requestedCmHandleWithProperties = CmHandle.getCmHandleWithProperties(requestedCmHandleId,
                cmHandleProperties);
        if (dmiBatchRequestBody.getCmHandles() == null) {
            final List<CmHandle> cmHandles = new ArrayList<>();
            cmHandles.add(requestedCmHandleWithProperties);
            dmiBatchRequestBody.setCmHandles(cmHandles);
        } else {
            dmiBatchRequestBody.getCmHandles().add(requestedCmHandleWithProperties);
        }
    }
}
