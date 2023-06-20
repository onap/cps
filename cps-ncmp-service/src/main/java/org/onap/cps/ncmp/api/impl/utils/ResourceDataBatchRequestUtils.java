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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.operations.CmHandle;
import org.onap.cps.ncmp.api.impl.operations.DmiBatchOperation;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.models.DataOperationDefinition;
import org.onap.cps.ncmp.api.models.DataOperationRequest;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceDataBatchRequestUtils {

    private static final String UNKNOWN_SERVICE_NAME = null;

    /**
     * Create a list of DMI batch operation per DMI service (name).
     *
     * @param dataOperationRequestIn incoming data operation request details
     * @param yangModelCmHandles     involved cm handles represented as YangModelCmHandle (incl. metadata)
     *
     * @return {@code Map<String, List<DmiBatchOperation>>} Create a list of DMI batch operation per DMI service (name).
     */
    public static Map<String, List<DmiBatchOperation>> processPerOperationInBatchRequest(
            final DataOperationRequest dataOperationRequestIn,
            final Collection<YangModelCmHandle> yangModelCmHandles) {

        final Map<String, Map<String, Map<String, String>>> dmiPropertiesPerCmHandleIdPerServiceName =
                DmiServiceNameOrganizer.getDmiPropertiesPerCmHandleIdPerServiceName(yangModelCmHandles);

        final Map<String, String> dmiServiceNamesPerCmHandleId =
            getDmiServiceNamesPerCmHandleId(dmiPropertiesPerCmHandleIdPerServiceName);

        final Map<String, List<DmiBatchOperation>> dmiBatchOperationsOutPerDmiServiceName = new HashMap<>();

        for (final DataOperationDefinition dataOperationDefinitionIn :
            dataOperationRequestIn.getDataOperationDefinitions()) {
            for (final String cmHandleId : dataOperationDefinitionIn.getCmHandleIds()) {
                final String dmiServiceName = dmiServiceNamesPerCmHandleId.get(cmHandleId);
                final Map<String, String> cmHandleIdProperties
                        = dmiPropertiesPerCmHandleIdPerServiceName.get(dmiServiceName).get(cmHandleId);
                if (cmHandleIdProperties == null) {
                    publishErrorMessageToClientTopic(cmHandleId);
                } else {
                    final DmiBatchOperation dmiBatchOperationOut = getOrAddDmiBatchOperation(dmiServiceName,
                            dataOperationDefinitionIn, dmiBatchOperationsOutPerDmiServiceName);
                    final CmHandle cmHandle = CmHandle.buildCmHandleWithProperties(cmHandleId, cmHandleIdProperties);
                    dmiBatchOperationOut.getCmHandles().add(cmHandle);
                }
            }
        }
        return dmiBatchOperationsOutPerDmiServiceName;
    }

    private static void publishErrorMessageToClientTopic(final String requestedCmHandleId) {
        log.warn("cm handle {} not found", requestedCmHandleId);
        // TODO Need to publish an error response to client given topic.
        //  Code should be implemented into https://jira.onap.org/browse/CPS-1583 (
        //  NCMP : Handle non-existing cm handles)
    }

    private static Map<String, String> getDmiServiceNamesPerCmHandleId(
            final Map<String, Map<String, Map<String, String>>> dmiDmiPropertiesPerCmHandleIdPerServiceName) {
        final Map<String, String> dmiServiceNamesPerCmHandleId = new HashMap<>();
        for (final Map.Entry<String, Map<String, Map<String, String>>> dmiDmiPropertiesEntry
                : dmiDmiPropertiesPerCmHandleIdPerServiceName.entrySet()) {
            final String dmiServiceName = dmiDmiPropertiesEntry.getKey();
            final Set<String> cmHandleIds = dmiDmiPropertiesPerCmHandleIdPerServiceName.get(dmiServiceName).keySet();
            for (final String cmHandleId : cmHandleIds) {
                dmiServiceNamesPerCmHandleId.put(cmHandleId, dmiServiceName);
            }
        }
        dmiDmiPropertiesPerCmHandleIdPerServiceName.put(UNKNOWN_SERVICE_NAME, Collections.emptyMap());
        return dmiServiceNamesPerCmHandleId;
    }

    private static DmiBatchOperation getOrAddDmiBatchOperation(final String dmiServiceName,
                                                               final DataOperationDefinition
                                                                       dataOperationDefinitionIn,
                                                               final Map<String, List<DmiBatchOperation>>
                                                                       dmiBatchOperationsOutPerDmiServiceName) {
        dmiBatchOperationsOutPerDmiServiceName
                .computeIfAbsent(dmiServiceName, dmiServiceNameAsKey -> new ArrayList<>());
        final List<DmiBatchOperation> dmiBatchOperationsOut
                = dmiBatchOperationsOutPerDmiServiceName.get(dmiServiceName);
        final boolean isNewOperation = dmiBatchOperationsOut.isEmpty()
                || !dmiBatchOperationsOut.get(dmiBatchOperationsOut.size() - 1).getOperationId()
                .equals(dataOperationDefinitionIn.getOperationId());
        if (isNewOperation) {
            final DmiBatchOperation newDmiBatchOperationOut =
                    DmiBatchOperation.buildDmiBatchRequestBodyWithoutCmHandles(dataOperationDefinitionIn);
            dmiBatchOperationsOut.add(newDmiBatchOperationOut);
            return newDmiBatchOperationOut;
        }
        return dmiBatchOperationsOut.get(dmiBatchOperationsOut.size() - 1);
    }
}
