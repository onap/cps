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

package org.onap.cps.ncmp.impl.dmi

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.onap.cps.ncmp.api.exceptions.DmiClientRequestException
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import spock.lang.Specification

import static org.onap.cps.ncmp.api.NcmpResponseStatus.DMI_SERVICE_NOT_RESPONDING
import static org.onap.cps.ncmp.api.NcmpResponseStatus.UNABLE_TO_READ_RESOURCE_DATA
import static org.onap.cps.ncmp.api.NcmpResponseStatus.UNKNOWN_ERROR
import static org.onap.cps.ncmp.api.data.models.OperationType.READ
import static org.onap.cps.ncmp.impl.models.RequiredDmiService.DATA
import static org.onap.cps.ncmp.impl.models.RequiredDmiService.MODEL

class DmiRestClientSpec extends Specification {

    static final NO_AUTH_HEADER = null
    static final BASIC_AUTH_HEADER = 'Basic c29tZSB1c2VyOnNvbWUgcGFzc3dvcmQ='
    static final BEARER_AUTH_HEADER = 'Bearer my-bearer-token'
    static final urlTemplateParameters = new UrlTemplateParameters('/{pathParam1}/{pathParam2}', ['pathParam1': 'my', 'pathParam2': 'url'])

    def mockDataServicesWebClient = Mock(WebClient)
    def mockModelServicesWebClient = Mock(WebClient)
    def mockHealthChecksWebClient = Mock(WebClient)

    def mockRequestBody = Mock(WebClient.RequestBodyUriSpec)
    def mockResponse = Mock(WebClient.ResponseSpec)

    def mockDmiProperties = Mock(DmiProperties)

    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    DmiRestClient objectUnderTest = new DmiRestClient(mockDmiProperties, jsonObjectMapper, mockDataServicesWebClient, mockModelServicesWebClient, mockHealthChecksWebClient)

    def setup() {
        mockRequestBody.uri(_,_) >> mockRequestBody
        mockRequestBody.headers(_) >> mockRequestBody
        mockRequestBody.body(_) >> mockRequestBody
        mockRequestBody.retrieve() >> mockResponse
    }

    def 'DMI POST Operation with JSON for DMI Data Service '() {
        given: 'the Data web client returns a valid response entity for the expected parameters'
            mockDataServicesWebClient.post() >> mockRequestBody
            mockResponse.toEntity(Object.class) >> Mono.just(new ResponseEntity<>('from Data service', HttpStatus.I_AM_A_TEAPOT))
        when: 'POST operation is invoked fro Data Service'
            def response = objectUnderTest.synchronousPostOperationWithJsonData(DATA, urlTemplateParameters, 'some json', READ, NO_AUTH_HEADER)
        then: 'the output of the method is equal to the output from the test template'
            assert response.statusCode == HttpStatus.I_AM_A_TEAPOT
            assert response.body == 'from Data service'
    }

    def 'DMI POST Operation with JSON for DMI Model Service '() {
        given: 'the Model web client returns a valid response entity for the expected parameters'
            mockModelServicesWebClient.post() >> mockRequestBody
            mockResponse.toEntity(Object.class) >> Mono.just(new ResponseEntity<>('from Model service', HttpStatus.I_AM_A_TEAPOT))
        when: 'POST operation is invoked for Model Service'
            def response = objectUnderTest.synchronousPostOperationWithJsonData(MODEL, urlTemplateParameters, 'some json', READ, NO_AUTH_HEADER)
        then: 'the output of the method is equal to the output from the test template'
            assert response.statusCode == HttpStatus.I_AM_A_TEAPOT
            assert response.body == 'from Model service'
    }

    def 'Dmi service sends client error response when #scenario'() {
        given: 'the web client unable to return response entity but error'
            mockDataServicesWebClient.post() >> mockRequestBody
            mockResponse.toEntity(Object.class) >> Mono.error(exceptionType)
        when: 'POST operation is invoked'
            objectUnderTest.synchronousPostOperationWithJsonData(DATA, urlTemplateParameters, 'some json', READ, NO_AUTH_HEADER)
        then: 'a http client exception is thrown'
            def thrown = thrown(DmiClientRequestException)
        and: 'the exception has the relevant details from the error response'
            assert thrown.ncmpResponseStatus == expectedNcmpResponseStatusCode
            assert thrown.httpStatusCode == httpStatusCode
        where: 'the following errors occur'
            scenario                  | httpStatusCode | exceptionType                                                                                    || expectedNcmpResponseStatusCode
            'dmi service unavailable' | 503            | new WebClientRequestException(new RuntimeException('some-error'), null, null, new HttpHeaders()) || DMI_SERVICE_NOT_RESPONDING
            'dmi request timeout'     | 408            | new WebClientResponseException('message', httpStatusCode, 'statusText', null, null, null)        || DMI_SERVICE_NOT_RESPONDING
            'dmi server error'        | 500            | new WebClientResponseException('message', httpStatusCode, 'statusText', null, null, null)        || UNABLE_TO_READ_RESOURCE_DATA
            'dmi service unavailable' | 503            | new HttpServerErrorException(HttpStatusCode.valueOf(503))                                        || DMI_SERVICE_NOT_RESPONDING
            'unknown error'           | 500            | new Throwable('message')                                                                         || UNKNOWN_ERROR
    }

    def 'Dmi trust level is determined by spring boot health status'() {
        given: 'a health check response'
            def dmiPluginHealthCheckResponseJsonData = TestUtils.getResourceFileContent('dmiPluginHealthCheckResponse.json')
            def jsonNode = jsonObjectMapper.convertJsonString(dmiPluginHealthCheckResponseJsonData, JsonNode.class)
            ((ObjectNode) jsonNode).put('status', 'my status')
            mockHealthChecksWebClient.get() >> mockRequestBody
            mockResponse.onStatus(_,_)>> mockResponse
            mockResponse.bodyToMono(JsonNode.class) >> Mono.just(jsonNode)
        when: 'get trust level of the dmi plugin'
            def urlTemplateParameters = new UrlTemplateParameters('some url', [:])
            def result = objectUnderTest.getDmiHealthStatus(urlTemplateParameters).block()
        then: 'the status value from the json is returned'
            assert result == 'my status'
    }

    def 'Failing to get dmi plugin health status #scenario'() {
        given: 'web client instance with #scenario'
            mockHealthChecksWebClient.get() >> mockRequestBody
            mockResponse.onStatus(_, _) >> mockResponse
            mockResponse.bodyToMono(_) >> Mono.error(exceptionType)
        when: 'attempt to get health status of the dmi plugin'
            def urlTemplateParameters = new UrlTemplateParameters('some url', [:])
            def result = objectUnderTest.getDmiHealthStatus(urlTemplateParameters).block()
        then: 'result will be empty'
            assert result == ''
        where: 'the following responses are used'
            scenario                  | exceptionType
            'dmi request timeout'     | new WebClientResponseException('some-message', 408, 'some-text', null, null, null)
            'dmi service unavailable' | new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE)
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

    def 'DMI GET Operation for DMI Data Service '() {
        given: 'the Data web client returns a valid response entity for the expected parameters'
            mockDataServicesWebClient.get() >> mockRequestBody
            def jsonNode = jsonObjectMapper.convertJsonString('{"status":"some status"}', JsonNode.class)
            ((ObjectNode) jsonNode).put('status', 'some status')
            mockResponse.bodyToMono(JsonNode.class) >> Mono.just(jsonNode)
        when: 'GET operation is invoked for Data Service'
            def response = objectUnderTest.getDataJobStatus(urlTemplateParameters, NO_AUTH_HEADER).block()
        then: 'the response equals to the expected value'
            assert response == 'some status'
    }

    def 'Get data job result from DMI.'() {
        given: 'the Data web client returns a valid response entity for the expected parameters'
            mockDataServicesWebClient.get() >> mockRequestBody
            def resultObject = new Object()
            mockResponse.bodyToMono(Object.class) >> Mono.just(resultObject)
        when: 'GET operation is invoked for Data Service'
            def response = objectUnderTest.getDataJobResult(urlTemplateParameters, NO_AUTH_HEADER).block()
        then: 'the response equals to the expected value'
            assert response != null
    }
}
