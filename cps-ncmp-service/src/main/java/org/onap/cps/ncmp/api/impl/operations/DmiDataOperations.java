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

import static org.onap.cps.ncmp.api.impl.operations.DmiOperations.DataStoreEnum.PASSTHROUGH_RUNNING;
import static org.onap.cps.ncmp.api.impl.operations.OperationEnum.READ;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration;
import org.onap.cps.ncmp.api.impl.executor.TaskExecutor;
import org.onap.cps.ncmp.api.impl.utils.DmiServiceNameOrganizer;
import org.onap.cps.ncmp.api.impl.utils.DmiServiceUrlBuilder;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.ncmp.api.inventory.CmHandleState;
import org.onap.cps.ncmp.api.inventory.InventoryPersistence;
import org.onap.cps.spi.exceptions.CpsException;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Operations class for DMI data.
 */
@Component
@Slf4j
public class DmiDataOperations extends DmiOperations {

    private static final long DEFAULT_ASYNC_TASK_EXECUTOR_TIMEOUT_IN_MILLISECONDS = 30000L;
    private static final String NO_CM_HANDLE_ID = "";

    /**
     * Constructor for {@code DmiOperations}. This method also manipulates url properties.
     *
     * @param dmiRestClient {@code DmiRestClient}
     */
    public DmiDataOperations(final InventoryPersistence inventoryPersistence,
                             final JsonObjectMapper jsonObjectMapper,
                             final NcmpConfiguration.DmiProperties dmiProperties,
                             final DmiRestClient dmiRestClient,
                             final DmiServiceUrlBuilder dmiServiceUrlBuilder,
                             final TaskExecutor taskExecutor,
                             final DmiServiceNameOrganizer dmiServiceNameOrganizer) {
        super(inventoryPersistence, jsonObjectMapper, dmiProperties, dmiRestClient, dmiServiceUrlBuilder,
                taskExecutor, dmiServiceNameOrganizer);
    }

    /**
     * This method fetches the resource data from operational data store for given cm handle
     * identifier on given resource using dmi client.
     *
     * @param cmHandleId          network resource identifier
     * @param resourceId          resource identifier
     * @param optionsParamInQuery options query
     * @param dataStoreName       name of data store
     * @param requestId           requestId for async responses
     * @param topicParamInQuery   topic name for (triggering) async responses
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<Object> getResourceDataFromDmi(final String cmHandleId,
                                                         final String resourceId,
                                                         final String optionsParamInQuery,
                                                         final String dataStoreName,
                                                         final String requestId,
                                                         final String topicParamInQuery) {
        final YangModelCmHandle yangModelCmHandle = getYangModelCmHandle(cmHandleId);
        final CmHandleState cmHandleState = yangModelCmHandle.getCompositeState().getCmHandleState();
        validateIfCmHandleStateReady(yangModelCmHandle, cmHandleState);
        final String jsonRequestBody = getDmiRequestBody(READ, requestId, null, null,
                yangModelCmHandle);
        final String dmiResourceDataUrl = getDmiRequestUrl(cmHandleId, resourceId, optionsParamInQuery, dataStoreName,
                topicParamInQuery, yangModelCmHandle.resolveDmiServiceName(RequiredDmiService.DATA));
        return dmiRestClient.postOperationWithJsonData(dmiResourceDataUrl, jsonRequestBody, READ);
    }

    /**
     * This method fetches the resource data by data store for given list of cm handles using dmi client.
     *
     * @param cmHandleIds         list of cm handles
     * @param resourceId          resource identifier
     * @param optionsParamInQuery options query
     * @param dataStoreName           data store name
     * @param requestId           requestId for async responses
     * @param topicParamInQuery   topic name for (triggering) async responses
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<Object> getResourceDataFromDmi(final List<String> cmHandleIds,
                                                         final String resourceId,
                                                         final String optionsParamInQuery,
                                                         final String dataStoreName,
                                                         final String requestId,
                                                         final String topicParamInQuery) {
        final Collection<YangModelCmHandle> yangModelCmHandles
                = inventoryPersistence.getYangModelCmHandles(cmHandleIds);
        final Map<String, Map<String, Map<String, String>>> dmiServiceNameCmHandlePropertiesMap =
                dmiServiceNameOrganizer.organizeByDmiServiceName(yangModelCmHandles);

        dmiServiceNameCmHandlePropertiesMap.entrySet().parallelStream().forEach(
                dmiServiceNameCmHandlePropertiesEntry -> {
                    final String dmiBulkResourceDataUrl = getDmiServiceBulkRequestUrl(resourceId, optionsParamInQuery,
                            dataStoreName, topicParamInQuery, dmiServiceNameCmHandlePropertiesEntry.getKey());
                    final String jsonRequestBodyAsJsonString =
                            jsonObjectMapper.asJsonString(dmiServiceNameCmHandlePropertiesEntry.getValue());
                    final String jsonRequestBody
                            = getDmiBulkRequestBody(READ, requestId, jsonRequestBodyAsJsonString);
                    sendDmiResourceDataRequestToDmiService(dmiBulkResourceDataUrl, jsonRequestBody);
                });
        return new ResponseEntity<>(HttpStatus.ACCEPTED);
    }

    /**
     * This method fetches all the resource data from operational data store for given cm handle
     * identifier using dmi client.
     *
     * @param cmHandleId network resource identifier
     * @param dataStore  data store enum
     * @param requestId  requestId for async responses
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<Object> getResourceDataFromDmi(final String cmHandleId,
                                                         final String dataStore,
                                                         final String requestId) {
        final YangModelCmHandle yangModelCmHandle = getYangModelCmHandle(cmHandleId);
        final String jsonRequestBody = getDmiRequestBody(READ, requestId, null, null,
                yangModelCmHandle);
        final String dmiResourceDataUrl = getDmiRequestUrl(cmHandleId, "/", null, dataStore,
                null, yangModelCmHandle.resolveDmiServiceName(RequiredDmiService.DATA));
        final CmHandleState cmHandleState = yangModelCmHandle.getCompositeState().getCmHandleState();
        validateIfCmHandleStateReady(yangModelCmHandle, cmHandleState);
        return dmiRestClient.postOperationWithJsonData(dmiResourceDataUrl, jsonRequestBody,
                READ);
    }

    /**
     * This method creates the resource data from pass-through running data store for given cm handle
     * identifier on given resource using dmi client.
     *
     * @param cmHandleId    network resource identifier
     * @param resourceId  resource identifier
     * @param operation   operation enum
     * @param requestData the request data
     * @param dataType    data type
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<Object> writeResourceDataPassThroughRunningFromDmi(final String cmHandleId,
                                                                             final String resourceId,
                                                                             final OperationEnum operation,
                                                                             final String requestData,
                                                                             final String dataType) {
        final YangModelCmHandle yangModelCmHandle = getYangModelCmHandle(cmHandleId);
        final String jsonRequestBody = getDmiRequestBody(operation, null, requestData, dataType,
                yangModelCmHandle);
        final String dmiUrl = getDmiRequestUrl(cmHandleId, resourceId, null,
                PASSTHROUGH_RUNNING.getValue(), null, yangModelCmHandle
                        .resolveDmiServiceName(RequiredDmiService.DATA));
        final CmHandleState cmHandleState = yangModelCmHandle.getCompositeState().getCmHandleState();
        validateIfCmHandleStateReady(yangModelCmHandle, cmHandleState);
        return dmiRestClient.postOperationWithJsonData(dmiUrl, jsonRequestBody, operation);
    }

    private YangModelCmHandle getYangModelCmHandle(final String cmHandleId) {
        return inventoryPersistence.getYangModelCmHandle(cmHandleId);
    }

    private String getDmiRequestBody(final OperationEnum operation, final String requestId, final String requestData,
                                     final String dataType, final YangModelCmHandle yangModelCmHandle) {
        final DmiRequestBody dmiRequestBody = DmiRequestBody.builder()
                .operation(operation)
                .requestId(requestId)
                .data(requestData)
                .dataType(dataType)
                .build();
        dmiRequestBody.asDmiProperties(yangModelCmHandle.getDmiProperties());
        return jsonObjectMapper.asJsonString(dmiRequestBody);
    }

    private String getDmiBulkRequestBody(final OperationEnum operation,
                                         final String requestId,
                                         final String requestData) {
        final DmiRequestBody dmiBulkRequestBody = DmiRequestBody.builder()
                .operation(operation)
                .requestId(requestId)
                .data(requestData)
                .build();
        return jsonObjectMapper.asJsonString(dmiBulkRequestBody);
    }

    private String getDmiRequestUrl(final String cmHandleId,
                                      final String resourceId,
                                      final String optionsParamInQuery,
                                      final String dataStoreName,
                                      final String topicParamInQuery,
                                      final String dmiServiceName) {
        return dmiServiceUrlBuilder.getDmiDatastoreUrl(
                dmiServiceUrlBuilder.populateQueryParams(resourceId, optionsParamInQuery,
                        topicParamInQuery), dmiServiceUrlBuilder.populateUriVariables(
                        dmiServiceName, cmHandleId, dataStoreName));
    }

    private String getDmiServiceBulkRequestUrl(final String resourceId,
                                               final String optionsParamInQuery,
                                               final String dataStoreName,
                                               final String topicParamInQuery,
                                               final String dmiServiceName) {
        return dmiServiceUrlBuilder.getBulkRequestUrl(
                dmiServiceUrlBuilder.populateQueryParams(resourceId, optionsParamInQuery,
                        topicParamInQuery), dmiServiceUrlBuilder.populateUriVariables(
                        dmiServiceName, NO_CM_HANDLE_ID, dataStoreName));
    }

    private void validateIfCmHandleStateReady(final YangModelCmHandle yangModelCmHandle,
                                              final CmHandleState cmHandleState) {
        if (cmHandleState != CmHandleState.READY) {
            throw new CpsException("State mismatch exception.", "Cm-Handle not in READY state. "
                    + "cm handle state is "
                    + yangModelCmHandle.getCompositeState().getCmHandleState());
        }
    }

    private void sendDmiResourceDataRequestToDmiService(final String dmiBulkResourceDataUrl,
                                                        final String dmiResourceDataRequestAsJsonString) {
        taskExecutor.executeTask(() ->
                                dmiRestClient.postOperationWithJsonData(dmiBulkResourceDataUrl,
                                        dmiResourceDataRequestAsJsonString, READ),
                        DEFAULT_ASYNC_TASK_EXECUTOR_TIMEOUT_IN_MILLISECONDS)
                .whenCompleteAsync(this::handleTaskCompletion);
    }

    private void handleTaskCompletion(final Object response, final Throwable throwable) {
        // TODO Need to publish an error response to client given topic.
        //  Code should be implemented into https://jira.onap.org/browse/CPS-1558 (
        //  NCMP : Handle non responding DMI-Plugin)
        if (throwable != null) {
            if (throwable instanceof TimeoutException) {
                log.info("Task didn't completed within the required time.");
            } else {
                log.info("Execution of task is failed. caused by : {}", throwable.getMessage());
            }
        }
    }
}
