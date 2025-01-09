/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
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

package org.onap.cps.ncmp.impl.data.utils;

import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLES_NOT_FOUND;
import static org.onap.cps.ncmp.api.NcmpResponseStatus.CM_HANDLES_NOT_READY;

import io.cloudevents.CloudEvent;
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
import org.onap.cps.events.EventsPublisher;
import org.onap.cps.ncmp.api.NcmpResponseStatus;
import org.onap.cps.ncmp.api.data.models.DataOperationDefinition;
import org.onap.cps.ncmp.api.data.models.DataOperationRequest;
import org.onap.cps.ncmp.api.inventory.models.CmHandleState;
import org.onap.cps.ncmp.config.CpsApplicationContext;
import org.onap.cps.ncmp.impl.data.models.DmiDataOperation;
import org.onap.cps.ncmp.impl.data.models.DmiOperationCmHandle;
import org.onap.cps.ncmp.impl.dmi.DmiServiceNameOrganizer;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class DmiDataOperationsHelper {

    private static final String UNKNOWN_SERVICE_NAME = null;

    /**
     * Create a list of DMI data operation per DMI service (name).
     *
     * @param topicParamInQuery      client given topic
     * @param requestId              unique identifier per request
     * @param dataOperationRequestIn incoming data operation request details
     * @param yangModelCmHandles     involved cm handles represented as YangModelCmHandle (incl. metadata)
     * @return {@code Map<String, List<DmiBatchOperation>>} Create a list of DMI batch operation per DMI service (name).
     */
    public static Map<String, List<DmiDataOperation>> processPerDefinitionInDataOperationsRequest(
            final String topicParamInQuery,
            final String requestId,
            final DataOperationRequest dataOperationRequestIn,
            final Collection<YangModelCmHandle> yangModelCmHandles) {

        final Map<String, List<DmiDataOperation>> dmiDataOperationsOutPerDmiServiceName = new HashMap<>();
        final MultiValueMap<DmiDataOperation, Map<NcmpResponseStatus,
                List<String>>> cmHandleReferencesPerResponseCodesPerOperation = new LinkedMultiValueMap<>();
        final Map<String, String> nonReadyAlternateIdPerCmHandleId =
            filterAndGetNonReadyAlternateIdPerCmHandleId(yangModelCmHandles);

        final Map<String, Map<String, Map<String, String>>> dmiPropertiesPerCmHandleIdPerServiceName =
                DmiServiceNameOrganizer.getDmiPropertiesPerCmHandleIdPerServiceName(yangModelCmHandles);

        final Map<String, String> dmiServiceNamesPerCmHandleId =
                getDmiServiceNamesPerCmHandleId(dmiPropertiesPerCmHandleIdPerServiceName);

        final Map<String, String> moduleSetTagPerCmHandle = getModuleSetTagPerCmHandleId(yangModelCmHandles);

        for (final DataOperationDefinition dataOperationDefinitionIn :
                dataOperationRequestIn.getDataOperationDefinitions()) {
            final List<String> nonExistingCmHandleReferences = new ArrayList<>();
            final List<String> nonReadyCmHandleReferences = new ArrayList<>();
            for (final String cmHandleReference : dataOperationDefinitionIn.getCmHandleReferences()) {
                if (nonReadyAlternateIdPerCmHandleId.containsKey(cmHandleReference)
                    || nonReadyAlternateIdPerCmHandleId.containsValue(cmHandleReference)) {
                    nonReadyCmHandleReferences.add(cmHandleReference);
                } else {
                    final String cmHandleId = getCmHandleId(cmHandleReference, yangModelCmHandles);
                    final String dmiServiceName = dmiServiceNamesPerCmHandleId.get(cmHandleId);
                    final Map<String, String> cmHandleIdProperties
                            = dmiPropertiesPerCmHandleIdPerServiceName.get(dmiServiceName).get(cmHandleId);
                    if (cmHandleIdProperties == null) {
                        nonExistingCmHandleReferences.add(cmHandleReference);
                    } else {
                        final DmiDataOperation dmiBatchOperationOut = getOrAddDmiBatchOperation(dmiServiceName,
                                dataOperationDefinitionIn, dmiDataOperationsOutPerDmiServiceName);
                        final DmiOperationCmHandle dmiOperationCmHandle = DmiOperationCmHandle
                                .buildDmiOperationCmHandle(cmHandleId, cmHandleIdProperties,
                                        moduleSetTagPerCmHandle.get(cmHandleId));
                        dmiBatchOperationOut.getCmHandles().add(dmiOperationCmHandle);
                    }
                }
            }
            populateCmHandleIdsPerOperationIdPerResponseCode(cmHandleReferencesPerResponseCodesPerOperation,
                    DmiDataOperation.buildDmiDataOperationRequestBodyWithoutCmHandles(dataOperationDefinitionIn),
                    CM_HANDLES_NOT_FOUND, nonExistingCmHandleReferences);
            populateCmHandleIdsPerOperationIdPerResponseCode(cmHandleReferencesPerResponseCodesPerOperation,
                    DmiDataOperation.buildDmiDataOperationRequestBodyWithoutCmHandles(dataOperationDefinitionIn),
                    CM_HANDLES_NOT_READY, nonReadyCmHandleReferences);
        }
        publishErrorMessageToClientTopic(topicParamInQuery, requestId, cmHandleReferencesPerResponseCodesPerOperation);
        return dmiDataOperationsOutPerDmiServiceName;
    }

    private static Map<String, String> getModuleSetTagPerCmHandleId(
                                                       final Collection<YangModelCmHandle> yangModelCmHandles) {
        final Map<String, String> moduleSetTagPerCmHandle = new HashMap<>(yangModelCmHandles.size());
        yangModelCmHandles.forEach(yangModelCmHandle ->
                moduleSetTagPerCmHandle.put(yangModelCmHandle.getId(), yangModelCmHandle.getModuleSetTag()));
        return moduleSetTagPerCmHandle;
    }

    /**
     * Creates data operation cloud event and publish it to client topic.
     *
     * @param clientTopic                              client given topic
     * @param requestId                                unique identifier per request
     * @param cmHandleIdsPerResponseCodesPerOperation  list of cm handle ids per operation with response code
     */
    public static void publishErrorMessageToClientTopic(final String clientTopic,
                                                         final String requestId,
                                                         final MultiValueMap<DmiDataOperation,
                                                                 Map<NcmpResponseStatus, List<String>>>
                                                                    cmHandleIdsPerResponseCodesPerOperation) {
        if (!cmHandleIdsPerResponseCodesPerOperation.isEmpty()) {
            final CloudEvent dataOperationCloudEvent = DataOperationEventCreator.createDataOperationEvent(clientTopic,
                    requestId, cmHandleIdsPerResponseCodesPerOperation);
            final EventsPublisher<CloudEvent> eventsPublisher = CpsApplicationContext.getCpsBean(EventsPublisher.class);
            log.warn("publishing error message to client topic: {} ,requestId: {}, data operation cloud event id: {}",
                    clientTopic, requestId, dataOperationCloudEvent.getId());
            eventsPublisher.publishCloudEvent(clientTopic, requestId, dataOperationCloudEvent);
        }
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

    private static DmiDataOperation getOrAddDmiBatchOperation(final String dmiServiceName,
                                                               final DataOperationDefinition
                                                                       dataOperationDefinitionIn,
                                                               final Map<String, List<DmiDataOperation>>
                                                                       dmiBatchOperationsOutPerDmiServiceName) {
        dmiBatchOperationsOutPerDmiServiceName
                .computeIfAbsent(dmiServiceName, dmiServiceNameAsKey -> new ArrayList<>());
        final List<DmiDataOperation> dmiBatchOperationsOut
                = dmiBatchOperationsOutPerDmiServiceName.get(dmiServiceName);
        final boolean isNewOperation = dmiBatchOperationsOut.isEmpty()
                || !dmiBatchOperationsOut.get(dmiBatchOperationsOut.size() - 1).getOperationId()
                .equals(dataOperationDefinitionIn.getOperationId());
        if (isNewOperation) {
            final DmiDataOperation newDmiBatchOperationOut =
                    DmiDataOperation.buildDmiDataOperationRequestBodyWithoutCmHandles(dataOperationDefinitionIn);
            dmiBatchOperationsOut.add(newDmiBatchOperationOut);
            return newDmiBatchOperationOut;
        }
        return dmiBatchOperationsOut.get(dmiBatchOperationsOut.size() - 1);
    }

    private static Map<String, String> filterAndGetNonReadyAlternateIdPerCmHandleId(
        final Collection<YangModelCmHandle> yangModelCmHandles) {
        final Map<String, String> cmHandleReferenceMap = new HashMap<>(yangModelCmHandles.size());
        for (final YangModelCmHandle yangModelCmHandle: yangModelCmHandles) {
            if (yangModelCmHandle.getCompositeState().getCmHandleState() != CmHandleState.READY) {
                cmHandleReferenceMap.put(yangModelCmHandle.getId(), yangModelCmHandle.getAlternateId());
            }
        }
        return cmHandleReferenceMap;
    }

    private static String getCmHandleId(final String cmHandleReference,
                                        final Collection<YangModelCmHandle> yangModelCmHandles) {
        for (final YangModelCmHandle yangModelCmHandle: yangModelCmHandles) {
            if (cmHandleReference.equals(yangModelCmHandle.getId())
                || cmHandleReference.equals(yangModelCmHandle.getAlternateId())) {
                return yangModelCmHandle.getId();
            }
        }
        return cmHandleReference;
    }

    private static void populateCmHandleIdsPerOperationIdPerResponseCode(final MultiValueMap<DmiDataOperation,
            Map<NcmpResponseStatus, List<String>>> cmHandleIdsPerResponseCodesPerOperation,
                                                                        final DmiDataOperation dmiDataOperation,
                                                                        final NcmpResponseStatus
                                                                                 ncmpResponseStatus,
                                                                        final List<String> cmHandleIds) {
        if (!cmHandleIds.isEmpty()) {
            cmHandleIdsPerResponseCodesPerOperation.add(dmiDataOperation, Map.of(ncmpResponseStatus, cmHandleIds));
        }
    }
}
