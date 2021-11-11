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

package org.onap.cps.ncmp.api.impl.operation;

import static org.onap.cps.ncmp.api.impl.operation.RequiredDmiService.DATA;
import static org.onap.cps.ncmp.api.impl.operation.RequiredDmiService.MODEL;
import static org.onap.cps.ncmp.api.models.GenericRequestBody.OperationEnum.READ;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.exception.NcmpException;
import org.onap.cps.ncmp.api.models.GenericRequestBody;
import org.onap.cps.ncmp.api.models.PersistenceCmHandle;
import org.onap.cps.spi.model.ModuleReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DmiOperations {
    @Getter
    public enum DataStoreEnum {
        PASSTHROUGH_OPERATIONAL("ncmp-datastore:passthrough-operational"),
        PASSTHROUGH_RUNNING("ncmp-datastore:passthrough-running");
        private String value;

        DataStoreEnum(final String value) {
            this.value = value;
        }

        @Override
        @JsonValue
        public String toString() {
            return value;
        }
    }

    private PersistenceCmHandleRetriever cmHandlePropertiesRetriever;
    private ObjectMapper objectMapper;
    private DmiRestClient dmiRestClient;

    private static final String DMI_API_PATH = "/dmi";
    private static final String DMI_CM_HANDLE_PATH = "/v1/ch/{cmHandle}";
    private static final String DMI_CM_HANDLE_DATASTORE_PATH = DMI_CM_HANDLE_PATH + "/data/ds";
    private static final String URL_SEPARATOR = "/";
    private static final String RESOURCE_IDENTIFIER = "resourceIdentifier";
    private static final String OPTIONS_QUERY_KEY = "options";


    /**
     * Constructor for {@code DmiOperations}. This method also manipulates url properties.
     *
     * @param dmiRestClient {@code DmiRestClient}
     */
    public DmiOperations(final PersistenceCmHandleRetriever cmHandlePropertiesRetriever,
                         final ObjectMapper objectMapper,
                         final DmiRestClient dmiRestClient) {
        this.cmHandlePropertiesRetriever = cmHandlePropertiesRetriever;
        this.objectMapper = objectMapper;
        this.dmiRestClient = dmiRestClient;
    }

    /**
     * Get resources from DMI.
     *
     * @param dmiServiceName dmi service name
     * @param cmHandle cmHandle
     * @param resourceName name of the resource(s)
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<String> getResourceFromDmi(final String dmiServiceName,
                                                     final String cmHandle,
                                                     final String resourceName) {
        final var dmiResourceDataUrl = getDmiResourceUrl(dmiServiceName, cmHandle, resourceName);
        final var httpHeaders = new HttpHeaders();
        return dmiRestClient.postOperation(dmiResourceDataUrl, httpHeaders);
    }



    /**
     * Get resources from DMI for modules.
     *
     * @param dmiServiceName dmi service name
     * @param jsonData module names and revisions as JSON
     * @param cmHandle cmHandle
     * @param resourceName name of the resource(s)
     * @return {@code ResponseEntity} response entity
     */
    private ResponseEntity<String> getResourceFromDmiWithJsonData(final String dmiServiceName,
                                                               final String jsonData,
                                                               final String cmHandle,
                                                               final String resourceName) {
        final String dmiResourceDataUrl = getDmiResourceUrl(dmiServiceName, cmHandle, resourceName);
        return dmiRestClient.postOperationWithJsonData(dmiResourceDataUrl, jsonData, new HttpHeaders());
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

        final var dmiResourceDataUrl = getDmiDatastoreUrl(
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
        final StringBuilder stringBuilder =
            getStringBuilderForPassThroughUrl(persistenceCmHandle.resolveDmiServiceName(DATA),
            cmHandle, resourceId, DataStoreEnum.PASSTHROUGH_RUNNING);
        return dmiRestClient.postOperationWithJsonData(stringBuilder.toString(), jsonBody, new HttpHeaders());
    }

    private static String getDmiResourceUrl(final String dmiServiceName,
                                            final String cmHandle,
                                            final String resourceName) {
        final var stringBuilder = new StringBuilder(dmiServiceName);
        stringBuilder.append(DMI_API_PATH);
        stringBuilder.append(DMI_CM_HANDLE_PATH.replace("{cmHandle}", cmHandle));
        stringBuilder.append(URL_SEPARATOR + resourceName);
        return stringBuilder.toString();
    }


    private static String getDmiDatastoreUrl(final String dmiServiceName,
                                             final String cmHandle,
                                             final String resourceId,
                                             final String optionsParamInQuery,
                                             final DataStoreEnum dataStoreEnum) {
        final var stringBuilder = getStringBuilderForPassThroughUrl(dmiServiceName,
            cmHandle, resourceId, dataStoreEnum);
        appendOptionsQuery(stringBuilder, optionsParamInQuery);
        return stringBuilder.toString();
    }

    private static StringBuilder getStringBuilderForPassThroughUrl(final String dmiServiceName,
                                                                   final String cmHandle,
                                                                   final String resourceId,
                                                                   final DataStoreEnum dataStoreEnum) {
        final var stringBuilder = new StringBuilder(dmiServiceName);
        stringBuilder.append(DMI_API_PATH);
        stringBuilder.append(DMI_CM_HANDLE_DATASTORE_PATH.replace("{cmHandle}", cmHandle));
        stringBuilder.append(URL_SEPARATOR + dataStoreEnum.getValue());
        stringBuilder.append("?" + RESOURCE_IDENTIFIER + "=" + resourceId);
        return stringBuilder;
    }

    private static void appendOptionsQuery(final StringBuilder stringBuilder,
                                           final String optionsParamInQuery) {
        if (optionsParamInQuery != null) {
            stringBuilder.append("&").append(OPTIONS_QUERY_KEY).append("=").append(optionsParamInQuery);
        }
    }

    private static HttpHeaders prepareHeader(final String acceptParam) {
        final var httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.ACCEPT, acceptParam);
        return httpHeaders;
    }

    private String toBodyAsString(GenericRequestBody genericRequestBody) {
        try {
            return objectMapper.writeValueAsString(genericRequestBody);
        } catch (final JsonProcessingException e) {
            log.error("Parsing error occurred while converting Object to JSON.");
            throw new NcmpException("Parsing error occurred while converting given object to JSON.",
                e.getMessage());
        }
    }

    /**********************************************************************************************************
     *    M O D E L - O P E R A T I O N S
     **********************************************************************************************************/
    public ResponseEntity<String> getModuleReferences(final PersistenceCmHandle persistenceCmHandle) {
        final GenericRequestBody genericRequestBody = GenericRequestBody.builder()
            .build();
        genericRequestBody.toCmHandleProperties(persistenceCmHandle.getAdditionalProperties());
        return getResourceFromDmiWithJsonData(persistenceCmHandle.resolveDmiServiceName(MODEL),
            toBodyAsString(genericRequestBody), persistenceCmHandle.getId(), "modules");
    }

    public ResponseEntity<String>  getNewYangResourcesFromDmi(final PersistenceCmHandle persistenceCmHandle,
                                                              final List<ModuleReference> unknownModuleReferences) {
        final String jsonDataWithDataAndCmHandleProperties = getRequestBodyToFetchYangResources(
            unknownModuleReferences, persistenceCmHandle.getAdditionalProperties());
        return getResourceFromDmiWithJsonData(
            persistenceCmHandle.resolveDmiServiceName(MODEL),
            jsonDataWithDataAndCmHandleProperties,
            persistenceCmHandle.getId(),
            "moduleResources");
    }

    private static String getRequestBodyToFetchYangResources(final List<ModuleReference> unknownModuleReferences,
                                                             final List<PersistenceCmHandle.AdditionalProperty> cmHandleProperties) {
        final JsonArray moduleReferencesAsJson = getModuleReferencesAsJson(unknownModuleReferences);
        final JsonObject data = new JsonObject();
        data.add("modules", moduleReferencesAsJson);
        final JsonObject jsonRequestObject = new JsonObject();
        jsonRequestObject.add("data", data);
        final Gson gson = new Gson();
        jsonRequestObject.add("cmHandleProperties", toJson(cmHandleProperties));
        return jsonRequestObject.toString();
    }

    private static JsonArray getModuleReferencesAsJson(final List<ModuleReference> unknownModuleReferences) {
        final JsonArray moduleReferences = new JsonArray();

        for (final ModuleReference moduleReference : unknownModuleReferences) {
            final JsonObject moduleReferenceAsJson = new JsonObject();
            moduleReferenceAsJson.addProperty("name", moduleReference.getModuleName());
            moduleReferenceAsJson.addProperty("revision", moduleReference.getRevision());
            moduleReferences.add(moduleReferenceAsJson);
        }
        return moduleReferences;
    }

    private static JsonObject toJson(final List<PersistenceCmHandle.AdditionalProperty> cmHandleProperties) {
        final JsonObject asJsonObject = new JsonObject();
        for (final PersistenceCmHandle.AdditionalProperty additionalProperty : cmHandleProperties) {
            asJsonObject.addProperty(additionalProperty.getName(), additionalProperty.getValue());
        }
        return asJsonObject;
    }


}
