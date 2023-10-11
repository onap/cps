/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada
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

package org.onap.cps.ncmp.api.impl.operations;

import static org.onap.cps.ncmp.api.NcmpResponseStatus.DMI_SERVICE_NOT_RESPONDING;
import static org.onap.cps.ncmp.api.NcmpResponseStatus.UNABLE_TO_READ_RESOURCE_DATA;
import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_RUNNING;
import static org.onap.cps.ncmp.api.impl.operations.OperationType.READ;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NcmpResponseStatus;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration;
import org.onap.cps.ncmp.api.impl.exception.HttpClientRequestException;
import org.onap.cps.ncmp.api.impl.executor.TaskExecutor;
import org.onap.cps.ncmp.api.impl.inventory.CmHandleState;
import org.onap.cps.ncmp.api.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.impl.utils.DmiServiceUrlBuilder;
import org.onap.cps.ncmp.api.impl.utils.data.operation.ResourceDataOperationRequestUtils;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.models.DataOperationRequest;
import org.onap.cps.spi.exceptions.CpsException;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Operations class for DMI data.
 */
@Component
@Slf4j
public class DmiDataOperations extends DmiOperations {

    private static final long DEFAULT_ASYNC_TASK_EXECUTOR_TIMEOUT_IN_MILLISECONDS = 30000L;

    public DmiDataOperations(final InventoryPersistence inventoryPersistence,
                             final JsonObjectMapper jsonObjectMapper,
                             final NcmpConfiguration.DmiProperties dmiProperties,
                             final DmiRestClient dmiRestClient,
                             final DmiServiceUrlBuilder dmiServiceUrlBuilder) {
        super(inventoryPersistence, jsonObjectMapper, dmiProperties, dmiRestClient, dmiServiceUrlBuilder);
    }

    /**
     * This method fetches the resource data from operational data store for given cm handle
     * identifier on given resource using dmi client.
     *
     * @param dataStoreName       name of data store
     * @param cmHandleId          network resource identifier
     * @param resourceId          resource identifier
     * @param optionsParamInQuery options query
     * @param topicParamInQuery   topic name for (triggering) async responses
     * @param requestId           requestId for async responses
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<Object> getResourceDataFromDmi(final String dataStoreName,
                                                         final String cmHandleId,
                                                         final String resourceId,
                                                         final String optionsParamInQuery,
                                                         final String topicParamInQuery,
                                                         final String requestId) {
        final YangModelCmHandle yangModelCmHandle = getYangModelCmHandle(cmHandleId);
        final CmHandleState cmHandleState = yangModelCmHandle.getCompositeState().getCmHandleState();
        validateIfCmHandleStateReady(yangModelCmHandle, cmHandleState);
        final String jsonRequestBody = getDmiRequestBody(READ, requestId, null, null,
                yangModelCmHandle);
        final String dmiResourceDataUrl = getDmiRequestUrl(dataStoreName, cmHandleId, resourceId, optionsParamInQuery,
                topicParamInQuery, yangModelCmHandle.resolveDmiServiceName(RequiredDmiService.DATA));
        return dmiRestClient.postOperationWithJsonData(dmiResourceDataUrl, jsonRequestBody, READ);
    }

    /**
     * This method fetches all the resource data from operational data store for given cm handle
     * identifier using dmi client.
     *
     * @param dataStoreName data store name
     * @param cmHandleId    network resource identifier
     * @param requestId     requestId for async responses
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<Object> getResourceDataFromDmi(final String dataStoreName,
                                                         final String cmHandleId,
                                                         final String requestId) {
        final YangModelCmHandle yangModelCmHandle = getYangModelCmHandle(cmHandleId);
        final String jsonRequestBody = getDmiRequestBody(READ, requestId, null, null,
                yangModelCmHandle);
        final String dmiResourceDataUrl = getDmiRequestUrl(dataStoreName, cmHandleId, "/",
                null, null,
                yangModelCmHandle.resolveDmiServiceName(RequiredDmiService.DATA));
        final CmHandleState cmHandleState = yangModelCmHandle.getCompositeState().getCmHandleState();
        validateIfCmHandleStateReady(yangModelCmHandle, cmHandleState);
        return dmiRestClient.postOperationWithJsonData(dmiResourceDataUrl, jsonRequestBody, READ);
    }

    /**
     * This method requests the resource data by data store for given list of cm handles using dmi client.
     * The data wil be returned as message on the topic specified.
     *
     * @param topicParamInQuery        topic name for (triggering) async responses
     * @param dataOperationRequest     data operation request to execute operations
     * @param requestId                requestId for as a response
     */
    public void requestResourceDataFromDmi(final String topicParamInQuery,
                                           final DataOperationRequest dataOperationRequest,
                                           final String requestId)  {

        final Set<String> cmHandlesIds
                = getDistinctCmHandleIdsFromDataOperationRequest(dataOperationRequest);

        final Collection<YangModelCmHandle> yangModelCmHandles
                = inventoryPersistence.getYangModelCmHandles(cmHandlesIds);

        final Map<String, List<DmiDataOperation>> operationsOutPerDmiServiceName
                = ResourceDataOperationRequestUtils.processPerDefinitionInDataOperationsRequest(topicParamInQuery,
                requestId, dataOperationRequest, yangModelCmHandles);

        buildDataOperationRequestUrlAndSendToDmiService(topicParamInQuery, requestId, operationsOutPerDmiServiceName);
    }

    /**
     * This method creates the resource data from pass-through running data store for given cm handle
     * identifier on given resource using dmi client.
     *
     * @param cmHandleId    network resource identifier
     * @param resourceId    resource identifier
     * @param operationType operation enum
     * @param requestData   the request data
     * @param dataType      data type
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<Object> writeResourceDataPassThroughRunningFromDmi(final String cmHandleId,
                                                                             final String resourceId,
                                                                             final OperationType operationType,
                                                                             final String requestData,
                                                                             final String dataType) {
        final YangModelCmHandle yangModelCmHandle = getYangModelCmHandle(cmHandleId);
        final String jsonRequestBody = getDmiRequestBody(operationType, null, requestData, dataType,
                yangModelCmHandle);
        final String dmiUrl = getDmiRequestUrl(PASSTHROUGH_RUNNING.getDatastoreName(), cmHandleId, resourceId,
                null, null,
                yangModelCmHandle.resolveDmiServiceName(RequiredDmiService.DATA));
        final CmHandleState cmHandleState = yangModelCmHandle.getCompositeState().getCmHandleState();
        validateIfCmHandleStateReady(yangModelCmHandle, cmHandleState);
        return dmiRestClient.postOperationWithJsonData(dmiUrl, jsonRequestBody, operationType);
    }

    private YangModelCmHandle getYangModelCmHandle(final String cmHandleId) {
        return inventoryPersistence.getYangModelCmHandle(cmHandleId);
    }

    private String getDmiRequestBody(final OperationType operationType,
                                     final String requestId,
                                     final String requestData,
                                     final String dataType,
                                     final YangModelCmHandle yangModelCmHandle) {
        final DmiRequestBody dmiRequestBody = DmiRequestBody.builder()
                .operationType(operationType)
                .requestId(requestId)
                .data(requestData)
                .dataType(dataType)
                .build();
        dmiRequestBody.asDmiProperties(yangModelCmHandle.getDmiProperties());
        return jsonObjectMapper.asJsonString(dmiRequestBody);
    }

    private String getDmiRequestUrl(final String dataStoreName,
                                    final String cmHandleId,
                                    final String resourceId,
                                    final String optionsParamInQuery,
                                    final String topicParamInQuery,
                                    final String dmiServiceName) {
        return dmiServiceUrlBuilder.getDmiDatastoreUrl(
                dmiServiceUrlBuilder.populateQueryParams(resourceId, optionsParamInQuery,
                        topicParamInQuery), dmiServiceUrlBuilder.populateUriVariables(dataStoreName, dmiServiceName,
                        cmHandleId));
    }

    private String getDmiServiceDataOperationRequestUrl(final String dmiServiceName,
                                                        final String topicParamInQuery,
                                                        final String requestId) {
        final MultiValueMap<String, String> dataOperationRequestQueryParams = dmiServiceUrlBuilder
                .getDataOperationRequestQueryParams(topicParamInQuery, requestId);
        return dmiServiceUrlBuilder.getDataOperationRequestUrl(dataOperationRequestQueryParams,
                dmiServiceUrlBuilder.populateDataOperationRequestUriVariables(dmiServiceName));
    }

    private void validateIfCmHandleStateReady(final YangModelCmHandle yangModelCmHandle,
                                              final CmHandleState cmHandleState) {
        if (cmHandleState != CmHandleState.READY) {
            throw new CpsException("State mismatch exception.", "Cm-Handle not in READY state. "
                    + "cm handle state is "
                    + yangModelCmHandle.getCompositeState().getCmHandleState());
        }
    }

    private static Set<String> getDistinctCmHandleIdsFromDataOperationRequest(final DataOperationRequest
                                                                              dataOperationRequest) {
        return dataOperationRequest.getDataOperationDefinitions().stream()
                .flatMap(dataOperationDefinition ->
                        dataOperationDefinition.getCmHandleIds().stream()).collect(Collectors.toSet());
    }

    private void buildDataOperationRequestUrlAndSendToDmiService(final String topicParamInQuery,
                                                                 final String requestId,
                                                                 final Map<String, List<DmiDataOperation>>
                                                                groupsOutPerDmiServiceName) {

        groupsOutPerDmiServiceName.forEach((dmiServiceName, dmiDataOperationRequestBodies) -> {
            final String dmiDataOperationResourceUrl =
                    getDmiServiceDataOperationRequestUrl(dmiServiceName, topicParamInQuery, requestId);
            sendDataOperationRequestToDmiService(dmiDataOperationResourceUrl, dmiDataOperationRequestBodies);
        });
    }

    private void sendDataOperationRequestToDmiService(final String dataOperationResourceUrl,
                                                      final List<DmiDataOperation> dmiDataOperationRequestBodies) {
        final DmiDataOperationRequest dmiDataOperationRequest = DmiDataOperationRequest.builder()
                .operations(dmiDataOperationRequestBodies).build();
        final String dmiDataOperationRequestAsJsonString =
                jsonObjectMapper.asJsonString(dmiDataOperationRequest);
        TaskExecutor.executeTask(() -> dmiRestClient.postOperationWithJsonData(dataOperationResourceUrl,
                                dmiDataOperationRequestAsJsonString, READ),
                        DEFAULT_ASYNC_TASK_EXECUTOR_TIMEOUT_IN_MILLISECONDS)
                .whenCompleteAsync((response, throwable) -> handleTaskCompletionException(throwable,
                        dataOperationResourceUrl, dmiDataOperationRequestBodies));
    }

    private void handleTaskCompletionException(final Throwable throwable,
                                               final String dataOperationResourceUrl,
                                               final List<DmiDataOperation> dmiDataOperationRequestBodies) {
        if (throwable != null) {
            final MultiValueMap<String, String> dataOperationResourceUrlParameters =
                    UriComponentsBuilder.fromUriString(dataOperationResourceUrl).build().getQueryParams();
            final String topicName = dataOperationResourceUrlParameters.get("topic").get(0);
            final String requestId = dataOperationResourceUrlParameters.get("requestId").get(0);

            final MultiValueMap<DmiDataOperation, Map<NcmpResponseStatus, List<String>>>
                    cmHandleIdsPerResponseCodesPerOperation = new LinkedMultiValueMap<>();

            dmiDataOperationRequestBodies.forEach(dmiDataOperationRequestBody -> {
                final List<String> cmHandleIds = dmiDataOperationRequestBody.getCmHandles().stream()
                        .map(CmHandle::getId).toList();
                if (throwable.getCause() instanceof HttpClientRequestException) {
                    cmHandleIdsPerResponseCodesPerOperation.add(dmiDataOperationRequestBody,
                            Map.of(UNABLE_TO_READ_RESOURCE_DATA, cmHandleIds));
                } else {
                    cmHandleIdsPerResponseCodesPerOperation.add(dmiDataOperationRequestBody,
                            Map.of(DMI_SERVICE_NOT_RESPONDING, cmHandleIds));
                }
            });
            ResourceDataOperationRequestUtils.publishErrorMessageToClientTopic(topicName, requestId,
                    cmHandleIdsPerResponseCodesPerOperation);
        }
    }
}
