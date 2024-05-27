/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2024 Nordix Foundation
 *  Modifications Copyright (C) 2022 Bell Canada
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

package org.onap.cps.ncmp.api.impl.client

import static org.onap.cps.ncmp.api.NcmpResponseStatus.UNKNOWN_ERROR
import static org.onap.cps.ncmp.api.impl.operations.OperationType.CREATE
import static org.onap.cps.ncmp.api.impl.operations.OperationType.PATCH
import static org.onap.cps.ncmp.api.impl.operations.OperationType.READ
import static org.onap.cps.ncmp.api.impl.operations.RequiredDmiService.DATA
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE
import static org.onap.cps.ncmp.api.NcmpResponseStatus.DMI_SERVICE_NOT_RESPONDING
import static org.onap.cps.ncmp.api.NcmpResponseStatus.UNABLE_TO_READ_RESOURCE_DATA

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.onap.cps.ncmp.api.impl.exception.DmiClientRequestException
import org.onap.cps.ncmp.api.impl.exception.InvalidDmiResourceUrlException
import org.onap.cps.ncmp.api.impl.config.DmiProperties
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.reactive.function.client.WebClientRequestException
import reactor.core.publisher.Mono
import spock.lang.Specification

class DmiRestClientSpec extends Specification {

    static final NO_AUTH_HEADER = null
    static final BASIC_AUTH_HEADER = 'Basic c29tZSB1c2VyOnNvbWUgcGFzc3dvcmQ='
    static final BEARER_AUTH_HEADER = 'Bearer my-bearer-token'

    def mockDataServicesWebClient = Mock(WebClient)
    def mockModelServicesWebClient = Mock(WebClient)
    def mockHealthChecksWebClient = Mock(WebClient)

    def mockRequestBody = Mock(WebClient.RequestBodyUriSpec)
    def mockResponse = Mock(WebClient.ResponseSpec)

    def responseBody = [message: 'Success']
    def mockDmiProperties = Mock(DmiProperties)

    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    DmiRestClient objectUnderTest = new DmiRestClient(mockDmiProperties, jsonObjectMapper, mockDataServicesWebClient, mockModelServicesWebClient, mockHealthChecksWebClient)

    def setup() {
        mockRequestBody.uri(_) >> mockRequestBody
        mockRequestBody.headers(_) >> mockRequestBody
        mockRequestBody.body(_) >> mockRequestBody
        mockRequestBody.retrieve() >> mockResponse
    }

    def 'DMI POST Operation with JSON for status #httpStatusCode'() {
        given: 'the web client returns a valid response entity for the expected parameters'
            mockDataServicesWebClient.post() >> mockRequestBody
            mockResponse.toEntity(Object.class) >> Mono.just(new ResponseEntity<>(responseBody, httpStatusCode))
        when: 'POST operation is invoked'
            def response = objectUnderTest.postOperationWithJsonData(DATA, '/my/url', 'some json', READ, NO_AUTH_HEADER)
        then: 'the output of the method is equal to the output from the test template'
            assert response.statusCode == httpStatusCode
            assert response.body == responseBody
        where: 'the following status codes are used'
            httpStatusCode << [HttpStatus.OK, HttpStatus.CREATED, HttpStatus.ACCEPTED]
    }

    def 'Failing DMI POST operation for server error'() {
        given: 'the web client throws an exception'
            mockDataServicesWebClient.post() >> { throw new HttpServerErrorException(SERVICE_UNAVAILABLE, null, null, null) }
        when: 'POST operation is invoked'
            objectUnderTest.postOperationWithJsonData(DATA, '/some', 'some json', READ, NO_AUTH_HEADER)
        then: 'a http client exception is thrown'
            def thrown = thrown(DmiClientRequestException)
        and: 'the exception has the relevant details from the error response'
            thrown.ncmpResponseStatus.code == '102'
            thrown.httpStatusCode == 503
    }

    def 'Failing DMI POST operation due to invalid dmi resource url.'() {
        when: 'POST operation is invoked with invalid dmi resource url'
            objectUnderTest.postOperationWithJsonData(DATA, '/invalid dmi url', null, null, NO_AUTH_HEADER)
        then: 'invalid dmi resource url exception is thrown'
            def thrown = thrown(InvalidDmiResourceUrlException)
        and: 'the exception has the relevant details from the error response'
            thrown.httpStatus == 400
            thrown.message == 'Invalid dmi resource url: /invalid dmi url'
        where: 'the following operations are executed'
            operation << [CREATE, READ, PATCH]
    }

    def 'Dmi service sends client error response when #scenario'() {
        given: 'the web client unable to return response entity but error'
            mockDataServicesWebClient.post() >> mockRequestBody
            mockResponse.toEntity(Object.class) >> Mono.error(exceptionType)
        when: 'POST operation is invoked'
            objectUnderTest.postOperationWithJsonData(DATA, '/my/url', 'some json', READ, NO_AUTH_HEADER)
        then: 'a http client exception is thrown'
            def thrown = thrown(DmiClientRequestException)
        and: 'the exception has the relevant details from the error response'
            thrown.ncmpResponseStatus == expectedNcmpResponseStatusCode
            thrown.httpStatusCode == httpStatusCode
        where: 'the following errors occur'
            scenario                  | httpStatusCode | exceptionType                                                                                    || expectedNcmpResponseStatusCode
            'dmi service unavailable' | 503            | new WebClientRequestException(new RuntimeException('some-error'), null, null, new HttpHeaders()) || DMI_SERVICE_NOT_RESPONDING
            'dmi request timeout'     | 408            | new WebClientResponseException('message', httpStatusCode, 'statusText', null, null, null)        || DMI_SERVICE_NOT_RESPONDING
            'dmi server error'        | 500            | new WebClientResponseException('message', httpStatusCode, 'statusText', null, null, null)        || UNABLE_TO_READ_RESOURCE_DATA
            'unknown error'           | 500            | new Throwable('message')                                                                         || UNKNOWN_ERROR
    }

    def 'Dmi trust level is determined by spring boot health status'() {
        given: 'a health check response'
            def dmiPluginHealthCheckResponseJsonData = TestUtils.getResourceFileContent('dmiPluginHealthCheckResponse.json')
            def jsonNode = jsonObjectMapper.convertJsonString(dmiPluginHealthCheckResponseJsonData, JsonNode.class)
            ((ObjectNode) jsonNode).put('status', 'my status')
            mockHealthChecksWebClient.get() >> mockRequestBody
            mockResponse.bodyToMono(JsonNode.class) >> Mono.just(jsonNode)
        when: 'get trust level of the dmi plugin'
            def result = objectUnderTest.getDmiHealthStatus('some/url')
        then: 'the status value from the json is returned'
            assert result == 'my status'
    }

    def 'Failing to get dmi plugin health status #scenario'() {
        given: 'rest template with #scenario'
            mockHealthChecksWebClient.get() >> mockRequestBody
            mockResponse.bodyToMono(_) >> healthStatusResponse
        when: 'attempt to get health status of the dmi plugin'
            def result = objectUnderTest.getDmiHealthStatus('some url')
        then: 'result will be empty'
            assert result == ''
        where: 'the following responses are used'
            scenario    | healthStatusResponse
            'null'      | null
            'exception' | { throw new Exception() }
    }

    def 'DMI auth header #scenario'() {
        when: 'Specific dmi properties are provided'
            mockDmiProperties.dmiBasicAuthEnabled >> authEnabled
            mockDmiProperties.authUsername >> 'some user'
            mockDmiProperties.authPassword >> 'some password'
        then: 'http headers to conditionally have Authorization header'
            def httpHeaders = new HttpHeaders()
            objectUnderTest.configureHttpHeaders(httpHeaders, ncmpAuthHeader)
            def outputAuthHeader = (httpHeaders.Authorization == null ? null : httpHeaders.Authorization[0])
            assert outputAuthHeader == expectedAuthHeader
        where: 'the following configurations are used'
            scenario                                          | authEnabled | ncmpAuthHeader     || expectedAuthHeader
            'DMI basic auth enabled, no NCMP bearer token'    | true        | NO_AUTH_HEADER     || BASIC_AUTH_HEADER
            'DMI basic auth enabled, with NCMP bearer token'  | true        | BEARER_AUTH_HEADER || BASIC_AUTH_HEADER
            'DMI basic auth disabled, no NCMP bearer token'   | false       | NO_AUTH_HEADER     || NO_AUTH_HEADER
            'DMI basic auth disabled, with NCMP bearer token' | false       | BEARER_AUTH_HEADER || BEARER_AUTH_HEADER
            'DMI basic auth disabled, with NCMP basic auth'   | false       | BASIC_AUTH_HEADER  || NO_AUTH_HEADER
    }
}