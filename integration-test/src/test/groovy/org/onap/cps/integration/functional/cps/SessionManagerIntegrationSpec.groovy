/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

package org.onap.cps.integration.functional.cps

import org.onap.cps.integration.base.FunctionalSpecBase
import org.onap.cps.ri.utils.SessionManager
import org.onap.cps.api.exceptions.SessionManagerException

class SessionManagerIntegrationSpec extends FunctionalSpecBase {

    SessionManager objectUnderTest

    def shortTimeoutForTesting = 300L
    def sessionId

    def setup() {
        objectUnderTest = sessionManager
        sessionId = objectUnderTest.startSession()
    }

    def cleanup(){
        objectUnderTest.closeSession(sessionId, objectUnderTest.WITH_COMMIT)
    }

    def 'Lock anchor.'(){
        when: 'session tries to acquire anchor lock by passing anchor entity details'
            objectUnderTest.lockAnchor(sessionId, FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, shortTimeoutForTesting)
        then: 'no exception is thrown'
            noExceptionThrown()
    }

    def 'Attempt to lock anchor when another session is holding the lock.'(){
        given: 'another session that holds an anchor lock'
            def otherSessionId = objectUnderTest.startSession()
            objectUnderTest.lockAnchor(otherSessionId,FUNCTIONAL_TEST_DATASPACE_1,BOOKSTORE_ANCHOR_1,shortTimeoutForTesting)
        when: 'a session tries to acquire the same anchor lock'
            objectUnderTest.lockAnchor(sessionId,FUNCTIONAL_TEST_DATASPACE_1,BOOKSTORE_ANCHOR_1,shortTimeoutForTesting)
        then: 'a session manager exception is thrown specifying operation reached timeout'
            def thrown = thrown(SessionManagerException)
            thrown.message.contains('Timeout')
        then: 'when the other session holding the lock is closed, lock can finally be acquired'
            objectUnderTest.closeSession(otherSessionId, objectUnderTest.WITH_COMMIT)
            objectUnderTest.lockAnchor(sessionId,FUNCTIONAL_TEST_DATASPACE_1,BOOKSTORE_ANCHOR_1,shortTimeoutForTesting)
    }

    def 'Lock anchor twice using the same session.'(){
        given: 'session that already holds an anchor lock'
            objectUnderTest.lockAnchor(sessionId, FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, shortTimeoutForTesting)
        when: 'same session tries to acquire same anchor lock'
            objectUnderTest.lockAnchor(sessionId, FUNCTIONAL_TEST_DATASPACE_1, BOOKSTORE_ANCHOR_1, shortTimeoutForTesting)
        then: 'no exception is thrown'
            noExceptionThrown()
    }

}
