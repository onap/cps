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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.apache.logging.log4j.util.Strings;
import org.apache.logging.log4j.util.TriConsumer;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration;
import org.onap.cps.ncmp.api.models.DmiResponse;
import org.onap.cps.ncmp.api.models.PersistenceCmHandle;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

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
                .operation(READ)
                .requestId(isValidTopicName(topicParamInQuery) ? UUID.randomUUID().toString() : null)
                .build();
        dmiRequestBody.asDmiProperties(persistenceCmHandle.getDmiProperties());
        final String jsonBody = jsonObjectMapper.asJsonString(dmiRequestBody);

        final var dmiResourceDataUrl = getDmiDatastoreUrl(populateQueryParams(resourceId, optionsParamInQuery,
                topicParamInQuery), populateUriVariables(persistenceCmHandle, cmHandle, dataStore));
        final var httpHeaders = prepareHeader(acceptParamInHeader);
        final ResponseEntity<Object> responseEntity = dmiRestClient.postOperationWithJsonData(dmiResourceDataUrl,
                jsonBody, httpHeaders);
        if (isValidTopicName(topicParamInQuery)) {
            return ResponseEntity.status(responseEntity.getStatusCode()).body(buildDmiResponse(dmiRequestBody));
        }
        return responseEntity;
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
                getDmiDatastoreUrl(populateQueryParams(resourceId, null, null),
                        populateUriVariables(persistenceCmHandle, cmHandle, PASSTHROUGH_RUNNING));
        return dmiRestClient.postOperationWithJsonData(dmiUrl, jsonBody, new HttpHeaders());
    }

    private String getDmiDatastoreUrl(final MultiValueMap<String, String> queryParams,
                                             final Map<String, Object> uriVariables) {
        final UriComponentsBuilder uriComponentsBuilder = getCmHandleUrl()
                .pathSegment("data")
                .pathSegment("ds")
                .pathSegment("{dataStore}")
                .queryParams(queryParams)
                .uriVariables(uriVariables);
        return uriComponentsBuilder.buildAndExpand().toUriString();
    }

    private Map<String, Object> populateUriVariables(final PersistenceCmHandle persistenceCmHandle,
                                                     final String cmHandle,
                                                     final DataStoreEnum dataStore) {
        return new HashMap<>() {
            {
                put("dmiServiceName", persistenceCmHandle.resolveDmiServiceName(DATA));
                put("dmiBasePath", dmiProperties.getDmiBasePath());
                put("cmHandle", cmHandle);
                put("dataStore", dataStore.getValue());
            }
        };
    }

    private MultiValueMap<String, String> populateQueryParams(final String resourceId,
                                                              final String optionsParamInQuery,
                                                              final String topicParamInQuery) {
        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        getQueryParamConsumer().accept("resourceIdentifier", resourceId, queryParams);
        getQueryParamConsumer().accept("options", optionsParamInQuery, queryParams);
        if (isValidTopicName(topicParamInQuery)) {
            getQueryParamConsumer().accept("topic", topicParamInQuery, queryParams);
        }
        return queryParams;
    }

    private TriConsumer<String, String, MultiValueMap<String, String>> getQueryParamConsumer() {
        return (paramName, paramValue, paramMap) -> {
            if (Strings.isNotEmpty(paramValue)) {
                paramMap.add(paramName, paramValue);
            }
        };
    }

    private DmiResponse buildDmiResponse(final DmiRequestBody dmiRequestBody) {
        final DmiResponse dmiResponse = new DmiResponse();
        dmiResponse.setRequestId(dmiRequestBody.getRequestId());
        return dmiResponse;
    }

    private static boolean isValidTopicName(final String topicName) {
        return Strings.isNotEmpty(topicName) && topicPattern.matcher(topicName).matches();
    }

}
