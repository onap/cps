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
package org.onap.cps.ncmp.client;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
@Component
public class DmiRestClient {

    private RestTemplate restTemplate;

    public DmiRestClient(final RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * POST operation on dmi-plugin.
     *
     * @param getResourceUrl dmi get url
     *
     * @return the response entity
     */
    public ResponseEntity<String> postOperation(final String getResourceUrl, final String body) {
        return restTemplate.exchange(getResourceUrl, HttpMethod.POST, new HttpEntity<>(""), String.class);
    }
}