/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022 Nordix Foundation. All rights reserved.
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

    def 'Logging exception via notification error handler'(){
        given:
            def buffer = new ByteArrayOutputStream()
            System.out = new PrintStream(buffer)
        when: 'onException method is called with relevant exception and context'
            objectUnderTest.onException(new Exception('sample exception'), 'some context')
        then: 'log results contains the given parameter values'
            println(buffer)
            buffer.toString().contains('Failed to process')
            buffer.toString().contains('Error cause: sample exception')
            buffer.toString().contains('Error context: [some context]')
    }
}

