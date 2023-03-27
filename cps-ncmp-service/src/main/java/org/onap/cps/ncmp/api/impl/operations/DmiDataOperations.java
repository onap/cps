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
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum;
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.READ;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration;
import org.onap.cps.ncmp.api.impl.executor.CpsNcmpTaskExecutor;
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
                             final CpsNcmpTaskExecutor cpsNcmpTaskExecutor) {
        super(inventoryPersistence, jsonObjectMapper, dmiProperties, dmiRestClient, dmiServiceUrlBuilder,
                cpsNcmpTaskExecutor);
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
                topicParamInQuery, yangModelCmHandle, false);
        return dmiRestClient.postOperationWithJsonData(dmiResourceDataUrl, Collections.singletonList(jsonRequestBody),
                READ);
    }

    /**
     * This method fetches the resource data by data store for given list of cm handles using dmi client.
     *
     * @param cmHandleIds         list of cm handles
     * @param resourceId          resource identifier
     * @param optionsParamInQuery options query
     * @param dataStoreName           data store enum
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
        final Map<String, List<String>> dmiResourceDataRequestMap = new HashMap<>();
        final Collection<YangModelCmHandle> yangModelCmHandles
                = inventoryPersistence.getYangModelCmHandles(cmHandleIds);

        populateDmiResourceRequestMapByDmiService(resourceId, optionsParamInQuery, dataStoreName, requestId,
                topicParamInQuery, dmiResourceDataRequestMap, yangModelCmHandles);

        sendDmiResourceDataRequestToDmiService(dmiResourceDataRequestMap);
        // TODO Need to capture non error response and send an ACK to client app here.
        //  It should be implemented once DMI service implemented after https://jira.onap.org/browse/CPS-1555 (
        //  DMI-Plugin :  Expose endpoint to accept bulk request)
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
        final String jsonRequestBody = getDmiRequestBody(READ, requestId, null, null, yangModelCmHandle);
        final String dmiResourceDataUrl = getDmiRequestUrl(cmHandleId, "/", null, dataStore,
                null, yangModelCmHandle, false);
        final CmHandleState cmHandleState = yangModelCmHandle.getCompositeState().getCmHandleState();
        validateIfCmHandleStateReady(yangModelCmHandle, cmHandleState);
        return dmiRestClient.postOperationWithJsonData(dmiResourceDataUrl, Collections.singletonList(jsonRequestBody),
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
                PASSTHROUGH_RUNNING.getValue(), null, yangModelCmHandle, false);
        final CmHandleState cmHandleState = yangModelCmHandle.getCompositeState().getCmHandleState();
        validateIfCmHandleStateReady(yangModelCmHandle, cmHandleState);
        return dmiRestClient.postOperationWithJsonData(dmiUrl, Collections.singletonList(jsonRequestBody), operation);
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

    private String getDmiRequestUrl(final String cmHandleId,
                                      final String resourceId,
                                      final String optionsParamInQuery,
                                      final String dataStoreName,
                                      final String topicParamInQuery,
                                      final YangModelCmHandle yangModelCmHandle,
                                    final boolean isBulkRequest) {
        return dmiServiceUrlBuilder.getDmiDatastoreUrl(
                dmiServiceUrlBuilder.populateQueryParams(resourceId, optionsParamInQuery,
                        topicParamInQuery), dmiServiceUrlBuilder.populateUriVariables(
                        yangModelCmHandle, cmHandleId, dataStoreName), isBulkRequest);
    }

    private void validateIfCmHandleStateReady(final YangModelCmHandle yangModelCmHandle,
                                              final CmHandleState cmHandleState) {
        if (cmHandleState != CmHandleState.READY) {
            throw new CpsException("State mismatch exception.", "Cm-Handle not in READY state. "
                    + "cm handle state is "
                    + yangModelCmHandle.getCompositeState().getCmHandleState());
        }
    }

    private void populateDmiResourceRequestMapByDmiService(final String resourceId, final String optionsParamInQuery,
                                                           final String dataStoreName, final String requestId,
                                                           final String topicParamInQuery,
                                                           final Map<String, List<String>> dmiResourceDataRequestMap,
                                                           final Collection<YangModelCmHandle> yangModelCmHandles) {
        yangModelCmHandles.stream().filter(yangModelCmHandle ->
                        yangModelCmHandle.getCompositeState().getCmHandleState() == CmHandleState.READY)
                .forEach(yangModelCmHandle -> {
                    final String jsonRequestBody
                            = getDmiRequestBody(READ, requestId, null, null, yangModelCmHandle);
                    final String dmiResourceDataUrl
                            = getDmiRequestUrl(yangModelCmHandle.getId(), resourceId, optionsParamInQuery,
                            dataStoreName, topicParamInQuery, yangModelCmHandle, true);
                    if (dmiResourceDataRequestMap.containsKey(dmiResourceDataUrl)) {
                        dmiResourceDataRequestMap.get(dmiResourceDataUrl).add(jsonRequestBody);
                    } else {
                        dmiResourceDataRequestMap.put(dmiResourceDataUrl, new ArrayList<>() {
                            {
                                add(jsonRequestBody);
                            }
                        });
                    }
                });
    }

    private void sendDmiResourceDataRequestToDmiService(final Map<String, List<String>> dmiResourceDataRequestMap) {
        dmiResourceDataRequestMap.entrySet().parallelStream().forEach(dmiResourceDataRequestEntry ->
                cpsNcmpTaskExecutor.executeTask(() ->
                                        dmiRestClient.postOperationWithJsonData(dmiResourceDataRequestEntry.getKey(),
                                                dmiResourceDataRequestEntry.getValue(), READ),
                                DEFAULT_ASYNC_TASK_EXECUTOR_TIMEOUT_IN_MILLISECONDS)
                        .whenCompleteAsync(this::handleTaskCompletion));
    }

    private void handleTaskCompletion(final Object response, final Throwable throwable) {
        // TODO Need to publish an error response to client given topic.
        //  Code should be implemented into https://jira.onap.org/browse/CPS-1558 (
        //  NCMP : Handle non responding DMI-Plugin)
    }
}
