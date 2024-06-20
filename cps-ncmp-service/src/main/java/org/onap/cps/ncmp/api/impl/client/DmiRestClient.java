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
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.REQUEST_TIMEOUT;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.config.DmiProperties;
import org.onap.cps.ncmp.api.impl.exception.DmiClientRequestException;
import org.onap.cps.ncmp.api.impl.operations.OperationType;
import org.onap.cps.ncmp.api.impl.operations.RequiredDmiService;
import org.onap.cps.ncmp.api.impl.utils.url.builder.UrlTemplateParameters;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class DmiRestClient {

    private static final String NOT_SPECIFIED = "";
    private static final String NO_AUTHORIZATION = null;

    private final DmiProperties dmiProperties;
    private final JsonObjectMapper jsonObjectMapper;
    @Qualifier("dataServicesWebClient")
    private final WebClient dataServicesWebClient;
    @Qualifier("modelServicesWebClient")
    private final WebClient modelServicesWebClient;
    @Qualifier("healthChecksWebClient")
    private final WebClient healthChecksWebClient;

    /**
     * Sends a synchronous (blocking) POST operation to the DMI with a JSON body containing module references.
     *
     * @param requiredDmiService      Determines if the required service is for a data or model operation.
     * @param urlTemplateParameters   The DMI resource URL template with variables.
     * @param requestBodyAsJsonString JSON data body.
     * @param operationType           The type of operation being executed (for error reporting only).
     * @param authorization           Contents of the Authorization header, or null if not present.
     * @return ResponseEntity containing the response from the DMI.
     * @throws DmiClientRequestException If there is an error during the DMI request.
     */
    public ResponseEntity<Object> synchronousPostOperationWithJsonData(final RequiredDmiService requiredDmiService,
                                                                       final UrlTemplateParameters
                                                                               urlTemplateParameters,
                                                                       final String requestBodyAsJsonString,
                                                                       final OperationType operationType,
                                                                       final String authorization) {
        final Mono<ResponseEntity<Object>> responseEntityMono =
            asynchronousPostOperationWithJsonData(requiredDmiService,
                    urlTemplateParameters,
                requestBodyAsJsonString,
                operationType,
                authorization);
        return responseEntityMono.block();
    }

    /**
     * Asynchronously performs an HTTP POST operation with the given JSON data.
     *
     * @param requiredDmiService      The service object required for retrieving or configuring the WebClient.
     * @param urlTemplateParameters   The URL template with variables for the POST request.
     * @param requestBodyAsJsonString The JSON string that will be sent as the request body.
     * @param operationType           An enumeration or object that holds information about the type of operation
     *                                being performed.
     * @param authorization           The authorization token to be added to the request headers.
     * @return A Mono emitting the response entity containing the server's response.
     */
    public Mono<ResponseEntity<Object>> asynchronousPostOperationWithJsonData(final RequiredDmiService
                                                                                      requiredDmiService,
                                                                              final UrlTemplateParameters
                                                                                      urlTemplateParameters,
                                                                              final String requestBodyAsJsonString,
                                                                              final OperationType operationType,
                                                                              final String authorization) {
        final WebClient webClient = getWebClient(requiredDmiService);
        return webClient.post()
                .uri(urlTemplateParameters.urlTemplate(), urlTemplateParameters.urlVariables())
                .headers(httpHeaders -> configureHttpHeaders(httpHeaders, authorization))
                .body(BodyInserters.fromValue(requestBodyAsJsonString))
                .retrieve()
                .toEntity(Object.class)
                .onErrorMap(throwable -> handleDmiClientException(throwable, operationType.getOperationName()));
    }

    /**
     * Retrieves the health status of the DMI plugin.
     * This method performs an HTTP GET request to the DMI health check endpoint specified by the URL template
     * parameters. If the response status code indicates a client error (4xx) or a server error (5xx), it logs a warning
     * and returns an empty Mono. In case of an error during the request, it logs the exception and returns a default
     * value of "NOT_SPECIFIED". If the response body contains a JSON node with a "status" field, the value of this
     * field is returned.
     *
     * @param urlTemplateParameters the URL template parameters for the DMI health check endpoint
     * @return a Mono emitting the health status as a String, or "NOT_SPECIFIED" if an error occurs
     */
    public Mono<String> getDmiHealthStatus(final UrlTemplateParameters urlTemplateParameters) {
        return healthChecksWebClient.get()
                .uri(urlTemplateParameters.urlTemplate(), urlTemplateParameters.urlVariables())
                .headers(httpHeaders -> configureHttpHeaders(httpHeaders, NO_AUTHORIZATION))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .map(responseHealthStatus -> responseHealthStatus.path("status").asText())
                .onErrorResume(Exception.class, ex -> {
                    log.warn("Failed to retrieve health status from {}. Status: {}",
                            urlTemplateParameters.urlTemplate(), ex.getMessage());
                    return Mono.empty();
                })
                .defaultIfEmpty(NOT_SPECIFIED);
    }

    private WebClient getWebClient(final RequiredDmiService requiredDmiService) {
        return requiredDmiService.equals(RequiredDmiService.DATA) ? dataServicesWebClient : modelServicesWebClient;
    }

    private void configureHttpHeaders(final HttpHeaders httpHeaders, final String authorization) {
        if (dmiProperties.isDmiBasicAuthEnabled()) {
            httpHeaders.setBasicAuth(dmiProperties.getAuthUsername(), dmiProperties.getAuthPassword());
        } else if (authorization != null && authorization.toLowerCase(Locale.getDefault()).startsWith("bearer ")) {
            httpHeaders.add(HttpHeaders.AUTHORIZATION, authorization);
        }
    }

    private DmiClientRequestException handleDmiClientException(final Throwable throwable, final String operationType) {
        if (throwable instanceof WebClientResponseException webClientResponseException) {
            if (webClientResponseException.getStatusCode().isSameCodeAs(REQUEST_TIMEOUT)) {
                throw new DmiClientRequestException(webClientResponseException.getStatusCode().value(),
                        webClientResponseException.getMessage(),
                        jsonObjectMapper.asJsonString(webClientResponseException.getResponseBodyAsString()),
                        DMI_SERVICE_NOT_RESPONDING);
            }
            throw new DmiClientRequestException(webClientResponseException.getStatusCode().value(),
                    webClientResponseException.getMessage(),
                    jsonObjectMapper.asJsonString(webClientResponseException.getResponseBodyAsString()),
                    UNABLE_TO_READ_RESOURCE_DATA);

        }
        final String exceptionMessage = "Unable to " + operationType + " resource data.";
        if (throwable instanceof WebClientRequestException webClientRequestException) {
            throw new DmiClientRequestException(HttpStatus.SERVICE_UNAVAILABLE.value(),
                    webClientRequestException.getMessage(),
                    exceptionMessage, DMI_SERVICE_NOT_RESPONDING);
        }
        if (throwable instanceof HttpServerErrorException httpServerErrorException) {
            throw new DmiClientRequestException(httpServerErrorException.getStatusCode().value(), exceptionMessage,
                    httpServerErrorException.getResponseBodyAsString(), DMI_SERVICE_NOT_RESPONDING);
        }
        throw new DmiClientRequestException(INTERNAL_SERVER_ERROR.value(), exceptionMessage, throwable.getMessage(),
                UNKNOWN_ERROR);
    }
}
