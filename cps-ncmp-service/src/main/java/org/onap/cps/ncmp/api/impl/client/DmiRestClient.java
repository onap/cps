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
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.client;

import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration.DmiProperties;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class DmiRestClient {

    private RestTemplate restTemplate;
    private DmiProperties dmiProperties;

    public DmiRestClient(final RestTemplate restTemplate, final DmiProperties dmiProperties) {
        this.restTemplate = restTemplate;
        this.dmiProperties = dmiProperties;
    }

    public ResponseEntity<Object> putOperationWithJsonData(final String dmiResourceUrl,
                                                            final String jsonData, final HttpHeaders headers) {
        final var httpEntity = new HttpEntity<>(jsonData, configureHttpHeaders(headers));
        return restTemplate.exchange(dmiResourceUrl, HttpMethod.PUT, httpEntity, Object.class);
    }

    public ResponseEntity<Void> postOperationWithJsonData(final String dmiResourceUrl,
                                                            final String jsonData, final HttpHeaders headers) {
        final var httpEntity = new HttpEntity<>(jsonData, configureHttpHeaders(headers));
        return restTemplate.postForEntity(dmiResourceUrl, httpEntity, Void.class);
    }

    private HttpHeaders configureHttpHeaders(final HttpHeaders httpHeaders) {
        httpHeaders.setBasicAuth(dmiProperties.getAuthUsername(), dmiProperties.getAuthPassword());
        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
        return httpHeaders;
    }
}