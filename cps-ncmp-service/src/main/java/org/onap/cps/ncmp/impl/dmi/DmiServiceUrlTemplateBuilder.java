/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2024 Nordix Foundation
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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.util.UriComponentsBuilder;

@NoArgsConstructor
public class DmiServiceUrlTemplateBuilder {

    private final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
    private static final String FIXED_PATH_SEGMENT = null;
    private static final String VERSION_SEGMENT = "v1";
    private final Map<String, String> pathSegments = new LinkedHashMap<>();
    private final Map<String, String> queryParameters = new LinkedHashMap<>();

    /**
     * Static factory method to create a new instance of DmiServiceUrlTemplateBuilder.
     *
     * @return a new instance of DmiServiceUrlTemplateBuilder
     */
    public static DmiServiceUrlTemplateBuilder newInstance() {
        return new DmiServiceUrlTemplateBuilder();
    }

    /**
     * Add a fixed pathSegment to the URL.
     *
     * @param pathSegment the path segment
     * @return this builder instance
     */
    public DmiServiceUrlTemplateBuilder fixedPathSegment(final String pathSegment) {
        pathSegments.put(pathSegment, FIXED_PATH_SEGMENT);
        return this;
    }

    /**
     * Add a variable pathSegment to the URL.
     * Do NOT add { } braces. the builder will take care of that
     *
     * @param pathSegment the name of the variable path segment (with { and }
     * @param value       the value to be insert in teh URL for the given variable path segment
     * @return this builder instance
     */
    public DmiServiceUrlTemplateBuilder variablePathSegment(final String pathSegment, final String value) {
        pathSegments.put(pathSegment, value);
        return this;
    }

    /**
     * Add a query parameter to the URL.
     * Do NOT encode as the builder wil take care of encoding
     *
     * @param queryParameterName  the name of the variable
     * @param queryParameterValue the value of the variable (only Strings are supported).
     *
     * @return this builder instance
     */
    public DmiServiceUrlTemplateBuilder queryParameter(final String queryParameterName,
                                                       final String queryParameterValue) {
        if (Strings.isNotBlank(queryParameterValue)) {
            queryParameters.put(queryParameterName, queryParameterValue);
        }
        return this;
    }

    /**
     * Constructs a URL template with variables based on the accumulated path segments and query parameters.
     *
     * @param dmiServiceBaseUrl the base URL of the DMI service, e.g., "http://dmi-service.com".
     * @param dmiBasePath       the base path of the DMI service
     * @return a UrlTemplateParameters instance containing the complete URL template and URL variables
     */
    public UrlTemplateParameters createUrlTemplateParameters(final String dmiServiceBaseUrl, final String dmiBasePath) {
        this.uriComponentsBuilder.pathSegment(dmiBasePath)
            .pathSegment(VERSION_SEGMENT);

        final Map<String, String> urlTemplateVariables = new HashMap<>();

        pathSegments.forEach((pathSegmentName, variablePathValue) ->  {
            if (StringUtils.equals(variablePathValue, FIXED_PATH_SEGMENT)) {
                this.uriComponentsBuilder.pathSegment(pathSegmentName);
            } else {
                this.uriComponentsBuilder.pathSegment("{" + pathSegmentName + "}");
                urlTemplateVariables.put(pathSegmentName, variablePathValue);
            }
        });

        queryParameters.forEach((paramName, paramValue) -> {
            this.uriComponentsBuilder.queryParam(paramName, "{" + paramName + "}");
            urlTemplateVariables.put(paramName, paramValue);
        });

        final String urlTemplate = dmiServiceBaseUrl + this.uriComponentsBuilder.build().toUriString();
        return new UrlTemplateParameters(urlTemplate, urlTemplateVariables);
    }

    /**
     * Constructs a URL for DMI health check based on the given base URL.
     *
     * @param dmiServiceBaseUrl the base URL of the DMI service, e.g., "http://dmi-service.com".
     * @return a {@link UrlTemplateParameters} instance containing the complete URL template and empty URL variables,
     *     suitable for DMI health check.
     */
    public UrlTemplateParameters createUrlTemplateParametersForHealthCheck(final String dmiServiceBaseUrl) {
        this.uriComponentsBuilder.pathSegment("actuator")
                .pathSegment("health");

        final String urlTemplate = dmiServiceBaseUrl + this.uriComponentsBuilder.build().toUriString();
        return new UrlTemplateParameters(urlTemplate, Collections.emptyMap());
    }

}
