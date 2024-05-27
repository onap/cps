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

package org.onap.cps.ncmp.api.impl.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

/**
 * Configures and creates a WebClient bean that triggers an initialization (warmup) of the host name resolver and
 * loads the necessary native libraries to avoid the extra time needed to load resources for first request.
 */
@Configuration
@RequiredArgsConstructor
public class DmiWebClientConfiguration {

    private final HttpClientConfiguration httpClientConfiguration;

    /**
     * Configures and create a WebClient bean for DMI data service.
     *
     * @return a WebClient instance for data services.
     */
    @Bean
    public WebClient dataServicesWebClient() {
        final HttpClientConfiguration.DataServices httpClientConfiguration
                = this.httpClientConfiguration.getDataServices();

        final HttpClient httpClient = createHttpClient("dataConnectionPool",
                httpClientConfiguration.getMaximumConnectionsTotal(),
                httpClientConfiguration.getConnectionTimeoutInSeconds(),
                httpClientConfiguration.getReadTimeoutInSeconds(),
                httpClientConfiguration.getWriteTimeoutInSeconds());
        return buildAndGetWebClient(httpClient, httpClientConfiguration.getMaximumInMemorySizeInMegabytes());
    }

    /**
     * Configures and creates a WebClient bean for DMI model service.
     *
     * @return a WebClient instance for model services.
     */
    @Bean
    public WebClient modelServicesWebClient() {
        final HttpClientConfiguration.ModelServices httpClientConfiguration
                = this.httpClientConfiguration.getModelServices();

        final HttpClient httpClient = createHttpClient("modelConnectionPool",
                httpClientConfiguration.getMaximumConnectionsTotal(),
                httpClientConfiguration.getConnectionTimeoutInSeconds(),
                httpClientConfiguration.getReadTimeoutInSeconds(),
                httpClientConfiguration.getWriteTimeoutInSeconds());
        return buildAndGetWebClient(httpClient, httpClientConfiguration.getMaximumInMemorySizeInMegabytes());
    }

    /**
     * Configures and creates a WebClient bean for DMI health service.
     *
     * @return a WebClient instance for health checks.
     */
    @Bean
    public WebClient healthChecksWebClient() {
        final HttpClientConfiguration.HealthCheckServices httpClientConfiguration
                = this.httpClientConfiguration.getHealthCheckServices();

        final HttpClient httpClient = createHttpClient("healthConnectionPool",
                httpClientConfiguration.getMaximumConnectionsTotal(),
                httpClientConfiguration.getConnectionTimeoutInSeconds(),
                httpClientConfiguration.getReadTimeoutInSeconds(),
                httpClientConfiguration.getWriteTimeoutInSeconds());
        return buildAndGetWebClient(httpClient, httpClientConfiguration.getMaximumInMemorySizeInMegabytes());
    }

    private static HttpClient createHttpClient(final String connectionProviderName,
                                               final Integer maximumConnectionsTotal,
                                               final Integer connectionTimeoutInSeconds,
                                               final Integer readTimeoutInSeconds,
                                               final Integer writeTimeoutInSeconds) {
        final ConnectionProvider dmiWebClientConnectionProvider = ConnectionProvider.create(connectionProviderName,
                maximumConnectionsTotal);

        final HttpClient httpClient = HttpClient.create(dmiWebClientConnectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutInSeconds * 1000)
                .doOnConnected(connection -> connection.addHandlerLast(new ReadTimeoutHandler(readTimeoutInSeconds,
                        TimeUnit.SECONDS)).addHandlerLast(new WriteTimeoutHandler(writeTimeoutInSeconds,
                        TimeUnit.SECONDS)));
        httpClient.warmup().block();
        return httpClient;
    }

    private static WebClient buildAndGetWebClient(final HttpClient httpClient,
                                                  final Integer maximumInMemorySizeInMegabytes) {
        return WebClient.builder()
                .defaultHeaders(header -> header.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .defaultHeaders(header -> header.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(
                        maximumInMemorySizeInMegabytes * 1024 * 1024)).build();
    }
}
