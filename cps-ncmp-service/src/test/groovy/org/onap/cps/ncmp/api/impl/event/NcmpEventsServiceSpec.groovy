/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2022 Nordix Foundation
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.cps.ncmp.api.impl.event

import org.onap.cps.ncmp.api.NetworkCmProxyDataService
import org.onap.cps.ncmp.api.models.NcmpServiceCmHandle
import org.onap.ncmp.cmhandle.lcm.event.Event
import org.onap.ncmp.cmhandle.lcm.event.NcmpEvent
import spock.lang.Specification

class NcmpEventsServiceSpec extends Specification {

    def mockNetworkCmProxyDataService = Mock(NetworkCmProxyDataService)
    def mockNcmpEventsPublisher = Mock(NcmpEventsPublisher)
    def mockNcmpEventsMapper = Mock(NcmpEventsMapper)

    def objectUnderTest = new NcmpEventsService(mockNetworkCmProxyDataService, mockNcmpEventsPublisher, mockNcmpEventsMapper)

    def 'Create and Publish event for #operation'() {
        given: 'a cm handle id and operation'
            def cmHandleId = 'test-cm-handle-id'
            def cmHandleOperation = Event.Operation.valueOf(operation)
        and: 'responses are mocked'
            mockResponses(cmHandleId, cmHandleOperation, 'test-topic')
        when: 'service is called to publish ncmp event'
            objectUnderTest.publishNcmpEvent(cmHandleId, cmHandleOperation)
        then: 'no exception is thrown'
            noExceptionThrown()
        where: 'for following operations'
            operation << ['CREATE', 'UPDATE', 'DELETE']
    }

    def mockResponses(cmHandleId, operation, topicName) {

        def ncmpServiceCmhandle = new NcmpServiceCmHandle(cmHandleId: cmHandleId, publicProperties: ['publicProperty1': 'value1'])
        def ncmpEvent = new NcmpEvent(eventId: UUID.randomUUID().toString(), eventCorrelationId: cmHandleId)

        mockNetworkCmProxyDataService.getNcmpServiceCmHandle(cmHandleId) >> ncmpServiceCmhandle
        mockNcmpEventsMapper.toNcmpEvent(cmHandleId, operation, ncmpServiceCmhandle) >> ncmpEvent
        mockNcmpEventsPublisher.publishEvent(topicName, cmHandleId, ncmpEvent) >> {}
    }


}
