/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
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

import java.util.Arrays;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(HttpClientConfiguration.class)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
public class NcmpConfiguration {

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
     * Rest template bean.
     *
     * @param restTemplateBuilder the rest template builder
     * @param httpClientConfiguration the http client configuration
     * @return rest template instance
     */
    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
    public static RestTemplate restTemplate(final RestTemplateBuilder restTemplateBuilder,
                                            final HttpClientConfiguration httpClientConfiguration) {

        final ConnectionConfig connectionConfig = ConnectionConfig.copy(ConnectionConfig.DEFAULT)
                .setConnectTimeout(Timeout.of(httpClientConfiguration.getConnectionTimeoutInSeconds()))
                .build();

        final PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .setMaxConnTotal(httpClientConfiguration.getMaximumConnectionsTotal())
                .setMaxConnPerRoute(httpClientConfiguration.getMaximumConnectionsPerRoute())
                .build();

        final CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .evictExpiredConnections()
                .evictIdleConnections(
                        TimeValue.of(httpClientConfiguration.getIdleConnectionEvictionThresholdInSeconds()))
                .build();

        final ClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);

        final RestTemplate restTemplate = restTemplateBuilder
                .requestFactory(() -> requestFactory)
                .setConnectTimeout(httpClientConfiguration.getConnectionTimeoutInSeconds())
                .build();

        setRestTemplateMessageConverters(restTemplate);
        return restTemplate;
    }

    private static void setRestTemplateMessageConverters(final RestTemplate restTemplate) {
        final MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter =
            new MappingJackson2HttpMessageConverter();
        mappingJackson2HttpMessageConverter.setSupportedMediaTypes(
            Arrays.asList(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN));
        restTemplate.getMessageConverters().add(mappingJackson2HttpMessageConverter);
    }

}
