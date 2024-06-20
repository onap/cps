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

package org.onap.cps.ncmp.api.impl.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.web.reactive.function.client.WebClient
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [HttpClientConfiguration])
@TestPropertySource(properties = ['ncmp.dmi.httpclient.data-services.connectionTimeoutInSeconds=1', 'ncmp.dmi.httpclient.model-services.maximumInMemorySizeInMegabytes=1'])
@EnableConfigurationProperties
class DmiWebClientConfigurationSpec extends Specification {

    def webClientBuilder = Mock(WebClient.Builder) {
        defaultHeaders(_) >> it
        clientConnector(_) >> it
        codecs(_) >> it
        build() >> Mock(WebClient)
    }

    def httpClientConfiguration = Spy(HttpClientConfiguration.class)

    def objectUnderTest = new DmiWebClientConfiguration(httpClientConfiguration)

    def 'Web Client Configuration construction.'() {
        expect: 'the system can create an instance'
            new DmiWebClientConfiguration(httpClientConfiguration) != null
    }

    def 'Creating a web client instance data service.'() {
        given: 'Web client configuration is invoked'
            def dataServicesWebClient = objectUnderTest.dataServicesWebClient(webClientBuilder)
        expect: 'the system can create an instance for data service'
            assert dataServicesWebClient != null
            assert dataServicesWebClient instanceof WebClient
    }

    def 'Creating a web client instance model service.'() {
        given: 'Web client configuration invoked'
            def modelServicesWebClient = objectUnderTest.modelServicesWebClient(webClientBuilder)
        expect: 'the system can create an instance for model service'
            assert modelServicesWebClient != null
            assert modelServicesWebClient instanceof WebClient
    }

    def 'Creating a web client instance health service.'() {
        given: 'Web client configuration invoked'
            def healthChecksWebClient = objectUnderTest.healthChecksWebClient(webClientBuilder)
        expect: 'the system can create an instance for health service'
            assert healthChecksWebClient != null
            assert healthChecksWebClient instanceof WebClient
    }
}
