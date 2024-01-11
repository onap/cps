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
import org.onap.cps.ncmp.rest.exceptions.OperationNotSupportedException
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class NcmpDatastoreRequestHandlerSpec extends Specification {

    def spiedCpsNcmpTaskExecutor = Spy(CpsNcmpTaskExecutor)
    def mockNetworkCmProxyDataService = Mock(NetworkCmProxyDataService)

    def objectUnderTest = new NcmpPassthroughResourceRequestHandler(spiedCpsNcmpTaskExecutor, mockNetworkCmProxyDataService)

    def setup() {
        objectUnderTest.timeOutInMilliSeconds = 100
    }

    def 'Attempt to execute async get request with #scenario.'() {
        given: 'notification feature is turned on/off'
            objectUnderTest.notificationFeatureEnabled = notificationFeatureEnabled
        and: ' a flag to track the network service call'
            def networkServiceMethodCalled = false
        and: 'the (mocked) service will use the flag to indicate if it is called'
            mockNetworkCmProxyDataService.getResourceDataForCmHandle('ds', 'ch1', 'resource1', 'options', _, _) >> {
                networkServiceMethodCalled = true
            }
        when: 'get request is executed with topic = #topic'
            objectUnderTest.executeRequest('ds', 'ch1', 'resource1', 'options', topic, false)
        then: 'the task is executed in an async fashion or not'
            expectedCalls * spiedCpsNcmpTaskExecutor.executeTask(*_)
        and: 'the service request is always invoked within 1 seconds'
            new PollingConditions().within(1) {
                assert networkServiceMethodCalled == true
            }
        where: 'the following parameters are used'
            scenario                   | notificationFeatureEnabled | topic   || expectedCalls
            'feature on, valid topic'  | true                       | 'valid' || 1
            'feature on, no topic'     | true                       | null    || 0
            'feature off, valid topic' | false                      | 'valid' || 0
            'feature off, no topic'    | false                      | null    || 0
    }

    def 'Attempt to execute async data operation request with feature #scenario.'() {
        given: 'a extended request handler that supports bulk requests'
           def objectUnderTest = new NcmpPassthroughResourceRequestHandler(spiedCpsNcmpTaskExecutor, mockNetworkCmProxyDataService)
        and: 'notification feature is turned on/off'
            objectUnderTest.notificationFeatureEnabled = notificationFeatureEnabled
        when: 'data operation request is executed'
            objectUnderTest.executeRequest('someTopic', new DataOperationRequest())
        then: 'the task is executed in an async fashion or not'
            expectedCalls * spiedCpsNcmpTaskExecutor.executeTask(*_)
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
            mockNetworkCmProxyDataService.executeDataOperationForCmHandles('myTopic', dataOperationRequest, _) >> {
                networkServiceMethodCalled = true
            }
        when: 'data operation request is executed'
            objectUnderTest.executeRequest('myTopic', dataOperationRequest)
        then: 'the task is executed in an async fashion'
            1 * spiedCpsNcmpTaskExecutor.executeTask(*_)
        and: 'the network service is invoked within 1 seconds'
            new PollingConditions().within(1) {
                assert networkServiceMethodCalled == true
            }
        where: 'the following datastores are used'
            datastore << ['ncmp-datastore:passthrough-running', 'ncmp-datastore:passthrough-operational']
    }

    def 'Attempt to execute async data operation request with error #scenario'() {
        given: 'notification feature is turned on'
            objectUnderTest.notificationFeatureEnabled = true
        and: 'a data operation definition with datastore: #datastore'
            def dataOperationDefinition = new DataOperationDefinition(operation: 'read', datastore: datastore)
        when: 'data operation request is executed'
            def dataOperationRequest = new DataOperationRequest(dataOperationDefinitions: [dataOperationDefinition])
            objectUnderTest.executeRequest('myTopic', dataOperationRequest)
        then: 'the correct error is thrown'
            def thrown = thrown(InvalidDatastoreException)
            assert thrown.message.contains(expectedErrorMessage)
        where: 'the following datastore names are used'
            scenario                | datastore                    || expectedErrorMessage
            'unsupported datastore' | 'ncmp-datastore:operational' || 'not supported'
            'invalid datastore'     | 'invalid'                    || 'invalid datastore name'
    }

    def 'Attempt to execute async data operation request with #scenario operation: #operation.'() {
        given: 'notification feature is turned on'
            objectUnderTest.notificationFeatureEnabled = true
        and: 'a data operation definition with operation: #operation'
            def dataOperationDefinition = new DataOperationDefinition(operation: operation, datastore: 'ncmp-datastore:passthrough-running')
        when: 'bulk request is executed'
            objectUnderTest.executeRequest('someTopic', new DataOperationRequest(dataOperationDefinitions:[dataOperationDefinition]))
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

}
