/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.impl.provmns.http;

import io.micrometer.common.KeyValues;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequestObservationContext;
import org.springframework.web.reactive.function.client.ClientRequestObservationConvention;
import org.springframework.web.reactive.function.client.DefaultClientRequestObservationConvention;

/**
 * Custom ClientRequestObservationConvention to reduce high cardinality metrics
 * by masking ProvMnS API URIs that contain FDN (Fully Distinguished Name) values.
 */
@Component
public class ClientRequestMetricsTagCustomizer extends DefaultClientRequestObservationConvention implements
        ClientRequestObservationConvention {

    @Value("${rest.api.provmns-base-path:/ProvMnS}")
    private String provMnsBasePath;

    @Override
    public KeyValues getLowCardinalityKeyValues(final ClientRequestObservationContext clientRequestObservationContext) {
        return super.getLowCardinalityKeyValues(clientRequestObservationContext).and(
                additionalTags(clientRequestObservationContext));
    }

    /**
     * Creates additional tags for the metrics, specifically masking ProvMnS API URIs
     * to reduce cardinality by replacing actual FDN values with a template placeholder.
     *
     * @param clientRequestObservationContext the client request observation context
     * @return KeyValues containing the modified URI tag if applicable
     */
    protected KeyValues additionalTags(final ClientRequestObservationContext clientRequestObservationContext) {
        final String uriTemplate = clientRequestObservationContext.getUriTemplate();
        final String provMnsApiPath = provMnsBasePath + "/v1/";
        if (uriTemplate != null && uriTemplate.contains(provMnsApiPath)) {
            final String queryParameters = extractQueryParameters(uriTemplate);
            final String maskedUri = provMnsApiPath + "{fdn}" + queryParameters;
            return KeyValues.of("uri", maskedUri);
        } else {
            return KeyValues.empty();
        }
    }

    /**
     * Extracts query parameters from the URI template.
     *
     * @param uriTemplate the original URI template
     * @return query parameters string (including the '?' prefix) or empty string if none
     */
    private String extractQueryParameters(final String uriTemplate) {
        final int queryIndex = uriTemplate.indexOf('?');
        return queryIndex != -1 ? uriTemplate.substring(queryIndex) : "";
    }
}