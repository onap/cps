/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2026 OpenInfra Foundation Europe. All rights reserved.
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

package org.onap.cps.ncmp.rest.controller

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.http.HttpStatus.*
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*

@WebMvcTest([ProvMnSExtensionController])
class ProvMnSExtensionControllerSpec extends Specification{

    @Autowired
    MockMvc mvc

    @Value('${rest.api.provmns-extension-base-path}')
    def provMnSExtensionBasePath

    def 'Post data.'() {
        given: 'ProvMnSExtension url'
            def provMnSExtensionUrl = "$provMnSExtensionBasePath/v1/someFdn=1/someAction"
        when: 'post request is performed'
            def response = mvc.perform(post(provMnSExtensionUrl).contentType(MediaType.APPLICATION_JSON)
                    .content('{}')).andReturn().response
        then: 'response status is expected (Not Implemented)'
            assert response.status == NOT_IMPLEMENTED.value()
    }
}
