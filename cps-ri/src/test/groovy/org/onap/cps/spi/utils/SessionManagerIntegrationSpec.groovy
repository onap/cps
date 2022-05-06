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

import org.hibernate.Session
import org.onap.cps.spi.exceptions.SessionManagerException
import org.onap.cps.spi.impl.CpsPersistenceSpecBase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql

class SessionManagerIntegrationSpec extends CpsPersistenceSpecBase{

    final static String SET_DATA = '/data/anchor.sql'

    @Autowired
    SessionManager objectUnderTest

    def sessionId
    def shortTimeoutForTesting = 200L

    def setup(){
        sessionId = objectUnderTest.startSession()
    }

    def cleanup(){
        objectUnderTest.closeSession(sessionId)
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
        given: 'another session that holds an anchor lock'
            def otherSessionId = objectUnderTest.startSession()
            objectUnderTest.lockAnchor(otherSessionId,DATASPACE_NAME,ANCHOR_NAME1,shortTimeoutForTesting)
        when: 'a session tries to acquire the same anchor lock'
            objectUnderTest.lockAnchor(sessionId,DATASPACE_NAME,ANCHOR_NAME1,shortTimeoutForTesting)
        then: 'a session manager exception is thrown specifying operation reached timeout'
            def thrown = thrown(SessionManagerException)
            thrown.message.contains('Timeout')
        then: 'when the other session holding the lock is closed, lock can finally be acquired'
            objectUnderTest.closeSession(otherSessionId)
            objectUnderTest.lockAnchor(sessionId,DATASPACE_NAME,ANCHOR_NAME1,shortTimeoutForTesting)
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'Lock anchor twice using the same session.'(){
        given: 'session that already holds an anchor lock'
            objectUnderTest.lockAnchor(sessionId, DATASPACE_NAME, ANCHOR_NAME1, shortTimeoutForTesting)
        when: 'same session tries to acquire same anchor lock'
            objectUnderTest.lockAnchor(sessionId, DATASPACE_NAME, ANCHOR_NAME1, shortTimeoutForTesting)
        then: 'no exception is thrown'
            noExceptionThrown()
    }

    def 'Close all sessions in shutdown with exception.' (){
        given: 'a valid session ID with an invalid session value'
            objectUnderTest.sessionMap.replace(sessionId,_ as Session)
        when: 'shutdown method to close all sessions is called'
            objectUnderTest.closeAllSessionsInShutdown()
        then: 'exception is thrown'
            def thrown = thrown(SessionManagerException)
            thrown.message.contains('aborted')
        cleanup:
            sessionId = objectUnderTest.startSession()
    }

}
