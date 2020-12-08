/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
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

package org.onap.cps.rest.exceptions

import groovy.json.JsonSlurper
import org.onap.cps.api.CpService
import org.onap.cps.spi.exceptions.CpsException
import org.onap.cps.spi.exceptions.CpsNotFoundException
import org.onap.cps.spi.exceptions.ModelValidationException
import org.onap.cps.rest.controller.CpsRestController
import spock.lang.Specification

import static org.springframework.http.HttpStatus.BAD_REQUEST
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup

class CpsRestExceptionHandlerSpec extends Specification {

    def cpsRestController = new CpsRestController()
    def mockCpService = Mock(CpService.class)
    def objectUnderTest = new CpsRestExceptionHandler()
    def mockMvc = standaloneSetup(cpsRestController).setControllerAdvice(objectUnderTest).build()

    def setup() {
        cpsRestController.cpService = mockCpService
    }

    def 'Get request with runtime exception returns HTTP Status Internal Server Error'() {

        when: 'runtime exception is thrown by the service'
            def errorMessage = 'runtime error message'
            setupTestException(new IllegalStateException(errorMessage))
            def response = performTestRequest()

        then: 'an HTTP Internal Server Error response is returned with the correct message'
            assertTestResponse(response, INTERNAL_SERVER_ERROR, errorMessage, null)
    }

    def 'Get request with generic CPS exception returns HTTP Status Internal Server Error'() {

        when: 'generic CPS exception is thrown by the service'
            def errorMessage = 'cps generic error message'
            def errorDetails = 'cps generic error details'
            setupTestException(new CpsException(errorMessage, errorDetails))
            def response = performTestRequest()

        then: 'an HTTP Internal Server Error response is returned with the correct message'
            assertTestResponse(response, INTERNAL_SERVER_ERROR, errorMessage, errorDetails)
    }

    def 'Get request with no data found CPS exception returns HTTP Status Not Found'() {

        when: 'no data found CPS exception is thrown by the service'
            def errorMessage = 'cps no data error message'
            def errorDetails = 'cps no data error details'
            setupTestException(new CpsNotFoundException(errorMessage, errorDetails))
            def response = performTestRequest()

        then: 'an HTTP Not Found response is returned with the correct message'
            assertTestResponse(response, NOT_FOUND, errorMessage, errorDetails)
    }

    def 'Get request with a Model validation exception returns HTTP Status Bad Request'() {

        when: 'CPS validation exception is thrown by the service'
            def errorMessage = 'cps validation error message'
            def errorDetails = 'cps validation error details'
            setupTestException(new ModelValidationException(errorMessage, errorDetails))
            def response = performTestRequest()

        then: 'an HTTP Bad Request response is returned with the correct message'
            assertTestResponse(response, BAD_REQUEST, errorMessage, errorDetails)
    }

    /*
     * NB. The test uses 'get JSON by id' endpoint and associated service method invocation
     * to test the exception handling. The endpoint chosen is not a subject of test.
     */

    def setupTestException(exception) {
        mockCpService.getJsonById(_) >> { throw exception }
    }

    def performTestRequest() {
        return mockMvc.perform(get('/json-object/1')).andReturn().response
    }

    void assertTestResponse(response, expectedStatus, expectedErrorMessage, expectedErrorDetails) {
        assert response.status == expectedStatus.value()
        def content = new JsonSlurper().parseText(response.contentAsString)
        assert content['status'] == expectedStatus.toString()
        assert content['message'] == expectedErrorMessage
        assert expectedErrorDetails == null || content['details'] == expectedErrorDetails
    }

}
