/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2024 Nordix Foundation
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

package org.onap.cps.ncmp.impl.data

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.exceptions.PolicyExecutorException
import org.onap.cps.ncmp.api.exceptions.ServerNcmpException
import org.onap.cps.ncmp.impl.data.policyexecutor.PolicyExecutor
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import spock.lang.Specification
import org.springframework.http.HttpHeaders

import static org.onap.cps.ncmp.api.data.models.OperationType.CREATE
import static org.onap.cps.ncmp.api.data.models.OperationType.DELETE
import static org.onap.cps.ncmp.api.data.models.OperationType.PATCH
import static org.onap.cps.ncmp.api.data.models.OperationType.UPDATE

class PolicyExecutorSpec extends Specification {

    def mockWebClient = Mock(WebClient)
    def mockRequestBodyUriSpec = Mock(WebClient.RequestBodyUriSpec)
    def mockResponseSpec = Mock(WebClient.ResponseSpec)

    PolicyExecutor objectUnderTest = new PolicyExecutor(mockWebClient)

    def logAppender = Spy(ListAppender<ILoggingEvent>)

    ObjectMapper objectMapper = new ObjectMapper()

    def setup() {
        setupLogger()
        objectUnderTest.enabled = true
        mockWebClient.post() >> mockRequestBodyUriSpec
        mockRequestBodyUriSpec.uri(*_) >> mockRequestBodyUriSpec
        mockRequestBodyUriSpec.headers(*_) >> mockRequestBodyUriSpec
        mockRequestBodyUriSpec.body(*_) >> mockRequestBodyUriSpec
        mockRequestBodyUriSpec.retrieve() >> mockResponseSpec
    }

    def cleanup() {
        ((Logger) LoggerFactory.getLogger(PolicyExecutor)).detachAndStopAllAppenders()
    }

    def 'Permission check with allow response.'() {
        given: 'allow response'
            mockResponse([decision:'allow'], HttpStatus.OK)
        when: 'permission is checked for an operation'
            objectUnderTest.checkPermission(new YangModelCmHandle(), operationType, 'my credentials','my resource','my change')
        then: 'system logs the operation is allowed'
            assert getLogEntry(2) == 'Policy Executor allows the operation'
        and: 'no exception occurs'
            noExceptionThrown()
        where: 'all write operations are tested'
            operationType << [ CREATE, DELETE, PATCH, UPDATE ]
    }

    def 'Permission check with other response (not allowed).'() {
        given: 'other response'
            mockResponse([decision:'other'], HttpStatus.OK)
        when: 'permission is checked for an operation'
            objectUnderTest.checkPermission(new YangModelCmHandle(), PATCH, 'my credentials','my resource','my change')
        then: 'Policy Executor exception is thrown'
            def thrownException = thrown(PolicyExecutorException)
            assert thrownException.message.endsWith(': other')
    }

    def 'Permission check with non 2xx response.'() {
        given: 'other response'
            mockResponse([], HttpStatus.I_AM_A_TEAPOT)
        when: 'permission is checked for an operation'
            objectUnderTest.checkPermission(new YangModelCmHandle(), PATCH, 'my credentials','my resource','my change')
        then: 'Server Ncmp exception is thrown'
            def thrownException = thrown(ServerNcmpException)
            assert thrownException.message == 'Policy Executor invocation failed'
            assert thrownException.details == 'HTTP status code: 418'
    }

    def 'Permission check with invalid response from Policy Executor.'() {
        given: 'invalid response from Policy executor'
            mockResponseSpec.toEntity(*_) >> invalidResponse
        when: 'permission is checked for an operation'
            objectUnderTest.checkPermission(new YangModelCmHandle(), CREATE, 'my credentials','my resource','my change')
        then: 'system logs the expected message'
            assert getLogEntry(1) == expectedMessage
        where: 'following invalid responses are received'
            invalidResponse                                        || expectedMessage
            Mono.empty()                                           || 'No valid response from policy, ignored'
            Mono.just(new ResponseEntity<>(null, HttpStatus.OK))   || 'No valid response body from policy, ignored'
    }

    def 'Permission check feature disabled.'() {
        given: 'feature is disabled'
            objectUnderTest.enabled = false
        when: 'permission is checked for an operation'
            objectUnderTest.checkPermission(new YangModelCmHandle(), PATCH, 'my credentials','my resource','my change')
        then: 'system logs that the feature not enabled'
            assert getLogEntry(0) == 'Policy Executor Enabled: false'
    }

    def 'Configure headers.'() {
        given: 'empty headers'
            def httpHeaders = new HttpHeaders()
        when: 'headers are configured'
            objectUnderTest.configureHttpHeaders(httpHeaders, 'my authorization')
        then: 'first authorization header is set correctly'
            assert httpHeaders.get('Authorization')[0] == 'my authorization'
    }

    def 'Client exception handling.'() {
        when: 'some client exception is handled'
            def result = objectUnderTest.handleClientException(new Exception('my exception'))
        then: 'no exception is returned'
            assert result == null;
        and: 'the message is logged'
            assert getLogEntry(0) == 'Policy Executor invocation failed: my exception'
    }

    def mockResponse(mockResponseAsMap, httpStatus) {
        JsonNode jsonNode = objectMapper.readTree(objectMapper.writeValueAsString(mockResponseAsMap))
        def mono = Mono.just(new ResponseEntity<>(jsonNode, httpStatus))
        mockResponseSpec.toEntity(*_) >> mono
    }

    def setupLogger() {
        def logger = LoggerFactory.getLogger(PolicyExecutor)
        logger.setLevel(Level.TRACE)
        logger.addAppender(logAppender)
        logAppender.start()
    }

    def getLogEntry(index) {
        logAppender.list[index].formattedMessage
    }
}
