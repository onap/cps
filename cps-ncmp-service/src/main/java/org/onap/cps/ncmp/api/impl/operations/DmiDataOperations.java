/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Nordix Foundation
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

import static org.onap.cps.ncmp.api.impl.operations.RequiredDmiService.DATA;
import static org.onap.cps.ncmp.api.models.GenericRequestBody.OperationEnum.READ;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.models.GenericRequestBody;
import org.onap.cps.ncmp.api.models.PersistenceCmHandle;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DmiDataOperations extends DmiOperations {

    private static final String DMI_CM_HANDLE_DATASTORE_PATH = DMI_CM_HANDLE_PATH + "/data/ds";

    /**
     * Constructor for {@code DmiOperations}. This method also manipulates url properties.
     *
     * @param dmiRestClient {@code DmiRestClient}
     */
    public DmiDataOperations(final PersistenceCmHandleRetriever cmHandlePropertiesRetriever,
                             final ObjectMapper objectMapper,
                             final DmiRestClient dmiRestClient) {
        super(cmHandlePropertiesRetriever, objectMapper, dmiRestClient);
    }

    /**
     * This method fetches the resource data from operational data store for given cm handle
     * identifier on given resource using dmi client.
     *
     * @param cmHandle    network resource identifier
     * @param resourceId  resource identifier
     * @param optionsParamInQuery options query
     * @param acceptParamInHeader accept parameter
     * @param  dataStore
     * @return {@code ResponseEntity} response entity
     */

    public ResponseEntity<Object> getResourceDataFromDmi(final String cmHandle,
                                                          final String resourceId,
                                                          final String optionsParamInQuery,
                                                          final String acceptParamInHeader,
                                                          final DataStoreEnum dataStore) {
        PersistenceCmHandle persistenceCmHandle =
            cmHandlePropertiesRetriever.retrieveCmHandleDmiServiceNameAndProperties(cmHandle);
        GenericRequestBody genericRequestBody = GenericRequestBody.builder()
            .operation(READ)
            .build();
        genericRequestBody.toCmHandleProperties(persistenceCmHandle.getAdditionalProperties());
        final String jsonBody = toBodyAsString(genericRequestBody);

        final var dmiResourceDataUrl = getDmiDatastoreUrlWithOptions(
            persistenceCmHandle.resolveDmiServiceName(DATA), cmHandle, resourceId,
            optionsParamInQuery, dataStore);
        final var httpHeaders = prepareHeader(acceptParamInHeader);
        return dmiRestClient.putOperationWithJsonData(dmiResourceDataUrl, jsonBody, httpHeaders);
    }

    /**
     * This method creates the resource data from pass-through running data store for given cm handle
     * identifier on given resource using dmi client.
     *
     * @param cmHandle    network resource identifier
     * @param resourceId  resource identifier
     * @param requestData
     * @param dataType
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<String> createResourceDataPassThroughRunningFromDmi(final String cmHandle,
                                                                              final String resourceId,
                                                                              final String requestData,
                                                                              final String dataType) {
        PersistenceCmHandle persistenceCmHandle =
            cmHandlePropertiesRetriever.retrieveCmHandleDmiServiceNameAndProperties(cmHandle);
        GenericRequestBody genericRequestBody = GenericRequestBody.builder()
            .operation(READ)
            .data(requestData)
            .dataType(dataType)
            .build();
        genericRequestBody.toCmHandleProperties(persistenceCmHandle.getAdditionalProperties());
        final String jsonBody = toBodyAsString(genericRequestBody);
        final String resourceIdentifierUrl =
            getResourceIdentifierUrl(persistenceCmHandle.resolveDmiServiceName(DATA),
            cmHandle, resourceId, DataStoreEnum.PASSTHROUGH_RUNNING);
        return dmiRestClient.postOperationWithJsonData(resourceIdentifierUrl, jsonBody, new HttpHeaders());
    }

    private static String getResourceIdentifierUrl(final String dmiServiceName,
                                                   final String cmHandle,
                                                   final String resourceId,
                                                   final DataStoreEnum dataStoreEnum) {
        final var stringBuilder = new StringBuilder(dmiServiceName);
        stringBuilder.append(DMI_API_PATH);
        stringBuilder.append(DMI_CM_HANDLE_DATASTORE_PATH.replace("{cmHandle}", cmHandle));
        stringBuilder.append(URL_SEPARATOR + dataStoreEnum.getValue());
        stringBuilder.append("?resourceIdentifier=" + resourceId);
        return stringBuilder.toString();
    }

    private static String getDmiDatastoreUrlWithOptions(final String dmiServiceName,
                                                        final String cmHandle,
                                                        final String resourceId,
                                                        final String optionsParamInQuery,
                                                        final DataStoreEnum dataStoreEnum) {
        final String resourceIdentifierUrl = getResourceIdentifierUrl(dmiServiceName,
            cmHandle, resourceId, dataStoreEnum);
        appendOptionsQuery(resourceIdentifierUrl, optionsParamInQuery);
        return resourceIdentifierUrl;
    }


}
