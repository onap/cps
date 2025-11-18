/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2025 OpenInfra Foundation Europe. All rights reserved.
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

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.onap.cps.ncmp.api.exceptions.DmiClientRequestException
import org.onap.cps.ncmp.config.DmiHttpClientConfig
import org.onap.cps.ncmp.impl.utils.http.UrlTemplateParameters
import org.onap.cps.utils.JsonObjectMapper
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.InvalidUrlException
import spock.lang.Specification

import static org.onap.cps.ncmp.api.data.models.OperationType.CREATE
import static org.onap.cps.ncmp.api.data.models.OperationType.READ
import static org.onap.cps.ncmp.impl.models.RequiredDmiService.DATA
import static org.onap.cps.ncmp.impl.models.RequiredDmiService.MODEL

class DmiRestClientIntegrationSpec extends Specification {

    def mockWebServer = new MockWebServer()
    def baseUrl = mockWebServer.url('/')

    def urlTemplateParameters = new UrlTemplateParameters('/myPath', [someParam: 'value'])
    def mockDmiServiceAuthenticationProperties = Mock(DmiServiceAuthenticationProperties)

    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    def webClientBuilder = WebClient.builder().baseUrl(baseUrl.toString())
    def dmiWebClientsConfiguration = new DmiWebClientsConfiguration(new DmiHttpClientConfig())
    def webClientForMockServer = dmiWebClientsConfiguration.dataServicesWebClient(webClientBuilder)

    def objectUnderTest = new DmiRestClient(mockDmiServiceAuthenticationProperties, jsonObjectMapper, webClientForMockServer, webClientForMockServer, webClientForMockServer)

    def cleanup() throws IOException {
        mockWebServer.shutdown()
    }

    def 'Synchronous DMI #method request.'() {
        given: 'Web Server wil return OK response'
            mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value))
        when: 'synchronous #method request is made'
            def result
            switch(method) {
                case 'get':
                    result = objectUnderTest.synchronousGetOperation(DATA, urlTemplateParameters)
                    break
                case 'post':
                    result = objectUnderTest.synchronousPostOperation(DATA, urlTemplateParameters, 'body', CREATE, '')
                    break
                case 'put':
                    result = objectUnderTest.synchronousPutOperation(DATA, 'body', urlTemplateParameters)
                    break
                case 'patch':
                    result = objectUnderTest.synchronousPatchOperation(DATA, 'body', urlTemplateParameters, 'application/json-patch+json')
                    break
                case 'delete':
                    result = objectUnderTest.synchronousDeleteOperation(DATA, urlTemplateParameters)
            }
        then: 'the result has the same status code of 200'
            assert result.statusCode.value() == 200
        where: 'the following http methods are used'
            method << ['get', 'post', 'put', 'patch', 'delete']
    }

    def 'Synchronous DMI post request with invalid JSON.'() {
        given: 'Web Server wil return OK response'
            mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value)
                .setBody('invalid-json:!!')
                .addHeader('Content-Type', 'application/json'))
        when: 'synchronous post request is attempted (on Model service this time for coverage on service selector)'
            objectUnderTest.synchronousPostOperation(MODEL, urlTemplateParameters, 'body', READ, 'some authorization')
        then: 'a dmi client request exception is thrown with the correct error codes'
            def thrown = thrown(DmiClientRequestException)
            assert thrown.getHttpStatusCode() == 500
            assert thrown.ncmpResponseStatus.code == '108'
    }

    def 'DMI Request with non-responding server.'() {
        given: 'the web server is shut down'
            mockWebServer.shutdown()
        when: 'a synchronous post request is attempted'
            objectUnderTest.synchronousPostOperation(DATA, urlTemplateParameters,'body', CREATE, '' )
        then: 'a dmi client request exception is thrown with status code of 503 Service Unavailable'
            def thrown = thrown(DmiClientRequestException)
            assert thrown.getHttpStatusCode() == 503
    }

    def 'DMI Request with #scenario.'() {
        given: 'the mock server or exception setup'
            mockWebServer.enqueue(new MockResponse().setResponseCode(responseCode.value))
        when: 'a synchronous post request is attempted'
            objectUnderTest.synchronousPostOperation(DATA, urlTemplateParameters,'body', CREATE, '')
        then: 'a DMI client request exception is thrown with the right status'
            def thrown = thrown(DmiClientRequestException)
            assert thrown.httpStatusCode == expectedStatus
        where: 'the following HTTP Errors are applied'
            scenario             | responseCode                   || expectedStatus
            'Client error (418)' | HttpStatus.I_AM_A_TEAPOT       || 418
            'Timeout (408)'      | HttpStatus.REQUEST_TIMEOUT     || 408
            'Server error (503)' | HttpStatus.SERVICE_UNAVAILABLE || 503
    }

    def 'DMI Request with unexpected runtime exception.'() {
        given: 'Mock a bad URL that causes IllegalArgumentException before HTTP call'
            def badUrlParameters = new UrlTemplateParameters(':://bad url', [someParam: 'value'])
        when: 'a synchronous request is attempted'
            objectUnderTest.synchronousGetOperation(DATA, badUrlParameters)
        then: 'a invalid url exception is thrown (no mapping)'
            thrown(InvalidUrlException)
    }

    def 'DMI health status check with #scenario.'() {
        given: 'Web Server will return OK response with status TEST'
            mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value)
                .setBody(jsonBody)
                .addHeader('Content-Type', 'application/json'))
        when: 'DMI health status is checked'
            def result = objectUnderTest.getDmiHealthStatus(urlTemplateParameters).block()
        then: 'result is as expected'
            result == expectedResult
        where: 'following json data is used'
            scenario       | jsonBody            || expectedResult
            'Valid Json'   | '{"status":"TEST"}' || 'TEST'
            'Invalid Json' | 'invalid-json:!!'   || ''
    }

    def 'Asynchronous DMI data request.'() {
        given: 'web Server will return OK response with status TEST'
            mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.OK.value).setBody('TEST'))
        when: 'asynchronous DMI data request is made'
            def result = objectUnderTest.asynchronousDmiDataRequest(urlTemplateParameters, 'some authorization').block()
        then: 'result is TEST'
            assert result == 'TEST'
    }

    def 'Asynchronous DMI Data Request with Http Error'() {
        given: 'web Server will return OK response with status TEST'
            mockWebServer.enqueue(new MockResponse().setResponseCode(HttpStatus.I_AM_A_TEAPOT.value))
        when: 'asynchronous DMI data request is attempted'
            objectUnderTest.asynchronousDmiDataRequest(urlTemplateParameters, 'some authorization').block()
        then: 'a DMI client request exception is thrown with the same status code 418'
            def thrown = thrown(DmiClientRequestException)
            assert thrown.getHttpStatusCode() == 418
    }

}
