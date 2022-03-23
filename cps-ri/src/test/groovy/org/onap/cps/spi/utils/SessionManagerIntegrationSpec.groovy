/*
 *  ============LICENSE_START=======================================================
 *  Copyright (C) 2021-2022 Nordix Foundation
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
import org.onap.cps.spi.impl.CpsPersistenceSpecBase

class SessionManagerIntegrationSpec extends CpsPersistenceSpecBase{

    def objectUnderTest = new SessionManager();

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
}
