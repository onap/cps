/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.utils;

import static org.onap.cps.ncmp.api.impl.operations.RequiredDmiService.DATA;

import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.apache.logging.log4j.util.TriConsumer;
import org.onap.cps.ncmp.api.impl.config.NcmpConfiguration;
import org.onap.cps.ncmp.api.impl.operations.DmiOperations;
import org.onap.cps.ncmp.api.impl.yangmodels.YangModelCmHandle;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class DmiServiceUrlBuilder {

    private static final String TOPIC_QUERY_PARAM_NAME = "topic";
    private static final String OPTIONS_QUERY_PARAM_NAME = "options";
    private static final String RESOURCE_IDENTIFIER_QUERY_PARAM_NAME = "resourceIdentifier";
    private static final String DMI_SERVICE_NAME_URI_VAR_NAME = "dmiServiceName";
    private static final String DMI_BASE_PATH_URI_VAR_NAME = "dmiBasePath";
    private static final String CM_HANDLE_URI_VAR_NAME = "cmHandle";
    private static final String DATA_STORE_URI_VAR_NAME = "dataStore";
    private static final String DMI_SERVICE_NAME = "{dmiServiceName}";
    private static final String DMI_BASE_PATH = "{dmiBasePath}";
    private static final String V1 = "v1";
    private static final String CH = "ch";
    private static final String CM_HANDLE = "{cmHandle}";
    private static final String DATA_PATH = "data";
    private static final String DS = "ds";
    private static final String DATA_STORE = "{dataStore}";
    private static final String DEFAULT_DMI_BASE_PATH = "dmi";

    private final NcmpConfiguration.DmiProperties dmiProperties;

    /**
     * This method creates the dmi service url.
     *
     * @param queryParams  query param map as key,value pair
     * @param uriVariables uri param map as key (placeholder),value pair
     * @return {@code String} dmi service url as string
     */
    public String getDmiDatastoreUrl(final MultiValueMap<String, String> queryParams,
                                     final Map<String, Object> uriVariables) {
        final UriComponentsBuilder uriComponentsBuilder = getCmHandleUrl()
                .pathSegment(DATA_PATH)
                .pathSegment(DS)
                .pathSegment(DATA_STORE)
                .queryParams(queryParams)
                .uriVariables(uriVariables);
        return uriComponentsBuilder.buildAndExpand().toUriString();
    }

    /**
     * This method creates the dmi service url builder object with path variables.
     *
     * @return {@code UriComponentsBuilder} dmi service url builder object
     */
    public UriComponentsBuilder getCmHandleUrl() {
        return UriComponentsBuilder.newInstance()
                .path(DMI_SERVICE_NAME)
                .pathSegment(DMI_BASE_PATH)
                .pathSegment(V1)
                .pathSegment(CH)
                .pathSegment(CM_HANDLE);
    }

    /**
     * This method creates the dmi service url.
     *
     * @param yangModelCmHandle get dmi service name
     * @param cmHandle            cm handle name for dmi registration
     * @return {@code String} dmi service url as string
     */
    public Map<String, Object> populateUriVariables(final YangModelCmHandle yangModelCmHandle,
                                                    final String cmHandle,
                                                    final DmiOperations.DataStoreEnum dataStore) {
        final Map<String, Object> uriVariables = new HashMap<>();
        final String dmiBasePath = dmiProperties.getDmiBasePath();
        uriVariables.put(DMI_SERVICE_NAME_URI_VAR_NAME, yangModelCmHandle.resolveDmiServiceName(DATA));
        uriVariables.put(DMI_BASE_PATH_URI_VAR_NAME, Strings.isNotEmpty(dmiBasePath)
                ? dmiBasePath : DEFAULT_DMI_BASE_PATH);
        uriVariables.put(CM_HANDLE_URI_VAR_NAME, cmHandle);
        uriVariables.put(DATA_STORE_URI_VAR_NAME, dataStore.getValue());
        return uriVariables;
    }

    /**
     * This method is used to populate map from query params.
     *
     * @param resourceId          unique id of response for valid topic
     * @param optionsParamInQuery options into url param
     * @param topicParamInQuery   topic into url param
     * @return all valid query params as map
     */
    public MultiValueMap<String, String> populateQueryParams(final String resourceId,
                                                             final String optionsParamInQuery,
                                                             final String topicParamInQuery) {
        final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
        getQueryParamConsumer().accept(RESOURCE_IDENTIFIER_QUERY_PARAM_NAME, resourceId, queryParams);
        getQueryParamConsumer().accept(OPTIONS_QUERY_PARAM_NAME, optionsParamInQuery, queryParams);
        if (Strings.isNotEmpty(topicParamInQuery)) {
            getQueryParamConsumer().accept(TOPIC_QUERY_PARAM_NAME, topicParamInQuery, queryParams);
        }
        return queryParams;
    }

    private TriConsumer<String, String, MultiValueMap<String, String>> getQueryParamConsumer() {
        return (paramName, paramValue, paramMap) -> {
            if (Strings.isNotEmpty(paramValue)) {
                paramMap.add(paramName, paramValue);
            }
        };
    }
}
