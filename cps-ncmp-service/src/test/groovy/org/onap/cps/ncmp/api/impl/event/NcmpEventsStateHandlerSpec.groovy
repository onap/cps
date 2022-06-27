/*
 * ============LICENSE_START=======================================================
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

package org.onap.cps.ncmp.api.impl.event


import spock.lang.Specification

import static org.onap.cps.ncmp.api.impl.event.NcmpCmHandleStateTransition.ADVISED_TO_LOCKED
import static org.onap.cps.ncmp.api.impl.event.NcmpCmHandleStateTransition.ADVISED_TO_READY
import static org.onap.cps.ncmp.api.impl.event.NcmpCmHandleStateTransition.ANY_TO_DELETING
import static org.onap.cps.ncmp.api.impl.event.NcmpCmHandleStateTransition.DELETING_TO_DELETED
import static org.onap.cps.ncmp.api.impl.event.NcmpCmHandleStateTransition.NOTHING_TO_ADVISED

class NcmpEventsStateHandlerSpec extends Specification {

    def mockNcmpEventsService = Mock(NcmpEventsService)
    def mockNcmpEventsCreator = Mock(NcmpEventsCreator)

    def objectUnderTest = new NcmpEventsStateHandler(mockNcmpEventsService, mockNcmpEventsCreator)

    def cmHandleId = 'test-cm-handle-id'

    def 'Publish NCMP Event during state transition'() {
        when: 'State handler is called with relevant state transition'
            objectUnderTest.publishNcmpEventForStateTransition(cmHandleId, stateTransition)
        then: 'no exception is thrown'
            noExceptionThrown()
        where: 'relevant states are as follows'
            stateTransition << [NOTHING_TO_ADVISED, ADVISED_TO_LOCKED, ADVISED_TO_READY, ANY_TO_DELETING, DELETING_TO_DELETED]
    }


}
