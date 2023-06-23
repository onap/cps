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
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperation;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.models.DataOperationDefinition;
import org.onap.cps.ncmp.api.models.DataOperationRequest;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceDataOperationRequestUtils {

    private static final String UNKNOWN_SERVICE_NAME = null;

    /**
     * Create a list of DMI data operations per DMI service (name).
     *
     * @param dataOperationRequestIn incoming data operation request details
     * @param yangModelCmHandles     involved cm handles represented as YangModelCmHandle (incl. metadata)
     *
     * @return {@code Map<String, List<DmiDataOperation>>} Create a list of DMI data operations operation
     *                                                     per DMI service (name).
     */
    public static Map<String, List<DmiDataOperation>> processPerDefinitionInDataOperationsRequest(
            final DataOperationRequest dataOperationRequestIn,
            final Collection<YangModelCmHandle> yangModelCmHandles) {

        final Map<String, Map<String, Map<String, String>>> dmiPropertiesPerCmHandleIdPerServiceName =
                DmiServiceNameOrganizer.getDmiPropertiesPerCmHandleIdPerServiceName(yangModelCmHandles);

        final Map<String, String> dmiServiceNamesPerCmHandleId =
            getDmiServiceNamesPerCmHandleId(dmiPropertiesPerCmHandleIdPerServiceName);

        final Map<String, List<DmiDataOperation>> dmiDataOperationsOutPerDmiServiceName = new HashMap<>();

        for (final DataOperationDefinition dataOperationDefinitionIn :
            dataOperationRequestIn.getDataOperationDefinitions()) {
            for (final String cmHandleId : dataOperationDefinitionIn.getCmHandleIds()) {
                final String dmiServiceName = dmiServiceNamesPerCmHandleId.get(cmHandleId);
                final Map<String, String> cmHandleIdProperties
                        = dmiPropertiesPerCmHandleIdPerServiceName.get(dmiServiceName).get(cmHandleId);
                if (cmHandleIdProperties == null) {
                    publishErrorMessageToClientTopic(cmHandleId);
                } else {
                    final DmiDataOperation dmiDataOperationOut = getOrAddDmiDataOperation(dmiServiceName,
                            dataOperationDefinitionIn, dmiDataOperationsOutPerDmiServiceName);
                    final CmHandle cmHandle = CmHandle.buildCmHandleWithProperties(cmHandleId, cmHandleIdProperties);
                    dmiDataOperationOut.getCmHandles().add(cmHandle);
                }
            }
        }
        return dmiDataOperationsOutPerDmiServiceName;
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

    private static DmiDataOperation getOrAddDmiDataOperation(final String dmiServiceName,
                                                             final DataOperationDefinition
                                                                       dataOperationDefinitionIn,
                                                             final Map<String, List<DmiDataOperation>>
                                                                       dmiDataOperationsOutPerDmiServiceName) {
        dmiDataOperationsOutPerDmiServiceName
                .computeIfAbsent(dmiServiceName, dmiServiceNameAsKey -> new ArrayList<>());
        final List<DmiDataOperation> dmiDataOperationsOut
                = dmiDataOperationsOutPerDmiServiceName.get(dmiServiceName);
        final boolean isNewOperation = dmiDataOperationsOut.isEmpty()
                || !dmiDataOperationsOut.get(dmiDataOperationsOut.size() - 1).getOperationId()
                .equals(dataOperationDefinitionIn.getOperationId());
        if (isNewOperation) {
            final DmiDataOperation newDmiDataOperationOut =
                    DmiDataOperation.buildDmiDataOperationRequestBodyWithoutCmHandles(dataOperationDefinitionIn);
            dmiDataOperationsOut.add(newDmiDataOperationOut);
            return newDmiDataOperationOut;
        }
        return dmiDataOperationsOut.get(dmiDataOperationsOut.size() - 1);
    }
}
