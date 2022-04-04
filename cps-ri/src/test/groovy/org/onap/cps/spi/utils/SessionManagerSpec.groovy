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
import org.onap.cps.spi.entities.AnchorEntity
import org.onap.cps.spi.exceptions.SessionManagerException
import org.onap.cps.spi.repository.AnchorRepository
import org.onap.cps.spi.repository.DataspaceRepository
import org.testcontainers.shaded.com.google.common.util.concurrent.UncheckedExecutionException
import spock.lang.Specification
import org.hibernate.Session

import java.util.concurrent.ExecutionException

class SessionManagerSpec extends Specification {

    def spiedTimeLimiterProvider = Spy(TimeLimiterProvider)
    def mockDataspaceRepository = Mock(DataspaceRepository)
    def mockAnchorRepository = Mock(AnchorRepository)
    def mockSession = Mock(Session)

    def objectUnderTest = new SessionManager(spiedTimeLimiterProvider, mockDataspaceRepository, mockAnchorRepository)

    def 'Lock anchor entity with #exceptionDuringTest exception.'(){
        given: 'a dummy session'
            objectUnderTest.sessionMap.put('dummySession', mockSession)
        and: 'the anchor name can be resolved'
            def mockAnchorEntity = Mock(AnchorEntity)
            mockAnchorEntity.getId() > 456
            mockAnchorRepository.getByDataspaceAndName(_, _) >> mockAnchorEntity
        and: 'timeLimiter throws an #exceptionDuringTest exception'
            def mockTimeLimiter = Mock(TimeLimiter)
            spiedTimeLimiterProvider.getTimeLimiter(_) >> mockTimeLimiter
            mockTimeLimiter.callWithTimeout(*_) >> { throw exceptionDuringTest }
        when: 'session tries to acquire anchor lock'
            objectUnderTest.lockAnchor('dummySession', 'some-dataspace','some-anchor', 123L)
        then: 'a session manager exception is thrown with teh expected detail'
            def thrown = thrown(SessionManagerException)
            thrown.details.contains(expectedDetail)
        where:
            exceptionDuringTest               || expectedDetail
            new InterruptedException()        || 'interrupted'
            new ExecutionException()          || 'aborted'
    }

    def 'Close session that does not exist.'() {
        when: 'attempt to close session that does not exists'
            objectUnderTest.closeSession('unknown session id')
        then: 'a session manager exception is thrown with the unknown id in the details'
            def thrown = thrown(SessionManagerException)
            assert thrown.details.contains('unknown session id')
    }

    def 'Hibernate exception while closing session.'() {
        given: 'a test session with a transaction'
            objectUnderTest.sessionMap.put('testSessionId', mockSession)
            mockSession.getTransaction() >> Mock(Transaction)
        and: 'an hibernate exception when closing that session'
            def hibernateException = new HibernateException('test')
            mockSession.close() >> { throw hibernateException }
        when: 'attempt to close session'
            objectUnderTest.closeSession('testSessionId')
        then: 'a session manager exception is thrown with the session id in the details'
            def thrown = thrown(SessionManagerException)
            assert thrown.details.contains('testSessionId')
        and: 'the original exception as cause'
            assert thrown.cause == hibernateException
    }

    def 'Attempt to lock anchor entity with session Id that does not exists'(){
        when: 'attempt to acquire anchor lock with session that does not exists'
            objectUnderTest.lockAnchor('unknown session id','','',123L)
        then: 'a session manager exception is thrown with the unknown id in teh details'
            def thrown = thrown(SessionManagerException)
            thrown.details.contains('unknown session id')
    }

}
