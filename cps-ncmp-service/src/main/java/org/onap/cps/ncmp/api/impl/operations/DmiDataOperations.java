/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.operations;

import static org.onap.cps.ncmp.api.impl.operations.DmiOperations.DataStoreEnum.PASSTHROUGH_RUNNING;
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum;
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.READ;
import static org.onap.cps.ncmp.api.impl.operations.RequiredDmiService.DATA;

import java.util.UUID;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration;
import org.onap.cps.ncmp.api.models.DmiResponse;
import org.onap.cps.ncmp.api.models.PersistenceCmHandle;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

/**
 * Operations class for DMI data.
 */
@Component
public class DmiDataOperations extends DmiOperations {

    /**
     * Constructor for {@code DmiOperations}. This method also manipulates url properties.
     *
     * @param dmiRestClient {@code DmiRestClient}
     */
    public DmiDataOperations(final PersistenceCmHandleRetriever cmHandlePropertiesRetriever,
                             final JsonObjectMapper jsonObjectMapper,
                             final NcmpConfiguration.DmiProperties dmiProperties,
                             final DmiRestClient dmiRestClient) {
        super(cmHandlePropertiesRetriever, jsonObjectMapper, dmiProperties, dmiRestClient);
    }

    /**
     * This method fetches the resource data from operational data store for given cm handle
     * identifier on given resource using dmi client.
     *
     * @param cmHandle    network resource identifier
     * @param resourceId  resource identifier
     * @param optionsParamInQuery options query
     * @param acceptParamInHeader accept parameter
     * @param dataStore  data store enum
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<Object> getResourceDataFromDmi(final String cmHandle,
                                                          final String resourceId,
                                                          final String optionsParamInQuery,
                                                          final String acceptParamInHeader,
                                                          final DataStoreEnum dataStore,
                                                         final String topicParamInQuery) {
        final PersistenceCmHandle persistenceCmHandle =
            cmHandlePropertiesRetriever.retrieveCmHandleDmiServiceNameAndDmiProperties(cmHandle);
        final DmiRequestBody dmiRequestBody = DmiRequestBody.builder()
            .operation(READ).requestId(UUID.randomUUID().toString())
            .build();
        dmiRequestBody.asDmiProperties(persistenceCmHandle.getDmiProperties());
        final String jsonBody = jsonObjectMapper.asJsonString(dmiRequestBody);

        final var dmiResourceDataUrl = getDmiDatastoreUrlWithOptions(
            persistenceCmHandle.resolveDmiServiceName(DATA), cmHandle, resourceId,
            optionsParamInQuery, dataStore, topicParamInQuery);
        final var httpHeaders = prepareHeader(acceptParamInHeader);
        dmiRestClient.postOperationWithJsonData(dmiResourceDataUrl, jsonBody, httpHeaders);
        return ResponseEntity.status(HttpStatus.OK).body(buildDmiResponse(topicParamInQuery, dmiRequestBody));
    }

    private DmiResponse buildDmiResponse(final String topicParamInQuery, final DmiRequestBody dmiRequestBody) {
        final DmiResponse dmiResponse = new DmiResponse();
        dmiResponse.setRequestId(dmiRequestBody.getRequestId());
        dmiResponse.setTopic(topicParamInQuery);
        return dmiResponse;
    }

    /**
     * This method creates the resource data from pass-through running data store for given cm handle
     * identifier on given resource using dmi client.
     *
     * @param cmHandle    network resource identifier
     * @param resourceId  resource identifier
     * @param operation   operation enum
     * @param requestData the request data
     * @param dataType    data type
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<Object> writeResourceDataPassThroughRunningFromDmi(final String cmHandle,
                                                                             final String resourceId,
                                                                             final OperationEnum operation,
                                                                             final String requestData,
                                                                             final String dataType) {
        final PersistenceCmHandle persistenceCmHandle =
            cmHandlePropertiesRetriever.retrieveCmHandleDmiServiceNameAndDmiProperties(cmHandle);
        final DmiRequestBody dmiRequestBody = DmiRequestBody.builder()
            .operation(operation)
            .data(requestData)
            .dataType(dataType)
            .build();
        dmiRequestBody.asDmiProperties(persistenceCmHandle.getDmiProperties());
        final String jsonBody = jsonObjectMapper.asJsonString(dmiRequestBody);
        final String dmiUrl =
            getResourceInDataStoreUrl(persistenceCmHandle.resolveDmiServiceName(DATA),
            cmHandle, resourceId, PASSTHROUGH_RUNNING);
        return dmiRestClient.postOperationWithJsonData(dmiUrl, jsonBody, new HttpHeaders());
    }

    private String getResourceInDataStoreUrl(final String dmiServiceName,
                                             final String cmHandle,
                                             final String resourceId,
                                             final DataStoreEnum dataStoreEnum) {
        return getCmHandleUrl(dmiServiceName, cmHandle)
            + "data"
            + URL_SEPARATOR
            + "ds"
            + URL_SEPARATOR
            + dataStoreEnum.getValue()
            + "?resourceIdentifier="
            + resourceId;
    }

    private String getDmiDatastoreUrlWithOptions(final String dmiServiceName,
                                                 final String cmHandle,
                                                 final String resourceId,
                                                 final String optionsParamInQuery,
                                                 final DataStoreEnum dataStoreEnum,
                                                 final String topicParamInQuery) {
        String resourceInDataStoreUrl = getResourceInDataStoreUrl(dmiServiceName,
                cmHandle, resourceId, dataStoreEnum);
        resourceInDataStoreUrl = appendOptionsQuery(resourceInDataStoreUrl, optionsParamInQuery);
        return appendTopicQuery(resourceInDataStoreUrl, topicParamInQuery);
    }

}
