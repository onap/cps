/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
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
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.operations;

import static org.onap.cps.ncmp.api.impl.operations.DmiOperations.DataStoreEnum.PASSTHROUGH_RUNNING;
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum;
import static org.onap.cps.ncmp.api.impl.operations.DmiRequestBody.OperationEnum.READ;

import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration;
import org.onap.cps.ncmp.api.impl.utils.DmiServiceUrlBuilder;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.HttpHeaders;
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
    public DmiDataOperations(final YangModelCmHandleRetriever cmHandlePropertiesRetriever,
                             final JsonObjectMapper jsonObjectMapper,
                             final NcmpConfiguration.DmiProperties dmiProperties,
                             final DmiRestClient dmiRestClient, final DmiServiceUrlBuilder dmiServiceUrlBuilder) {
        super(cmHandlePropertiesRetriever, jsonObjectMapper, dmiProperties, dmiRestClient, dmiServiceUrlBuilder);
    }

    /**
     * This method fetches the resource data from operational data store for given cm handle
     * identifier on given resource using dmi client.
     *
     * @param cmHandleId    network resource identifier
     * @param resourceId  resource identifier
     * @param optionsParamInQuery options query
     * @param dataStore           data store enum
     * @param requestId           requestId for async responses
     * @param topicParamInQuery   topic name for (triggering) async responses
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<Object> getResourceDataFromDmi(final String cmHandleId,
                                                         final String resourceId,
                                                         final String optionsParamInQuery,
                                                         final DataStoreEnum dataStore,
                                                         final String requestId,
                                                         final String topicParamInQuery) {
        final YangModelCmHandle yangModelCmHandle =
                yangModelCmHandleRetriever.getDmiServiceNamesAndProperties(cmHandleId);
        final DmiRequestBody dmiRequestBody = DmiRequestBody.builder()
            .operation(READ)
            .requestId(requestId)
            .build();
        dmiRequestBody.asDmiProperties(yangModelCmHandle.getDmiProperties());
        final String jsonBody = jsonObjectMapper.asJsonString(dmiRequestBody);
        final var dmiResourceDataUrl = dmiServiceUrlBuilder.getDmiDatastoreUrl(
                dmiServiceUrlBuilder.populateQueryParams(resourceId, optionsParamInQuery,
                topicParamInQuery), dmiServiceUrlBuilder.populateUriVariables(
                        yangModelCmHandle, cmHandleId, dataStore));
        return dmiRestClient.postOperationWithJsonData(dmiResourceDataUrl, jsonBody);
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
        final YangModelCmHandle yangModelCmHandle =
            yangModelCmHandleRetriever.getDmiServiceNamesAndProperties(cmHandleId);
        final DmiRequestBody dmiRequestBody = DmiRequestBody.builder()
            .operation(operation)
            .data(requestData)
            .dataType(dataType)
            .build();
        dmiRequestBody.asDmiProperties(yangModelCmHandle.getDmiProperties());
        final String jsonBody = jsonObjectMapper.asJsonString(dmiRequestBody);
        final String dmiUrl =
                dmiServiceUrlBuilder.getDmiDatastoreUrl(dmiServiceUrlBuilder.populateQueryParams(resourceId,
                                null, null),
                        dmiServiceUrlBuilder.populateUriVariables(yangModelCmHandle, cmHandleId, PASSTHROUGH_RUNNING));
        return dmiRestClient.postOperationWithJsonData(dmiUrl, jsonBody);
    }

}
