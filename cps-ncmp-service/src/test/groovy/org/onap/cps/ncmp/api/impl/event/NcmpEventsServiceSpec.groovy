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


import org.onap.ncmp.cmhandle.lcm.event.Event
import org.onap.ncmp.cmhandle.lcm.event.NcmpEvent
import spock.lang.Specification

class NcmpEventsServiceSpec extends Specification {

    def mockNcmpEventsPublisher = Mock(NcmpEventsPublisher)

    def objectUnderTest = new NcmpEventsService(mockNcmpEventsPublisher)

    def 'Publish the NCMP Event provided'() {
        given: 'a cm handle id and an event to be published'
            def cmHandleId = 'test-cm-handle-id'
            def ncmpEvent = ncmpSampleEventToBePublished(cmHandleId)
        when: 'service is called to publish ncmp event'
            objectUnderTest.publishNcmpEvent(cmHandleId, ncmpEvent)
        then: 'no exception is thrown'
            noExceptionThrown()
    }

    def ncmpSampleEventToBePublished(cmHandleId) {
        def ncmpEvent = new NcmpEvent(eventId: UUID.randomUUID().toString(), eventCorrelationId: cmHandleId,
            event: new Event(cmHandleId: cmHandleId))
        return ncmpEvent
    }

}
