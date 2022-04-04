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
import com.google.common.util.concurrent.TimeLimiter
import spock.lang.Shared;

class SessionManagerIntegrationSpec extends CpsPersistenceSpecBase{

    final static String SET_DATA = '/data/anchor.sql'
    TimeLimiterProvider timeLimiterProvider = Spy(TimeLimiterProvider)
    SessionManager objectUnderTest = new SessionManager(timeLimiterProvider)

    @Shared
    def sessionId
    def sessionId2
    def anchorId = 3001
    def someTimeoutInMilliseconds = 100L

    def setup(){
        sessionId = objectUnderTest.startSession()
        sessionId2 = objectUnderTest.startSession()
    }

    def cleanup(){
        objectUnderTest.closeSession(sessionId)
        objectUnderTest.closeSession(sessionId2)
    }

    def 'start session'() {
        when: 'start session'
            def result = objectUnderTest.startSession()
        then: 'session ID is returned'
            assert result instanceof String
    }

    def 'close session'(){
        given: 'session Id from calling the start session method'
            def sessionIdToClose = objectUnderTest.startSession()
        when: 'close session method is called'
            objectUnderTest.closeSession(sessionIdToClose)
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
        when: 'session tries to acquire anchor lock by passing anchor entity details'
            objectUnderTest.lockAnchor(sessionId, anchorId, someTimeoutInMilliseconds)
        then: 'no exception is thrown'
            noExceptionThrown()
    }

    def 'lock anchor entity with Interrupted exception'(){
        given: 'timeLimiter throws an interrupted exception'
            def mockTimeLimiter = Mock(TimeLimiter)
            timeLimiterProvider.getTimeLimiter(_) >> mockTimeLimiter
            mockTimeLimiter.callWithTimeout(*_) >> { throw new InterruptedException() }
        when: 'session tries to acquire anchor lock'
            objectUnderTest.lockAnchor('',0,0L)
        then: 'a session manager exception is thrown specifying operation was interrupted'
            def thrown = thrown(SessionManagerException)
            thrown.details.contains('interrupted')
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'attempt to lock anchor entity with invalid sessionId'(){
        given: 'invalid session ID'
            def invalidSessionId = 'invalid-sessionId'
        when: 'session tries to acquire anchor lock'
            objectUnderTest.lockAnchor(invalidSessionId,anchorId,someTimeoutInMilliseconds)
        then: 'a session manager exception is thrown specifying operation was aborted'
            def thrown = thrown(SessionManagerException)
            thrown.details.contains('aborted')
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'attempt to lock anchor entity when another is holding the lock'(){
        given: 'a session that holds an anchor lock'
            objectUnderTest.lockAnchor(sessionId,anchorId,someTimeoutInMilliseconds)
        when: 'another session tries to acquire the same anchor lock'
            objectUnderTest.lockAnchor(sessionId2,anchorId,someTimeoutInMilliseconds)
        then: 'a session manager exception is thrown specifying operation reached timeout'
            def thrown = thrown(SessionManagerException)
            thrown.message.contains('Timeout')
    }

    @Sql([CLEAR_DATA, SET_DATA])
    def 'release locks held by session'(){
        given: 'anchorEntity that is locked'
            objectUnderTest.lockAnchor(sessionId,anchorId,someTimeoutInMilliseconds)
        when: 'session tries to release lock(s) held by session'
            objectUnderTest.releaseLocks(sessionId)
        then: 'no exception is thrown'
            noExceptionThrown()
    }

}
