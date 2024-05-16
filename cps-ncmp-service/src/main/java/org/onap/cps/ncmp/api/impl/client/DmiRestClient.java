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

package org.onap.cps.ncmp.api.impl.client;

import static org.onap.cps.ncmp.api.NcmpResponseStatus.DMI_SERVICE_NOT_RESPONDING;
import static org.onap.cps.ncmp.api.NcmpResponseStatus.UNABLE_TO_READ_RESOURCE_DATA;
import static org.onap.cps.ncmp.api.NcmpResponseStatus.UNKNOWN_ERROR;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.config.DmiWebClientConfiguration.DmiProperties;
import org.onap.cps.ncmp.api.impl.exception.DmiClientRequestException;
import org.onap.cps.ncmp.api.impl.exception.InvalidDmiResourceUrlException;
import org.onap.cps.ncmp.api.impl.operations.OperationType;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
@RequiredArgsConstructor
@Slf4j
public class DmiRestClient {

    private static final String HEALTH_CHECK_URL_EXTENSION = "/actuator/health";
    private static final String NOT_SPECIFIED = "";
    private static final String NO_AUTHORIZATION = null;
    private final WebClient webClient;
    private final DmiProperties dmiProperties;
    private final JsonObjectMapper jsonObjectMapper;

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
            return ResponseEntity.ok(webClient.post().uri(toUri(dmiResourceUrl))
                    .headers(httpHeaders -> configureHttpHeaders(httpHeaders, authorization))
                    .body(BodyInserters.fromValue(requestBodyAsJsonString))
                    .retrieve()
                    .bodyToMono(Object.class)
                    .onErrorMap(httpError -> handleDmiClientException(httpError, operationType.getOperationName()))
                    .block());
        } catch (final HttpServerErrorException e) {
            throw handleDmiClientException(e, operationType.getOperationName());
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
                    .uri(toUri(dmiPluginBaseUrl + HEALTH_CHECK_URL_EXTENSION))
                    .headers(httpHeaders -> configureHttpHeaders(httpHeaders, NO_AUTHORIZATION))
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

    private static URI toUri(final String dmiResourceUrl) {
        try {
            return new URI(dmiResourceUrl);
        } catch (final URISyntaxException e) {
            throw new InvalidDmiResourceUrlException(dmiResourceUrl, BAD_REQUEST.value());
        }
    }

    private DmiClientRequestException handleDmiClientException(final Throwable e, final String operationType) {
        final String exceptionMessage = "Unable to " + operationType + " resource data.";
        if (e instanceof WebClientResponseException wcre) {
            if (wcre.getStatusCode().isSameCodeAs(HttpStatus.REQUEST_TIMEOUT)) {
                throw new DmiClientRequestException(wcre.getStatusCode().value(), wcre.getMessage(),
                        jsonObjectMapper.asJsonString(wcre.getResponseBodyAsString()), DMI_SERVICE_NOT_RESPONDING);
            }
            throw new DmiClientRequestException(wcre.getStatusCode().value(), wcre.getMessage(),
                    jsonObjectMapper.asJsonString(wcre.getResponseBodyAsString()), UNABLE_TO_READ_RESOURCE_DATA);

        }
        if (e instanceof HttpServerErrorException httpServerErrorException) {
            throw new DmiClientRequestException(httpServerErrorException.getStatusCode().value(), exceptionMessage,
                    httpServerErrorException.getResponseBodyAsString(), DMI_SERVICE_NOT_RESPONDING);
        }
        throw new DmiClientRequestException(INTERNAL_SERVER_ERROR.value(), exceptionMessage, e.getMessage(),
                UNKNOWN_ERROR);
    }
}
