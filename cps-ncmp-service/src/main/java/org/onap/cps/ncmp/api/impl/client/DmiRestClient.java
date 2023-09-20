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
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration.DmiProperties;
import org.onap.cps.ncmp.api.impl.exception.HttpClientRequestException;
import org.onap.cps.ncmp.api.impl.operations.OperationType;
import org.onap.cps.ncmp.api.impl.trustlevel.dmiavailability.DmiPluginStatus;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Component
@AllArgsConstructor
@Slf4j
public class DmiRestClient {

    private RestTemplate restTemplate;
    private DmiProperties dmiProperties;

    /**
     * Sends POST operation to DMI with json body containing module references.
     * @param dmiResourceUrl dmi resource url
     * @param requestBodyAsJsonString json data body
     * @param operationType the type of operation being executed (for error reporting only)
     * @return response entity of type String
     */
    public ResponseEntity<Object> postOperationWithJsonData(final String dmiResourceUrl,
                                                            final String requestBodyAsJsonString,
                                                            final OperationType operationType) {
        final var httpEntity = new HttpEntity<>(requestBodyAsJsonString, configureHttpHeaders(new HttpHeaders()));
        try {
            return restTemplate.postForEntity(dmiResourceUrl, httpEntity, Object.class);
        } catch (final HttpStatusCodeException httpStatusCodeException) {
            final String exceptionMessage = "Unable to " + operationType.toString() + " resource data.";
            throw new HttpClientRequestException(exceptionMessage, httpStatusCodeException.getResponseBodyAsString(),
                    httpStatusCodeException.getRawStatusCode());
        }
    }

    /**
     * Sends GET operation to DMI plugin's health check URL.
     *
     * @param       dmiPluginBaseUrl the base URL of the dmi-plugin
     * @return      DmiPluginStatus as UP or DOWN
     */
    public DmiPluginStatus getDmiPluginStatus(final String dmiPluginBaseUrl) {
        try {
            final HttpEntity<Object> httpHeaders = new HttpEntity<>(configureHttpHeaders(new HttpHeaders()));
            final JsonNode dmiPluginHealthStatus = restTemplate.getForObject(dmiPluginBaseUrl + "/manage/health",
                    JsonNode.class, httpHeaders);
            if (dmiPluginHealthStatus != null) {
                if (dmiPluginHealthStatus.get("status").asText().equals("UP")) {
                    return DmiPluginStatus.UP;
                }
            }
        } catch (final Exception exception) {
            log.warn("Could not send request for health check since {}", exception.getMessage());
        }
        return DmiPluginStatus.DOWN;
    }

    private HttpHeaders configureHttpHeaders(final HttpHeaders httpHeaders) {
        if (dmiProperties.isDmiBasicAuthEnabled()) {
            httpHeaders.setBasicAuth(dmiProperties.getAuthUsername(), dmiProperties.getAuthPassword());
        }
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return httpHeaders;
    }
}
