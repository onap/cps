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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.config.WebClientConfiguration.DmiProperties;
import org.onap.cps.ncmp.api.impl.exception.HttpClientRequestException;
import org.onap.cps.ncmp.api.impl.operations.OperationType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class DmiRestClient {

    private static final String HEALTH_CHECK_URL_EXTENSION = "/actuator/health";
    private static final String NOT_SPECIFIED = "";
    private final WebClient webClient;
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
        try {
            // create an async version of this call
            return webClient.post().uri(new URI(dmiResourceUrl))
                    .headers(httpHeaders -> configureHttpHeaders(httpHeaders, authorization))
                    .body(BodyInserters.fromValue(requestBodyAsJsonString))
                    .retrieve()
                    .toEntity(Object.class)
                    .block();
        } catch (final HttpStatusCodeException httpStatusCodeException) {
            final String exceptionMessage = "Unable to " + operationType.toString() + " resource data.";
            throw new HttpClientRequestException(exceptionMessage, httpStatusCodeException.getResponseBodyAsString(),
                httpStatusCodeException.getStatusCode().value());
        } catch (final URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Get DMI plugin health status.
     *
     * @param       dmiPluginBaseUrl the base URL of the dmi-plugin
     * @return      plugin health status ("UP" is all OK, "" (not-specified) in case of any exception)
     */
    public String getDmiHealthStatus(final String dmiPluginBaseUrl) {
        try {
            final JsonNode responseHealthStatus = webClient.get()
                    .uri(new URI(dmiPluginBaseUrl + HEALTH_CHECK_URL_EXTENSION))
                    .headers(httpHeaders -> configureHttpHeaders(httpHeaders, null))
                    .retrieve()
                    .bodyToMono(JsonNode.class).block();

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
        return httpHeaders;
    }
}
