/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021 Pantheon.tech
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.nfproxy.rest.controller

import org.onap.cps.api.CpsAdminService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import spock.lang.Specification

@WebMvcTest
class NfProxyControllerSpec extends Specification {

    @Autowired
    MockMvc mvc

    @Value('${rest.api.base-path}')
    def basePath

    def 'Hello world method invocation.'(){
        when: 'hello-world request performed'
            def response = mvc.perform(MockMvcRequestBuilders.get("$basePath/v1/hello-world")).andReturn().response
        then: 'success response returned'
            response.status == HttpStatus.OK.value()
            response.getContentAsString().contains("Hello World!")
    }

    def 'Example error handling.'(){
        when: 'hello-error request performed'
            def response = mvc.perform(MockMvcRequestBuilders.get("$basePath/v1/hello-error")).andReturn().response
        then: 'error response returned'
            response.status == HttpStatus.INTERNAL_SERVER_ERROR.value()
            response.getContentAsString().contains("Example error")
    }
}
