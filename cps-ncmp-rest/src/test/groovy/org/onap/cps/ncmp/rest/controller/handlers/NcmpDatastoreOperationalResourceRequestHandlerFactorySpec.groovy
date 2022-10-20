/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
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

import org.onap.cps.ncmp.api.NetworkCmProxyQueryService
import org.onap.cps.ncmp.api.NetworkCmProxyDataService
import org.onap.cps.ncmp.rest.executor.CpsNcmpTaskExecutor
import org.onap.cps.spi.FetchDescendantsOption
import spock.lang.Specification

class NcmpDatastoreOperationalResourceRequestHandlerFactorySpec extends Specification {

    def mockNetworkCmProxyDataService = Mock(NetworkCmProxyDataService)
    def mockNetworkCmProxyQueryService = Mock(NetworkCmProxyQueryService)
    def mockCpsNcmpTaskExecutor = Mock(CpsNcmpTaskExecutor)

    def timeoutForTest = 5000

    def objectUnderTest = new NcmpDatastoreOperationalResourceRequestHandler(
        mockNetworkCmProxyDataService, mockNetworkCmProxyQueryService, mockCpsNcmpTaskExecutor,
        timeoutForTest, true)

    def 'Query Resource Data Asynchronously'() {
        given: 'a topic parameter for an asynchronous request'
            def topicParam = 'some-topic-param'
        when: 'a query request is invoked for queryResourceData with the topic param'
            objectUnderTest.queryResourceData('some-handle-id', '//some/cps/path', topicParam, true)
        then: 'the task executor is used to make an asynchronous call'
            1 * mockCpsNcmpTaskExecutor.executeTask(_, timeoutForTest)
    }

    def 'Query Resource Data Synchronously'() {
        given: 'a null for topic parameter for a synchronous request'
            def topicParam = null
        when: 'a query request is invoked for queryResourceData with the topic param'
            objectUnderTest.queryResourceData('some-handle-id', '//some/cps/path', topicParam, true)
        then: 'the task executor is not used to make an asynchronous call'
            0 * mockCpsNcmpTaskExecutor.executeTask(_, timeoutForTest)
        and: 'the query resource data operational method is invoked immediately'
            1 * mockNetworkCmProxyQueryService.queryResourceDataOperational('some-handle-id', '//some/cps/path', FetchDescendantsOption.INCLUDE_ALL_DESCENDANTS)
    }

}