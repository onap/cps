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
@TestPropertySource(properties = ["ncmp.dmi.httpclient.data.readTimeoutInSeconds=123", "ncmp.dmi.httpclient.model.maximumConnectionsTotal=200"])
class HttpClientConfigurationSpec extends Specification {

    @Autowired
    private HttpClientConfiguration httpClientConfiguration

    def 'Test HttpClientConfiguration properties with custom and default values'() {
        expect: 'properties are populated correctly'
            assert httpClientConfiguration.data.connectionTimeoutInSeconds == 30
            assert httpClientConfiguration.data.readTimeoutInSeconds == 123
            assert httpClientConfiguration.data.writeTimeoutInSeconds == 30
            assert httpClientConfiguration.data.maximumConnectionsTotal == 100
            assert httpClientConfiguration.data.maximumInMemorySizeInMegabytes == 16
    }

    def 'Test HttpClientConfiguration properties with custom and default values1'() {
        expect: 'properties are populated correctly'
            assert httpClientConfiguration.model.connectionTimeoutInSeconds == 30
            assert httpClientConfiguration.model.readTimeoutInSeconds == 30
            assert httpClientConfiguration.model.writeTimeoutInSeconds == 30
            assert httpClientConfiguration.model.maximumConnectionsTotal == 200
            assert httpClientConfiguration.model.maximumInMemorySizeInMegabytes == 16
    }

    def 'Test HttpClientConfiguration properties with custom and default values2'() {
        expect: 'properties are populated correctly'
            assert httpClientConfiguration.health.connectionTimeoutInSeconds == 30
            assert httpClientConfiguration.health.readTimeoutInSeconds == 30
            assert httpClientConfiguration.health.writeTimeoutInSeconds == 30
            assert httpClientConfiguration.health.maximumConnectionsTotal == 10
            assert httpClientConfiguration.health.maximumInMemorySizeInMegabytes == 1
    }
}
