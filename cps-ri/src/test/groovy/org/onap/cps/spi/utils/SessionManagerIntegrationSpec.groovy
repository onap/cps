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

import org.hibernate.SessionException
import org.onap.cps.spi.exceptions.SessionManagerException
import org.onap.cps.spi.impl.CpsPersistenceSpecBase
import org.springframework.test.context.jdbc.Sql

class SessionManagerIntegrationSpec extends CpsPersistenceSpecBase{

    static final String SET_DATA = '/data/anchor.sql'

    SessionManager objectUnderTest = new SessionManager()

    def 'start session'() {
        when: 'start session'
            def result = objectUnderTest.startSession()
        then: 'session ID is returned'
            assert result instanceof String
            objectUnderTest.closeSession(result)
    }

    def 'close session'(){
        given: 'session Id from calling the start session method'
            def sessionId = objectUnderTest.startSession()
        when: 'close session method is called'
            objectUnderTest.closeSession(sessionId)
        then: 'no exception is thrown'
            noExceptionThrown()
    }

    def 'close session that does not exist' (){
        given: 'session Id that does not exist'
            def unknownSessionId = 'unknown session id'
        when: 'close session method is called'
            objectUnderTest.closeSession(unknownSessionId)
        then: 'a session exception is thrown'
            def thrown = thrown(SessionException)
            assert thrown.message.contains(unknownSessionId)
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'lock anchor entity'(){
        given: 'anchor entity details of anchor to lock'
            def sessionId = objectUnderTest.startSession()
            def anchorId = 3001
            def someTimeoutInMilliseconds = 100L
        when: 'session tries to acquire anchor lock'
            objectUnderTest.lockAnchor(sessionId,anchorId,someTimeoutInMilliseconds)
        then: 'no exception is thrown'
            noExceptionThrown()
            objectUnderTest.closeSession(sessionId)
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'attempt to lock anchor entity with invalid sessionId'(){
        given: 'anchor entity details of anchor to lock'
            def sessionId = 'invalid-sessionId'
            def anchorId = 3001
            def someTimeoutInMilliseconds = 100L
        when: 'session tries to acquire anchor lock'
            objectUnderTest.lockAnchor(sessionId,anchorId,someTimeoutInMilliseconds)
        then: 'exception is thrown'
            def thrown = thrown(SessionManagerException)
            thrown.details.contains("aborted")
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'attempt to lock anchor entity when another is holding the lock'(){
        given: 'a session that holds an anchor lock'
            def sessionId = objectUnderTest.startSession()
            def anchorId = 3001
            def someTimeoutInMilliseconds = 100L
            objectUnderTest.lockAnchor(sessionId,anchorId,someTimeoutInMilliseconds)
        when: 'another session tries to acquire anchor lock'
            def sessionId2 = objectUnderTest.startSession()
            objectUnderTest.lockAnchor(sessionId2,anchorId,someTimeoutInMilliseconds)
        then: 'exception is thrown'
            def thrown = thrown(SessionManagerException)
            thrown.message.contains("Timeout")
            objectUnderTest.closeSession(sessionId)
            objectUnderTest.closeSession(sessionId2)
    }


    @Sql([CLEAR_DATA, SET_DATA])
    def 'unlock anchor'(){
        given: 'anchorEntity that is locked'
            def sessionId = objectUnderTest.startSession()
            def sessionId2 = objectUnderTest.startSession()
            def anchorId = 3001
            def someTimeoutInMilliseconds = 100L
            objectUnderTest.lockAnchor(sessionId,anchorId,someTimeoutInMilliseconds)
            objectUnderTest.releaseLocks(sessionId)
        when: 'session tries to release lock held by session'
            objectUnderTest.lockAnchor(sessionId2,anchorId,someTimeoutInMilliseconds)
        then: 'no exception is thrown'
            noExceptionThrown()
            objectUnderTest.closeSession(sessionId2)
    }

}
