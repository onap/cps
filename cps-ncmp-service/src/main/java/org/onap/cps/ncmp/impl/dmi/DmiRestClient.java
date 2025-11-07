/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.dmi;

import static org.onap.cps.ncmp.api.NcmpResponseStatus.DMI_SERVICE_NOT_RESPONDING;
import static org.onap.cps.ncmp.api.NcmpResponseStatus.UNABLE_TO_READ_RESOURCE_DATA;
import static org.onap.cps.ncmp.api.NcmpResponseStatus.UNKNOWN_ERROR;
import static org.onap.cps.ncmp.api.data.models.OperationType.READ;
import static org.onap.cps.ncmp.impl.models.RequiredDmiService.DATA;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.REQUEST_TIMEOUT;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.NcmpResponseStatus;
import org.onap.cps.ncmp.api.data.models.OperationType;
import org.onap.cps.ncmp.api.exceptions.DmiClientRequestException;
import org.onap.cps.ncmp.impl.models.RequiredDmiService;
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class DmiRestClient {

    public static final String NO_AUTHORIZATION = null;

    private static final String NOT_SPECIFIED = "";

    private final DmiServiceAuthenticationProperties dmiServiceAuthenticationProperties;
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
    public ResponseEntity<Object> synchronousPostOperation(final RequiredDmiService requiredDmiService,
                                                           final UrlTemplateParameters urlTemplateParameters,
                                                           final String requestBodyAsJsonString,
                                                           final OperationType operationType,
                                                           final String authorization) {
        return asynchronousPostOperation(requiredDmiService,
                                         urlTemplateParameters,
                                         requestBodyAsJsonString,
                                         operationType,
                                         authorization).block();
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
    public Mono<ResponseEntity<Object>> asynchronousPostOperation(final RequiredDmiService requiredDmiService,
                                                                  final UrlTemplateParameters urlTemplateParameters,
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
     * Sends a synchronous (blocking) GET operation to the DMI.
     *
     * @param requiredDmiService    Determines if the required service is for a data or model operation.
     * @param urlTemplateParameters The DMI resource URL template with variables.
     * @param operationType         The type of operation being executed (for error reporting only).
     * @return ResponseEntity containing the response from the DMI.
     * @throws DmiClientRequestException If there is an error during the DMI request.
     */
    public ResponseEntity<Object> synchronousGetOperation(final RequiredDmiService requiredDmiService,
                                                          final UrlTemplateParameters urlTemplateParameters,
                                                          final OperationType operationType) {
        return getWebClient(requiredDmiService)
            .get()
            .uri(urlTemplateParameters.urlTemplate(), urlTemplateParameters.urlVariables())
            .headers(httpHeaders -> configureHttpHeaders(httpHeaders, NO_AUTHORIZATION))
            .retrieve()
            .toEntity(Object.class)
            .onErrorMap(throwable ->
                handleDmiClientException(throwable, operationType.getOperationName()))
            .block();
    }

    /**
     * Sends a synchronous (blocking) PUT operation to the DMI.
     *
     * @param requiredDmiService    Determines if the required service is for a data or model operation.
     * @param urlTemplateParameters The DMI resource URL template with variables.
     * @param operationType         The type of operation being executed (for error reporting only).
     * @return ResponseEntity containing the response from the DMI.
     * @throws DmiClientRequestException If there is an error during the DMI request.
     */
    public ResponseEntity<Object> synchronousPutOperation(final RequiredDmiService requiredDmiService,
                                                          final UrlTemplateParameters urlTemplateParameters,
                                                          final OperationType operationType) {
        return getWebClient(requiredDmiService)
            .get()
            .uri(urlTemplateParameters.urlTemplate(), urlTemplateParameters.urlVariables())
            .headers(httpHeaders -> configureHttpHeaders(httpHeaders, NO_AUTHORIZATION))
            .retrieve()
            .toEntity(Object.class)
            .onErrorMap(throwable -> handleDmiClientException(throwable, operationType.getOperationName()))
            .block();
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
                .onErrorResume(Exception.class, e -> {
                    log.warn("Failed to retrieve health status from {}. Status: {}",
                            urlTemplateParameters.urlTemplate(), e.getMessage());
                    return Mono.empty();
                })
                .defaultIfEmpty(NOT_SPECIFIED);
    }

    /**
     * Retrieves the result of a data get request from the DMI service asynchronously.
     *
     * @param urlTemplateParameters   The URL template parameters for the DMI data endpoint.
     * @param authorization           The authorization token to be added to the request headers.
     * @return A Mono emitting the result of the request as a String.
     */
    public Mono<String> asynchronousDmiDataRequest(final UrlTemplateParameters urlTemplateParameters,
                                                   final String authorization) {

        return dataServicesWebClient.get()
                .uri(urlTemplateParameters.urlTemplate(), urlTemplateParameters.urlVariables())
                .headers(httpHeaders -> configureHttpHeaders(httpHeaders, authorization))
                .retrieve()
                .bodyToMono(String.class)
                .onErrorMap(throwable -> handleDmiClientException(throwable, READ.getOperationName()));
    }

    /**
     * Sends a synchronous (blocking) DELETE operation to the DMI with a JSON body.
     *
     * @param requiredDmiService    Determines if the required service is for a data or model operation.
     * @param urlTemplateParameters The DMI resource URL template with variables.
     * @return ResponseEntity from the DMI Plugin
     * @throws DmiClientRequestException If there is an error during the DMI request.
     *
     */
    public ResponseEntity<Object> synchronousDeleteOperation(final RequiredDmiService requiredDmiService,
                                                             final UrlTemplateParameters urlTemplateParameters) {
        return getWebClient(requiredDmiService)
                .delete()
                .uri(urlTemplateParameters.urlTemplate(), urlTemplateParameters.urlVariables())
                .headers(httpHeaders -> configureHttpHeaders(httpHeaders, NO_AUTHORIZATION))
                .retrieve()
                .toEntity(Object.class)
                .onErrorMap(throwable -> handleDmiClientException(throwable, OperationType.DELETE.getOperationName()))
                .block();
    }

    private WebClient getWebClient(final RequiredDmiService requiredDmiService) {
        return DATA.equals(requiredDmiService) ? dataServicesWebClient : modelServicesWebClient;
    }

    private void configureHttpHeaders(final HttpHeaders httpHeaders, final String authorization) {
        if (dmiServiceAuthenticationProperties.isDmiBasicAuthEnabled()) {
            httpHeaders.setBasicAuth(dmiServiceAuthenticationProperties.getAuthUsername(),
                dmiServiceAuthenticationProperties.getAuthPassword());
        } else if (authorization != null && authorization.toLowerCase(Locale.getDefault()).startsWith("bearer ")) {
            httpHeaders.add(HttpHeaders.AUTHORIZATION, authorization);
        }
    }

    private DmiClientRequestException handleDmiClientException(final Throwable throwable, final String operationType) {
        if (throwable instanceof WebClientResponseException webClientResponseException) {
            final NcmpResponseStatus ncmpResponseStatus =  webClientResponseException.getStatusCode()
                .isSameCodeAs(REQUEST_TIMEOUT) ? DMI_SERVICE_NOT_RESPONDING : UNABLE_TO_READ_RESOURCE_DATA;
            return new DmiClientRequestException(webClientResponseException.getStatusCode().value(),
                    webClientResponseException.getMessage(),
                    jsonObjectMapper.asJsonString(webClientResponseException.getResponseBodyAsString()),
                    ncmpResponseStatus);
        }
        final String exceptionMessage = "Unable to " + operationType + " resource data.";
        if (throwable instanceof WebClientRequestException) {
            return new DmiClientRequestException(HttpStatus.SERVICE_UNAVAILABLE.value(), throwable.getMessage(),
                    exceptionMessage, DMI_SERVICE_NOT_RESPONDING);
        }
        return new DmiClientRequestException(INTERNAL_SERVER_ERROR.value(), exceptionMessage, throwable.getMessage(),
                UNKNOWN_ERROR);
    }

}
