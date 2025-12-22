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

package org.onap.cps.ncmp.impl.dmi


import org.onap.cps.ncmp.config.DmiHttpClientConfig
import org.onap.cps.ncmp.impl.provmns.http.ClientRequestMetricsTagCustomizer
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.reactive.function.client.WebClient
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [DmiHttpClientConfig])
@EnableConfigurationProperties
class DmiWebClientsConfigurationSpec extends Specification {

    def webClientBuilder = Mock(WebClient.Builder) {
        defaultHeaders(_) >> it
        clientConnector(_) >> it
        codecs(_) >> it
        build() >> Mock(WebClient)
    }

    def dmiHttpClientConfiguration = Spy(DmiHttpClientConfig.class)
    def mockClientRequestMetricsTagCustomizer = Mock(ClientRequestMetricsTagCustomizer)
    def objectUnderTest = new DmiWebClientsConfiguration(dmiHttpClientConfiguration, mockClientRequestMetricsTagCustomizer)

    def 'Web client for data services.'() {
        when: 'creating a web client for dmi data services'
            def result = objectUnderTest.dataServicesWebClient(webClientBuilder)
        then: 'a web client is created successfully'
            assert result != null
            assert result instanceof WebClient
    }

    def 'Web client model services.'() {
        when: 'creating a web client for dmi model services'
            def result = objectUnderTest.modelServicesWebClient(webClientBuilder)
        then: 'a web client is created successfully'
            assert result != null
            assert result instanceof WebClient
    }

    def 'Web client health check services.'() {
        when: 'creating a web client for dmi health check services'
            def result = objectUnderTest.healthChecksWebClient(webClientBuilder)
        then: 'a web client is created successfully'
            assert result != null
            assert result instanceof WebClient
    }
}
