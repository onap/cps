/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.apache.logging.log4j.util.TriConsumer;
import org.onap.cps.ncmp.api.impl.config.DmiProperties;
import org.onap.cps.spi.utils.CpsValidator;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class DmiServiceUrlBuilder {
    private final DmiProperties dmiProperties;
    private final CpsValidator cpsValidator;

    /**
     * This method creates the dmi service url.
     *
     * @param queryParams  query param map as key,value pair
     * @param uriVariables uri param map as key (placeholder),value pair
     * @return {@code String} dmi service url as string
     */
    public String getDmiDatastoreUrl(final MultiValueMap<String, String> queryParams,
                                     final Map<String, Object> uriVariables) {
        return getUriComponentsBuilder(getResourceDataBasePathUriBuilder(), queryParams, uriVariables)
                .buildAndExpand().toUriString();
    }

    /**
     * This method builds data operation request url.
     *
     * @param dataoperationRequestQueryParams  query param map as key, value pair
     * @param dataoperationRequestUriVariables uri param map as key (placeholder), value pair
     * @return {@code String} data operation request url as string
     */
    public String getDataOperationRequestUrl(final MultiValueMap<String, String> dataoperationRequestQueryParams,
                                             final Map<String, Object> dataoperationRequestUriVariables) {
        return getDataOperationResourceDataBasePathUriBuilder()
                .queryParams(dataoperationRequestQueryParams)
                .uriVariables(dataoperationRequestUriVariables)
                .buildAndExpand().toUriString();
    }

    /**
     * This method creates the dmi service url builder object with path variables.
     *
     * @return {@code UriComponentsBuilder} dmi service url builder object
     */
    public UriComponentsBuilder getResourceDataBasePathUriBuilder() {
        return UriComponentsBuilder.newInstance()
                .path("{dmiServiceName}")
                .pathSegment("{dmiBasePath}")
                .pathSegment("v1")
                .pathSegment("ch")
                .pathSegment("{cmHandleId}");
    }

    /**
     * This method creates the dmi service url builder object with path variables for data operation request.
     *
     * @return {@code UriComponentsBuilder} dmi service url builder object
     */
    public UriComponentsBuilder getDataOperationResourceDataBasePathUriBuilder() {
        return UriComponentsBuilder.newInstance()
                .path("{dmiServiceName}")
                .pathSegment("{dmiBasePath}")
                .pathSegment("v1")
                .pathSegment("data");
    }

    /**
     * This method populates uri variables.
     *
     * @param dataStoreName data store name
     * @param dmiServiceName dmi service name
     * @param cmHandleId        cm handle id for dmi registration
     * @return {@code String} dmi service url as string
     */
    public Map<String, Object> populateUriVariables(final String dataStoreName,
                                                    final String dmiServiceName,
                                                    final String cmHandleId) {
        cpsValidator.validateNameCharacters(cmHandleId);
        final Map<String, Object> uriVariables = new HashMap<>();
        final String dmiBasePath = dmiProperties.getDmiBasePath();
        uriVariables.put("dmiServiceName", dmiServiceName);
        uriVariables.put("dmiBasePath", dmiBasePath);
        uriVariables.put("cmHandleId", cmHandleId);
        uriVariables.put("dataStore", dataStoreName);
        return uriVariables;
    }

    /**
     * This method populates uri variables for data operation request.
     *
     * @param dmiServiceName dmi service name
     * @return {@code Map<String, Object>} uri variables as map
     */
    public Map<String, Object> populateDataOperationRequestUriVariables(final String dmiServiceName) {
        final Map<String, Object> uriVariables = new HashMap<>();
        final String dmiBasePath = dmiProperties.getDmiBasePath();
        uriVariables.put("dmiServiceName", dmiServiceName);
        uriVariables.put("dmiBasePath", dmiBasePath);
        return uriVariables;
    }

    /**
     * This method is used to populate map from query params.
     *
     * @param resourceId          unique id of response for valid topic
     * @param optionsParamInQuery options as provided by client
     * @param topicParamInQuery   topic as provided by client
     * @param moduleSetTag   module set tag associated with the given cm handle
     * @return all valid query params as map
     */
    public MultiValueMap<String, String> populateQueryParams(final String resourceId,
                                                             final String optionsParamInQuery,
                                                             final String topicParamInQuery,
                                                             final String moduleSetTag) {
        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        getQueryParamConsumer().accept("resourceIdentifier", resourceId, queryParams);
        getQueryParamConsumer().accept("options", optionsParamInQuery, queryParams);
        if (Strings.isNotEmpty(topicParamInQuery)) {
            getQueryParamConsumer().accept("topic", topicParamInQuery, queryParams);
        }
        if (Strings.isNotEmpty(moduleSetTag)) {
            getQueryParamConsumer().accept("moduleSetTag", moduleSetTag, queryParams);
        }
        return queryParams;
    }

    /**
     * This method is used to populate map from query params for data operation request.
     *
     * @param topicParamInQuery topic into url param
     * @param requestId         unique id of response for valid topic
     * @return all valid query params as map
     */
    public MultiValueMap<String, String> getDataOperationRequestQueryParams(final String topicParamInQuery,
                                                                            final String requestId) {
        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        getQueryParamConsumer().accept("topic", topicParamInQuery, queryParams);
        getQueryParamConsumer().accept("requestId", requestId, queryParams);
        return queryParams;
    }

    /**
     * This method creates the dmi service url with path variables.
     *
     * @return {@code String} dmi service url
     */
    public String getWriteJobUrl(final String dmiServiceName, final String requestId) {
        return UriComponentsBuilder.newInstance()
                .path(dmiServiceName)
                .pathSegment(dmiProperties.getDmiBasePath())
                .pathSegment("v1")
                .pathSegment("writeJob")
                .pathSegment(requestId).toUriString();
    }

    private TriConsumer<String, String, MultiValueMap<String, String>> getQueryParamConsumer() {
        return (paramName, paramValue, paramMap) -> {
            if (Strings.isNotEmpty(paramValue)) {
                paramMap.add(paramName, URLEncoder.encode(paramValue, StandardCharsets.UTF_8));
            }
        };
    }

    private UriComponentsBuilder getUriComponentsBuilder(final UriComponentsBuilder uriComponentsBuilder,
                                                         final MultiValueMap<String, String> queryParams,
                                                         final Map<String, Object> uriVariables) {
        return uriComponentsBuilder
                .pathSegment("data")
                .pathSegment("ds")
                .pathSegment("{dataStore}")
                .queryParams(queryParams)
                .uriVariables(uriVariables);
    }
}
