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

package org.onap.cps.spi.utils

import org.onap.cps.spi.exceptions.SessionManagerException
import org.onap.cps.spi.impl.CpsPersistenceSpecBase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql

class SessionManagerIntegrationSpec extends CpsPersistenceSpecBase{

    final static String SET_DATA = '/data/anchor.sql'

    @Autowired
    SessionManager objectUnderTest

    def sessionId
    def otherSessionId
    def shortTimeoutForTesting = 200L  // Please note that 100ms seems to be too short!

    def setup(){
        sessionId = objectUnderTest.startSession()
        /*TODO Toine
            the test involving a 2nd session only work when started and closed using setup and cleanup methods
            since the 2nd session is only used in one test it should work by opening and closing that 2nd session there
            but is simply hangs ?!!
         */
        otherSessionId = objectUnderTest.startSession()
    }

    def cleanup(){
        objectUnderTest.closeSession(sessionId)
        objectUnderTest.closeSession(otherSessionId)
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Lock anchor.'(){
        when: 'session tries to acquire anchor lock by passing anchor entity details'
            objectUnderTest.lockAnchor(sessionId, DATASPACE_NAME, ANCHOR_NAME1, shortTimeoutForTesting)
        then: 'no exception is thrown'
            noExceptionThrown()
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Attempt to lock anchor when another session is holding the lock.'(){
        given: 'a session that holds an anchor lock'
            objectUnderTest.lockAnchor(sessionId,DATASPACE_NAME,ANCHOR_NAME1,shortTimeoutForTesting)
        when: 'another session tries to acquire the same anchor lock'
            objectUnderTest.lockAnchor(otherSessionId,DATASPACE_NAME,ANCHOR_NAME1,shortTimeoutForTesting)
        then: 'a session manager exception is thrown specifying operation reached timeout'
            def thrown = thrown(SessionManagerException)
            thrown.message.contains('Timeout')
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Release (all) locks held by session.'(){
        given: 'anchorEntity that is locked'
            objectUnderTest.lockAnchor(sessionId,DATASPACE_NAME,ANCHOR_NAME1,shortTimeoutForTesting)
        when: 'session tries to release lock(s) held by session'
            objectUnderTest.releaseLocks(sessionId)
        then: 'no exception is thrown'
            noExceptionThrown()
    }

}
