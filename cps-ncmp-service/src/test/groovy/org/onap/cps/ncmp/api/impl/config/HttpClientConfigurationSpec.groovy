/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2023 Nordix Foundation.
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

import java.time.Duration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.support.AnnotationConfigContextLoader
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [HttpClientConfiguration])
@EnableConfigurationProperties(HttpClientConfiguration.class)
@TestPropertySource(properties = ["httpclient5.connectionTimeoutInSeconds=1", "httpclient5.maximumConnectionsTotal=200"])
class HttpClientConfigurationSpec extends Specification {

    @Autowired
    private HttpClientConfiguration httpClientConfiguration

    def 'Test HttpClientConfiguration properties with custom and default values'() {
        expect: 'custom property values'
            httpClientConfiguration.getConnectionTimeoutInSeconds() == Duration.ofSeconds(1)
            httpClientConfiguration.getMaximumConnectionsTotal() == 200
        and: 'default property values'
            httpClientConfiguration.getMaximumConnectionsPerRoute() == 50
            httpClientConfiguration.getIdleConnectionEvictionThresholdInSeconds() == Duration.ofSeconds(5)
    }
}
