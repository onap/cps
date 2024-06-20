/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
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

import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_OPERATIONAL;
import static org.onap.cps.ncmp.api.impl.operations.DatastoreType.PASSTHROUGH_RUNNING;
import static org.onap.cps.ncmp.api.impl.operations.OperationType.READ;
import static org.onap.cps.ncmp.api.impl.operations.RequiredDmiService.DATA;

import io.micrometer.core.annotation.Timed;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.NcmpResponseStatus;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.config.DmiProperties;
import org.onap.cps.ncmp.api.impl.exception.DmiClientRequestException;
import org.onap.cps.ncmp.api.impl.utils.data.operation.ResourceDataOperationRequestUtils;
import org.onap.cps.ncmp.api.impl.utils.url.builder.DmiServiceUrlBuilder;
import org.onap.cps.ncmp.api.impl.utils.url.builder.UrlTemplateParameters;
import org.onap.cps.ncmp.api.models.CmResourceAddress;
import org.onap.cps.ncmp.api.models.DataOperationRequest;
import org.onap.cps.ncmp.impl.inventory.InventoryPersistence;
import org.onap.cps.ncmp.impl.inventory.models.CmHandleState;
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle;
import org.onap.cps.spi.exceptions.CpsException;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Operations class for DMI data.
 */
@RequiredArgsConstructor
@Service
public class DmiDataOperations {

    private final InventoryPersistence inventoryPersistence;
    private final JsonObjectMapper jsonObjectMapper;
    private final DmiProperties dmiProperties;
    private final DmiRestClient dmiRestClient;

    /**
     * This method fetches the resource data from the operational data store for a given CM handle
     * identifier on the specified resource using the DMI client.
     *
     * @param cmResourceAddress Target datastore, CM handle, and resource identifier.
     * @param options           Options query string.
     * @param topic             Topic name for triggering asynchronous responses.
     * @param requestId         Request ID for asynchronous responses.
     * @param authorization     Contents of the Authorization header, or null if not present.
     * @return {@code Mono<ResponseEntity<Object>>} A reactive type representing the response entity.
     */
    @Timed(value = "cps.ncmp.dmi.get",
            description = "Time taken to fetch the resource data from operational data store for given cm handle "
                    + "identifier on given resource using dmi client")
    public Mono<ResponseEntity<Object>> getResourceDataFromDmi(final CmResourceAddress cmResourceAddress,
                                                               final String options,
                                                               final String topic,
                                                               final String requestId,
                                                               final String authorization) {
        final YangModelCmHandle yangModelCmHandle = getYangModelCmHandle(cmResourceAddress.cmHandleId());
        final CmHandleState cmHandleState = yangModelCmHandle.getCompositeState().getCmHandleState();
        validateIfCmHandleStateReady(yangModelCmHandle, cmHandleState);
        final String jsonRequestBody = getDmiRequestBody(READ, requestId, null, null, yangModelCmHandle);
        final UrlTemplateParameters urlTemplateParameters = getUrlTemplateParameters(cmResourceAddress
                        .datastoreName(), yangModelCmHandle, cmResourceAddress.resourceIdentifier(), options, topic);
        return dmiRestClient.asynchronousPostOperationWithJsonData(DATA, urlTemplateParameters, jsonRequestBody,
                READ, authorization);
    }

    /**
     * This method fetches all the resource data from operational data store for given cm handle
     * identifier using dmi client.
     * Note: this method is only used for DataSync
     *
     * @param cmHandleId    network resource identifier
     * @param requestId     requestId for async responses
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<Object> getAllResourceDataFromDmi(final String cmHandleId, final String requestId) {
        final YangModelCmHandle yangModelCmHandle = getYangModelCmHandle(cmHandleId);
        final CmHandleState cmHandleState = yangModelCmHandle.getCompositeState().getCmHandleState();
        validateIfCmHandleStateReady(yangModelCmHandle, cmHandleState);

        final String jsonRequestBody = getDmiRequestBody(READ, requestId, null, null, yangModelCmHandle);
        final UrlTemplateParameters urlTemplateParameters = getUrlTemplateParameters(
                PASSTHROUGH_OPERATIONAL.getDatastoreName(), yangModelCmHandle, "/", null,
                null);
        return dmiRestClient.synchronousPostOperationWithJsonData(DATA, urlTemplateParameters, jsonRequestBody, READ,
                null);
    }

    /**
     * This method requests the resource data by data store for given list of cm handles using dmi client.
     * The data wil be returned as message on the topic specified.
     *
     * @param topicParamInQuery        topic name for (triggering) async responses
     * @param dataOperationRequest     data operation request to execute operations
     * @param requestId                requestId for as a response
     * @param authorization            contents of Authorization header, or null if not present
     */
    public void requestResourceDataFromDmi(final String topicParamInQuery,
                                           final DataOperationRequest dataOperationRequest,
                                           final String requestId,
                                           final String authorization)  {

        final Set<String> cmHandlesIds
                = getDistinctCmHandleIdsFromDataOperationRequest(dataOperationRequest);

        final Collection<YangModelCmHandle> yangModelCmHandles
            = inventoryPersistence.getYangModelCmHandles(cmHandlesIds);

        final Map<String, List<DmiDataOperation>> operationsOutPerDmiServiceName
                = ResourceDataOperationRequestUtils.processPerDefinitionInDataOperationsRequest(topicParamInQuery,
                requestId, dataOperationRequest, yangModelCmHandles);

        sendDataOperationRequestToDmiService(requestId, topicParamInQuery, operationsOutPerDmiServiceName,
                authorization);
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
     * @param authorization contents of Authorization header, or null if not present
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<Object> writeResourceDataPassThroughRunningFromDmi(final String cmHandleId,
                                                                             final String resourceId,
                                                                             final OperationType operationType,
                                                                             final String requestData,
                                                                             final String dataType,
                                                                             final String authorization) {
        final YangModelCmHandle yangModelCmHandle = getYangModelCmHandle(cmHandleId);
        final CmHandleState cmHandleState = yangModelCmHandle.getCompositeState().getCmHandleState();
        validateIfCmHandleStateReady(yangModelCmHandle, cmHandleState);

        final String jsonRequestBody = getDmiRequestBody(operationType, null, requestData, dataType,
                yangModelCmHandle);
        final UrlTemplateParameters urlTemplateParameters = getUrlTemplateParameters(
                PASSTHROUGH_RUNNING.getDatastoreName(), yangModelCmHandle, resourceId, null,
                null);
        return dmiRestClient.synchronousPostOperationWithJsonData(DATA, urlTemplateParameters, jsonRequestBody,
                operationType, authorization);
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
                .moduleSetTag(yangModelCmHandle.getModuleSetTag())
                .build();
        dmiRequestBody.asDmiProperties(yangModelCmHandle.getDmiProperties());
        return jsonObjectMapper.asJsonString(dmiRequestBody);
    }

    private UrlTemplateParameters getUrlTemplateParameters(final String datastoreName,
                                                           final YangModelCmHandle yangModelCmHandle,
                                                           final String resourceIdentifier,
                                                           final String optionsParamInQuery,
                                                           final String topicParamInQuery) {
        final String dmiServiceName = yangModelCmHandle.resolveDmiServiceName(RequiredDmiService.DATA);
        return DmiServiceUrlBuilder.newInstance()
                .fixedPathSegment("ch")
                .variablePathSegment("cmHandleId", yangModelCmHandle.getId())
                .fixedPathSegment("data")
                .fixedPathSegment("ds")
                .variablePathSegment("datastore", datastoreName)
                .queryParameter("resourceIdentifier", resourceIdentifier)
                .queryParameter("options", optionsParamInQuery)
                .queryParameter("topic", topicParamInQuery)
                .createUrlTemplateParameters(dmiServiceName, dmiProperties.getDmiBasePath());
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

    private void sendDataOperationRequestToDmiService(final String requestId, final String topicParamInQuery,
                                                      final Map<String, List<DmiDataOperation>>
                                                                         groupsOutPerDmiServiceName,
                                                      final String authorization) {

        Flux.fromIterable(groupsOutPerDmiServiceName.entrySet())
                .flatMap(dmiDataOperationsByDmiServiceName -> {
                    final String dmiServiceName = dmiDataOperationsByDmiServiceName.getKey();
                    final UrlTemplateParameters urlTemplateParameters = getUrlTemplateParameters(dmiServiceName,
                            requestId, topicParamInQuery);
                    final List<DmiDataOperation> dmiDataOperationRequestBodies
                            = dmiDataOperationsByDmiServiceName.getValue();
                    return sendDataOperationRequestToDmiService(urlTemplateParameters, dmiDataOperationRequestBodies,
                            authorization);
                }).subscribe();
    }

    private UrlTemplateParameters getUrlTemplateParameters(final String dmiServiceName, final String requestId,
                                                           final String topicParamInQuery) {
        return DmiServiceUrlBuilder.newInstance()
                .fixedPathSegment("data")
                .queryParameter("requestId", requestId)
                .queryParameter("topic", topicParamInQuery)
                .createUrlTemplateParameters(dmiServiceName, dmiProperties.getDmiBasePath());
    }

    private Mono<Void> sendDataOperationRequestToDmiService(final UrlTemplateParameters urlTemplateParameters,
                                                            final List<DmiDataOperation> dmiDataOperationRequestBodies,
                                                            final String authorization) {
        final String dmiDataOperationRequestAsJsonString
                = createDmiDataOperationRequestAsJsonString(dmiDataOperationRequestBodies);
        return dmiRestClient.asynchronousPostOperationWithJsonData(DATA, urlTemplateParameters,
                        dmiDataOperationRequestAsJsonString, READ, authorization)
                .then()
                .onErrorResume(DmiClientRequestException.class, dmiClientRequestException -> {
                    final String dataOperationResourceUri = DmiServiceUrlBuilder
                            .fromUriString(urlTemplateParameters.urlTemplate())
                            .buildAndExpand(urlTemplateParameters.urlVariables()).toUriString();
                    handleTaskCompletionException(dmiClientRequestException, dataOperationResourceUri,
                            dmiDataOperationRequestBodies);
                    return Mono.empty();
                });
    }

    private String createDmiDataOperationRequestAsJsonString(
            final List<DmiDataOperation> dmiDataOperationRequestBodies) {
        final DmiDataOperationRequest dmiDataOperationRequest = DmiDataOperationRequest.builder()
                .operations(dmiDataOperationRequestBodies)
                .build();
        return jsonObjectMapper.asJsonString(dmiDataOperationRequest);
    }

    private void handleTaskCompletionException(final DmiClientRequestException dmiClientRequestException,
                                               final String dataOperationResourceUrl,
                                               final List<DmiDataOperation> dmiDataOperationRequestBodies) {
        final MultiValueMap<String, String> dataOperationResourceUrlParameters =
                UriComponentsBuilder.fromUriString(dataOperationResourceUrl).build().getQueryParams();
        final String topicName = dataOperationResourceUrlParameters.get("topic").get(0);
        final String requestId = dataOperationResourceUrlParameters.get("requestId").get(0);

        final MultiValueMap<DmiDataOperation, Map<NcmpResponseStatus, List<String>>>
                cmHandleIdsPerResponseCodesPerOperation = new LinkedMultiValueMap<>();

        dmiDataOperationRequestBodies.forEach(dmiDataOperationRequestBody -> {
            final List<String> cmHandleIds = dmiDataOperationRequestBody.getCmHandles().stream()
                    .map(DmiOperationCmHandle::getId).toList();
            cmHandleIdsPerResponseCodesPerOperation.add(dmiDataOperationRequestBody,
                    Map.of(dmiClientRequestException.getNcmpResponseStatus(), cmHandleIds));
        });
        ResourceDataOperationRequestUtils.publishErrorMessageToClientTopic(topicName, requestId,
                cmHandleIdsPerResponseCodesPerOperation);
    }
}
