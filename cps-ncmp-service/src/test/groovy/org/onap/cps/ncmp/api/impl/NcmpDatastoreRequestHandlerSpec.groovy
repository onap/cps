/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2023-2024 Nordix Foundation
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

package org.onap.cps.ncmp.api.impl

import org.onap.cps.ncmp.api.NetworkCmProxyDataService
import org.onap.cps.ncmp.api.impl.exception.InvalidDatastoreException
import org.onap.cps.ncmp.api.impl.exception.InvalidOperationException
import org.onap.cps.ncmp.api.models.CmResourceAddress
import org.onap.cps.ncmp.api.models.DataOperationDefinition
import org.onap.cps.ncmp.api.models.DataOperationRequest
import org.onap.cps.ncmp.exceptions.OperationNotSupportedException
import org.onap.cps.ncmp.exceptions.PayloadTooLargeException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import reactor.core.publisher.Mono
import spock.lang.Specification

class NcmpDatastoreRequestHandlerSpec extends Specification {

    def mockNetworkCmProxyDataService = Mock(NetworkCmProxyDataService)

    def objectUnderTest = new NcmpPassthroughResourceRequestHandler(mockNetworkCmProxyDataService)

    def NO_AUTH_HEADER = null

    def setup() {
        objectUnderTest.timeOutInMilliSeconds = 100
    }

    def 'Attempt to execute async get request with #scenario.'() {
        given: 'notification feature is turned on/off'
            objectUnderTest.notificationFeatureEnabled = notificationFeatureEnabled
        and: 'a CM resource address'
            def cmResourceAddress = new CmResourceAddress('ds', 'ch1', 'resource1')
        and: 'the (mocked) service when called with the correct parameters returns a response from dmi'
            def resultFromDmi = new ResponseEntity('response from dmi',HttpStatus.I_AM_A_TEAPOT)
            def synchronousResult = Mono.justOrEmpty(resultFromDmi)
            mockNetworkCmProxyDataService.getResourceDataForCmHandle(cmResourceAddress, 'options', _, _, NO_AUTH_HEADER) >> synchronousResult
        when: 'get request is executed with topic = #topic'
            def response = objectUnderTest.executeRequest(cmResourceAddress, 'options', topic, false, NO_AUTH_HEADER)
        then: 'a successful result with/without request id is returned'
            if (expectSynchronousResponse) {
                assert response.toString().contains('response from dmi')
                assert response.toString().contains("I'm a teapot")
            } else {
                // expect request id in a map
                assert response.keySet()[0] == 'requestId'
            }
        where: 'the following parameters are used'
            scenario                   | notificationFeatureEnabled | topic   || expectSynchronousResponse
            'feature on, valid topic'  | true                       | 'valid' || false
            'feature on, no topic'     | true                       | null    || true
            'feature off, valid topic' | false                      | 'valid' || true
            'feature off, no topic'    | false                      | null    || true
    }

    def 'Attempt to execute async data operation request with feature #scenario.'() {
        given: 'a extended request handler that supports bulk requests'
           def objectUnderTest = new NcmpPassthroughResourceRequestHandler(mockNetworkCmProxyDataService)
        and: 'notification feature is turned on/off'
            objectUnderTest.notificationFeatureEnabled = notificationFeatureEnabled
        when: 'data operation request is executed'
            objectUnderTest.executeRequest('someTopic', new DataOperationRequest(), NO_AUTH_HEADER)
        then: 'the task is executed in an async fashion or not'
            expectedCalls * mockNetworkCmProxyDataService.executeDataOperationForCmHandles('someTopic', _, _, null)
        where: 'the following parameters are used'
            scenario | notificationFeatureEnabled || expectedCalls
            'on'     | true                       || 1
            'off'    | false                      || 0
    }

    def 'Execute async data operation request with datastore #datastore.'() {
        given: 'notification feature is turned on'
            objectUnderTest.notificationFeatureEnabled = true
        and: 'a data operation request with datastore: #datastore'
            def dataOperationDefinition = new DataOperationDefinition(operation: 'read', datastore: datastore)
            def dataOperationRequest = new DataOperationRequest(dataOperationDefinitions: [dataOperationDefinition])
        when: 'data operation request is executed'
            def response = objectUnderTest.executeRequest('myTopic', dataOperationRequest, NO_AUTH_HEADER)
        and: 'a map with request id is returned'
            assert response.keySet()[0] == 'requestId'
        then: 'the network service is invoked'
            1 * mockNetworkCmProxyDataService.executeDataOperationForCmHandles('myTopic', dataOperationRequest, _, NO_AUTH_HEADER)
        where: 'the following datastores are used'
            datastore << ['ncmp-datastore:passthrough-running', 'ncmp-datastore:passthrough-operational']
    }

    def 'Attempt to execute async data operation request with error #scenario'() {
        given: 'a data operation definition with datastore: #datastore'
            def dataOperationDefinition = new DataOperationDefinition(operation: 'read', datastore: datastore)
        when: 'data operation request is executed'
            def dataOperationRequest = new DataOperationRequest(dataOperationDefinitions: [dataOperationDefinition])
            objectUnderTest.executeRequest('myTopic', dataOperationRequest, NO_AUTH_HEADER)
        then: 'the correct error is thrown'
            def thrown = thrown(InvalidDatastoreException)
            assert thrown.message.contains(expectedErrorMessage)
        where: 'the following datastore names are used'
            scenario                | datastore                    || expectedErrorMessage
            'unsupported datastore' | 'ncmp-datastore:operational' || 'not supported'
            'invalid datastore'     | 'invalid'                    || 'invalid datastore name'
    }

    def 'Attempt to execute async data operation request with #scenario operation: #operation.'() {
        given: 'a data operation definition with operation: #operation'
            def dataOperationDefinition = new DataOperationDefinition(operation: operation, datastore: 'ncmp-datastore:passthrough-running')
        when: 'data operation request is executed'
            objectUnderTest.executeRequest('someTopic', new DataOperationRequest(dataOperationDefinitions:[dataOperationDefinition]), NO_AUTH_HEADER)
        then: 'the expected type of exception is thrown'
            thrown(expectedException)
        where: 'the following operations are used'
            scenario      | operation || expectedException
            'invalid'     | 'invalid' || InvalidOperationException
            'unsupported' | 'create'  || OperationNotSupportedException
            'unsupported' | 'update'  || OperationNotSupportedException
            'unsupported' | 'patch'   || OperationNotSupportedException
            'unsupported' | 'delete'  || OperationNotSupportedException
    }

    def 'Attempt to execute async data operation request with too many cm handles.'() {
        given: 'a data operation definition with too many cm handles'
            def tooMany = objectUnderTest.MAXIMUM_CM_HANDLES_PER_OPERATION+1
            def cmHandleIds = new String[tooMany]
            def dataOperationDefinition = new DataOperationDefinition(operationId: 'abc', operation: 'read', datastore: 'ncmp-datastore:passthrough-running', cmHandleIds: cmHandleIds)
        when: 'data operation request is executed'
            objectUnderTest.executeRequest('someTopic', new DataOperationRequest(dataOperationDefinitions:[dataOperationDefinition]), NO_AUTH_HEADER)
        then: 'a payload too large exception is thrown'
            def exceptionThrown = thrown(PayloadTooLargeException)
        and: 'the error message contains the offending number of cm handles'
            assert exceptionThrown.message == "Operation 'abc' affects too many (${tooMany}) cm handles"
    }

}
