/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.dmi;

import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.config.DmiHttpClientConfig;
import org.onap.cps.ncmp.config.ServiceConfig;
import org.onap.cps.ncmp.impl.provmns.http.ClientRequestMetricsTagCustomizer;
import org.onap.cps.ncmp.impl.utils.http.WebClientConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class DmiWebClientsConfiguration extends WebClientConfiguration {

    private final DmiHttpClientConfig dmiHttpClientConfig;
    private final ClientRequestMetricsTagCustomizer clientRequestMetricsTagCustomizer;

    /**
     * Configures and creates a web client bean for DMI data services.
     *
     * @param webClientBuilder The builder instance to create the WebClient.
     * @return a WebClient instance configured for data services.
     */
    @Bean
    public WebClient dataServicesWebClient(final WebClient.Builder webClientBuilder) {
        return configureWebClientWithMetrics(webClientBuilder, dmiHttpClientConfig.getDataServices());
    }

    /**
     * Configures and creates a web client bean for DMI model services.
     *
     * @param webClientBuilder The builder instance to create the WebClient.
     * @return a WebClient instance configured for model services.
     */
    @Bean
    public WebClient modelServicesWebClient(final WebClient.Builder webClientBuilder) {
        return configureWebClientWithMetrics(webClientBuilder, dmiHttpClientConfig.getModelServices());
    }

    /**
     * Configures and creates a web client bean for DMI health check services.
     *
     * @param webClientBuilder The builder instance to create the WebClient.
     * @return a WebClient instance configured for health check services.
     */
    @Bean
    public WebClient healthChecksWebClient(final WebClient.Builder webClientBuilder) {
        return configureWebClient(webClientBuilder, dmiHttpClientConfig.getHealthCheckServices());
    }

    /**
     * Configures WebClient with custom metrics observation convention for ProvMnS API calls.
     *
     * @param webClientBuilder The builder instance to create the WebClient.
     * @param serviceConfig The service configuration.
     * @return a WebClient instance configured with custom metrics.
     */
    private WebClient configureWebClientWithMetrics(final WebClient.Builder webClientBuilder,
                                                    final ServiceConfig serviceConfig) {
        webClientBuilder.observationConvention(clientRequestMetricsTagCustomizer);
        return configureWebClient(webClientBuilder, serviceConfig);
    }
}
