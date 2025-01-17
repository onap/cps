/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2024-2025 Nordix Foundation.
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
@ContextConfiguration(classes = [PolicyExecutorHttpClientConfig])
@EnableConfigurationProperties
class PolicyExecutorHttpClientConfigSpec extends Specification {

    @Autowired
    PolicyExecutorHttpClientConfig policyExecutorHttpClientConfig

    def 'Http client configuration properties for policy executor http client.'() {
        expect: 'properties are populated correctly for all services'
            with(policyExecutorHttpClientConfig.allServices) {
                assert maximumInMemorySizeInMegabytes == 31
                assert maximumConnectionsTotal == 32
                assert pendingAcquireMaxCount == 33
                assert connectionTimeoutInSeconds == 34
                assert writeTimeoutInSeconds == 36
                assert responseTimeoutInSeconds == 60
            }
    }

    def 'Increased read timeout.'() {
        expect: 'Read timeout is 10 seconds more then configured to enable a separate timeout method in policy executor with the required timeout'
            assert policyExecutorHttpClientConfig.allServices.readTimeoutInSeconds == 35 + 10

    }
}
