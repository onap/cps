/*-
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

package org.onap.cps.ncmp.impl.policyexecutor

import org.onap.cps.ncmp.config.PolicyExecutorHttpClientConfig
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.client.WebClient
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [PolicyExecutorHttpClientConfig])
@EnableConfigurationProperties
class PolicyExecutorWebClientConfigurationSpec extends Specification {

    def webClientBuilder = Mock(WebClient.Builder) {
        defaultHeaders(_) >> it
        clientConnector(_) >> it
        codecs(_) >> it
        build() >> Mock(WebClient)
    }

    def httpClientConfiguration = Spy(PolicyExecutorHttpClientConfig.class)

    def objectUnderTest = new PolicyExecutorWebClientConfiguration(httpClientConfiguration)

    def 'Creating a web client instance for policy executor.'() {
        given: 'Web client configuration is invoked'
            def policyExecutorWebClient = objectUnderTest.policyExecutorWebClient(webClientBuilder)
        expect: 'the system can create an instance for data service'
            assert policyExecutorWebClient != null
            assert policyExecutorWebClient instanceof WebClient
    }
}
