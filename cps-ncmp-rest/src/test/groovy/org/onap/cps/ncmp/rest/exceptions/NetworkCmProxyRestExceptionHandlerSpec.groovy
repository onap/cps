/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2021 highstreet technologies GmbH
 *  Modification Copyright (C) 2021 Nordix Foundation
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

package org.onap.cps.ncmp.rest.exceptions

import groovy.json.JsonSlurper
import org.onap.cps.ncmp.api.NetworkCmProxyDataService
import org.onap.cps.spi.FetchDescendantsOption
import org.onap.cps.spi.exceptions.CpsException
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Shared
import spock.lang.Specification

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@WebMvcTest
class NetworkCmProxyRestExceptionHandlerSpec extends Specification {

    @Autowired
    MockMvc mvc

    @SpringBean
    NetworkCmProxyDataService mockNetworkCmProxyDataService = Mock()

    @Value('${rest.api.ncmp-base-path}')
    def basePath

    def dataNodeBaseEndpoint

    @Shared
    def errorMessage = 'some error message'
    @Shared
    def errorDetails = 'some error details'

    def cmHandle = 'some handle'
    def xpath = 'some xpath'

    def setup() {
        dataNodeBaseEndpoint = "$basePath/v1"
    }

    def 'Get request with runtime exception returns HTTP Status Internal Server Error.'() {
        when: 'runtime exception is thrown by the service'
            setupTestException(new IllegalStateException(errorMessage))
            def response = performTestRequest()
        then: 'an HTTP Internal Server Error response is returned with correct message and details'
            assertTestResponse(response, INTERNAL_SERVER_ERROR, errorMessage, null)
    }

    def 'Get request with generic CPS exception returns HTTP Status Internal Server Error.'() {
        when: 'generic CPS exception is thrown by the service'
            setupTestException(new CpsException(errorMessage, errorDetails))
            def response = performTestRequest()
        then: 'an HTTP Internal Server Error response is returned with correct message and details'
            assertTestResponse(response, INTERNAL_SERVER_ERROR, errorMessage, errorDetails)
    }

    def setupTestException(exception) {
        mockNetworkCmProxyDataService.getDataNode(cmHandle, xpath, FetchDescendantsOption.OMIT_DESCENDANTS) >>
                { throw exception}
    }

    def performTestRequest() {
        return mvc.perform(get("$dataNodeBaseEndpoint/cm-handles/$cmHandle/node").param('xpath', xpath))
                .andReturn().response
    }

    static void assertTestResponse(response, expectedStatus,expectedErrorMessage,
                                   expectedErrorDetails) {
        assert response.status == expectedStatus.value()
        def content = new JsonSlurper().parseText(response.contentAsString)
        assert content['status'] == expectedStatus.toString()
        assert content['message'] == expectedErrorMessage
        assert expectedErrorDetails == null || content['details'] == expectedErrorDetails
    }
}
