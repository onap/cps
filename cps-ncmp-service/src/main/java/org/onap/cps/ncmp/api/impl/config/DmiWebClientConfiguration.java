
/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.util.concurrent.TimeUnit;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DmiWebClientConfiguration {

    @Value("${ncmp.dmi.httpclient.connectionTimeoutInSeconds:20000}")
    private Integer connectionTimeout;

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
     * Configures and create a WebClient bean.
     *
     * @return a WebClient instance.
     */
    @Bean
    public WebClient webClient() {
        final var httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeout * 1000) // millis
                .doOnConnected(connection ->
                        connection
                                .addHandlerLast(new ReadTimeoutHandler(connectionTimeout, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(connectionTimeout, TimeUnit.SECONDS)));

        return WebClient.builder()
                .defaultHeaders(header -> header.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
                .defaultHeaders(header -> header.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
