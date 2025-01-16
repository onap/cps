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
@ContextConfiguration(classes = [DmiHttpClientConfig])
@EnableConfigurationProperties
class DmiHttpClientConfigSpec extends Specification {

    @Autowired
    DmiHttpClientConfig dmiHttpClientConfig

    def 'Test http client configuration properties of data with custom and default values'() {
        expect: 'properties are populated correctly for data services'
            with(dmiHttpClientConfig.dataServices) {
                assert maximumInMemorySizeInMegabytes == 1
                assert maximumConnectionsTotal == 2
                assert pendingAcquireMaxCount == 3
                assert connectionTimeoutInSeconds == 4
                assert readTimeoutInSeconds == 5
                assert writeTimeoutInSeconds == 6
                assert responseTimeoutInSeconds == 60
            }
    }

    def 'Test http client configuration properties of model with custom and default values'() {
        expect: 'properties are populated correctly for model services'
            with(dmiHttpClientConfig.modelServices) {
                assert maximumInMemorySizeInMegabytes == 11
                assert maximumConnectionsTotal == 12
                assert pendingAcquireMaxCount == 13
                assert connectionTimeoutInSeconds == 14
                assert readTimeoutInSeconds == 15
                assert writeTimeoutInSeconds == 16
                assert responseTimeoutInSeconds == 60
            }
    }

    def 'Test http client configuration properties of health with default values'() {
        expect: 'properties are populated correctly for health check services'
            with(dmiHttpClientConfig.healthCheckServices) {
                assert maximumInMemorySizeInMegabytes == 21
                assert maximumConnectionsTotal == 22
                assert pendingAcquireMaxCount == 23
                assert connectionTimeoutInSeconds == 24
                assert readTimeoutInSeconds == 25
                assert writeTimeoutInSeconds == 26
                assert responseTimeoutInSeconds == 60
            }
    }
}
