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

import static org.onap.cps.ncmp.api.impl.operations.OperationType.READ
import static org.onap.cps.ncmp.api.impl.operations.OperationType.PATCH
import static org.onap.cps.ncmp.api.impl.operations.OperationType.CREATE
import static org.springframework.http.HttpStatus.GATEWAY_TIMEOUT
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.onap.cps.ncmp.api.impl.config.DmiWebClientConfiguration
import org.onap.cps.ncmp.api.impl.exception.InvalidDmiResourceUrlException
import org.onap.cps.ncmp.api.impl.exception.DmiClientRequestException
import org.onap.cps.ncmp.utils.TestUtils
import org.onap.cps.utils.JsonObjectMapper
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import spock.lang.Specification
import org.springframework.web.reactive.function.client.WebClientResponseException

@SpringBootTest
@ContextConfiguration(classes = [DmiWebClientConfiguration, DmiRestClient, ObjectMapper])
class DmiRestClientSpec extends Specification {

    static final NO_AUTH_HEADER = null
    static final BASIC_AUTH_HEADER = 'Basic c29tZS11c2VyOnNvbWUtcGFzc3dvcmQ='
    static final BEARER_AUTH_HEADER = 'Bearer my-bearer-token'

    @Autowired
    DmiWebClientConfiguration.DmiProperties dmiProperties

    @Autowired
    DmiRestClient objectUnderTest

    @SpringBean
    WebClient mockWebClient = Mock(WebClient);

    @SpringBean
    JsonObjectMapper jsonObjectMapper = new JsonObjectMapper(new ObjectMapper())

    def mockRequestBodyUriSpec = Mock(WebClient.RequestBodyUriSpec)
    def mockResponseSpec = Mock(WebClient.ResponseSpec)
    def mockResponseEntity = Mock(ResponseEntity)

    def setup() {
        mockRequestBodyUriSpec.uri(_) >> mockRequestBodyUriSpec
        mockRequestBodyUriSpec.headers(_) >> mockRequestBodyUriSpec
        mockRequestBodyUriSpec.retrieve() >> mockResponseSpec
    }

    def 'DMI POST operation with JSON.'() {
        given: 'the web client returns a valid response entity for the expected parameters'
            mockWebClient.post() >> mockRequestBodyUriSpec
            mockRequestBodyUriSpec.body(_) >> mockRequestBodyUriSpec
            def monoSpec = Mono.just(mockResponseEntity)
            mockResponseSpec.bodyToMono(Object.class) >>  monoSpec
            monoSpec.block() >> mockResponseEntity
        when: 'POST operation is invoked'
            def response = objectUnderTest.postOperationWithJsonData('/my/url', 'some json', READ, null)
        then: 'the output of the method is equal to the output from the test template'
            assert response.statusCode.value() == 200
            assert response.hasBody()
    }

    def 'Failing DMI POST operation where #scenario.'() {
        given: 'the web client throws an exception'
            mockWebClient.post() >> { throw exception }
        when: 'POST operation is invoked'
            objectUnderTest.postOperationWithJsonData('/some', 'some json', operation, null)
        then: 'a http client exception is thrown'
            def thrown = thrown(DmiClientRequestException)
        and: 'the exception has the relevant details from the error response'
            assert thrown.ncmpResponseStatus.code == expectedNcmpResponseStatusCode
            assert  thrown.httpStatusCode == expectedHttpStatusCode
        where: 'the following scenarios are applied'
            scenario                      | operation | exception                                                                                       || expectedHttpStatusCode | expectedNcmpResponseStatusCode
            'Dmi service gateway timeout' | CREATE    | new HttpServerErrorException(GATEWAY_TIMEOUT, null, 'gateway timeout'.getBytes(), null)         || 504                    | '102'
            'Dmi service unavailable'     | READ      | new HttpServerErrorException(SERVICE_UNAVAILABLE, null, 'service unavailable'.getBytes(), null) || 503                    | '102'
    }

    def 'Failing DMI POST operation due to invalid dmi resource url.'() {
        when: 'POST operation is invoked with invalid dmi resource url'
            objectUnderTest.postOperationWithJsonData('/invalid dmi url', null, null, null)
        then: 'invalid dmi resource url exception is thrown'
            def thrown = thrown(InvalidDmiResourceUrlException)
        and: 'the exception has the relevant details from the error response'
            assert thrown.httpStatus == 400
            assert thrown.message == 'Invalid dmi resource url: /invalid dmi url'
        where: 'the following operations are executed'
            operation << [CREATE, READ, PATCH]
    }

    def 'Dmi service sends client error response when #scenario'() {
        given: 'the web client unable to return response entity but error'
            mockWebClient.post() >> mockRequestBodyUriSpec
            mockRequestBodyUriSpec.body(_) >> mockRequestBodyUriSpec
            def monoSpec = Mono.error(exception)
            mockResponseSpec.bodyToMono(Object.class) >>  monoSpec
        when: 'POST operation is invoked'
            objectUnderTest.postOperationWithJsonData('/my/url', 'some json', READ, null)
        then: 'a http client exception is thrown'
            def thrown = thrown(DmiClientRequestException)
        and: 'the exception has the relevant details from the error response'
            assert thrown.ncmpResponseStatus.code == expectedNcmpResponseStatusCode
            assert thrown.httpStatusCode == expectedHttpStatusCode
        where: 'the following scenarios are applied'
            scenario                                 | operation | exception                                                                         || expectedHttpStatusCode | expectedNcmpResponseStatusCode
            'dmi service is unable to read resource' | CREATE    | new WebClientResponseException('some error message', 404, null, null, null, null) || 404                    | '103'
            'dmi request timeout'                    | READ      | new WebClientResponseException('request timeout', 408, null, null, null, null)    || 408                    | '102'
            'unknown exception'                      | PATCH     | new Exception('unknown error')                                                    || 500                    | '108'
    }

    def 'Dmi trust level is determined by spring boot health status'() {
        given: 'a health check response'
            def dmiPluginHealthCheckResponseJsonData = TestUtils.getResourceFileContent('dmiPluginHealthCheckResponse.json')
            def jsonNode = jsonObjectMapper.convertJsonString(dmiPluginHealthCheckResponseJsonData, JsonNode.class)
            ((ObjectNode) jsonNode).put('status', 'my status')
            def monoResponse = Mono.just(jsonNode)
            mockWebClient.get() >> mockRequestBodyUriSpec
            mockResponseSpec.bodyToMono(_) >> monoResponse
            monoResponse.block() >> jsonNode
        when: 'get trust level of the dmi plugin'
            def result = objectUnderTest.getDmiHealthStatus('some/url')
        then: 'the status value from the json is return'
            assert result == 'my status'
    }

    def 'Failing to get dmi plugin health status #scenario'() {
        given: 'rest template with #scenario'
            mockWebClient.get() >> healthStatusResponse
        when: 'attempt to get health status of the dmi plugin'
            def result = objectUnderTest.getDmiHealthStatus('some url')
        then: 'result will be empty'
            assert result == ''
        where: 'the following responses are used'
            scenario    | healthStatusResponse
            'null'      | null
            'exception' | {throw new Exception()}
    }

    def 'DMI auth header #scenario'() {
        when: 'Specific dmi properties are provided'
            dmiProperties.dmiBasicAuthEnabled = authEnabled
        then: 'http headers to conditionally have Authorization header'
            def authHeaderValues = objectUnderTest.configureHttpHeaders(new HttpHeaders(), ncmpAuthHeader).getOrEmpty('Authorization')
            def outputAuthHeader = (authHeaderValues == null ? null : authHeaderValues[0])
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
