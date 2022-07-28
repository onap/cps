/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration.DmiProperties;
import org.onap.cps.ncmp.api.impl.exception.HttpClientRequestException;
import org.onap.cps.ncmp.api.impl.operations.DmiRequestBody;
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
     * @param jsonData json data body
     * @param operation the type of operation being executed (for error reporting only)
     * @return response entity of type String
     */
    public ResponseEntity<Object> postOperationWithJsonData(final String dmiResourceUrl,
                                                            final String jsonData,
                                                            final DmiRequestBody.OperationEnum operation) {
        final var httpEntity = new HttpEntity<>(jsonData, configureHttpHeaders(new HttpHeaders()));
        try {
            log.info("dmiResourceUrl: {}, jsonData: {}, operation: {}", dmiResourceUrl, jsonData, operation.toString());
            return restTemplate.postForEntity(dmiResourceUrl, httpEntity, Object.class);
        } catch (final HttpStatusCodeException httpStatusCodeException) {
            final String exceptionMessage = "Unable to " + operation.toString() + " resource data.";
            log.info("exceptionMessage: {}, responseBodyAsString: {}, rawStatusCode: {}", exceptionMessage,
                    httpStatusCodeException.getResponseBodyAsString(), httpStatusCodeException.getRawStatusCode());
            throw new HttpClientRequestException(exceptionMessage, httpStatusCodeException.getResponseBodyAsString(),
                httpStatusCodeException.getRawStatusCode());
        }
    }

    private HttpHeaders configureHttpHeaders(final HttpHeaders httpHeaders) {
        httpHeaders.setBasicAuth(dmiProperties.getAuthUsername(), dmiProperties.getAuthPassword());
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return httpHeaders;
    }
}
