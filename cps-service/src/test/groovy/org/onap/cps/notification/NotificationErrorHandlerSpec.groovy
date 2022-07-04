/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation
 *  ================================================================================
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  SPDX-License-Identifier: Apache-2.0
 *  ============LICENSE_END=========================================================
 */

package org.onap.cps.notification

import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Specification

class NotificationErrorHandlerSpec extends Specification{

    @Autowired
    NotificationErrorHandler objectUnderTest = new NotificationErrorHandler()

    def 'Logging exception via notification error handler'() {
        given: 'redirect system.out to a readable stream'
            def systemOutAsStream = new ByteArrayOutputStream()
            System.out = new PrintStream(systemOutAsStream)
        when: 'some exception occurs'
            objectUnderTest.onException(new Exception('sample exception'), 'some context')
        then: 'log output results contains the correct error details'
            def systemOutAsString = systemOutAsStream.toString()
            systemOutAsString.contains('Failed to process')
            systemOutAsString.contains('Error cause: sample exception')
            systemOutAsString.contains('Error context: [some context]')
    }
}

