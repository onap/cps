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
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.web.reactive.function.client.WebClient
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [DmiWebClientConfiguration.DmiProperties])
@TestPropertySource(properties = ["ncmp.dmi.httpclient.connectionTimeoutInSeconds=1", "ncmp.dmi.httpclient.maximumInMemorySizeInMegabytes=1"])
class DmiWebClientConfigurationSpec extends Specification {

    @Autowired
    DmiWebClientConfiguration.DmiProperties dmiProperties

    def objectUnderTest = new DmiWebClientConfiguration()

    def setup() {
        objectUnderTest.connectionTimeoutInSeconds = 10
        objectUnderTest.maximumInMemorySizeInMegabytes = 1
        objectUnderTest.maximumConnectionsTotal = 2
    }

    def 'DMI Properties.'() {
        expect: 'properties are set to values in test configuration yaml file'
            dmiProperties.authUsername == 'some-user'
            dmiProperties.authPassword == 'some-password'
    }

    def 'Web Client Configuration construction.'() {
        expect: 'the system can create an instance'
            new DmiWebClientConfiguration() != null
    }

    def 'Creating a WebClient instance.'() {
        given: 'WebClient configuration invoked'
            def webClientInstance = objectUnderTest.webClient()
        expect: 'the system can create an instance'
            assert webClientInstance != null
            assert webClientInstance instanceof WebClient
    }
}
