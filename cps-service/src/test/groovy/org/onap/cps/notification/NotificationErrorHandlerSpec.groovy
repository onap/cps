/*
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2022-2023 Nordix Foundation
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

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.slf4j.LoggerFactory

import spock.lang.Specification

class NotificationErrorHandlerSpec extends Specification{

    NotificationErrorHandler objectUnderTest = new NotificationErrorHandler()
    def logWatcher = Spy(ListAppender<ILoggingEvent>)

    @BeforeEach
    void setup() {
        ((Logger) LoggerFactory.getLogger(NotificationErrorHandler.class)).addAppender(logWatcher);
        logWatcher.start();
    }

    @AfterEach
    void teardown() {
        ((Logger) LoggerFactory.getLogger(NotificationErrorHandler.class)).detachAndStopAllAppenders();
    }

    def 'Logging exception via notification error handler #scenario'() {
        when: 'exception #scenario occurs'
            objectUnderTest.onException(exception, 'some context')
        then: 'log output results contains the correct error details'
            def logMessage = logWatcher.list[0].getFormattedMessage()
            assert logMessage.contains('Failed to process')
            assert logMessage.contains("Error cause: ${exptectedCauseString}")
            assert logMessage.contains("Error context: [some context]")
        where:
            scenario        | exception                                               || exptectedCauseString
            'with cause'    | new Exception('message')                                || 'message'
            'without cause' | new Exception('message', new RuntimeException('cause')) || 'java.lang.RuntimeException: cause'
    }
}
