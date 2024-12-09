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

package org.onap.cps.ncmp.impl.data.policyexecutor

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.onap.cps.ncmp.api.exceptions.NcmpException
import org.onap.cps.ncmp.api.exceptions.PolicyExecutorException
import org.onap.cps.ncmp.impl.inventory.models.YangModelCmHandle
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import spock.lang.Specification

import java.util.concurrent.TimeoutException

import static org.onap.cps.ncmp.api.data.models.OperationType.CREATE
import static org.onap.cps.ncmp.api.data.models.OperationType.DELETE
import static org.onap.cps.ncmp.api.data.models.OperationType.PATCH
import static org.onap.cps.ncmp.api.data.models.OperationType.UPDATE

class PolicyExecutorSpec extends Specification {

    def mockWebClient = Mock(WebClient)
    def mockRequestBodyUriSpec = Mock(WebClient.RequestBodyUriSpec)
    def mockResponseSpec = Mock(WebClient.ResponseSpec)
    def spiedObjectMapper = Spy(ObjectMapper)

    PolicyExecutor objectUnderTest = new PolicyExecutor(mockWebClient, spiedObjectMapper)

    def logAppender = Spy(ListAppender<ILoggingEvent>)

    def someValidJson = '{"Hello":"World"}'

    def setup() {
        setupLogger()
        objectUnderTest.enabled = true
        objectUnderTest.serverAddress = 'some host'
        objectUnderTest.serverPort = 'some port'
        mockWebClient.post() >> mockRequestBodyUriSpec
        mockRequestBodyUriSpec.uri(*_) >> mockRequestBodyUriSpec
        mockRequestBodyUriSpec.header(*_) >> mockRequestBodyUriSpec
        mockRequestBodyUriSpec.body(*_) >> mockRequestBodyUriSpec
        mockRequestBodyUriSpec.retrieve() >> mockResponseSpec
    }

    def cleanup() {
        ((Logger) LoggerFactory.getLogger(PolicyExecutor)).detachAndStopAllAppenders()
    }

    def 'Permission check with "allow" decision.'() {
        given: 'allow response'
            mockResponse([permissionResult:'allow'], HttpStatus.OK)
        when: 'permission is checked for an operation'
            objectUnderTest.checkPermission(new YangModelCmHandle(), operationType, 'my credentials','my resource',someValidJson)
        then: 'system logs the operation is allowed'
            assert getLogEntry(2) == 'Operation allowed.'
        and: 'no exception occurs'
            noExceptionThrown()
        where: 'all write operations are tested'
            operationType << [ CREATE, DELETE, PATCH, UPDATE ]
    }

    def 'Permission check with "other" decision (not allowed).'() {
        given: 'other response'
            mockResponse([permissionResult:'other', id:123, message:'I dont like Mondays' ], HttpStatus.OK)
        when: 'permission is checked for an operation'
            objectUnderTest.checkPermission(new YangModelCmHandle(), PATCH, 'my credentials','my resource',someValidJson)
        then: 'Policy Executor exception is thrown'
            def thrownException = thrown(PolicyExecutorException)
            assert thrownException.message == 'Operation not allowed. Decision id 123 : other'
            assert thrownException.details == 'I dont like Mondays'
    }

    def 'Permission check with non-2xx response and "allow" default decision.'() {
        given: 'non-2xx http response'
            mockErrorResponse()
        and: 'the configured default decision is "allow"'
            objectUnderTest.defaultDecision = 'allow'
        when: 'permission is checked for an operation'
            objectUnderTest.checkPermission(new YangModelCmHandle(), PATCH, 'my credentials','my resource',someValidJson)
        then: 'No exception is thrown'
            noExceptionThrown()
    }

    def 'Permission check with non-2xx response and "other" default decision.'() {
        given: 'non-2xx http response'
            def webClientException = mockErrorResponse()
        and: 'the configured default decision is NOT "allow"'
            objectUnderTest.defaultDecision = 'deny by default'
        when: 'permission is checked for an operation'
            objectUnderTest.checkPermission(new YangModelCmHandle(), PATCH, 'my credentials', 'my resource', someValidJson)
        then: 'Policy Executor exception is thrown'
            def thrownException = thrown(PolicyExecutorException)
            assert thrownException.message == 'Operation not allowed. Decision id N/A : deny by default'
            assert thrownException.details == 'Policy Executor returned HTTP Status code 418. Falling back to configured default decision: deny by default'
        and: 'the cause is the original web client exception'
            assert thrownException.cause == webClientException
    }

    def 'Permission check with invalid response from Policy Executor.'() {
        given: 'invalid response from Policy executor'
            mockResponseSpec.toEntity(*_) >> Mono.just(new ResponseEntity<>(null, HttpStatus.OK))
        when: 'permission is checked for an operation'
            objectUnderTest.checkPermission(new YangModelCmHandle(), CREATE, 'my credentials', 'my resource', someValidJson)
        then: 'system logs the expected message'
            assert getLogEntry(1) == 'No valid response body from Policy Executor, ignored'
    }

    def 'Permission check with timeout exception.'() {
        given: 'a timeout during the request'
            def timeoutException = new TimeoutException()
            mockResponseSpec.toEntity(*_) >> { throw new RuntimeException(timeoutException) }
        and: 'the configured default decision is NOT "allow"'
            objectUnderTest.defaultDecision = 'deny by default'
        when: 'permission is checked for an operation'
            objectUnderTest.checkPermission(new YangModelCmHandle(), CREATE, 'my credentials', 'my resource', someValidJson)
        then: 'Policy Executor exception is thrown'
            def thrownException = thrown(PolicyExecutorException)
            assert thrownException.message == 'Operation not allowed. Decision id N/A : deny by default'
            assert thrownException.details == 'Policy Executor request timed out. Falling back to configured default decision: deny by default'
        and: 'the cause is the original time out exception'
            assert thrownException.cause == timeoutException
    }

    def 'Permission check with unknown host.'() {
        given: 'a unknown host exception during the request'
            def unknownHostException = new UnknownHostException()
            mockResponseSpec.toEntity(*_) >> { throw new RuntimeException(unknownHostException) }
        and: 'the configured default decision is NOT "allow"'
            objectUnderTest.defaultDecision = 'deny by default'
        when: 'permission is checked for an operation'
            objectUnderTest.checkPermission(new YangModelCmHandle(), CREATE, 'my credentials', 'my resource', someValidJson)
        then: 'Policy Executor exception is thrown'
            def thrownException = thrown(PolicyExecutorException)
            assert thrownException.message == 'Operation not allowed. Decision id N/A : deny by default'
            assert thrownException.details == 'Cannot connect to Policy Executor (some host:some port). Falling back to configured default decision: deny by default'
        and: 'the cause is the original unknown host exception'
            assert thrownException.cause == unknownHostException
    }

    def 'Permission check with #scenario exception and default decision "allow".'() {
        given: 'a #scenario exception during the request'
            mockResponseSpec.toEntity(*_) >> { throw new RuntimeException(cause)}
        and: 'the configured default decision is "allow"'
            objectUnderTest.defaultDecision = 'allow'
        when: 'permission is checked for an operation'
            objectUnderTest.checkPermission(new YangModelCmHandle(), CREATE, 'my credentials', 'my resource', someValidJson)
        then: 'no exception is thrown'
            noExceptionThrown()
        where: 'the following exceptions are thrown during the request'
            scenario       | cause
            'timeout'      | new TimeoutException()
            'unknown host' | new UnknownHostException()
    }

    def 'Permission check with other runtime exception.'() {
        given: 'some other runtime exception'
            def originalException =  new RuntimeException()
            mockResponseSpec.toEntity(*_) >> { throw originalException}
        when: 'permission is checked for an operation'
            objectUnderTest.checkPermission(new YangModelCmHandle(), CREATE, 'my credentials', 'my resource', someValidJson)
        then: 'The original exception is thrown'
            def thrownException = thrown(RuntimeException)
            assert thrownException == originalException
    }

    def 'Permission check with an invalid change request json.'() {
        when: 'permission is checked for an invalid change request'
            objectUnderTest.checkPermission(new YangModelCmHandle(), CREATE, 'my credentials', 'my resource', 'invalid json string')
        then: 'an ncmp exception thrown'
            def ncmpException = thrown(NcmpException)
            ncmpException.message == 'Cannot convert Change Request data to Object'
            ncmpException.details.contains('invalid json string')
    }

    def 'Permission check feature disabled.'() {
        given: 'feature is disabled'
            objectUnderTest.enabled = false
        when: 'permission is checked for an operation'
            objectUnderTest.checkPermission(new YangModelCmHandle(), PATCH, 'my credentials','my resource',someValidJson)
        then: 'system logs that the feature not enabled'
            assert getLogEntry(0) == 'Policy Executor Enabled: false'
    }

    def mockResponse(mockResponseAsMap, httpStatus) {
        JsonNode jsonNode = spiedObjectMapper.readTree(spiedObjectMapper.writeValueAsString(mockResponseAsMap))
        def mono = Mono.just(new ResponseEntity<>(jsonNode, httpStatus))
        mockResponseSpec.toEntity(*_) >> mono
    }

    def mockErrorResponse() {
        def webClientResponseException = Mock(WebClientResponseException)
        webClientResponseException.getStatusCode() >> HttpStatus.I_AM_A_TEAPOT
        mockResponseSpec.toEntity(*_) >> { throw webClientResponseException }
        return webClientResponseException
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
