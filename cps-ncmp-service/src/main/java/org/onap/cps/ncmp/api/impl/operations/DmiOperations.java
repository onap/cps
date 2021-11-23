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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.exception.NcmpException;
import org.springframework.http.HttpHeaders;

@Slf4j
public class DmiOperations {

    @Getter
    public enum DataStoreEnum {
        PASSTHROUGH_OPERATIONAL("ncmp-datastore:passthrough-operational"),
        PASSTHROUGH_RUNNING("ncmp-datastore:passthrough-running");
        private String value;

        DataStoreEnum(final String value) {
            this.value = value;
        }
    }

    protected ObjectMapper objectMapper;
    protected PersistenceCmHandleRetriever cmHandlePropertiesRetriever;
    protected DmiRestClient dmiRestClient;

    static final String URL_SEPARATOR = "/";
    static final String DMI_API_PATH = "/dmi";
    static final String DMI_CM_HANDLE_PATH = "/v1/ch/{cmHandle}";

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

    static String getDmiResourceUrl(final String dmiServiceName,
                                            final String cmHandle,
                                            final String resourceName) {
        final var stringBuilder = new StringBuilder(dmiServiceName);
        stringBuilder.append(DMI_API_PATH);
        stringBuilder.append(DMI_CM_HANDLE_PATH.replace("{cmHandle}", cmHandle));
        stringBuilder.append(URL_SEPARATOR + resourceName);
        return stringBuilder.toString();
    }

    static String appendOptionsQuery(final String url,
                                   final String optionsParamInQuery) {
        if (Strings.isNullOrEmpty(optionsParamInQuery)) {
            return url;
        }
        return url + "&options=" + optionsParamInQuery;
    }

    static HttpHeaders prepareHeader(final String acceptParam) {
        final var httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.ACCEPT, acceptParam);
        return httpHeaders;
    }

    /**
     * Convert DmiRequestBody to JSON.
     *
     * @param dmiRequestBody the dmi request body
     * @return DmiRequestBody as JSON
     */
    String getDmiRequestBodyAsString(final DmiRequestBody dmiRequestBody) {
        try {
            return objectMapper.writeValueAsString(dmiRequestBody);
        } catch (final JsonProcessingException e) {
            log.error("Parsing error occurred while converting Object to JSON.");
            throw new NcmpException("Parsing error occurred while converting given object to JSON.",
                e.getMessage());
        }
    }

}
