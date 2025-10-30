/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.impl.data.policyexecutor

import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.config.PolicyExecutorHttpClientConfig
import org.onap.cps.ncmp.impl.policyexecutor.PolicyExecutorWebClientConfiguration
import org.onap.cps.ncmp.utils.WebClientBuilderTestConfig
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification

@SpringBootTest
@ContextConfiguration(classes = [JsonObjectMapper, ObjectMapper, PolicyExecutor, PolicyExecutorWebClientConfiguration,  PolicyExecutorHttpClientConfig, WebClientBuilderTestConfig ])
class PolicyExecutorConfigurationSpec extends Specification {

    @Autowired
    PolicyExecutor objectUnderTest

    def 'Policy executor configuration properties.'() {
        expect: 'properties used from application.yml'
            assert objectUnderTest.enabled
            assert objectUnderTest.defaultDecision == 'some default decision'
            assert objectUnderTest.serverAddress == 'http://localhost'
            assert objectUnderTest.serverPort == '8785'
            assert objectUnderTest.readTimeoutInSeconds == 35
    }
}
