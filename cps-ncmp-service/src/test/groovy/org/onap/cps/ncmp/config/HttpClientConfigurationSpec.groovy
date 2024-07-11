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

package org.onap.cps.ncmp.config


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [HttpClientConfiguration])
@EnableConfigurationProperties(HttpClientConfiguration)
class HttpClientConfigurationSpec extends Specification {

    @Autowired
    HttpClientConfiguration httpClientConfiguration

    def 'Test http client configuration properties of data with custom and default values'() {
        expect: 'properties are populated correctly for data'
            with(httpClientConfiguration.dataServices) {
                assert connectionTimeoutInSeconds == 123
                assert readTimeoutInSeconds == 33
                assert writeTimeoutInSeconds == 30
                assert maximumConnectionsTotal == 100
                assert pendingAcquireMaxCount == 22
                assert maximumInMemorySizeInMegabytes == 7
            }
    }

    def 'Test http client configuration properties of model with custom and default values'() {
        expect: 'properties are populated correctly for model'
            with(httpClientConfiguration.modelServices) {
                assert connectionTimeoutInSeconds == 456
                assert readTimeoutInSeconds == 30
                assert writeTimeoutInSeconds == 30
                assert maximumConnectionsTotal == 111
                assert pendingAcquireMaxCount == 44
                assert maximumInMemorySizeInMegabytes == 8
            }
    }

    def 'Test http client configuration properties of health with default values'() {
        expect: 'properties are populated correctly for health'
            with(httpClientConfiguration.healthCheckServices) {
                assert connectionTimeoutInSeconds == 30
                assert readTimeoutInSeconds == 30
                assert writeTimeoutInSeconds == 30
                assert maximumConnectionsTotal == 10
                assert pendingAcquireMaxCount == 5
                assert maximumInMemorySizeInMegabytes == 1
            }
    }
}
