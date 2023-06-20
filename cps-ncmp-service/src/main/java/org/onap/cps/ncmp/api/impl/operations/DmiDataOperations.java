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

import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_RUNNING;
import static org.onap.cps.ncmp.api.impl.operations.OperationType.READ;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration;
import org.onap.cps.ncmp.api.impl.executor.TaskExecutor;
import org.onap.cps.ncmp.api.impl.utils.DmiServiceUrlBuilder;
import org.onap.cps.ncmp.api.impl.utils.ResourceDataBatchRequestUtils;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CmHandleState;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.ncmp.api.models.DataOperationRequest;
import org.onap.cps.spi.exceptions.CpsException;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;

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
                = getDistinctCmHandleIdsFromBatchRequest(dataOperationRequest);

        final Collection<YangModelCmHandle> yangModelCmHandles
                = getYangModelCmHandlesInReadyState(cmHandlesIds);

        final Map<String, List<DmiBatchOperation>> operationsOutPerDmiServiceName
                = ResourceDataBatchRequestUtils.processPerOperationInBatchRequest(dataOperationRequest,
                yangModelCmHandles);

        buildBatchRequestUrlAndSendToDmiService(topicParamInQuery, requestId, operationsOutPerDmiServiceName);
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

    private String getDmiServiceBatchRequestUrl(final String dmiServiceName,
                                                final String topicParamInQuery,
                                                final String requestId) {
        final MultiValueMap<String, String> batchRequestQueryParams = dmiServiceUrlBuilder
                .getBatchRequestQueryParams(topicParamInQuery, requestId);
        return dmiServiceUrlBuilder.getBatchRequestUrl(batchRequestQueryParams,
                dmiServiceUrlBuilder.populateBatchUriVariables(dmiServiceName));
    }

    private void validateIfCmHandleStateReady(final YangModelCmHandle yangModelCmHandle,
                                              final CmHandleState cmHandleState) {
        if (cmHandleState != CmHandleState.READY) {
            throw new CpsException("State mismatch exception.", "Cm-Handle not in READY state. "
                    + "cm handle state is "
                    + yangModelCmHandle.getCompositeState().getCmHandleState());
        }
    }

    private static Set<String> getDistinctCmHandleIdsFromBatchRequest(final DataOperationRequest
                                                                              dataOperationRequest) {
        return dataOperationRequest.getDataOperationDefinitions().stream()
                .flatMap(dataOperationDefinition ->
                        dataOperationDefinition.getCmHandleIds().stream()).collect(Collectors.toSet());
    }

    private Collection<YangModelCmHandle> getYangModelCmHandlesInReadyState(final Set<String> requestedCmHandleIds) {
        // TODO Need to publish an error response to client given topic.
        //  Code should be implemented into https://jira.onap.org/browse/CPS-1614 (
        //  NCMP : Error handling for non-ready cm handle state)
        return inventoryPersistence.getYangModelCmHandles(requestedCmHandleIds).stream()
                .filter(yangModelCmHandle -> yangModelCmHandle.getCompositeState().getCmHandleState()
                        == CmHandleState.READY).collect(Collectors.toList());
    }

    private void buildBatchRequestUrlAndSendToDmiService(final String topicParamInQuery,
                                                         final String requestId,
                                                         final Map<String, List<DmiBatchOperation>>
                                                                groupsOutPerDmiServiceName) {

        groupsOutPerDmiServiceName.entrySet().forEach(groupsOutPerDmiServiceNameEntry -> {
            final String dmiServiceName = groupsOutPerDmiServiceNameEntry.getKey();
            final List<DmiBatchOperation> dmiBatchRequestBodies = groupsOutPerDmiServiceNameEntry.getValue();
            final String dmiBatchResourceDataUrl = getDmiServiceBatchRequestUrl(dmiServiceName, topicParamInQuery,
                    requestId);
            sendBatchRequestToDmiService(dmiBatchResourceDataUrl, dmiBatchRequestBodies);
        });
    }

    private void sendBatchRequestToDmiService(final String batchResourceDataUrl,
                                              final List<DmiBatchOperation> dmiBatchRequestBodies) {
        final String batchRequestBodiesAsJsonString = jsonObjectMapper.asJsonString(dmiBatchRequestBodies);
        TaskExecutor.executeTask(() -> dmiRestClient.postOperationWithJsonData(batchResourceDataUrl,
                        batchRequestBodiesAsJsonString, READ), DEFAULT_ASYNC_TASK_EXECUTOR_TIMEOUT_IN_MILLISECONDS)
                .whenCompleteAsync(this::handleTaskCompletion);
    }

    private void handleTaskCompletion(final Object response, final Throwable throwable) {
        // TODO Need to publish an error response to client given topic.
        //  Code should be implemented into https://jira.onap.org/browse/CPS-1558 (
        //  NCMP : Handle non responding DMI-Plugin)
    }
}
