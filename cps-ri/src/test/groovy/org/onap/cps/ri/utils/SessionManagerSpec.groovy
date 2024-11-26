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

package org.onap.cps.ri.utils

import com.google.common.util.concurrent.TimeLimiter
import com.google.common.util.concurrent.UncheckedExecutionException
import org.hibernate.HibernateException
import org.hibernate.Session
import org.hibernate.Transaction
import org.onap.cps.ri.models.AnchorEntity
import org.onap.cps.ri.repository.AnchorRepository
import org.onap.cps.ri.repository.DataspaceRepository
import org.onap.cps.api.exceptions.SessionManagerException
import spock.lang.Specification

class SessionManagerSpec extends Specification {

    def mockCpsSessionFactory = Mock(CpsSessionFactory)
    def spiedTimeLimiterProvider = Spy(TimeLimiterProvider)
    def mockDataspaceRepository = Mock(DataspaceRepository)
    def mockAnchorRepository = Mock(AnchorRepository)
    def mockSession1 = Mock(Session)
    def mockSession2 = Mock(Session)
    def mockTransaction1 = Mock(Transaction)
    def mockTransaction2 = Mock(Transaction)

    def objectUnderTest = new SessionManager(mockCpsSessionFactory, spiedTimeLimiterProvider, mockDataspaceRepository, mockAnchorRepository)

    def setup(){
        mockSession1.getTransaction() >> mockTransaction1
        mockSession2.getTransaction() >> mockTransaction2
    }

    def 'Lock anchor entity with #exceptionDuringTest exception.'() {
        given: 'a dummy session'
            objectUnderTest.sessionMap.put('dummy-session', mockSession1)
        and: 'the anchor name can be resolved'
            def mockAnchorEntity = Mock(AnchorEntity)
            mockAnchorEntity.getId() > 456
            mockAnchorRepository.getByDataspaceAndName(_, _) >> mockAnchorEntity
        and: 'timeLimiter throws an #exceptionDuringTest exception'
            def mockTimeLimiter = Mock(TimeLimiter)
            spiedTimeLimiterProvider.getTimeLimiter(_) >> mockTimeLimiter
            mockTimeLimiter.callWithTimeout(*_) >> { throw exceptionDuringTest }
        when: 'session tries to acquire anchor lock'
            objectUnderTest.lockAnchor('dummy-session', 'some-dataspace', 'some-anchor', 123L)
        then: 'a session manager exception is thrown with the expected detail'
            def thrown = thrown(SessionManagerException)
            thrown.details.contains(expectedExceptionDetail)
        where:
            exceptionDuringTest               || expectedExceptionDetail
            new InterruptedException()        || 'interrupted'
            new UncheckedExecutionException() || 'aborted'
    }

    def 'Close a session' () {
        given: 'a session in the session map'
            objectUnderTest.sessionMap.putAll([testSessionId1:mockSession1])
        when: 'the session manager closes session'
            objectUnderTest.closeSession('testSessionId1', commit)
        then: 'commit or rollback is called on the transaction as appropriate'
            if (commit) {
                1 * mockTransaction1.commit()
            } else {
                1 * mockTransaction1.rollback()
            }
        and: 'the correct session is closed'
            1 * mockSession1.close()
        where:
            commit << [SessionManager.WITH_COMMIT, SessionManager.WITH_ROLLBACK]
    }

    def 'Close session that does not exist.'() {
        when: 'attempt to close session that does not exist'
            objectUnderTest.closeSession('unknown session id', SessionManager.WITH_COMMIT)
        then: 'a session manager exception is thrown with the unknown id in the details'
            def thrown = thrown(SessionManagerException)
            assert thrown.details.contains('unknown session id')
    }

    def 'Hibernate exception while closing session.'() {
        given: 'a test session in session map'
            objectUnderTest.sessionMap.put('testSessionId', mockSession1)
        and: 'an hibernate exception when closing that session'
            def hibernateException = new HibernateException('test')
            mockSession1.close() >> { throw hibernateException }
        when: 'attempt to close session'
            objectUnderTest.closeSession('testSessionId', SessionManager.WITH_COMMIT)
        then: 'a session manager exception is thrown with the session id in the details'
            def thrown = thrown(SessionManagerException)
            assert thrown.details.contains('testSessionId')
        and: 'the original exception as cause'
            assert thrown.cause == hibernateException
    }

    def 'Attempt to lock anchor entity with session Id that does not exist'() {
        when: 'attempt to acquire anchor lock with session that does not exist'
            objectUnderTest.lockAnchor('unknown session id', '', '', 123L)
        then: 'a session manager exception is thrown with the unknown id in the details'
            def thrown = thrown(SessionManagerException)
            thrown.details.contains('unknown session id')
    }

    def 'Close all sessions in shutdown.'() {
        given: 'sessions that holds transactions in the session map'
            objectUnderTest.sessionMap.putAll([testSessionId1:mockSession1, otherSessionId:mockSession2])
        when: 'shutdown method to close all sessions is called'
            objectUnderTest.closeAllSessionsInShutdown()
        then: 'commit is called on each transaction'
            1 * mockTransaction1.rollback()
            1 * mockTransaction2.rollback()
        and: 'each session is closed'
            1 * mockSession1.close()
            1 * mockSession2.close()
        then: 'session factory is closed'
            1 * mockCpsSessionFactory.closeSessionFactory()
    }

}
