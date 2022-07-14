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

import spock.lang.Specification

class LcmEventsCreatorSpec extends Specification {

    def objectUnderTest = new LcmEventsCreator()
    def cmHandleId = 'test-cm-handle'

    def 'Map the LcmEvent for operation #operation'() {
        when: 'the event is populated'
            def result = objectUnderTest.populateLcmEvent(cmHandleId)
        then: 'event header is mapped correctly'
            assert result.eventSource == 'org.onap.ncmp'
            assert result.eventCorrelationId == cmHandleId
        and: 'the result has the correct cm handle id'
            assert result.event.cmHandleId == cmHandleId
        and: 'the old and new values are not set yet'
            assert result.event.oldValues == null
            assert result.event.newValues == null
    }
}
