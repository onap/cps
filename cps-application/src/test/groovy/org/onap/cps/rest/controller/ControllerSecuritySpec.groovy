/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2020 Pantheon.tech
 * Modifications Copyright (C) 2023 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.rest.controller

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Ignore
import spock.lang.Specification

@WebMvcTest(TestController)
class ControllerSecuritySpec extends Specification {

    @Autowired
    MockMvc mvc

    def testEndpoint = '/test'

    def 'Get request with authentication'() {
        when: 'request is sent with authentication'
            def response = mvc.perform(
                    get(testEndpoint).header("Authorization", 'Basic Y3BzdXNlcjpjcHNyMGNrcyE=')
            ).andReturn().response
        then: 'HTTP OK status code is returned'
            assert response.status == HttpStatus.OK.value()
    }

    @Ignore // CPS-2126
    def 'Get request without authentication is not authorized'() {
        when: 'request is sent without authentication'
            def response = mvc.perform(get(testEndpoint)).andReturn().response
        then: 'HTTP Unauthorized status code is returned'
            assert response.status == HttpStatus.UNAUTHORIZED.value()
    }

    @Ignore // CPS-2126
    def 'Get request with invalid authentication is not authorized'() {
        when: 'request is sent with invalid authentication'
            def response = mvc.perform(
                    get(testEndpoint).header("Authorization", 'Basic invalid auth')
            ).andReturn().response
        then: 'HTTP Unauthorized status code is returned'
            assert response.status == HttpStatus.UNAUTHORIZED.value()
    }
}
