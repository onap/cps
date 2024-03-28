/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2023 Nordix Foundation
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

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.onap.cps.ncmp.api.impl.config.WebClientConfiguration;
import org.onap.cps.ncmp.api.impl.exception.HttpClientRequestException
import org.onap.cps.ncmp.utils.TestUtils
import org.spockframework.spring.SpringBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import spock.lang.Specification

import static org.onap.cps.ncmp.api.impl.operations.OperationType.READ
import static org.onap.cps.ncmp.api.impl.operations.OperationType.PATCH
import static org.onap.cps.ncmp.api.impl.operations.OperationType.CREATE

@SpringBootTest
@ContextConfiguration(classes = [WebClientConfiguration, DmiRestClient, ObjectMapper])
class DmiRestClientSpec extends Specification {

    static final NO_AUTH_HEADER = null
    static final BASIC_AUTH_HEADER = 'Basic c29tZS11c2VyOnNvbWUtcGFzc3dvcmQ='
    static final BEARER_AUTH_HEADER = 'Bearer my-bearer-token'

    @Autowired
    WebClientConfiguration.DmiProperties dmiProperties

    @Autowired
    DmiRestClient objectUnderTest

    @SpringBean
    WebClient mockWebClient = Mock(WebClient);

    @Autowired
    ObjectMapper objectMapper

    def mockRequestBodyUriSpec = Mock(WebClient.RequestBodyUriSpec)
    def mockResponseSpec = Mock(WebClient.ResponseSpec)
    def mockResponseEntity = Mock(ResponseEntity)
    def monoSpec = Mono.just(mockResponseEntity)

    def 'DMI POST operation with JSON.'() {
        given: 'the web client returns a valid response entity for the expected parameters'
            mockWebClient.post() >> mockRequestBodyUriSpec
            mockRequestBodyUriSpec.body(_) >> mockRequestBodyUriSpec
            mockRequestBodyUriSpec.uri(_) >> mockRequestBodyUriSpec
            mockRequestBodyUriSpec.headers(_) >> mockRequestBodyUriSpec
            mockRequestBodyUriSpec.retrieve() >> mockResponseSpec
            mockResponseSpec.toEntity(Object.class) >> monoSpec
            monoSpec.block() >> mockResponseEntity
        when: 'POST operation is invoked'
            def result = objectUnderTest.postOperationWithJsonData('/my/url', 'some json', READ, null)
        then: 'the output of the method is equal to the output from the test template'
            result == mockResponseEntity
    }

    def 'Failing DMI POST operation.'() {
        given: 'the rest template returns a valid response entity'
            def serverResponse = 'server response'.getBytes()
            def httpServerErrorException = new HttpServerErrorException(HttpStatus.FORBIDDEN, 'status text', serverResponse, null)
            mockWebClient.post() >> { throw httpServerErrorException }
        when: 'POST operation is invoked'
            def result = objectUnderTest.postOperationWithJsonData('/some', 'some json', operation, null)
        then: 'a Http Client Exception is thrown'
            def thrown = thrown(HttpClientRequestException)
        and: 'the exception has the relevant details from the error response'
            assert thrown.httpStatus == 403
            assert thrown.message == "Unable to ${operation} resource data."
            assert thrown.details == 'server response'
        where: 'the following operation is executed'
            operation << [CREATE, READ, PATCH]
    }

    def 'Dmi trust level is determined by spring boot health status'() {
        given: 'a health check response'
            def dmiPluginHealthCheckResponseJsonData = TestUtils.getResourceFileContent('dmiPluginHealthCheckResponse.json')
            def jsonNode = objectMapper.readValue(dmiPluginHealthCheckResponseJsonData, JsonNode.class)
            ((ObjectNode) jsonNode).put('status', 'my status')
            def monoResponse = Mono.just(jsonNode)
            mockWebClient.get() >> mockRequestBodyUriSpec
            mockRequestBodyUriSpec.uri(_) >> mockRequestBodyUriSpec
            mockRequestBodyUriSpec.headers(_) >> mockRequestBodyUriSpec
            mockRequestBodyUriSpec.retrieve() >> mockResponseSpec
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
