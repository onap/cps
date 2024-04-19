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

package org.onap.cps.ncmp.api.impl.utils.data.operation;

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
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.events.EventsPublisher;
import org.onap.cps.ncmp.api.NcmpResponseStatus;
import org.onap.cps.ncmp.api.impl.inventory.CmHandleState;
import org.onap.cps.ncmp.api.impl.operations.CmHandle;
import org.onap.cps.ncmp.api.impl.operations.DmiDataOperation;
import org.onap.cps.ncmp.api.impl.utils.DmiServiceNameOrganizer;
import org.onap.cps.ncmp.api.impl.utils.context.CpsApplicationContext;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.models.DataOperationDefinition;
import org.onap.cps.ncmp.api.models.DataOperationRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ResourceDataOperationRequestUtils {

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
                List<String>>> cmHandleIdsPerResponseCodesPerOperation = new LinkedMultiValueMap<>();
        final Set<String> nonReadyCmHandleIdsLookup = filterAndGetNonReadyCmHandleIds(yangModelCmHandles);

        final Map<String, Map<String, Map<String, String>>> dmiPropertiesPerCmHandleIdPerServiceName =
                DmiServiceNameOrganizer.getDmiPropertiesPerCmHandleIdPerServiceName(yangModelCmHandles);

        final Map<String, String> dmiServiceNamesPerCmHandleId =
                getDmiServiceNamesPerCmHandleId(dmiPropertiesPerCmHandleIdPerServiceName);

        for (final DataOperationDefinition dataOperationDefinitionIn :
                dataOperationRequestIn.getDataOperationDefinitions()) {
            final List<String> nonExistingCmHandleIds = new ArrayList<>();
            final List<String> nonReadyCmHandleIds = new ArrayList<>();
            for (final String cmHandleId : dataOperationDefinitionIn.getCmHandleIds()) {
                if (nonReadyCmHandleIdsLookup.contains(cmHandleId)) {
                    nonReadyCmHandleIds.add(cmHandleId);
                } else {
                    final String dmiServiceName = dmiServiceNamesPerCmHandleId.get(cmHandleId);
                    final Map<String, String> cmHandleIdProperties
                            = dmiPropertiesPerCmHandleIdPerServiceName.get(dmiServiceName).get(cmHandleId);
                    if (cmHandleIdProperties == null) {
                        nonExistingCmHandleIds.add(cmHandleId);
                    } else {
                        final DmiDataOperation dmiBatchOperationOut = getOrAddDmiBatchOperation(dmiServiceName,
                                dataOperationDefinitionIn, dmiDataOperationsOutPerDmiServiceName);
                        final CmHandle cmHandle = CmHandle.buildCmHandleWithProperties(cmHandleId,
                                cmHandleIdProperties);
                        dmiBatchOperationOut.getCmHandles().add(cmHandle);
                    }
                }
            }
            populateCmHandleIdsPerOperationIdPerResponseCode(cmHandleIdsPerResponseCodesPerOperation,
                    DmiDataOperation.buildDmiDataOperationRequestBodyWithoutCmHandles(dataOperationDefinitionIn),
                    CM_HANDLES_NOT_FOUND, nonExistingCmHandleIds);
            populateCmHandleIdsPerOperationIdPerResponseCode(cmHandleIdsPerResponseCodesPerOperation,
                    DmiDataOperation.buildDmiDataOperationRequestBodyWithoutCmHandles(dataOperationDefinitionIn),
                    CM_HANDLES_NOT_READY, nonReadyCmHandleIds);
        }
        publishErrorMessageToClientTopic(topicParamInQuery, requestId, cmHandleIdsPerResponseCodesPerOperation);
        return dmiDataOperationsOutPerDmiServiceName;
    }

    /**
     * Handles the async task completion for an entire data, publishing errors to client topic on task failure.
     *
     * @param topicParamInQuery      client given topic
     * @param requestId              unique identifier per request
     * @param dataOperationRequest   incoming data operation request details
     * @param throwable              error cause, or null if task completed with no exception
     */
    public static void handleAsyncTaskCompletionForDataOperationsRequest(
            final String topicParamInQuery,
            final String requestId,
            final DataOperationRequest dataOperationRequest,
            final Throwable throwable) {
        if (throwable == null) {
            log.info("Data operations request {} completed.", requestId);
        } else if (throwable instanceof TimeoutException) {
            log.error("Data operations request {} timed out.", requestId);
            ResourceDataOperationRequestUtils.publishErrorMessageToClientTopicForEntireOperation(topicParamInQuery,
                    requestId, dataOperationRequest, NcmpResponseStatus.DMI_SERVICE_NOT_RESPONDING);
        } else {
            log.error("Data operations request {} failed.", requestId, throwable);
            ResourceDataOperationRequestUtils.publishErrorMessageToClientTopicForEntireOperation(topicParamInQuery,
                    requestId, dataOperationRequest, NcmpResponseStatus.UNKNOWN_ERROR);
        }
    }

    /**
     * Creates data operation cloud event for when the entire data operation fails and publishes it to client topic.
     *
     * @param topicParamInQuery      client given topic
     * @param requestId              unique identifier per request
     * @param dataOperationRequestIn incoming data operation request details
     * @param ncmpResponseStatus     response code to be sent for all cm handle ids in all operations
     */
    private static void publishErrorMessageToClientTopicForEntireOperation(
            final String topicParamInQuery,
            final String requestId,
            final DataOperationRequest dataOperationRequestIn,
            final NcmpResponseStatus ncmpResponseStatus) {

        final MultiValueMap<DmiDataOperation, Map<NcmpResponseStatus, List<String>>>
                cmHandleIdsPerResponseCodesPerOperation = new LinkedMultiValueMap<>();

        for (final DataOperationDefinition dataOperationDefinitionIn :
                dataOperationRequestIn.getDataOperationDefinitions()) {
            cmHandleIdsPerResponseCodesPerOperation.add(
                    DmiDataOperation.buildDmiDataOperationRequestBodyWithoutCmHandles(dataOperationDefinitionIn),
                    Map.of(ncmpResponseStatus, dataOperationDefinitionIn.getCmHandleIds()));
        }
        publishErrorMessageToClientTopic(topicParamInQuery, requestId, cmHandleIdsPerResponseCodesPerOperation);
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

    private static Set<String> filterAndGetNonReadyCmHandleIds(final Collection<YangModelCmHandle> yangModelCmHandles) {
        return yangModelCmHandles.stream()
                .filter(yangModelCmHandle -> yangModelCmHandle.getCompositeState().getCmHandleState()
                        != CmHandleState.READY).map(YangModelCmHandle::getId).collect(Collectors.toSet());
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
