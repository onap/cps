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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.NoArgsConstructor;
import org.apache.logging.log4j.util.Strings;
import org.springframework.web.util.UriComponentsBuilder;

@NoArgsConstructor
public class DmiServiceUrlBuilder {

    private static final String FIXED_PATH_SEGMENT = null;

    final UriComponentsBuilder uriComponentsBuilder = UriComponentsBuilder.newInstance();
    final Map<String, Object> pathSegments = new LinkedHashMap<>();

    public static DmiServiceUrlBuilder newInstance() {
        return new DmiServiceUrlBuilder();
    }

    /**
     * Add a fixed pathSegment to the URI.
     *
     * @param pathSegment the path segment
     * @return this builder
     */
    public DmiServiceUrlBuilder pathSegment(final String pathSegment) {
        pathSegments.put(pathSegment, FIXED_PATH_SEGMENT);
        return this;
    }

    /**
     * Add a variable pathSegment to the URI.
     * Do NOT add { } braces. the builder will take care of that
     *
     * @param pathSegment the name of the variable path segment (with { and }
     * @param value       the value to be insert in teh URI for the given variable path segment
     * @return this builder
     */
    public DmiServiceUrlBuilder variablePathSegment(final String pathSegment, final Object value) {
        pathSegments.put(pathSegment, value);
        return this;
    }

    /**
     * Add a query parameter to the URI.
     * Do NOT encode as the builder wil take care of encoding
     *
     * @param name  the name of the variable
     * @param value the value of the variable (only Strings are supported).
     *
     * @return this builder
     */
    public DmiServiceUrlBuilder queryParameter(final String name, final String value) {
        if (Strings.isNotBlank(value)) {
            uriComponentsBuilder.queryParam(name, value);
        }
        return this;
    }

    /**
     * Build the URI as a correctly percentage-encoded String.
     *
     * @param dmiServiceName the name of the dmi service
     * @param dmiBasePath    the base path of the dmi service
     *
     * @return URI as a string
     */
    public String build(final String dmiServiceName, final String dmiBasePath) {
        uriComponentsBuilder
            .path("{dmiServiceName}")
            .pathSegment("{dmiBasePath}")
            .pathSegment("v1");

        final Map<String, Object> uriVariables = new HashMap<>();
        uriVariables.put("dmiServiceName", dmiServiceName);
        uriVariables.put("dmiBasePath", dmiBasePath);

        pathSegments.forEach((pathSegment, variablePathValue) ->  {
            if (variablePathValue == FIXED_PATH_SEGMENT) {
                uriComponentsBuilder.pathSegment(pathSegment);
            } else {
                uriComponentsBuilder.pathSegment("{" + pathSegment + "}");
                uriVariables.put(pathSegment, variablePathValue);
            }
        });
        return uriComponentsBuilder.buildAndExpand(uriVariables).encode().toUriString();
    }

}
