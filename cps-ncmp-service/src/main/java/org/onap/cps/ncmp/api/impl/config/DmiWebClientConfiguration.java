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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.resolver.DefaultAddressResolverGroup;
import java.time.Duration;
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
 * Configures and creates WebClient beans for various DMI services including data, model, and health check services.
 * The configuration utilizes Netty-based HttpClient with custom connection settings, read and write timeouts,
 * and initializes WebClient with these settings to ensure optimal performance and resource management.
 */
@Configuration
@RequiredArgsConstructor
public class DmiWebClientConfiguration {

    private final HttpClientConfiguration httpClientConfiguration;
    private static final Duration DEFAULT_RESPONSE_TIMEOUT = Duration.ofSeconds(30);

    /**
     * Configures and creates a web client bean for DMI data services.
     *
     * @param webClientBuilder The builder instance to create the WebClient.
     * @return a WebClient instance configured for data services.
     */
    @Bean
    public WebClient dataServicesWebClient(final WebClient.Builder webClientBuilder) {
        return configureWebClient(webClientBuilder, httpClientConfiguration.getDataServices());
    }

    /**
     * Configures and creates a web client bean for DMI model services.
     *
     * @param webClientBuilder The builder instance to create the WebClient.
     * @return a WebClient instance configured for model services.
     */
    @Bean
    public WebClient modelServicesWebClient(final WebClient.Builder webClientBuilder) {
        return configureWebClient(webClientBuilder, httpClientConfiguration.getModelServices());
    }

    /**
     * Configures and creates a web client bean for DMI health check services.
     *
     * @param webClientBuilder The builder instance to create the WebClient.
     * @return a WebClient instance configured for health check services.
     */
    @Bean
    public WebClient healthChecksWebClient(final WebClient.Builder webClientBuilder) {
        return configureWebClient(webClientBuilder, httpClientConfiguration.getHealthCheckServices());
    }

    private WebClient configureWebClient(final WebClient.Builder webClientBuilder,
                                         final HttpClientConfiguration.ServiceConfig serviceConfig) {
        final ConnectionProvider connectionProvider = getConnectionProvider(serviceConfig);
        final HttpClient httpClient = createHttpClient(serviceConfig, connectionProvider);
        return buildAndGetWebClient(webClientBuilder, httpClient, serviceConfig.getMaximumInMemorySizeInMegabytes());
    }

    private static HttpClient createHttpClient(final HttpClientConfiguration.ServiceConfig serviceConfig,
                                               final ConnectionProvider connectionProvider) {
        return HttpClient.create(connectionProvider)
                .responseTimeout(DEFAULT_RESPONSE_TIMEOUT)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, serviceConfig.getConnectionTimeoutInSeconds() * 1000)
                .doOnConnected(connection -> connection.addHandlerLast(new ReadTimeoutHandler(
                        serviceConfig.getReadTimeoutInSeconds(), TimeUnit.SECONDS)).addHandlerLast(
                        new WriteTimeoutHandler(serviceConfig.getWriteTimeoutInSeconds(), TimeUnit.SECONDS)))
                .resolver(DefaultAddressResolverGroup.INSTANCE)
                .compress(true);
    }

    @SuppressFBWarnings("BC_UNCONFIRMED_CAST_OF_RETURN_VALUE")
    private static ConnectionProvider getConnectionProvider(final HttpClientConfiguration.ServiceConfig serviceConfig) {
        return ConnectionProvider.builder(serviceConfig.getConnectionProviderName())
                .maxConnections(serviceConfig.getMaximumConnectionsTotal())
                .pendingAcquireMaxCount(serviceConfig.getPendingAcquireMaxCount())
                .build();
    }

    private WebClient buildAndGetWebClient(final WebClient.Builder webClientBuilder, final HttpClient httpClient,
                                           final int maximumInMemorySizeInMegabytes) {
        return webClientBuilder
                .defaultHeaders(header -> header.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .defaultHeaders(header -> header.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer.defaultCodecs()
                        .maxInMemorySize(maximumInMemorySizeInMegabytes * 1024 * 1024)).build();
    }
}
