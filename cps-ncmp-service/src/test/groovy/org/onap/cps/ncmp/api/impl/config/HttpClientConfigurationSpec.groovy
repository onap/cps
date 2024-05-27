/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation.
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
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [HttpClientConfiguration])
@EnableConfigurationProperties(HttpClientConfiguration.class)
@TestPropertySource(properties = ["ncmp.dmi.httpclient.dataServices.readTimeoutInSeconds=789", "ncmp.dmi.httpclient.modelServices.maximumConnectionsTotal=111"])
class HttpClientConfigurationSpec extends Specification {

    @Autowired
    private HttpClientConfiguration httpClientConfiguration

    def 'Test http client configuration properties of data with custom and default values'() {
        expect: 'properties are populated correctly for data'
            assert httpClientConfiguration.dataServices.connectionTimeoutInSeconds == 123
            assert httpClientConfiguration.dataServices.readTimeoutInSeconds == 789
            assert httpClientConfiguration.dataServices.writeTimeoutInSeconds == 30
            assert httpClientConfiguration.dataServices.maximumConnectionsTotal == 100
            assert httpClientConfiguration.dataServices.maximumInMemorySizeInMegabytes == 7
    }

    def 'Test http client configuration properties of model with custom and default values'() {
        expect: 'properties are populated correctly for model'
            assert httpClientConfiguration.modelServices.connectionTimeoutInSeconds == 456
            assert httpClientConfiguration.modelServices.readTimeoutInSeconds == 30
            assert httpClientConfiguration.modelServices.writeTimeoutInSeconds == 30
            assert httpClientConfiguration.modelServices.maximumConnectionsTotal == 111
            assert httpClientConfiguration.modelServices.maximumInMemorySizeInMegabytes == 8
    }

    def 'Test http client configuration properties of health with default values'() {
        expect: 'properties are populated correctly for health'
            assert httpClientConfiguration.healthCheckServices.connectionTimeoutInSeconds == 30
            assert httpClientConfiguration.healthCheckServices.readTimeoutInSeconds == 30
            assert httpClientConfiguration.healthCheckServices.writeTimeoutInSeconds == 30
            assert httpClientConfiguration.healthCheckServices.maximumConnectionsTotal == 10
            assert httpClientConfiguration.healthCheckServices.maximumInMemorySizeInMegabytes == 1
    }
}
