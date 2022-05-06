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

import com.google.common.util.concurrent.TimeLimiter
import org.hibernate.HibernateException
import org.hibernate.Transaction
import org.onap.cps.spi.config.CpsSessionFactory
import org.onap.cps.spi.entities.AnchorEntity
import org.onap.cps.spi.exceptions.SessionManagerException
import org.onap.cps.spi.repository.AnchorRepository
import org.onap.cps.spi.repository.DataspaceRepository
import spock.lang.Specification
import org.hibernate.Session
import java.util.concurrent.ExecutionException

class SessionManagerSpec extends Specification {

    def mockCpsSessionFactory = Mock(CpsSessionFactory)
    def spiedTimeLimiterProvider = Spy(TimeLimiterProvider)
    def mockDataspaceRepository = Mock(DataspaceRepository)
    def mockAnchorRepository = Mock(AnchorRepository)
    def mockSession1 = Mock(Session)
    def mockSession2 = Mock(Session)

    def objectUnderTest = new SessionManager(mockCpsSessionFactory, spiedTimeLimiterProvider, mockDataspaceRepository, mockAnchorRepository)

    def 'Lock anchor entity with #exceptionDuringTest exception.'() {
        given: 'a dummy session'
            objectUnderTest.sessionMap.put('dummySession', mockSession1)
        and: 'the anchor name can be resolved'
            def mockAnchorEntity = Mock(AnchorEntity)
            mockAnchorEntity.getId() > 456
            mockAnchorRepository.getByDataspaceAndName(_, _) >> mockAnchorEntity
        and: 'timeLimiter throws an #exceptionDuringTest exception'
            def mockTimeLimiter = Mock(TimeLimiter)
            spiedTimeLimiterProvider.getTimeLimiter(_) >> mockTimeLimiter
            mockTimeLimiter.callWithTimeout(*_) >> { throw exceptionDuringTest }
        when: 'session tries to acquire anchor lock'
            objectUnderTest.lockAnchor('dummySession', 'some-dataspace', 'some-anchor', 123L)
            then: 'a session manager exception is thrown with the expected detail'
            def thrown = thrown(SessionManagerException)
            thrown.details.contains(expectedExceptionDetail)
        where:
            exceptionDuringTest        || expectedExceptionDetail
            new InterruptedException() || 'interrupted'
            new ExecutionException()   || 'aborted'
    }

    def 'Close a session' () {
        given: 'a session with transaction in the session map'
            objectUnderTest.sessionMap.putAll([testSessionId1:mockSession1])
            def mockTransaction1 = Mock(Transaction)
            mockSession1.getTransaction() >> mockTransaction1
        when: 'close session method is called'
            objectUnderTest.closeSession('testSessionId1')
        then: 'commit is called on the transaction'
            1 * mockTransaction1.commit()
        and: 'session is closed'
            1 * mockSession1.close()
    }

    def 'Close session that does not exist.'() {
        when: 'attempt to close session that does not exist'
        objectUnderTest.closeSession('unknown session id')
        then: 'a session manager exception is thrown with the unknown id in the details'
        def thrown = thrown(SessionManagerException)
        assert thrown.details.contains('unknown session id')
    }

    def 'Hibernate exception while closing session.'() {
        given: 'a test session with a transaction'
        objectUnderTest.sessionMap.put('testSessionId', mockSession1)
        mockSession1.getTransaction() >> Mock(Transaction)
        and: 'an hibernate exception when closing that session'
        def hibernateException = new HibernateException('test')
        mockSession1.close() >> { throw hibernateException }
        when: 'attempt to close session'
        objectUnderTest.closeSession('testSessionId')
        then: 'a session manager exception is thrown with the session id in the details'
        def thrown = thrown(SessionManagerException)
        assert thrown.details.contains('testSessionId')
        and: 'the original exception as cause'
        assert thrown.cause == hibernateException
    }

    def 'Attempt to lock anchor entity with session Id that does not exists'() {
        when: 'attempt to acquire anchor lock with session that does not exists'
        objectUnderTest.lockAnchor('unknown session id', '', '', 123L)
        then: 'a session manager exception is thrown with the unknown id in the details'
        def thrown = thrown(SessionManagerException)
        thrown.details.contains('unknown session id')
    }

    def 'Close all sessions in shutdown.'() {
        given: 'sessions that holds transactions in the session map'
            objectUnderTest.sessionMap.putAll([testSessionId1:mockSession1, otherSessionId:mockSession2])
            def mockTransaction1 = Mock(Transaction)
            mockSession1.getTransaction() >> mockTransaction1
            def mockTransaction2 = Mock(Transaction)
            mockSession2.getTransaction() >> mockTransaction2
        when: 'shutdown method to close all sessions is called'
            objectUnderTest.closeAllSessionsInShutdown()
        then: 'commit is called on each transaction'
            1 * mockTransaction1.rollback()
            1 * mockTransaction2.rollback()
        and: 'each session is closed'
            1 * mockSession1.close()
            1 * mockSession2.close()
    }

}
