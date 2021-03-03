/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2020 Pantheon.tech
 *  Copyright (C) 2021 Nordix Foundation
 *  Modifications Copyright (C) 2021 Bell Canada.
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
import org.modelmapper.ModelMapper
import org.onap.cps.api.CpsAdminService
import org.onap.cps.api.CpsDataService
import org.onap.cps.api.CpsModuleService
import org.onap.cps.api.CpsQueryService
import org.onap.cps.rest.controller.RestControllerSpecification
import org.onap.cps.spi.exceptions.AnchorAlreadyDefinedException
import org.onap.cps.spi.exceptions.CpsException
import org.onap.cps.spi.exceptions.DataInUseException
import org.onap.cps.spi.exceptions.DataValidationException
import org.onap.cps.spi.exceptions.ModelValidationException
import org.onap.cps.spi.exceptions.NotFoundInDataspaceException
import org.onap.cps.spi.exceptions.SchemaSetAlreadyDefinedException
import org.onap.cps.spi.exceptions.SchemaSetInUseException
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import spock.lang.Shared
import spock.lang.Unroll

import static org.springframework.http.HttpStatus.BAD_REQUEST
import static org.springframework.http.HttpStatus.CONFLICT
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR
import static org.springframework.http.HttpStatus.NOT_FOUND
import static org.springframework.http.HttpStatus.UNAUTHORIZED
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

@WebMvcTest
class CpsRestExceptionHandlerSpec extends RestControllerSpecification {

    @SpringBean
    CpsAdminService mockCpsAdminService = Mock()

    @SpringBean
    CpsModuleService mockCpsModuleService = Mock()

    @SpringBean
    CpsDataService mockCpsDataService = Mock()

    @SpringBean
    CpsQueryService mockCpsQueryService = Mock()

    @SpringBean
    ModelMapper modelMapper = Mock()

    @Autowired
    MockMvc mvc

    @Value('${rest.api.cps-base-path}')
    def basePath

    @Shared
    def errorMessage = 'some error message'
    @Shared
    def errorDetails = 'some error details'
    @Shared
    def dataspaceName = 'MyDataSpace'
    @Shared
    def existingObjectName = 'MyAdminObject'


    def 'Get request with runtime exception returns HTTP Status Internal Server Error'() {

        when: 'runtime exception is thrown by the service'
            setupTestException(new IllegalStateException(errorMessage))
            def response = performTestRequest()

        then: 'an HTTP Internal Server Error response is returned with correct message and details'
            assertTestResponse(response, INTERNAL_SERVER_ERROR, errorMessage, null)
    }

    def 'Get request with generic CPS exception returns HTTP Status Internal Server Error'() {

        when: 'generic CPS exception is thrown by the service'
            setupTestException(new CpsException(errorMessage, errorDetails))
            def response = performTestRequest()

        then: 'an HTTP Internal Server Error response is returned with correct message and details'
            assertTestResponse(response, INTERNAL_SERVER_ERROR, errorMessage, errorDetails)
    }

    def 'Get request with no data found CPS exception returns HTTP Status Not Found'() {

        when: 'no data found CPS exception is thrown by the service'
            def dataspaceName = 'MyDataSpace'
            def descriptionOfObject = 'Description'
            setupTestException(new NotFoundInDataspaceException(dataspaceName, descriptionOfObject))
            def response = performTestRequest()

        then: 'an HTTP Not Found response is returned with correct message and details'
            assertTestResponse(response, NOT_FOUND, 'Object not found',
                    'Description does not exist in dataspace MyDataSpace.')
    }

    @Unroll
    def 'request with an expectedObjectTypeInMessage object already defined exception returns HTTP Status Bad Request'() {

        when: 'no data found CPS exception is thrown by the service'
            setupTestException(exceptionThrown)
            def response = performTestRequest()

        then: 'an HTTP Bad Request response is returned with correct message an details'
            assertTestResponse(response, BAD_REQUEST,
                    "Duplicate ${expectedObjectTypeInMessage}",
                    "${expectedObjectTypeInMessage} with name ${existingObjectName} " +
                            'already exists for dataspace MyDataSpace.')
        where: 'the following exceptions are thrown'
            exceptionThrown                                                               || expectedObjectTypeInMessage
            new SchemaSetAlreadyDefinedException(dataspaceName, existingObjectName, null) || 'Schema Set'
            new AnchorAlreadyDefinedException(dataspaceName, existingObjectName, null)    || 'Anchor'
    }

    @Unroll
    def 'Get request with a #exceptionThrown.class.simpleName returns HTTP Status Bad Request'() {

        when: 'CPS validation exception is thrown by the service'
            setupTestException(exceptionThrown)
            def response = performTestRequest()

        then: 'an HTTP Bad Request response is returned with correct message and details'
            assertTestResponse(response, BAD_REQUEST, errorMessage, errorDetails)

        where: 'the following exceptions are thrown'
            exceptionThrown << [new ModelValidationException(errorMessage, errorDetails, null),
                                new DataValidationException(errorMessage, errorDetails, null)]
    }

    @Unroll
    def 'Delete request with a #exceptionThrown.class.simpleName returns HTTP Status Conflict'() {

        when: 'CPS validation exception is thrown by the service'
            setupTestException(exceptionThrown)
            def response = performTestRequest()

        then: 'an HTTP Conflict response is returned with correct message and details'
            assertTestResponse(response, CONFLICT, exceptionThrown.getMessage(), exceptionThrown.getDetails())

        where: 'the following exceptions are thrown'
            exceptionThrown << [new DataInUseException(dataspaceName, existingObjectName),
                                new SchemaSetInUseException(dataspaceName, existingObjectName)]
    }

    def 'Get request without authentication is not authorized'() {
        when: 'request is sent without authentication'
            def response =
                    mvc.perform(get("$basePath/v1/dataspaces/dataspace-name/anchors")).andReturn().response
        then: 'HTTP Unauthorized status code is returned'
            assert UNAUTHORIZED.value() == response.status
    }

    def 'Get request with invalid authentication is not authorized'() {
        when: 'request is sent with invalid authentication'
            def response =
                    mvc.perform(
                            get("$basePath/v1/dataspaces/dataspace-name/anchors")
                                    .header("Authorization", 'Basic invalid auth'))
                            .andReturn().response
        then: 'HTTP Unauthorized status code is returned'
            assert UNAUTHORIZED.value() == response.status
    }

    /*
     * NB. The test uses 'get JSON by id' endpoint and associated service method invocation
     * to test the exception handling. The endpoint chosen is not a subject of test.
     */

    def setupTestException(exception) {
        mockCpsAdminService.getAnchors(_) >> { throw exception}
    }

    def performTestRequest() {
        return mvc.perform(
                get("$basePath/v1/dataspaces/dataspace-name/anchors")
                        .header("Authorization", getAuthorizationHeader()))
                .andReturn().response
    }

    void assertTestResponse(response, expectedStatus,
                            expectedErrorMessage, expectedErrorDetails) {
        assert response.status == expectedStatus.value()
        def content = new JsonSlurper().parseText(response.contentAsString)
        assert content['status'] == expectedStatus.toString()
        assert content['message'] == expectedErrorMessage
        assert expectedErrorDetails == null || content['details'] == expectedErrorDetails
    }
}
