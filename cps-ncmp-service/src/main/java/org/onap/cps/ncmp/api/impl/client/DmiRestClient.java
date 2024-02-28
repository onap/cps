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

package org.onap.cps.ncmp.api.impl.client;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration.DmiProperties;
import org.onap.cps.ncmp.api.impl.exception.HttpClientRequestException;
import org.onap.cps.ncmp.api.impl.operations.OperationType;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class DmiRestClient {

    private static final String HEALTH_CHECK_URL_EXTENSION = "/actuator/health";
    private static final String NOT_SPECIFIED = "";
    private final RestTemplate restTemplate;
    private final DmiProperties dmiProperties;

    /**
     * Sends POST operation to DMI with json body containing module references.
     *
     * @param dmiResourceUrl          dmi resource url
     * @param requestBodyAsJsonString json data body
     * @param operationType           the type of operation being executed (for error reporting only)
     * @param authorization           contents of Authorization header, or null if not present
     * @return response entity of type String
     */
    public ResponseEntity<Object> postOperationWithJsonData(final String dmiResourceUrl,
                                                            final String requestBodyAsJsonString,
                                                            final OperationType operationType,
                                                            final String authorization) {
        final var httpEntity = new HttpEntity<>(requestBodyAsJsonString, configureHttpHeaders(new HttpHeaders(),
                authorization));
        try {
            return restTemplate.postForEntity(dmiResourceUrl, httpEntity, Object.class);
        } catch (final HttpStatusCodeException httpStatusCodeException) {
            final String exceptionMessage = "Unable to " + operationType.toString() + " resource data.";
            throw new HttpClientRequestException(exceptionMessage, httpStatusCodeException.getResponseBodyAsString(),
                httpStatusCodeException.getStatusCode().value());
        }
    }

    /**
     * Get DMI plugin health status.
     *
     * @param       dmiPluginBaseUrl the base URL of the dmi-plugin
     * @return      plugin health status ("UP" is all OK, "" (not-specified) in case of any exception)
     */
    public String getDmiHealthStatus(final String dmiPluginBaseUrl) {
        final HttpEntity<Object> httpHeaders = new HttpEntity<>(configureHttpHeaders(new HttpHeaders(), null));
        try {
            final JsonNode responseHealthStatus =
                restTemplate.getForObject(dmiPluginBaseUrl + HEALTH_CHECK_URL_EXTENSION,
                    JsonNode.class, httpHeaders);
            return responseHealthStatus == null ? NOT_SPECIFIED :
                responseHealthStatus.get("status").asText();
        } catch (final Exception e) {
            log.warn("Failed to retrieve health status from {}. Error Message: {}", dmiPluginBaseUrl, e.getMessage());
            return NOT_SPECIFIED;
        }
    }

    private HttpHeaders configureHttpHeaders(final HttpHeaders httpHeaders, final String authorization) {
        if (dmiProperties.isDmiBasicAuthEnabled()) {
            httpHeaders.setBasicAuth(dmiProperties.getAuthUsername(), dmiProperties.getAuthPassword());
        } else if (authorization != null && authorization.toLowerCase(Locale.getDefault()).startsWith("bearer ")) {
            httpHeaders.add(HttpHeaders.AUTHORIZATION, authorization);
        }
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return httpHeaders;
    }
}
