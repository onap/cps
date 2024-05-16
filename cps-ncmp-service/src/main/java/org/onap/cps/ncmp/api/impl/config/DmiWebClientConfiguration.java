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
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Configuration
@RequiredArgsConstructor
public class DmiWebClientConfiguration {

    @Value("${ncmp.dmi.httpclient.connectionTimeoutInSeconds:20000}")
    private Integer connectionTimeoutInSeconds;

    @Value("${ncmp.dmi.httpclient.maximumInMemorySizeInMegabytes:1}")
    private Integer maximumInMemorySizeInMegabytes;

    @Value("${ncmp.dmi.httpclient.maximumConnectionsTotal:100}")
    private Integer maximumConnectionsTotal;

    @Getter
    @Component
    public static class DmiProperties {
        @Value("${ncmp.dmi.auth.username}")
        private String authUsername;
        @Value("${ncmp.dmi.auth.password}")
        private String authPassword;
        @Value("${ncmp.dmi.api.base-path}")
        private String dmiBasePath;
        @Value("${ncmp.dmi.auth.enabled}")
        private boolean dmiBasicAuthEnabled;
    }

    /**
     * Configures and create a WebClient bean that triggers an initialization (warmup) of the host name resolver and
     * loads the necessary native libraries to avoid the extra time needed to load resources for first request.
     *
     * @return a WebClient instance.
     */
    @Bean
    public WebClient webClient() {

        final ConnectionProvider dmiWebClientConnectionProvider
                = ConnectionProvider.create("dmiWebClientConnectionPool", maximumConnectionsTotal);

        final HttpClient httpClient = HttpClient.create(dmiWebClientConnectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutInSeconds * 1000)
                .doOnConnected(connection ->
                        connection
                                .addHandlerLast(new ReadTimeoutHandler(connectionTimeoutInSeconds, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(connectionTimeoutInSeconds, TimeUnit.SECONDS)));
        httpClient.warmup().block();
        return WebClient.builder()
                .defaultHeaders(header -> header.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .defaultHeaders(header -> header.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(maximumInMemorySizeInMegabytes * 1024 * 1024))
                .build();
    }
}
