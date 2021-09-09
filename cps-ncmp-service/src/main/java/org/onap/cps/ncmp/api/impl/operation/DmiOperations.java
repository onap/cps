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

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

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

    private DmiRestClient dmiRestClient;
    private static final String DMI_API_PATH = "/dmi";
    private static final String DMI_CM_HANDLE_PATH = "/v1/ch/{cmHandle}";
    private static final String DMI_CM_HANDLE_DATASTORE_PATH = DMI_CM_HANDLE_PATH + "/data/ds";
    private static final String URL_SEPARATOR = "/";

    /**
     * Constructor for {@code DmiOperations}. This method also manipulates url properties.
     *
     * @param dmiRestClient {@code DmiRestClient}
     */
    public DmiOperations(final DmiRestClient dmiRestClient) {
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
    public ResponseEntity<String> getResourceFromDmiWithJsonData(final String dmiServiceName,
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
     * @param dmiServiceName dmi service name
     * @param cmHandle    network resource identifier
     * @param resourceId  resource identifier
     * @param fieldsQuery fields query
     * @param depthQuery  depth query
     * @param acceptParam accept parameter
     * @param jsonBody    json body for put operation
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<Object> getResourceDataOperationalFromDmi(final String dmiServiceName,
                                                                    final String cmHandle,
                                                                    final String resourceId,
                                                                    final String fieldsQuery,
                                                                    final Integer depthQuery,
                                                                    final String acceptParam,
                                                                    final String jsonBody) {
        final var dmiResourceDataUrl = getDmiDatastoreUrl(dmiServiceName, cmHandle, resourceId,
            fieldsQuery, depthQuery, DataStoreEnum.PASSTHROUGH_OPERATIONAL);
        final var httpHeaders = prepareHeader(acceptParam);
        return dmiRestClient.putOperationWithJsonData(dmiResourceDataUrl, jsonBody, httpHeaders);
    }

    /**
     * This method fetches the resource data from pass-through running data store for given cm handle
     * identifier on given resource using dmi client.
     *
     * @param dmiServiceName dmi service name
     * @param cmHandle    network resource identifier
     * @param resourceId  resource identifier
     * @param fieldsQuery fields query
     * @param depthQuery  depth query
     * @param acceptParam accept parameter
     * @param jsonBody    json body for put operation
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<Object> getResourceDataPassThroughRunningFromDmi(final String dmiServiceName,
                                                                           final String cmHandle,
                                                                           final String resourceId,
                                                                           final String fieldsQuery,
                                                                           final Integer depthQuery,
                                                                           final String acceptParam,
                                                                           final String jsonBody) {
        final var dmiResourceDataUrl = getDmiDatastoreUrl(dmiServiceName, cmHandle, resourceId,
            fieldsQuery, depthQuery, DataStoreEnum.PASSTHROUGH_RUNNING);
        final var httpHeaders = prepareHeader(acceptParam);
        return dmiRestClient.putOperationWithJsonData(dmiResourceDataUrl, jsonBody, httpHeaders);
    }

    /**
     * This method creates the resource data from pass-through running data store for given cm handle
     * identifier on given resource using dmi client.
     *
     * @param dmiServiceName dmi service name
     * @param cmHandle    network resource identifier
     * @param resourceId  resource identifier
     * @param jsonBody    json body for put operation
     * @return {@code ResponseEntity} response entity
     */
    public ResponseEntity<String> createResourceDataPassThroughRunningFromDmi(final String dmiServiceName,
                                                                            final String cmHandle,
                                                                            final String resourceId,
                                                                            final String jsonBody) {
        final var stringBuilder = getStringBuilderForPassThroughRunningUrl(dmiServiceName,
            cmHandle, resourceId, DataStoreEnum.PASSTHROUGH_RUNNING);
        return dmiRestClient.postOperationWithJsonData(stringBuilder.toString(), jsonBody, new HttpHeaders());
    }

    @NotNull
    private String getDmiResourceUrl(final String dmiServiceName,
                                     final String cmHandle,
                                     final String resourceName) {
        final var stringBuilder = new StringBuilder(dmiServiceName);
        stringBuilder.append(DMI_API_PATH);
        stringBuilder.append(DMI_CM_HANDLE_PATH.replace("{cmHandle}", cmHandle));
        stringBuilder.append(URL_SEPARATOR + resourceName);
        return stringBuilder.toString();
    }

    @NotNull
    private String getDmiDatastoreUrl(final String dmiServiceName,
                                      final String cmHandle,
                                      final String resourceId,
                                      final String fieldsQuery,
                                      final Integer depthQuery,
                                      final DataStoreEnum dataStoreEnum) {
        final var stringBuilder = getStringBuilderForPassThroughRunningUrl(dmiServiceName,
            cmHandle, resourceId, dataStoreEnum);
        appendFieldsAndDepth(stringBuilder, fieldsQuery, depthQuery);
        return stringBuilder.toString();
    }

    @NotNull
    private StringBuilder getStringBuilderForPassThroughRunningUrl(final String dmiServiceName,
                                                                   final String cmHandle,
                                                                   final String resourceId,
                                                                   final DataStoreEnum dataStoreEnum) {
        final var stringBuilder = new StringBuilder(dmiServiceName);
        stringBuilder.append(DMI_API_PATH);
        stringBuilder.append(DMI_CM_HANDLE_DATASTORE_PATH.replace("{cmHandle}", cmHandle));
        stringBuilder.append(URL_SEPARATOR + dataStoreEnum.getValue());
        stringBuilder.append(URL_SEPARATOR + resourceId);
        return stringBuilder;
    }

    private void appendFieldsAndDepth(final StringBuilder stringBuilder,
                                      final String fieldsQuery,
                                      final Integer depthQuery) {
        final var doesFieldExists = (fieldsQuery != null && !fieldsQuery.isEmpty());
        if (doesFieldExists) {
            stringBuilder.append("?").append("fields=").append(fieldsQuery);
        }
        if (depthQuery != null) {
            if (doesFieldExists) {
                stringBuilder.append("&");
            } else {
                stringBuilder.append("?");
            }
            stringBuilder.append("depth=").append(depthQuery);
        }
    }

    private HttpHeaders prepareHeader(final String acceptParam) {
        final var httpHeaders = new HttpHeaders();
        if (acceptParam != null && !acceptParam.isEmpty()) {
            httpHeaders.set(HttpHeaders.ACCEPT, acceptParam);
        }
        return httpHeaders;
    }
}
