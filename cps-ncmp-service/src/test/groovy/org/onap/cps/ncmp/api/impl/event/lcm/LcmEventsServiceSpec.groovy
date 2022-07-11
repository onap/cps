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

package org.onap.cps.ncmp.api.impl.event.lcm

import org.onap.ncmp.cmhandle.lcm.event.NcmpEvent
import spock.lang.Specification

class LcmEventsServiceSpec extends Specification {

    def mockLcmEventsPublisher = Mock(LcmEventsPublisher)

    def objectUnderTest = new LcmEventsService(mockLcmEventsPublisher)

    def 'Create and Publish lcm event where events are #scenario'() {
        given: 'a cm handle id and Ncmp Event'
            def cmHandleId = 'test-cm-handle-id'
            def ncmpEvent = new NcmpEvent(eventId: UUID.randomUUID().toString(), eventCorrelationId: cmHandleId)
        and: 'notifications enabled is #notificationsEnabled'
            objectUnderTest.notificationsEnabled = notificationsEnabled
        when: 'service is called to publish lcm event'
            objectUnderTest.publishLcmEvent('test-cm-handle-id', ncmpEvent)
        then: 'publisher is called #expectedTimesMethodCalled times'
            expectedTimesMethodCalled * mockLcmEventsPublisher.publishEvent(_, cmHandleId, ncmpEvent)
        where: 'the following values are used'
            scenario   | notificationsEnabled || expectedTimesMethodCalled
            'enabled'  | true                 || 1
            'disabled' | false                || 0
    }

}
