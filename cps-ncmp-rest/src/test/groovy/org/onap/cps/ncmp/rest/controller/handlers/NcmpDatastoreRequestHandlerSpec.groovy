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

package org.onap.cps.ncmp.rest.controller.handlers

import org.onap.cps.ncmp.api.NetworkCmProxyDataService
import org.onap.cps.ncmp.api.impl.exception.InvalidDatastoreException
import org.onap.cps.ncmp.api.impl.exception.InvalidOperationException
import org.onap.cps.ncmp.api.models.DataOperationDefinition
import org.onap.cps.ncmp.api.models.DataOperationRequest
import org.onap.cps.ncmp.api.models.CmResourceAddress
import org.onap.cps.ncmp.rest.exceptions.OperationNotSupportedException
import org.onap.cps.ncmp.rest.exceptions.PayloadTooLargeException
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

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
        and: 'the (mocked) service will indicate if it is called'
            1 * mockNetworkCmProxyDataService.getResourceDataForCmHandle(cmResourceAddress, 'options', _, _, NO_AUTH_HEADER) >> Mono.just(HttpStatus.OK)
        when: 'get request is executed with topic = #topic'
            def response= objectUnderTest.executeRequest(cmResourceAddress, 'options', topic, false, NO_AUTH_HEADER)
        then: 'a successful response with/without request id is returned'
            assert response.statusCode.value == 200
            assert response.body instanceof Map == expectedResponseBodyIsMap
        where: 'the following parameters are used'
            scenario                   | notificationFeatureEnabled | topic   || expectedCalls | expectedResponseBodyIsMap
            'feature on, valid topic'  | true                       | 'valid' || 1             | true
            'feature on, no topic'     | true                       | null    || 0             | false
            'feature off, valid topic' | false                      | 'valid' || 0             | false
            'feature off, no topic'    | false                      | null    || 0             | false
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
        and: ' a flag to track the network service call'
            def networkServiceMethodCalled = false
        and: 'the (mocked) service will use the flag to indicate it is called'
            mockNetworkCmProxyDataService.executeDataOperationForCmHandles('myTopic', dataOperationRequest, _, NO_AUTH_HEADER) >> {
                networkServiceMethodCalled = true
            }
        when: 'data operation request is executed'
            def response = objectUnderTest.executeRequest('myTopic', dataOperationRequest, NO_AUTH_HEADER)
        and: 'verify async response'
            assert response.statusCode.value == 200
            assert response.body.requestId != null
        then: 'the network service is invoked'
            new PollingConditions().within(1) {
                assert networkServiceMethodCalled == true
            }
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
