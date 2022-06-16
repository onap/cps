/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

package org.onap.cps.ncmp.rest.controller.async

import com.fasterxml.jackson.databind.ObjectMapper
import org.mapstruct.factory.Mappers
import org.onap.cps.ncmp.api.NetworkCmProxyDataService
import org.onap.cps.ncmp.rest.controller.NcmpRestInputMapper
import org.onap.cps.ncmp.rest.controller.NetworkCmProxyController
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor
import org.onap.cps.ncmp.rest.mapper.RestOutputCmHandleStateMapper
import org.onap.cps.ncmp.rest.util.DeprecationHelper
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Specification

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*

@WebMvcTest(NetworkCmProxyController)
@TestPropertySource(properties = "notification.async.enabled=false")
class NetworkCmProxyControllerAsyncSpec extends Specification {

    @Autowired
    MockMvc mvc

    @SpringBean
    NetworkCmProxyDataService mockNetworkCmProxyDataService = Mock()

    @SpringBean
    ObjectMapper objectMapper = new ObjectMapper()

    @SpringBean
    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(objectMapper)

    @SpringBean
    NcmpRestInputMapper ncmpRestInputMapper = Mappers.getMapper(NcmpRestInputMapper)

    @SpringBean
    RestOutputCmHandleStateMapper restOutputCmHandleStateMapper = Mappers.getMapper(RestOutputCmHandleStateMapper)

    @SpringBean
    CpsNcmpTaskExecutor spiedCpsTaskExecutor = Spy()

    @SpringBean
    DeprecationHelper stubbedDeprecationHelper = Stub()

    @Value('${rest.api.ncmp-base-path}/v1')
    def ncmpBasePathV1


    def 'async request for #datastoreInUrl disabled when flag is false'() {
        given: 'resource data url'
            def getUrl = "$ncmpBasePathV1/ch/testCmHandle/data/ds/ncmp-datastore:${datastoreInUrl}" +
                "?resourceIdentifier=parent/child&options=(a=1,b=2)&topic=my-topic-name"
        when: 'get data resource request is performed'
            def response = mvc.perform(
                get(getUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON_VALUE)
            ).andReturn().response
        then: 'async request id is generated'
            assert response.contentAsString.contains("Asynchronous messaging is currently disabled for " + datastoreInUrl)
        where: 'the following parameters are used'
            datastoreInUrl << ['passthrough-operational', 'passthrough-running']
    }

}

