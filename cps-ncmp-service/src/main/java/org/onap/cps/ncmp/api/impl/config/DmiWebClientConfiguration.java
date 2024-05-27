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
 * Configures and create a WebClient bean that triggers an initialization (warmup) of the host name resolver and
 * loads the necessary native libraries to avoid the extra time needed to load resources for first request.
 */
@Configuration
@RequiredArgsConstructor
public class DmiWebClientConfiguration {

    private final HttpClientConfiguration httpClientConfiguration;

    /**
     * Configures and create a WebClient bean for dmi data service.
     *
     * @return a web client instance.
     */
    @Bean
    public WebClient dataWebClient() {
        final HttpClientConfiguration.Data dmiDataHttpClientConfiguration = httpClientConfiguration.getData();
        final HttpClient httpClient = createHttpClient("dataConnectionPool",
                dmiDataHttpClientConfiguration.getMaximumConnectionsTotal(),
                dmiDataHttpClientConfiguration.getConnectionTimeoutInSeconds(),
                dmiDataHttpClientConfiguration.getReadTimeoutInSeconds(),
                dmiDataHttpClientConfiguration.getWriteTimeoutInSeconds());
        return buildAndGetWebClient(httpClient, dmiDataHttpClientConfiguration.getMaximumInMemorySizeInMegabytes());
    }

    /**
     * Configures and create a WebClient bean for dmi model service.
     *
     * @return a web client instance.
     */
    @Bean
    public WebClient modelWebClient() {
        final HttpClientConfiguration.Model dmiModelHttpClientConfiguration = httpClientConfiguration.getModel();
        final HttpClient httpClient = createHttpClient("modelConnectionPool",
                dmiModelHttpClientConfiguration.getMaximumConnectionsTotal(),
                dmiModelHttpClientConfiguration.getConnectionTimeoutInSeconds(),
                dmiModelHttpClientConfiguration.getReadTimeoutInSeconds(),
                dmiModelHttpClientConfiguration.getWriteTimeoutInSeconds());
        return buildAndGetWebClient(httpClient, dmiModelHttpClientConfiguration.getMaximumInMemorySizeInMegabytes());
    }

    /**
     * Configures and create a WebClient bean for dmi health service.
     *
     * @return a web client instance.
     */
    @Bean
    public WebClient healthWebClient() {
        final HttpClientConfiguration.Health dmiHealthHttpClientConfiguration = httpClientConfiguration.getHealth();
        final HttpClient httpClient = createHttpClient("healthConnectionPool",
                dmiHealthHttpClientConfiguration.getMaximumConnectionsTotal(),
                dmiHealthHttpClientConfiguration.getConnectionTimeoutInSeconds(),
                dmiHealthHttpClientConfiguration.getReadTimeoutInSeconds(),
                dmiHealthHttpClientConfiguration.getWriteTimeoutInSeconds());
        return buildAndGetWebClient(httpClient, dmiHealthHttpClientConfiguration.getMaximumInMemorySizeInMegabytes());
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
