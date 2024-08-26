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

package org.onap.cps.ncmp.impl.policyexecutor;

import lombok.RequiredArgsConstructor;
import org.onap.cps.ncmp.config.PolicyExecutorHttpClientConfig;
import org.onap.cps.ncmp.impl.utils.WebClientConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class PolicyExecutorWebClientConfiguration extends WebClientConfiguration {

    private final PolicyExecutorHttpClientConfig policyExecutorHttpClientConfig;

    /**
     * Configures and creates a web client bean for Policy Executor.
     *
     * @param webClientBuilder The builder instance to create the WebClient.
     * @return a WebClient instance configured for Policy Executor.
     */
    @Bean
    public WebClient policyExecutorWebClient(final WebClient.Builder webClientBuilder) {
        return configureWebClient(webClientBuilder, policyExecutorHttpClientConfig.getAllServices());
    }
}
