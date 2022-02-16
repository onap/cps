/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
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

import java.util.regex.Pattern;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.api.impl.client.DmiRestClient;
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration;
import org.onap.cps.utils.JsonObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@RequiredArgsConstructor
@Service
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

    protected final PersistenceCmHandleRetriever cmHandlePropertiesRetriever;
    protected final JsonObjectMapper jsonObjectMapper;
    protected final NcmpConfiguration.DmiProperties dmiProperties;
    protected final DmiRestClient dmiRestClient;

    // valid kafka topic name regex
    private static final String TOPIC_NAME_PATTERN = "^[a-zA-Z0-9]([._-](?![._-])|[a-zA-Z0-9]){0,120}[a-zA-Z0-9]$";
    protected static final Pattern topicPattern = Pattern.compile(TOPIC_NAME_PATTERN);

    UriComponentsBuilder getCmHandleUrl() {
        return UriComponentsBuilder.newInstance()
                .path("{dmiServiceName}")
                .pathSegment("{dmiBasePath}")
                .pathSegment("v1")
                .pathSegment("ch")
                .pathSegment("{cmHandle}");
    }

    String getDmiResourceUrl(final String dmiServiceName, final String cmHandle, final String resourceName) {
        return getCmHandleUrl()
                .pathSegment("{resourceName}")
                .buildAndExpand(dmiServiceName, dmiProperties.getDmiBasePath(), cmHandle, resourceName).toUriString();
    }

    static HttpHeaders prepareHeader(final String acceptParam) {
        final var httpHeaders = new HttpHeaders();
        httpHeaders.set(HttpHeaders.ACCEPT, acceptParam);
        return httpHeaders;
    }

}
