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

import com.google.gson.Gson
import org.onap.cps.api.NfProxyDataService
import org.onap.cps.spi.model.DataNodeBuilder
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification
import spock.lang.Unroll

import static org.onap.cps.spi.FetchDescendantsOption.OMIT_DESCENDANTS
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@WebMvcTest
class NfProxyControllerSpec extends Specification {

    @Autowired
    MockMvc mvc

    @SpringBean
    NfProxyDataService mockNfProxyDataService = Mock()

    @Value('${rest.api.xnf-base-path}')
    def basePath

    def dataNodeBaseEndpoint

    def setup() {
        dataNodeBaseEndpoint = "$basePath/v1"
    }

    @Unroll
    def 'Get data node.'() {
        given: 'the service returns a data node'
            def dataNode = new DataNodeBuilder().withXpath('some xpath').build()
            def cmHandleId = 'some handle'
            def xpath = 'some xPath'
            def endpoint = "$dataNodeBaseEndpoint/cm-handles/$cmHandleId/node"
            mockNfProxyDataService.getDataNode(cmHandleId, xpath, OMIT_DESCENDANTS) >> dataNode
        when: 'get request is performed through REST API'
            def response = mvc.perform(get(endpoint).param('cps-path', xpath)).andReturn().response
        then: 'a success response is returned'
            response.status == HttpStatus.OK.value()
        and: 'response contains expected datanode in json format'
            response.getContentAsString().contains(new Gson().toJson(dataNode))
    }
}
